package unet.socks;

import unet.socks.socks.inter.AType;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class UDPTest {

    public static void main(String[] args)throws IOException {
        // UDP server address and port



        //Socket socket = new Socket();
        //socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), 1080));

        //String remoteHost = "localhost";
        //int remotePort = 1000;
        InetAddress address = InetAddress.getByName("1.1.1.1");
        int port = 53;

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), 1080));
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        // Send SOCKS5 handshake
        out.write(new byte[]{5, 1, 0});

        // Receive SOCKS5 response
        byte[] response = new byte[2];
        in.read(response);
        if (response[0] != 5 || response[1] != 0) {
            throw new IOException("SOCKS5 handshake failed");
        }


        out.write(5); // SOCKS5 version
        out.write(3); // UDP associate command
        out.write(0); // Reserved byte
        out.write(1); // IPv4 address type
        out.write(address.getAddress()); // Destination address
        out.write((port & 0xff00) >> 8); // Destination port
        out.write(port & 0x00ff);

        in.read(); //SOCKS VERSION
        if (in.read() != 0) {
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
                return;
        }

        System.out.println("Local endpoint for UDP communication: " + localEndpoint);




        DatagramSocket dgsocket = new DatagramSocket();
        // UDP client socket
        // Send a UDP packet to the SOCKS5 server
        //byte[] requestData = new byte[] { 0x05, 0x01, 0x00 };
        byte[] buf = constructQueryPacket((short) new Random().nextInt(65535), "google.com");

        DatagramPacket requestPacket = new DatagramPacket(buf, buf.length, localEndpoint.getAddress(), localEndpoint.getPort());
        dgsocket.send(requestPacket);
        System.out.println("Request sent to the SOCKS5 server.");

        // Receive the response from the SOCKS5 server
        byte[] responseData = new byte[65535];
        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
        dgsocket.receive(responsePacket);
        System.out.println("Response received from the SOCKS5 server:");

        // Display the response data
        int responseLength = responsePacket.getLength();
        for (int i = 0; i < responseLength; i++) {
            System.out.print(String.format("%02X ", responseData[i]));
        }
    }

    //DNS UDP TEST...
    private static byte[] constructQueryPacket(short transactionId, String domainName) {
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
}
