package com.rsvmcs.qcrsip.core;


import com.rsvmcs.qcrsip.core.events.RequestEvent;
import com.rsvmcs.qcrsip.core.events.ResponseEvent;
import com.rsvmcs.qcrsip.core.events.TimeoutEvent;
import com.rsvmcs.qcrsip.core.io.TCPMessageChannel;
import com.rsvmcs.qcrsip.core.io.TCPMessageProcessor;
import com.rsvmcs.qcrsip.core.io.UDPMessageChannel;
import com.rsvmcs.qcrsip.core.io.UDPMessageProcessor;
import com.rsvmcs.qcrsip.core.model.*;


import java.net.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class SipProviderImpl implements SipProvider {
    private final ListeningPoint lp;
    private final SipStackImpl stack;
    private final Object processor; // TCPMessageProcessor or UDPMessageProcessor
    private volatile SipListener listener;

    // 连接池（不是按 CSeq，而是按 对端三元组）
    private final ConcurrentMap<String, TCPMessageChannel> tcpPool = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UDPMessageChannel> udpPool = new ConcurrentHashMap<>();

    public SipProviderImpl(ListeningPoint lp, SipStackImpl stack, Object proc) {
        this.lp = lp;
        this.stack = stack;
        this.processor = proc;

        // 让处理器反向回调到 provider
        if (proc instanceof TCPMessageProcessor) ((TCPMessageProcessor) proc).setProvider(this);
        if (proc instanceof UDPMessageProcessor) ((UDPMessageProcessor) proc).setProvider(this);
    }

    @Override
    public void addSipListener(SipListener l) { this.listener = l; }

    @Override
    public void removeSipListener(SipListener l) { if (Objects.equals(this.listener,l)) this.listener = null; }

    @Override
    public SipListener getSipListener() { return listener; }

    @Override
    public ListeningPoint getListeningPoint() { return lp; }

    private static String keyOf(String transport, InetSocketAddress dst) {
        return transport.toUpperCase() + "|" + dst.getAddress().getHostAddress() + "|" + dst.getPort();
    }

    @Override
    public void sendRequest(SipRequest request) throws Exception {
        SIPMessage msg = request;
        byte[] data = msg.encode().getBytes(StandardCharsets.US_ASCII);

        String transport = request.getTransport();
        InetSocketAddress dst =
                (request.getExplicitRemote() != null)
                        ? request.getExplicitRemote()
                        : new InetSocketAddress(request.getHost(), request.getPort());

        if ("TCP".equalsIgnoreCase(transport)) {
            String key = keyOf("TCP", dst);
            TCPMessageChannel ch = tcpPool.computeIfAbsent(key, k -> {
                try { return TCPMessageChannel.connect(dst,(TCPMessageProcessor)processor); } catch (Exception e) { throw new RuntimeException(e); }
            });
            //增加此判断保证客户端主动断开时也能再次重新连接
            ch.send(dst,data,tcpPool,key,(TCPMessageProcessor)processor);
        } else { // UDP
            String key = keyOf("UDP", dst);
            UDPMessageChannel ch = udpPool.computeIfAbsent(key, k -> {
                try { return new UDPMessageChannel(new InetSocketAddress(lp.getIp(), 0)); } catch (Exception e) { throw new RuntimeException(e); }
            });
            ch.send(dst, data);
        }
    }

    @Override
    public void sendResponse(SipResponse response) throws Exception {
        byte[] data = response.encode().getBytes(StandardCharsets.US_ASCII);

        // 优先沿接收通道回发（TCP）
        TCPMessageChannel replyTcp = response.getReplyTcpChannel();
        if (replyTcp != null) {
            replyTcp.send(null,data,null,null,null);
            return;
        }
        // 或者 UDP 按对端回发
        InetSocketAddress udpPeer = response.getReplyUdpPeer();
        if (udpPeer != null) {
            String key = keyOf("UDP", udpPeer);
            UDPMessageChannel ch = udpPool.computeIfAbsent(key, k -> {
                try { return new UDPMessageChannel(new InetSocketAddress(lp.getIp(), 0)); } catch (Exception e) { throw new RuntimeException(e); }
            });
            ch.send(udpPeer, data);
            return;
        }

        // 否则按 Response 的显式目的地（如果业务层设置了）
        if (response.getExplicitRemote() != null) {
            if ("TCP".equalsIgnoreCase(response.getTransport())) {
                String key=keyOf("TCP", response.getExplicitRemote());
                TCPMessageChannel ch = tcpPool.computeIfAbsent(key, k -> {
                    try { return TCPMessageChannel.connect(response.getExplicitRemote(),(TCPMessageProcessor)processor); } catch (Exception e) { throw new RuntimeException(e); }
                });
                ch.send(response.getExplicitRemote(),data,tcpPool,key,(TCPMessageProcessor)processor);
            } else {
                UDPMessageChannel ch = udpPool.computeIfAbsent(keyOf("UDP", response.getExplicitRemote()), k -> {
                    try { return new UDPMessageChannel(new InetSocketAddress(lp.getIp(), 0)); } catch (Exception e) { throw new RuntimeException(e); }
                });
                ch.send(response.getExplicitRemote(), data);
            }
            return;
        }

        throw new IllegalStateException("No route to send response (no reply channel / udpPeer / explicit remote).");
    }

    // 处理器回调到此 → 再交给 EventScanner/Listener
    @Override
    public void fireRequest(RequestEvent ev) {
        SipListener l = this.listener;
        if (l != null) l.processRequest(ev);
    }

    @Override
    public void fireResponse(ResponseEvent ev) {
        SipListener l = this.listener;
        if (l != null) l.processResponse(ev);
    }

    @Override
    public void fireTimeout(TimeoutEvent ev) {
        SipListener l = this.listener;
        if (l != null) l.processTimeout(ev);
    }
}