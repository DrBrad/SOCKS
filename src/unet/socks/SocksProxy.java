package unet.socks;

import unet.socks.socks.Socks4;
import unet.socks.socks.Socks5;
import unet.socks.socks.inter.SocksBase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocksProxy implements Runnable {

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public SocksProxy(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run(){
        try{
            in = socket.getInputStream();
            out = socket.getOutputStream();

            SocksBase socks;

            //SOCKS VERSION
            switch(in.read()){
                case 0x04:
                    socks = new Socks4(this);
                    //System.out.println("SOCKS 4");
                    socket.close();
                    return;

                case 0x05:
                    socks = new Socks5(this);
                    //System.out.println("SOCKS 5");
                    break;

                default:
                    socket.close();
                    return;
            }

            //COMMAND
            switch(socks.getCommand()){
                case CONNECT:
                    socks.connect();
                    //System.out.println("CONNECT");
                    break;

                case BIND:
                    System.out.println("BIND");
                    socks.bind();
                    break;

                case UDP:
                    if(socks instanceof Socks5){
                        ((Socks5) socks).udp();
                    }
                    System.out.println("UDP");
                    break;
            }

            //socket.close();

        }catch(IOException e){
            e.printStackTrace();
            try{
                socket.close();
            }catch(IOException ex){

            }
        }
    }

    public void relay(Socket socket)throws IOException {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    transfer(in, socket.getOutputStream());
                    //while(!socket.isClosed() && !relay.isClosed()){
                    //    in.transferTo(relay.getOutputStream());
                    //}
                }catch(IOException e){
                    //e.printStackTrace();
                }

                /*
                if(complete){
                    try{
                        tunnel.close();
                        proxy.getSocket().close();
                    }catch(IOException e){
                    }
                    return;
                }
                complete = true;
                */
            }
        });//.start();
        thread.start();

        try{
            transfer(socket.getInputStream(), out);
        }catch(IOException e){
            //e.printStackTrace();
        }

        try{
            thread.join();
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        socket.getInputStream().close();
        socket.getOutputStream().close();
        socket.close();

        in.close();
        out.close();
        socket.close();

        //while(!relay.isClosed() && !socket.isClosed()){
        //    relay.getInputStream().transferTo(out);
        //}
        /*
        if(complete){
            tunnel.close();
            proxy.getSocket().close();
            return;
        }
        complete = true;*/
    }

    private void transfer(InputStream in, OutputStream out)throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while((len = in.read(buf)) != -1){
            out.write(buf, 0, len);
            out.flush();
        }

        //out.flush();
        //in.close();
        //out.close();
    }

    public Socket getSocket(){
        return socket;
    }

    public InputStream getInputStream(){
        return in;
    }

    public OutputStream getOutputStream(){
        return out;
    }
}
