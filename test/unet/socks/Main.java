package unet.socks;

import java.io.IOException;

public class Main {

    public static void main(String[] args)throws IOException {
        SocksProxyServer server = new SocksProxyServer();
        server.start(8080);
    }
}
