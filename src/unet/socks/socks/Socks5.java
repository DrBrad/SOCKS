package unet.socks.socks;

import unet.socks.SocksProxy;
import unet.socks.socks.inter.AType;
import unet.socks.socks.inter.Command;
import unet.socks.socks.inter.ReplyCode;
import unet.socks.socks.inter.SocksBase;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Socks5 extends SocksBase {

    public static final byte SOCKS_VERSION = 0x05;

    public Socks5(SocksProxy proxy){
        super(proxy);
    }

    @Override
    public Command getCommand()throws IOException {
        if(!authenticate()){
            replyCommand(ReplyCode.GENERAL_FAILURE);
            throw new IOException("Failed to authenticate.");
        }

        proxy.getOutputStream().write(new byte[]{ SOCKS_VERSION, 0x00 });

        if(proxy.getInputStream().read() != SOCKS_VERSION){
            replyCommand(ReplyCode.UNASSIGNED);
            throw new IOException("Invalid Socks version");
        }
        Command command = Command.getCommandFromCode((byte) proxy.getInputStream().read());

        if(command.equals(Command.INVALID)){
            replyCommand(ReplyCode.COMMAND_NOT_SUPPORTED);
        }

        //RSV
        proxy.getInputStream().read();

        AType atype = AType.getATypeFromCode((byte) proxy.getInputStream().read());
        byte[] addr;

        switch(atype){
            case IPv4:
                addr = new byte[atype.getLength()];
                proxy.getInputStream().read(addr);
                address = new InetSocketAddress(InetAddress.getByAddress(addr),
                        ((proxy.getInputStream().read() & 0xff) << 8) | (proxy.getInputStream().read() & 0xff));
                break;

            case DOMAIN:
                addr = new byte[proxy.getInputStream().read()];
                proxy.getInputStream().read(addr);
                address = new InetSocketAddress(InetAddress.getByName(new String(addr)),
                        ((proxy.getInputStream().read() & 0xff) << 8) | (proxy.getInputStream().read() & 0xff));
                break;

            case IPv6:
                addr = new byte[atype.getLength()];
                proxy.getInputStream().read(addr);
                address = new InetSocketAddress(InetAddress.getByAddress(addr),
                        ((proxy.getInputStream().read() & 0xff) << 8) | (proxy.getInputStream().read() & 0xff));
                break;

            default:
                replyCommand(ReplyCode.A_TYPE_NOT_SUPPORTED);
                throw new IOException("Invalid A-Type.");
        }

        System.out.println(address.getAddress().getHostAddress()+"  "+address.getPort());

        return command;
    }

    @Override
    public void connect()throws IOException {
        try{
            Socket socket = new Socket();
            socket.connect(address);

            replyCommand(ReplyCode.GRANTED, address);

            try{
                proxy.relay(socket);
            }catch(IOException e){
            }

        }catch(IOException e){
            replyCommand(ReplyCode.HOST_UNREACHABLE, address);
            throw new IOException("Unable to connect to server.");
        }
    }

    @Override
    public void bind()throws IOException {
        //NOT SURE HOW WE WANT TO HANDLE THIS ONE...
        /*
        try{
            ServerSocket server = new ServerSocket(0);
            replyCommand(ReplyCode.GRANTED, new InetSocketAddress(InetAddress.getLocalHost(), server.getLocalPort()));

            //DO WE LOOP THIS...?
            Socket socket ;
            while((socket = server.accept()) != null){
                replyCommand(ReplyCode.GRANTED, new InetSocketAddress(socket.getInetAddress(), socket.getPort()));
                relay(socket);
            }

            server.close();

        }catch(IOException e){
            replyCommand(ReplyCode.CONNECTION_NOT_ALLOWED);
        }
        */
    }

    public void udp()throws IOException {

    }

    private boolean authenticate()throws IOException {
        byte method = (byte) proxy.getInputStream().read();
        String methods = "";

        for(int i = 0; i < method; i++){
            methods += ",-"+proxy.getInputStream().read()+'-';
        }

        return (methods.indexOf("-0-") != -1 || methods.indexOf("-00-") != -1);
    }

    private void replyCommand(ReplyCode code)throws IOException {
        byte[] reply = new byte[10];

        reply[0] = SOCKS_VERSION;
        reply[1] = code.getCode();
        reply[2] = 0x00;
        reply[3] = AType.IPv4.getCode();

        proxy.getOutputStream().write(reply);
    }

    private void replyCommand(ReplyCode code, InetSocketAddress address)throws IOException {
        byte[] reply = new byte[6+address.getAddress().getAddress().length];

        reply[0] = SOCKS_VERSION;
        reply[1] = code.getCode();
        reply[2] = 0x00;
        reply[3] = (address.getAddress() instanceof Inet4Address) ? AType.IPv4.getCode() : AType.IPv6.getCode();
        System.arraycopy(address.getAddress().getAddress(), 0, reply, 4, reply.length-6);
        reply[reply.length-2] = (byte)((address.getPort() & 0xff00) >> 8);
        reply[reply.length-1] = (byte)(address.getPort() & 0x00ff);

        proxy.getOutputStream().write(reply);
    }
}
