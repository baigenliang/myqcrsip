package com.rsvmcs.qcrsip.core.io;

import com.rsvmcs.qcrsip.core.model.SIPMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Map;

public class UDPMessageChannel {
    private final DatagramChannel channel;

    private final InetSocketAddress localAddr;

    public UDPMessageChannel(InetSocketAddress local) throws IOException {
//      this.channel = DatagramChannel.open();
//      this.channel.configureBlocking(false);
//      this.channel.bind(local); // 仅绑定一次
        this.channel = DatagramChannel.open();
        this.channel.configureBlocking(false);
        this.channel.bind(local);
        this.localAddr = (InetSocketAddress) channel.getLocalAddress();
    }

    public void send(InetSocketAddress dst, byte[] data) throws IOException {
//        ByteBuffer buf = ByteBuffer.wrap(data);
//        int zeroSends = 0;
//        while (buf.hasRemaining()) {
//            int n = channel.send(buf, dst);
//            if (n == 0) {
//                zeroSends++;
//                if (zeroSends > 2000) throw new IOException("UDP send would block too long");
//                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
//            } else {
//                zeroSends = 0;
//            }
//        }
        ByteBuffer buf = ByteBuffer.wrap(data);
        channel.send(buf, dst);
    }


    public DatagramChannel getChannel(){ return channel; }

    // 在 decode SIP 报文时调用这个，保证每个 SIPMessage 知道 localIP
    public void attachLocalAddress(SIPMessage sipMessage) {
        if (sipMessage != null) {
            sipMessage.setLocalAddress(localAddr);
        }
    }

//    //private final DatagramSocket socket;
//    private  DatagramChannel channel;
//
//    public UDPMessageChannel(DatagramSocket socket) throws IOException {
//        //this.socket = socket;
//        this.channel = DatagramChannel.open();
//        this.channel.configureBlocking(false);//非阻塞
//        this.channel.bind(new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort()));
//    }
//    public void send(InetSocketAddress dst, byte[] data) throws Exception {
//
//        //阻塞套接字的包装改为非阻塞模式发送
//        ByteBuffer buf = ByteBuffer.wrap(data);
//        channel.send(buf,dst); //直接使用NIO channel
//    }
//    //暂时未用到，发送都是在本函数内部发送的
//    public DatagramChannel getChannel() {
//        return channel;
//    }

}
