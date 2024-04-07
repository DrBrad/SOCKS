package unet.socks.socks;

import unet.socks.SocksProxy;
import unet.socks.socks.inter.Command;
import unet.socks.socks.inter.SocksBase;

import java.io.IOException;

public class Socks4 extends SocksBase {

    public Socks4(SocksProxy proxy){
        super(proxy);
    }

    @Override
    public Command getCommand()throws IOException {
        return Command.INVALID;
    }

    @Override
    public void connect()throws IOException {

    }

    @Override
    public void bind()throws IOException {

    }
}
