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

    public static TCPMessageChannel connect(InetSocketAddress dst) throws IOException {
        SocketChannel ch = SocketChannel.open();
        ch.configureBlocking(false); // 阻塞模式
        ch.socket().setTcpNoDelay(true);
        ch.socket().setKeepAlive(true);
        ch.connect(dst);
        while (!ch.finishConnect()) {
            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        }
        return new TCPMessageChannel(ch);
    }

    public void send(InetSocketAddress dst, byte[] bytes, Map<String, TCPMessageChannel> tcpPool, String key) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        try {
            while (buf.hasRemaining()) {
                channel.write(buf); // 写数据
            }
        } catch (IOException e) {
            if (dst != null && tcpPool != null && key != null) {
                System.err.println("连接已断开，尝试重连: " + e.getMessage());
                // 移除旧的
                tcpPool.remove(key);
                // 重新建立
                TCPMessageChannel newCh = TCPMessageChannel.connect(dst);
                tcpPool.put(key, newCh);
                // 重新发送一次
                newCh.send(dst, bytes, tcpPool, key);
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
