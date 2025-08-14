package com.rsvmcs.qcrsip.core.stack;

import com.rsvmcs.qcrsip.entity.ChannelRef;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;

public class NioTcpConnectionPool {
    private final ConcurrentMap<String, SocketChannel> pool = new ConcurrentHashMap<>();

    private String key(String host, int port){ return host + ":" + port; }

    public ChannelRef getOrConnect(String host, int port, int timeoutMs) throws IOException {
        String k = key(host, port);
        SocketChannel ch = pool.get(k);
        if (ch == null || !ch.isOpen() || !ch.isConnected()) {
            synchronized (pool) {
                ch = pool.get(k);
                if (ch == null || !ch.isOpen() || !ch.isConnected()) {
                    SocketChannel newCh = SocketChannel.open();
                    newCh.configureBlocking(true); // 简化：连接阶段阻塞，写入阶段直接 write
                    newCh.socket().connect(new InetSocketAddress(host, port), timeoutMs);
                    newCh.configureBlocking(false); // 连上后改非阻塞
                    pool.put(k, newCh);
                    ch = newCh;
                }
            }
        }
        return ChannelRef.forTcp(ch);
    }

    public void shutdown(){
        pool.values().forEach(ch -> {
            try { ch.close(); } catch (Exception ignore){}
        });
        pool.clear();
    }
}
