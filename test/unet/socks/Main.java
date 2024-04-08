package unet.socks;

import java.io.IOException;

public class Main {

    public static void main(String[] args)throws IOException {
        SocksServer server = new SocksServer();
        server.start(1080);
    }
}
