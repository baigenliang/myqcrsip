package com.rsvmcs.qcrsip.core.stack;

import com.rsvmcs.qcrsip.core.SipProviderImpl;
import com.rsvmcs.qcrsip.entity.ChannelRef;
import com.rsvmcs.qcrsip.entity.SIPMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class TCPMessageProcessor implements Runnable {
    private final SipProviderImpl provider;
    private Selector selector;
    private ServerSocketChannel server;
    private volatile boolean running;

    public TCPMessageProcessor(SipProviderImpl provider){ this.provider = provider; }

    public void start(String ip, int port) throws IOException {
        selector = Selector.open();
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(ip, port));
        server.register(selector, SelectionKey.OP_ACCEPT);
        running = true;
        Thread t = new Thread(this, "TCP-Processor-" + ip + ":" + port);
        t.start();
        System.out.println("[TCP] listening " + ip + ":" + port);
    }

    public void stop(){
        running = false;
        try { selector.wakeup(); server.close(); selector.close(); } catch (Exception ignore){}
    }

    @Override public void run() {
        ByteBuffer buf = ByteBuffer.allocate(64 * 1024);
        while (running){
            try {
                selector.select(500);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()){
                    SelectionKey key = it.next(); it.remove();
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()){
                        SocketChannel ch = server.accept();
                        if (ch != null){
                            ch.configureBlocking(false);
                            ch.register(selector, SelectionKey.OP_READ);
                        }
                    } else if (key.isReadable()){
                        SocketChannel ch = (SocketChannel) key.channel();
                        buf.clear();
                        int n = ch.read(buf);
                        if (n <= 0) { key.cancel(); ch.close(); continue; }
                        byte[] data = new byte[n];
                        buf.flip(); buf.get(data);
                        SIPMessage m = SIPMessage.parse(data);
                        if (m != null) {
                            provider.onMessageArrived(m, ChannelRef.forTcp(ch));
                        }
                    }
                }
            } catch (Exception e){
                if (running) e.printStackTrace();
            }
        }
    }
}
