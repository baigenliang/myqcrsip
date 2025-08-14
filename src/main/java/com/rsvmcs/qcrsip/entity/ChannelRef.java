package com.rsvmcs.qcrsip.entity;

import com.rsvmcs.qcrsip.core.stack.UDPMessageChannel;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ChannelRef {
    public enum Kind { TCP, UDP }

    private final Kind kind;
    private final SocketChannel tcp;
    private final UDPMessageChannel udp;
    private final InetSocketAddress udpPeer;

    private ChannelRef(Kind k, SocketChannel tcp, UDPMessageChannel udp, InetSocketAddress peer){
        this.kind = k; this.tcp = tcp; this.udp = udp; this.udpPeer = peer;
    }
    public static ChannelRef forTcp(SocketChannel ch){ return new ChannelRef(Kind.TCP, ch, null, null); }
    public static ChannelRef forUdp(UDPMessageChannel ch, InetSocketAddress peer){ return new ChannelRef(Kind.UDP, null, ch, peer); }

    public void writeAndFlush(byte[] data) throws Exception {
        if (kind == Kind.TCP) {
            ByteBuffer buf = ByteBuffer.wrap(data);
            while (buf.hasRemaining()) tcp.write(buf);
        } else {
            udp.send(udpPeer, data);
        }
    }
}
