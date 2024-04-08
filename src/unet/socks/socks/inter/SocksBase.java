package unet.socks.socks.inter;

import unet.socks.socks.ProxyHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public abstract class SocksBase {

    protected ProxyHandler proxy;
    //protected AType atype;
    protected InetSocketAddress address;
    //protected byte[] address;
    //protected int port;

    public SocksBase(ProxyHandler proxy){
        this.proxy = proxy;
    }

    public abstract Command getCommand()throws IOException;

    public abstract void connect()throws IOException;

    public abstract void bind()throws IOException;

    //private boolean complete;
}
