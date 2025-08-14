package com.rsvmcs.qcrsip.core.stack;

import com.rsvmcs.qcrsip.core.SipProviderImpl;
import com.rsvmcs.qcrsip.entity.ChannelRef;
import com.rsvmcs.qcrsip.entity.SIPMessage;

import java.net.*;

public class UDPMessageProcessor  implements Runnable {
    private final SipProviderImpl provider;
    private DatagramSocket socket;
    private volatile boolean running;

    public UDPMessageProcessor(SipProviderImpl provider){ this.provider = provider; }

    public void start(String ip, int port) throws Exception {
        socket = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(ip), port));
        running = true;
        Thread t = new Thread(this, "UDP-Processor-" + ip + ":" + port);
        t.start();
        System.out.println("[UDP] listening " + ip + ":" + port);
    }

    public void stop(){
        running = false;
        if (socket != null) socket.close();
    }

    @Override public void run() {
        byte[] buf = new byte[64 * 1024];
        while (running){
            try{
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);
                byte[] data = new byte[p.getLength()];
                System.arraycopy(p.getData(), p.getOffset(), data, 0, p.getLength());
                SIPMessage m = SIPMessage.parse(data);
                if (m != null) {
                    UDPMessageChannel ch = new UDPMessageChannel(socket); // 共享 socket 发送回去
                    provider.onMessageArrived(m, ChannelRef.forUdp(ch, new InetSocketAddress(p.getAddress(), p.getPort())));
                }
            }catch (Exception e){
                if (running) e.printStackTrace();
            }
        }
    }
}