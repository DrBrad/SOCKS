package unet.socks;

import unet.socks.socks.ProxyHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocksServer {

    private ServerSocket server;

    public void start(int port)throws IOException {
        if(isRunning()){
            throw new IllegalArgumentException("Server has already started.");
        }

        server = new ServerSocket(port);

        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    Socket socket;
                    while((socket = server.accept()) != null){
                        new Thread(new ProxyHandler(socket)).start();
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stop()throws IOException {
        if(!isRunning()){
            throw new IllegalArgumentException("Server is not currently running.");
        }

        server.close();
    }

    public boolean isRunning(){
        return (server != null && !server.isClosed());
    }

    public int getPort(){
        return server.getLocalPort();
    }
}
