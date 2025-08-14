package com.rsvmcs.qcrsip.core.stack;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class UDPMessageChannel {
    private final DatagramSocket socket;
    public UDPMessageChannel(String bindIp, int bindPort){
        try {
            this.socket = new DatagramSocket(new InetSocketAddress(bindIp, bindPort));
        } catch (Exception e){ throw new RuntimeException(e); }
    }
    public UDPMessageChannel(DatagramSocket shared){ this.socket = shared; } // 用接收器的 socket 回发

    public void send(InetSocketAddress peer, byte[] data) throws Exception {
        DatagramPacket p = new DatagramPacket(data, data.length, peer.getAddress(), peer.getPort());
        socket.send(p);
    }

    public void close(){ if (socket != null && !socket.isClosed()) socket.close(); }
}
