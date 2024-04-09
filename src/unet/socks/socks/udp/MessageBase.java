package unet.socks.socks.udp;

import unet.socks.socks.inter.AType;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class MessageBase {


    //READ DPG HEADERS
    /*
    0x00 - RESERVED
    0x00 - RESERVED
    0x00 - FRAG
    ATYPE 0x01 ??
    IP_ADDRESS
    PORT
    */

    private byte[] data;
    private InetSocketAddress address;

    public MessageBase(){
    }

    public MessageBase(InetSocketAddress address, byte[] data){
        this.address = address;
        this.data = data;
    }

    public MessageBase(InetAddress address, int port, byte[] data){
        this.address = new InetSocketAddress(address, port);
        this.data = data;
    }

    public byte[] encode(){
        byte[] buf = new byte[data.length+address.getAddress().getAddress().length+6];
        buf[0] = 0x00; //RESERVE
        buf[1] = 0x00; //RESERVE
        buf[2] = 0x00; //FRAG
        buf[3] = (address.getAddress() instanceof Inet4Address) ? AType.IPv4.getCode() : AType.IPv6.getCode();

        byte[] addr = address.getAddress().getAddress();
        System.arraycopy(addr, 0, buf, 4, addr.length);
        int offset = address.getAddress().getAddress().length+4;

        buf[offset] = (byte) ((address.getPort() & 0xff00) >> 8);
        buf[offset+1] = (byte) (address.getPort() & 0x00ff);

        System.arraycopy(data, 0, buf, offset+2, data.length);

        return buf;
    }

    public void decode(byte[] buf, int off, int len)throws UnknownHostException {
        off += 3;

        AType atype = AType.getATypeFromCode(buf[off]);
        byte[] addr;

        switch(atype){
            case IPv4:
            case IPv6:
                addr = new byte[atype.getLength()];
                System.arraycopy(buf, off+1, addr, 0, addr.length);
                off += addr.length+1;
                address = new InetSocketAddress(InetAddress.getByAddress(addr),
                        ((buf[off] & 0xff) << 8) | (buf[off+1] & 0xff));
                off += 2;
                break;

            case DOMAIN:
                addr = new byte[buf[off+1]];
                System.arraycopy(buf, off+2, addr, 0, addr.length);
                off += addr.length+2;
                address = new InetSocketAddress(InetAddress.getByAddress(addr),
                        ((buf[off] & 0xff) << 8) | (buf[off+1] & 0xff));
                off += 2;
                break;

            default:
                throw new IllegalArgumentException("Failed to parse packet.");
        }

        data = new byte[len-off];
        System.arraycopy(buf, off, data, 0, data.length);
    }

    public void setAddress(InetSocketAddress address){
        this.address = address;
    }

    public void setAddress(InetAddress address, int port){
        this.address = new InetSocketAddress(address, port);
    }

    public InetSocketAddress getAddress(){
        return address;
    }

    public InetAddress getHostAddress(){
        return address.getAddress();
    }

    public int getPort(){
        return address.getPort();
    }

    public void setData(byte[] data){
        this.data = data;
    }

    public byte[] getData(){
        return data;
    }
}
