package com.rsvmcs.qcrsip.core.io;

import com.rsvmcs.qcrsip.core.SipProviderImpl;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPMessageChannel {

    /**********************************采用传统阻塞*************************************************/
    // private final Socket socket;
//    private final SocketChannel channel;
//
////    public TCPMessageChannel(Socket socket) throws IOException {
////        this.socket = socket;
////    }
//    public TCPMessageChannel(SocketChannel channel) throws IOException {
//        this.channel = channel;
//    }
//
//    //异步非阻塞连接
//    public static TCPMessageChannel connect(InetSocketAddress inetSocketAddress) throws IOException {
//        SocketChannel ch = SocketChannel.open();
//        ch.configureBlocking(false);
//        ch.connect(inetSocketAddress);
//        while (!ch.finishConnect()) {
//            // 等待连接完成
//        }
//        return new TCPMessageChannel(ch);
//    }
//
//    //异步发送
//    public void send(byte[] bytes) throws IOException {
//        ByteBuffer buf = ByteBuffer.wrap(bytes);
//        while (buf.hasRemaining()) {
//            channel.write(buf); // ✅ 非阻塞写
//        }
//    }
//
//    //暂时未用到，发送都是在本函数内部发送的
//    public SocketChannel getChannel() {
//        return channel;
//    }

    //传统同步发送
//    public synchronized void send(byte[] data) throws Exception {
//        OutputStream os = socket.getOutputStream();
//        os.write(data);
//        os.flush();
//    }
//    public Socket getSocket() { return socket; }

    /**********************************采用NIO模型*************************************************/
//    private final SocketChannel channel;
//
//    public TCPMessageChannel(SocketChannel channel) throws IOException {
//        this.channel = channel;
//        this.channel.configureBlocking(true); //还是采用阻塞模式，否则第一次发送数据需要等待才能发送成功
//    }

//    public static TCPMessageChannel connect(InetSocketAddress dst) throws IOException {
////        SocketChannel ch = SocketChannel.open();
////        ch.configureBlocking(false);
////        ch.connect(dst);
////        while (!ch.finishConnect()) {
////            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
////        }
////        return new TCPMessageChannel(ch);
//        //异步非阻塞连接改为阻塞式，否则发送数据时有可能连接未建立完成
//        SocketChannel ch = SocketChannel.open();
//        ch.configureBlocking(true); // 阻塞模式，确保连接完成
//        // ch.socket().setTcpNoDelay(true); //可以不设置，保证两个configureBlocking地方是true
//        ch.connect(dst);
//        while (!ch.finishConnect()) {
//            // 在这里循环或者注册 OP_CONNECT 事件
//            // 直到真正完成 TCP 三次握手
//            System.out.println("OP_CONNECT或者TCP三次握手中");
//        }
//      //  System.out.println("OP_CONNECT或者TCP三次握手已完成");
//        return new TCPMessageChannel(ch);
//    }

//    public void send(byte[] bytes) throws IOException {
////        boolean connected = channel.finishConnect();
////        System.out.println("finishConnect=" + connected);
//        ByteBuffer buf = ByteBuffer.wrap(bytes);
//        while (buf.hasRemaining()) {
//              channel.write(buf); //确保完全写出
//        }
//    }

//    public void send(InetSocketAddress dst, byte[] bytes, Map<String, TCPMessageChannel> tcpPool, String key) throws IOException {
//        ByteBuffer buf = ByteBuffer.wrap(bytes);
//        try {
//            while (buf.hasRemaining()) {
//                channel.write(buf); // 写数据
//            }
//        } catch (IOException e) {
//            if(dst!=null && tcpPool!=null && key!=null) {
//                System.err.println("连接已断开，尝试重连: " + e.getMessage());
//                // 移除旧的
//                tcpPool.remove(key);
//                // 重新建立
//                TCPMessageChannel newCh = TCPMessageChannel.connect(dst);
//                tcpPool.put(key, newCh);
//                // 重新发送一次
//                newCh.send(dst, bytes, tcpPool, key);
//            }
//        }
//    }
//
//
//    public SocketChannel getChannel() {
//        return channel;
//    }
  /**********************支持客户端异常连接断开实时监测和数据发送异常时重连机制*******************************/
    private final SocketChannel channel;

    public TCPMessageChannel(SocketChannel channel) throws IOException {
        this.channel = channel;
        // 启动后台检测线程
        // startMonitor(); 并发一段时间后连接被自动关闭
    }

    public static TCPMessageChannel connect(InetSocketAddress dst, TCPMessageProcessor processor) throws IOException {

        System.out.println("[TCP] 开始连接: " + dst);
        SocketChannel ch = SocketChannel.open();
        ch.configureBlocking(true); // 阻塞模式 为了首次连接后不sleep也能直接成功write发送数据（避免写数据底层缓冲还没ready，只是写入到内核）
        System.out.println("[TCP] 调用 connect()");
        ch.connect(dst);
        while (!ch.finishConnect()) {
            System.out.println("[TCP] 等待 finishConnect...");
            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        }
        System.out.println("[TCP] finishConnect 完成: " + ch.isConnected());
        ch.socket().setTcpNoDelay(true);
        ch.socket().setKeepAlive(true);
        ch.configureBlocking(false); // 建立成功后再切回非阻塞，给 selector 用,否则阻塞模式selector无法收取数据
        System.out.println("[TCP] 已切换为非阻塞模式");

        // 注册到 processor 的 selector，统一监听(确保主动发送的连接也能正常响应回来)
//        ch.register(processor.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(128 * 1024));
//        System.out.println("[TCP] connected to " + dst);

        if(processor!=null) {
            SelectionKey key = ch.keyFor(processor.selector());
            if (key == null || !key.isValid()) {
                ch.register(processor.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(128 * 1024));
                System.out.println("[TCP] 注册到 selector: OP_READ");
            } else {
                // 已经注册过了，更新感兴趣的事件
                key.interestOps(SelectionKey.OP_READ);
                System.out.println("[TCP] 已存在 key, 更新为 OP_READ");
            }
        }

        System.out.println("[TCP] connect 完成返回 channel");
        return new TCPMessageChannel(ch);
    }

    public void send(InetSocketAddress dst, byte[] bytes, Map<String, TCPMessageChannel> tcpPool, String key, TCPMessageProcessor processor) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        try {
            System.out.println("[TCP] 发送数据开始, 长度=" + bytes.length + " dst=" + dst);
            while (buf.hasRemaining()) {
                int written=channel.write(buf); // 写数据、 阻塞模式一定能写完
                System.out.println("[TCP] channel.write 返回=" + written + ", remaining=" + buf.remaining());
            }
            System.out.println("[TCP] 数据发送完成");

        } catch (IOException e) {
            System.err.println("[TCP] 发送失败: " + e.getMessage());
            if (dst != null && tcpPool != null && key != null) {
                System.err.println("连接已断开，尝试重连: " + e.getMessage());
                // 移除旧的
                tcpPool.remove(key);
                // 重新建立
                TCPMessageChannel newCh = TCPMessageChannel.connect(dst,processor);
                tcpPool.put(key, newCh);
                System.err.println("[TCP] 重连完成, 再次发送");
                // 重新发送一次
                newCh.send(dst, bytes, tcpPool, key,processor);
            }
        }
    }

    public SocketChannel getChannel() {
        return channel;
    }

    // -------- 新增部分：连接监测 ----------
    private void startMonitor() {
        Thread t = new Thread(() -> {
            try {
                while (channel.isOpen()) {
                    // 主动探测：read 非阻塞方式
                    channel.socket().sendUrgentData(0xFF);
                    Thread.sleep(1000); // 5秒检测一次
                }
            } catch (Exception e) {
                try {
                    System.out.println("检测到连接断开，自动关闭: " + e.getMessage());
                    channel.close();
                } catch (IOException ignored) {}
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
