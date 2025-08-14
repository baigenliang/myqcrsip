package com.rsvmcs.qcrsip.core;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * NIO-based async TCP connection pool + send queue + auto-reconnect
 *
 * - RemoteKey: host:port
 * - sendAsync(key, data): enqueue and wake selector, will connect if needed
 * - on incoming bytes, call onMessage.accept(remoteKey, bytes)
 */
public class ConnectionManager {
    private final Selector selector;
    private final Thread selectorThread;
    private final ConcurrentMap<String, Connection> connections = new ConcurrentHashMap<>();
    private final BiConsumer<String, byte[]> onMessage; // (remoteKey, raw)
    private final ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(1);

    public ConnectionManager(BiConsumer<String, byte[]> onMessage) throws IOException {
        this.onMessage = onMessage;
        this.selector = Selector.open();
        this.selectorThread = new Thread(this::runLoop, "ConnManager-Selector");
        this.selectorThread.setDaemon(true);
        this.selectorThread.start();
    }

    // remoteKey = host:port
    public void sendAsync(String host, int port, byte[] data) {
        String key = host + ":" + port;
        connections.compute(key, (k, conn) -> {
            if (conn == null) {
                conn = new Connection(host, port);
                conn.enqueue(data);
                conn.startConnect();
                return conn;
            } else {
                conn.enqueue(data);
                if (!conn.isConnectedOrConnecting()) conn.startConnect();
                wakeupSelector();
                return conn;
            }
        });
    }

    public void closeAll() {
        for (Connection c : connections.values()) c.close();
        try { selector.close(); } catch(Exception ignored){}
        reconnectExecutor.shutdownNow();
    }

    private void runLoop() {
        try {
            while (selector.isOpen()) {
                selector.select(500);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next(); it.remove();
                    if (!key.isValid()) continue;
                    Object att = key.attachment();
                    if (att instanceof Connection) {
                        Connection conn = (Connection) att;
                        try {
                            if (key.isConnectable()) conn.finishConnect(key);
                            if (key.isWritable()) conn.doWrite(key);
                            if (key.isReadable()) conn.doRead(key);
                        } catch (IOException ioe) {
                            conn.handleIOException(ioe);
                        }
                    }
                }
                // also, for connections that have pending writes but not registered for WRITE, register
                for (Connection c : connections.values()) c.ensureInterest();
            }
        } catch (ClosedSelectorException cse) {
            // exiting
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void wakeupSelector() { selector.wakeup(); }

    private class Connection {
        final String host; final int port; final String key;
        volatile SocketChannel channel;
        final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
        volatile boolean connecting = false;
        volatile boolean connected = false;
        volatile SelectionKey selKey;

        Connection(String host, int port) { this.host = host; this.port = port; this.key = host+":"+port; }

        void enqueue(byte[] data) { writeQueue.add(ByteBuffer.wrap(data)); }

        boolean isConnectedOrConnecting() { return connecting || connected; }

        void startConnect() {
            try {
                connecting = true;
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(new InetSocketAddress(host, port));
                selKey = channel.register(selector, SelectionKey.OP_CONNECT, this);
                wakeupSelector();
            } catch(Exception e) {
                handleIOException(e);
            }
        }

        void finishConnect(SelectionKey key) throws IOException {
            if (channel.finishConnect()) {
                connected = true; connecting = false;
                key.interestOps(SelectionKey.OP_READ | (writeQueue.isEmpty()?0:SelectionKey.OP_WRITE));
            } else {
                // not connected
            }
        }

        void ensureInterest() {
            try {
                if (selKey != null && selKey.isValid()) {
                    int ops = SelectionKey.OP_READ;
                    if (!writeQueue.isEmpty()) ops |= SelectionKey.OP_WRITE;
                    selKey.interestOps(ops);
                }
            } catch (CancelledKeyException ignored) {}
        }

        void doWrite(SelectionKey key) throws IOException {
            ByteBuffer bb;
            SocketChannel ch = channel;
            while ((bb = writeQueue.peek()) != null) {
                ch.write(bb);
                if (bb.hasRemaining()) break; // socket buffer full
                writeQueue.poll();
            }
            ensureInterest();
        }

        void doRead(SelectionKey key) throws IOException {
            SocketChannel ch = channel;
            ByteBuffer buf = ByteBuffer.allocate(8192);
            int r = ch.read(buf);
            if (r == -1) throw new IOException("Remote closed");
            if (r > 0) {
                buf.flip();
                byte[] data = new byte[buf.remaining()];
                buf.get(data);
                // deliver raw by onMessage callback (note: selector thread calls)
                onMessage.accept(keyString(), data);
            }
        }

        String keyString() { return key; }

        void handleIOException(Exception e) {
            // connection broken -> schedule reconnect & move pending data intact
            closeChannel();
            connecting = false; connected = false;
            // schedule reconnect attempt after delay
            reconnectExecutor.schedule(() -> {
                try {
                    startConnect();
                } catch (Exception ex) { /* ignore */ }
            }, 1000, TimeUnit.MILLISECONDS);
        }

        void close() {
            closeChannel();
            connected = false; connecting = false;
        }

        void closeChannel() {
            try { if (selKey != null) selKey.cancel(); } catch(Exception ignored){}
            try { if (channel != null) channel.close(); } catch(Exception ignored){}
            selKey = null; channel = null;
        }
    }
}