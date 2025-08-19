package com.rsvmcs.qcrsip.core.io;

import com.rsvmcs.qcrsip.core.EventScanner;
import com.rsvmcs.qcrsip.core.ListeningPoint;
import com.rsvmcs.qcrsip.core.SipProviderImpl;
import com.rsvmcs.qcrsip.core.events.RequestEvent;
import com.rsvmcs.qcrsip.core.events.ResponseEvent;
import com.rsvmcs.qcrsip.core.model.SipRequest;
import com.rsvmcs.qcrsip.core.model.SipResponse;
import com.rsvmcs.qcrsip.core.model.SIPMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
public class TCPMessageProcessor extends MessageProcessor implements Runnable {

//    private final ListeningPoint lp;
//    private final SipProviderImpl provider;
//
//    private Selector selector;
//    private ServerSocketChannel server;
//    private Thread ioThread;
//    private volatile boolean running;
//
//    private final Map<SocketChannel, ByteBuffer> recvBufs = new HashMap<>();
//
//    public TCPMessageProcessor(ListeningPoint lp, SipProviderImpl provider) {
//        this.lp = lp; this.provider = provider;
//    }
//
//    @Override public synchronized void start() {
//        if (running) return;
//        try {
//            selector = Selector.open();
//            server = ServerSocketChannel.open();
//            server.configureBlocking(false);
//            server.bind(new InetSocketAddress(lp.getIp(), lp.getPort()));
//            server.register(selector, SelectionKey.OP_ACCEPT);
//            running = true;
//            ioThread = new Thread(this, "TCP-" + lp);
//            ioThread.start();
//            System.out.println("[TCP] listening " + lp);
//        } catch (Exception e) { throw new RuntimeException(e); }
//    }
//
//    @Override public synchronized void stop() {
//        running = false;
//        if (selector != null) selector.wakeup();
//        try { if (ioThread != null) ioThread.join(300); } catch (InterruptedException ignored) {}
//        try { if (server != null) server.close(); } catch (Exception ignored) {}
//        try { if (selector != null) selector.close(); } catch (Exception ignored) {}
//    }
//
//    @Override public void run() {
//        while (running) {
//            try {
//                selector.select(500);
//                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
//                while (it.hasNext()) {
//                    SelectionKey k = it.next(); it.remove();
//                    if (!k.isValid()) continue;
//                    if (k.isAcceptable()) accept();
//                    else if (k.isReadable()) read((SocketChannel) k.channel());
//                }
//            } catch (Throwable t) { t.printStackTrace(); }
//        }
//    }
//
//    private void accept() throws Exception {
//        SocketChannel ch = server.accept();
//        if (ch == null) return;
//        ch.configureBlocking(false);
//        ch.register(selector, SelectionKey.OP_READ);
//        recvBufs.put(ch, ByteBuffer.allocate(128 * 1024));
//        System.out.println("[TCP] accepted " + ch.getRemoteAddress());
//    }
//
//    //必须严格按照 SIP 消息边界 (双 CRLF + Content-Length) 提取，不要乱改 position。
//    // 每次 read 后 buffer.flip() → parse → buffer.compact()，否则会丢数据。改为下面函数
////    private void read(SocketChannel ch) {
////        ByteBuffer buf = recvBufs.get(ch);
////        if (buf == null) { buf = ByteBuffer.allocate(128*1024); recvBufs.put(ch, buf); }
////        ByteBuffer tmp = ByteBuffer.allocateDirect(64*1024);
////        try {
////            int n = ch.read(tmp);
////            if (n == -1) { close(ch); return; }
////            if (n == 0) return;
////            tmp.flip();
////            ensureCapacity(buf, tmp.remaining());
////            buf.put(tmp);
////            buf.flip();
////            int consumed;
////            do { consumed = tryExtractAndDeliver(ch, buf); } while (consumed > 0);
////            buf.compact();
////        } catch (Exception e) { e.printStackTrace(); close(ch); }
////    }
//
//    public void read(SocketChannel channel) throws IOException {
//        ByteBuffer buffer=recvBufs.get(channel);
//        int bytesRead = channel.read(buffer);
//        if (bytesRead == -1) {
//            channel.close();
//            return;
//        }
//        buffer.flip(); // ✅ 切换读模式
//        tryExtractAndDeliver(channel,buffer);
//        buffer.compact(); // ✅ 保留未读数据
//    }
//
////    private int tryExtractAndDeliver(SocketChannel ch, ByteBuffer buf) throws Exception {
////        byte[] arr = new byte[buf.remaining()];
////        buf.mark(); buf.get(arr);
////        int headerEnd = -1;
////        for (int i=0;i<arr.length-3;i++) {
////            if (arr[i]==13 && arr[i+1]==10 && arr[i+2]==13 && arr[i+3]==10) { headerEnd=i; break; }
////        }
////        if (headerEnd < 0) { buf.reset(); return 0; }
////        String header = new String(arr, 0, headerEnd, java.nio.charset.StandardCharsets.US_ASCII);
////        int cl = 0;
////        for (String line : header.split("\r\n")) {
////            int idx = line.indexOf(':');
////            if (idx > 0 && line.regionMatches(true, 0, "Content-Length", 0, 14)) {
////                try { cl = Integer.parseInt(line.substring(idx+1).trim()); } catch (Exception ignore) {}
////            }
////        }
////        int total = headerEnd + 4 + cl;
////        if (arr.length < total) { buf.reset(); return 0; }
////        byte[] frame = Arrays.copyOfRange(arr, 0, total);
////        buf.reset();
////        buf.position(buf.position() - arr.length + total);
////
////        provider.deliverTcp(lp, ch, (InetSocketAddress) ch.getRemoteAddress(), frame);
////        return total;
////    }
//
//    private void tryExtractAndDeliver(SocketChannel ch, ByteBuffer buffer) throws IOException {
//        while (buffer.remaining() > 0) {
//            buffer.mark(); // 记录当前位置
//
//            String data = new String(buffer.array(), buffer.position(), buffer.remaining());
//            int headerEnd = data.indexOf("\r\n\r\n");
//
//            if (headerEnd == -1) {
//                buffer.reset(); // 头部不完整，回退
//                return;
//            }
//
//            int contentLength = 0;
//            int clIndex = data.toLowerCase().indexOf("content-length:");
//            if (clIndex != -1 && clIndex < headerEnd) {
//                int lineEnd = data.indexOf("\r\n", clIndex);
//                String lenStr = data.substring(clIndex + 15, lineEnd).trim();
//                try {
//                    contentLength = Integer.parseInt(lenStr);
//                } catch (NumberFormatException ignore) {}
//            }
//
//            int totalLen = headerEnd + 4 + contentLength;
//            if (buffer.remaining() < totalLen) {
//                buffer.reset(); // 消息不完整，等更多数据
//                return;
//            }
//
//            byte[] msgBytes = new byte[totalLen];
//            buffer.get(msgBytes); // ✅ 移动 position 正常
//           // deliverMessage(new String(msgBytes));
//
//            provider.deliverTcp(lp, ch, (InetSocketAddress) ch.getRemoteAddress(), msgBytes);
//        }
//    }
//
//    private void ensureCapacity(ByteBuffer buf, int need) {
//        if (buf.remaining() >= need) return;
//        int newCap = Math.max(buf.capacity()*2, buf.capacity()+need);
//        ByteBuffer nb = ByteBuffer.allocate(newCap);
//        buf.flip(); nb.put(buf); buf.clear();
//        // 这里无需回写 map，引用仍在
//        recvBufs.replace(find(buf), nb);
//    }
//
//    private SocketChannel find(ByteBuffer v) {
//        for (Map.Entry<SocketChannel,ByteBuffer> e : recvBufs.entrySet()) if (e.getValue()==v) return e.getKey();
//        return null;
//    }
//
//    private void close(SocketChannel ch) {
//        try { ch.close(); } catch (Exception ignored) {}
//        recvBufs.remove(ch);
//    }


    private final ListeningPoint lp;
    private volatile boolean running = false;
    private ServerSocketChannel server;
    private Selector selector;
    private Thread ioThread;

    public TCPMessageProcessor(ListeningPoint lp) { this.lp = lp; }

    @Override
    public void start() throws Exception {
        selector = Selector.open();
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(lp.getIp(), lp.getPort()));
        server.register(selector, SelectionKey.OP_ACCEPT);
        running = true;
        ioThread = new Thread(this, "TCPMessageProcessor-" + lp);
        ioThread.start();
        System.out.println("[TCP] listening " + lp);
    }

    @Override
    public void stop() throws Exception {
        running = false;
        if (selector != null) selector.wakeup();
        if (ioThread != null) ioThread.join(300);
        if (server != null) server.close();
    }

    @Override
    public void run() {
        while (running) {
            try {
                selector.select(500);
                for (SelectionKey key : selector.selectedKeys()) {
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) {
                        SocketChannel ch = server.accept();
                        if (ch != null) {
                            ch.configureBlocking(false); // ⚠️ 必须是 false
                            ch.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(128 * 1024));
                            System.out.println("[TCP] accepted " + ch.getRemoteAddress());
                        }
                    } else if (key.isReadable()) {
                        SocketChannel ch = (SocketChannel) key.channel();
                        ByteBuffer in = (ByteBuffer) key.attachment();
                        int n = ch.read(in);
                        if (n == -1) { key.cancel(); ch.close(); continue; }
                        if (n <= 0) continue;

                        in.flip();
                        while (true) {
                            SIPMessage.Frame f = SIPMessage.tryExtractFrame(in);
                            if (f == null) break;
                            String txt = new String(f.bytes, StandardCharsets.US_ASCII);

                            SIPMessage msg;
                            try {
                                msg = SIPMessage.parse(txt);
                            } catch (Throwable t) {
                                // 极端异常：直接丢弃该帧，避免死循环
                                t.printStackTrace();
                                msg = null;
                            }
                            if (msg == null) {
                                // 可能是 keepalive/空帧，忽略
                                continue;
                            }

                            InetSocketAddress localAddress = (InetSocketAddress) ch.getLocalAddress();
                            msg.setLocalAddress(localAddress);

                            if (msg instanceof SipRequest) {
                                scanner.offer(new EventScanner.Item(
                                        EventScanner.Kind.REQUEST,
                                        new RequestEvent((SipRequest) msg, new TCPMessageChannel(ch), null),
                                        provider));
                            } else {
                                scanner.offer(new EventScanner.Item(
                                        EventScanner.Kind.RESPONSE,
                                        new ResponseEvent((SipResponse) msg),
                                        provider));
                            }
                        }
                        in.compact();
                    }
                }
                selector.selectedKeys().clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Selector selector() {
        return selector;
    }

}
