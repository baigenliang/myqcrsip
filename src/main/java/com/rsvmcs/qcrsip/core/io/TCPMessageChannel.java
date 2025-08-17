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
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public  class TCPMessageChannel {
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


    private final SocketChannel channel;

    public TCPMessageChannel(SocketChannel channel) throws IOException {
        this.channel = channel;
        this.channel.configureBlocking(false);
    }

    public static TCPMessageChannel connect(InetSocketAddress dst) throws IOException {
//        SocketChannel ch = SocketChannel.open();
//        ch.configureBlocking(false);
//        ch.connect(dst);
//        while (!ch.finishConnect()) {
//            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
//        }
//        return new TCPMessageChannel(ch);
        //异步非阻塞连接改为阻塞式，否则发送数据时有可能连接未建立完成
        SocketChannel ch = SocketChannel.open();
        ch.configureBlocking(true); // 阻塞模式，确保连接完成
        ch.connect(dst);
        while (!ch.finishConnect()) {
            // 在这里循环或者注册 OP_CONNECT 事件
            // 直到真正完成 TCP 三次握手
            System.out.println("OP_CONNECT或者TCP三次握手中");
        }
        System.out.println("OP_CONNECT或者TCP三次握手已完成");
        return new TCPMessageChannel(ch);
    }

    public void send(byte[] bytes) throws IOException {
//        ByteBuffer buf = ByteBuffer.wrap(bytes);
//        int zeroWrites = 0;
//        while (buf.hasRemaining()) {
//            int n = channel.write(buf);
//            if (n == 0) {
//                // 写会被阻塞，稍等再试（避免“只有断点才成功”）
//                zeroWrites++;
//                if (zeroWrites > 2000) throw new IOException("TCP write would block too long");
//                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
//            } else {
//                zeroWrites = 0;
//            }
//        }

        boolean connected = channel.finishConnect();
        System.out.println("finishConnect=" + connected);

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        while (buf.hasRemaining()) {
            int written=channel.write(buf); //确保完全写出
            System.out.println("written=" + written);
            if (written == 0) {
                System.out.println("注册等待中" );
            }
        }

    }


    public SocketChannel getChannel(){ return channel; }

}