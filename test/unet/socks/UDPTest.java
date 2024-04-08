package unet.socks;

import unet.socks.socks.inter.AType;

import java.io.*;
import java.net.*;

public class UDPTest {

    public static void main(String[] args)throws IOException {
        // UDP server address and port



        //Socket socket = new Socket();
        //socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), 1080));

        //String remoteHost = "localhost";
        //int remotePort = 1000;

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

        // Send UDP associate request
        byte[] request = {5, 3, 0, 1, 0, 0, 0, 0, 0, 0};
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.write(request);
        dataOutputStream.writeUTF("1.1.1.1");
        dataOutputStream.writeShort(53);
        out.write(byteArrayOutputStream.toByteArray());

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
        byte[] requestData = new byte[] { 0x05, 0x01, 0x00 };
        DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, localEndpoint.getAddress(), localEndpoint.getPort());
        dgsocket.send(requestPacket);
        System.out.println("Request sent to the SOCKS5 server.");

        // Receive the response from the SOCKS5 server
        byte[] responseData = new byte[1024];
        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
        dgsocket.receive(responsePacket);
        System.out.println("Response received from the SOCKS5 server:");

        // Display the response data
        int responseLength = responsePacket.getLength();
        for (int i = 0; i < responseLength; i++) {
            System.out.print(String.format("%02X ", responseData[i]));
        }
    }
}
