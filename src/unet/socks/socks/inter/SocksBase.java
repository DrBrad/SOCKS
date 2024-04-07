package unet.socks.socks.inter;

import unet.socks.SocksProxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public abstract class SocksBase {

    protected SocksProxy proxy;
    protected AType atype;
    protected byte[] address;
    protected int port;

    public SocksBase(SocksProxy proxy){
        this.proxy = proxy;
    }

    public abstract Command getCommand()throws IOException;

    public abstract void connect()throws IOException;

    public abstract void bind()throws IOException;

    //private boolean complete;
}
