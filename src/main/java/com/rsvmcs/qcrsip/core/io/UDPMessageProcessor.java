package com.rsvmcs.qcrsip.core.io;

import com.rsvmcs.qcrsip.core.EventScanner;
import com.rsvmcs.qcrsip.core.ListeningPoint;
import com.rsvmcs.qcrsip.core.SipProviderImpl;
import com.rsvmcs.qcrsip.core.events.RequestEvent;
import com.rsvmcs.qcrsip.core.events.ResponseEvent;
import com.rsvmcs.qcrsip.core.model.SipRequest;
import com.rsvmcs.qcrsip.core.model.SipResponse;
import com.rsvmcs.qcrsip.entity.ChannelRef;
import com.rsvmcs.qcrsip.core.model.SIPMessage;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class UDPMessageProcessor  extends MessageProcessor implements Runnable {
    private final ListeningPoint lp;
    private volatile boolean running = false;
    private DatagramChannel ch;
    private Thread ioThread;

    public UDPMessageProcessor(ListeningPoint lp) { this.lp = lp; }

    @Override
    public void start() throws Exception {
        ch = DatagramChannel.open();
        ch.configureBlocking(false);
        ch.bind(new InetSocketAddress(lp.getIp(), lp.getPort()));
        running = true;
        ioThread = new Thread(this, "UDPMessageProcessor-" + lp);
        ioThread.start();
        System.out.println("[UDP] listening " + lp);
    }

    @Override
    public void stop() throws Exception {
        running = false;
        if (ioThread != null) ioThread.join(300);
        if (ch != null) ch.close();
    }

    @Override
    public void run() {
        ByteBuffer buf = ByteBuffer.allocate(64 * 1024);
        while (running) {
            try {
                buf.clear();
                InetSocketAddress remote = (InetSocketAddress) ch.receive(buf);
                if (remote == null) { Thread.sleep(10); continue; }
                buf.flip();
                byte[] data = new byte[buf.remaining()];
                buf.get(data);
                String txt = new String(data, StandardCharsets.US_ASCII);

                SIPMessage msg;
                try { msg = SIPMessage.parse(txt); } catch (Throwable t) { t.printStackTrace(); msg = null; }
                if (msg == null) continue; // keepalive / 空帧


              //msg.setRemoteAddress(remote);  // 不需要加，客户端调用的时候设置  ok.setUdpPeer(e.getUdpPeer());

                if (msg instanceof SipRequest) {
                    scanner.offer(new EventScanner.Item(EventScanner.Kind.REQUEST, new RequestEvent((SipRequest) msg, null, remote), provider));
                } else {
                    scanner.offer(new EventScanner.Item(EventScanner.Kind.RESPONSE, new ResponseEvent((SipResponse) msg), provider));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
