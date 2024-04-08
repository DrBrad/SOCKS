package unet.socks.socks;

import unet.socks.socks.inter.AType;
import unet.socks.socks.inter.Command;
import unet.socks.socks.inter.ReplyCode;
import unet.socks.socks.inter.SocksBase;

import java.io.IOException;
import java.net.*;

public class Socks5 extends SocksBase {

    public static final byte SOCKS_VERSION = 0x05;

    public Socks5(ProxyHandler proxy){
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
            case IPv6:
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

            default:
                replyCommand(ReplyCode.A_TYPE_NOT_SUPPORTED);
                throw new IOException("Invalid A-Type.");
        }

        System.out.println(address.getAddress().getHostAddress()+"  "+address.getPort());

        return command;
    }

    @Override
    public void connect()throws IOException {
        Socket socket;
        try{
            socket = new Socket();
            socket.connect(address);

        }catch(IOException e){
            replyCommand(ReplyCode.HOST_UNREACHABLE, address);
            throw new IOException("Unable to connect to server.");
        }

        replyCommand(ReplyCode.GRANTED, address);
        proxy.relay(socket);
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
        try{
            DatagramSocket socket = new DatagramSocket();
            replyCommand(ReplyCode.GRANTED, new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort())); //REPLACE WITH EXTERNAL IP...

            System.out.println(socket.getLocalAddress()+"  "+socket.getLocalPort());

            DatagramPacket packet = new DatagramPacket(new byte[65535], 65535);
            socket.receive(packet);

            //READ DPG HEADERS
            /*
            0x00 - RESERVED
            0x00 - RESERVED
            0x00 - FRAG
            ATYPE 0x01 ??
            IP_ADDRESS
            PORT
            */

            byte[] data = packet.getData();
            int offset = 4;

            AType atype = AType.getATypeFromCode(data[offset]);
            byte[] addr;
            InetSocketAddress address;

            switch(atype){
                case IPv4:
                case IPv6:
                    addr = new byte[atype.getLength()];
                    System.arraycopy(data, offset, addr, 0, addr.length);
                    offset += addr.length;
                    address = new InetSocketAddress(InetAddress.getByAddress(addr),
                            ((data[offset] & 0xff) << 8) | (data[offset+1] & 0xff));
                    offset += 2;
                    break;

                case DOMAIN:
                    addr = new byte[data[offset+1]];
                    System.arraycopy(data, offset+1, addr, 0, addr.length);
                    offset += addr.length+1;
                    address = new InetSocketAddress(InetAddress.getByAddress(addr),
                            ((data[offset] & 0xff) << 8) | (data[offset+1] & 0xff));
                    offset += 2;
                    break;

                default:
                    return;
            }

            System.out.println(address.getAddress()+"  "+address.getPort());


            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            packet.setAddress(address.getAddress());
            packet.setPort(address.getPort());
            byte[] buf = new byte[packet.getLength()-offset];
            System.arraycopy(data, offset, buf, 0, buf.length);
            packet.setData(buf);
            socket.send(packet);

            socket.receive(packet);

            //ADD DPG HEADERS BACK...

            buf = packet.getData();
            address = new InetSocketAddress(packet.getAddress(), packet.getPort());
            addr = address.getAddress().getAddress();
            byte[] header = new byte[addr.length+6];
            header[0] = 0x00;
            header[1] = 0x00;
            header[2] = 0x00;
            header[3] = (packet.getAddress() instanceof Inet4Address) ? AType.IPv4.getCode() : AType.IPv6.getCode();

            System.arraycopy(addr, 0, header, 4, addr.length);
            header[header.length-2] = (byte) ((address.getPort() & 0xff00) >> 8);
            header[header.length-1] = (byte) (address.getPort() & 0x00ff);

            data = new byte[header.length+packet.getLength()];
            System.arraycopy(header, 0, data, 0, header.length);
            System.arraycopy(buf, 0, data, header.length, packet.getLength());

            System.out.println(header.length);

            packet.setAddress(clientAddress);
            packet.setPort(clientPort);
            packet.setData(data);
            socket.send(packet);




            /*

            //SEND TO SERVER
            packet.setAddress(address.getAddress());
            packet.setPort(address.getPort());
            socket.send(packet);

            //RECEIVE FROM SERVER...
            socket.receive(packet);

            packet.setAddress(clientAddress);
            packet.setPort(clientPort);
            socket.send(packet);

            System.out.println(packet.getLength()+"  DG PACKET RECEIVED...");
            */

        }catch(IOException e){
            e.printStackTrace();
            replyCommand(ReplyCode.CONNECTION_NOT_ALLOWED);
        }
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
        reply[reply.length-2] = (byte) ((address.getPort() & 0xff00) >> 8);
        reply[reply.length-1] = (byte) (address.getPort() & 0x00ff);

        proxy.getOutputStream().write(reply);
    }
}
