package unet.socks;

import unet.socks.socks.inter.AType;
import unet.socks.socks.inter.Command;
import unet.socks.socks.inter.ReplyCode;
import unet.socks.socks.udp.MessageBase;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class UDPTest {

    public static void main(String[] args)throws IOException {
        SocksServer server = new SocksServer();
        server.start(1080);

        startUDPServer();

        InetSocketAddress destination = new InetSocketAddress(InetAddress.getLoopbackAddress(), 5000);

        //GET PROXY ENDPOINT
        InetSocketAddress localEndpoint = createProxy(Command.UDP,
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 1080),
                destination); //DESTINATION IS IRRELEVANT

        System.out.println("Local endpoint for UDP communication: " + localEndpoint);

        //CONNECT TO ENDPOINT AND SEND MESSAGE
        DatagramSocket socket = new DatagramSocket();

        byte[] buf = new byte[20];
        new Random().nextBytes(buf);
        System.out.println("[UDP-CLIENT] - SENT: "+bytesToHex(buf, buf.length));

        //constructQueryPacket((short) new Random().nextInt(65535), "google.com")
        MessageBase message = new MessageBase(destination, buf);
        byte[] data = message.encode();
        DatagramPacket packet = new DatagramPacket(data, data.length, localEndpoint.getAddress(), localEndpoint.getPort());
        socket.send(packet);

        //RECEIVE MESSAGE
        socket.receive(packet);
        message = new MessageBase();
        message.decode(packet.getData(), packet.getOffset(), packet.getLength());

        //OUTPUT RESULT
        data = message.getData();
        System.out.println("[UDP-CLIENT] - RECEIVED: "+bytesToHex(data, data.length));
    }

    private static InetSocketAddress createProxy(Command command, InetSocketAddress proxy, InetSocketAddress destination)throws IOException {
        Socket socket = new Socket();
        socket.connect(proxy);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        // Send SOCKS5 handshake
        out.write(new byte[]{
                0x05,
                0x01,
                0x00
        });

        // Receive SOCKS5 response
        byte[] response = new byte[2];
        in.read(response);
        if(response[0] != 5 || response[1] != 0){
            throw new IOException("SOCKS5 handshake failed");
        }

        out.write(0x05); // SOCKS5 version
        out.write(command.getCode()); // UDP associate command
        out.write(0x00); // Reserved byte
        out.write(0x01); // IPv4 address type
        out.write(destination.getAddress().getAddress()); // Destination address
        out.write((destination.getPort() & 0xff00) >> 8); // Destination port
        out.write(destination.getPort() & 0x00ff);

        in.read(); //SOCKS VERSION
        if(in.read() != 0){
            throw new IOException("UDP associate request failed");
        }

        in.read(); //0x00

        byte[] addr;
        InetSocketAddress localEndpoint;

        switch(AType.getATypeFromCode((byte) in.read())){
            case IPv4:
                addr = new byte[4];
                in.read(addr);
                localEndpoint = new InetSocketAddress(InetAddress.getByAddress(addr), ((in.read() & 0xFF) << 8) | (in.read() & 0xFF));

                break;

            case IPv6:
                addr = new byte[16];
                in.read(addr);
                localEndpoint = new InetSocketAddress(InetAddress.getByAddress(addr), ((in.read() & 0xFF) << 8) | (in.read() & 0xFF));

                break;

            case DOMAIN:
                addr = new byte[in.read()];
                in.read(addr);
                localEndpoint = new InetSocketAddress(InetAddress.getByName(new String(addr)), ((in.read() & 0xFF) << 8) | (in.read() & 0xFF));
                break;

            default:
                return null;
        }

        return localEndpoint;
    }

    //DNS UDP TEST...
    private static byte[] constructQueryPacket(short transactionId, String domainName){
        ByteBuffer buffer = ByteBuffer.allocate(512); // Maximum DNS message size
        buffer.putShort(transactionId); // Transaction ID

        // Flags (Standard query with Recursion Desired)
        buffer.putShort((short) 0x0100);

        // Question Count (1)
        buffer.putShort((short) 0x0001);

        // Answer Count (0), Authority Record Count (0), Additional Record Count (0)
        buffer.putShort((short) 0x0000);
        buffer.putShort((short) 0x0000);
        buffer.putShort((short) 0x0000);

        // Query Name
        String[] domainParts = domainName.split("\\.");
        for (String part : domainParts) {
            byte[] partBytes = part.getBytes();
            buffer.put((byte) partBytes.length);
            buffer.put(partBytes);
        }
        buffer.put((byte) 0x00); // End of domain name

        // Query Type (A record)
        buffer.putShort((short) 0x0001);

        // Query Class (IN)
        buffer.putShort((short) 0x0001);

        // Convert ByteBuffer to byte array
        byte[] queryBytes = new byte[buffer.position()];
        buffer.flip();
        buffer.get(queryBytes);
        return queryBytes;
    }

    private static void startUDPServer(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    DatagramSocket socket = new DatagramSocket(5000);

                    DatagramPacket packet = new DatagramPacket(new byte[65535], 65535);
                    socket.receive(packet);

                    byte[] data = packet.getData();
                    System.out.println("[UDP-SERVER] - RECEIVED: "+bytesToHex(data, packet.getLength()));

                    byte[] buf = new byte[20];
                    new Random().nextBytes(buf);
                    System.out.println("[UDP-SERVER] - SENT: "+bytesToHex(buf, buf.length));

                    packet.setData(buf);
                    socket.send(packet);

                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static String bytesToHex(byte[] buf, int len){
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < len; i++){
            builder.append(String.format("%02X ", buf[i]));
        }

        return builder.toString();
    }
}
