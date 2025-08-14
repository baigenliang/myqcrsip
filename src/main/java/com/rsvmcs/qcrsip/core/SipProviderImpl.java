package com.rsvmcs.qcrsip.core;


import com.rsvmcs.qcrsip.core.stack.NioTcpConnectionPool;
import com.rsvmcs.qcrsip.core.stack.UDPMessageChannel;
import com.rsvmcs.qcrsip.entity.*;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;


public class SipProviderImpl implements SipProvider {
    private final SipStackImpl stack;
    private final ListeningPoint lp;
    private final EventScanner scanner;

    private volatile SipListener sipListener;

    // CSeq→回送通道；收到请求时填入（用于 sendResponse 找回通道）
    private final ConcurrentMap<Long, ChannelRef> inboundByCseq = new ConcurrentHashMap<>();

    // 目的地连接池（TCP）
    private final NioTcpConnectionPool tcpPool = new NioTcpConnectionPool();

    // UDP 发包器（按需创建）
    private final ConcurrentMap<String, UDPMessageChannel> udpSenders = new ConcurrentHashMap<>();

    public SipProviderImpl(SipStackImpl stack, ListeningPoint lp, EventScanner scanner){
        this.stack = stack; this.lp = lp; this.scanner = scanner;
    }

    // === SipProvider 接口 ===
    @Override public void addSipListener(SipListener listener){ this.sipListener = listener; }
    @Override public void removeSipListener(SipListener listener){ if (this.sipListener == listener) this.sipListener = null; }
    @Override public SipListener getSipListener(){ return sipListener; }
    @Override public ListeningPoint getListeningPoint(){ return lp; }

    public void shutdown(){ tcpPool.shutdown(); udpSenders.values().forEach(UDPMessageChannel::close); }

    // === 发送 ===
    @Override
    public void sendRequest(Request request) throws Exception {
        sendRequest(request, null); // null 表示用老的 Request-URI 解析
    }

    public void sendRequest(Request request, InetSocketAddress target) throws Exception {
        SIPMessage msg = (SIPMessage) request;
        byte[] data = msg.encode().getBytes(StandardCharsets.US_ASCII);

        // 如果没传目标，就走老逻辑从 Request-URI 解析
        InetSocketAddress dst = target != null
                ? target
                : parseTargetFromUri(((SipRequest) request).getRequestLine().getUri(), getTransportFromVia(msg));

        String transport = getTransportFromVia(msg);
        long cseq = msg.getCSeqNumber();

        if ("TCP".equalsIgnoreCase(transport)) {
            ChannelRef ref = tcpPool.getOrConnect(dst.getHostString(), dst.getPort(), 3000);
            ref.writeAndFlush(data);
            inboundByCseq.put(cseq, ref);
        } else {
            UDPMessageChannel ch = udpSenders.computeIfAbsent(
                    dst.toString(), k -> new UDPMessageChannel(lp.getIp(), 0));
            ch.send(dst, data);
            inboundByCseq.put(cseq, ChannelRef.forUdp(ch, dst));
        }
        System.out.println("Sending to " + dst + " (" + transport + "): " + new String(data));
    }

    private String getTransportFromVia(SIPMessage msg) {
        try {
            // SIPMessage → 获取第一条 Via 头
            String via = msg.getHeader("Via");
            String transport = (via != null && via.toUpperCase().contains("UDP")) ? "UDP" : "TCP";
            return  transport;
        } catch (Exception e) {
            // 忽略解析异常
        }
        return "UDP"; // 默认 UDP
    }


    @Override
    public void sendResponse(Response response) throws Exception {
        SIPMessage msg = (SIPMessage) response;
        byte[] data = msg.encode().getBytes(StandardCharsets.US_ASCII);
        long cseq = msg.getCSeqNumber();
        ChannelRef ref = inboundByCseq.get(cseq);
        if (ref == null) throw new IllegalStateException("No inbound channel for CSeq=" + cseq);
        ref.writeAndFlush(data);
    }

    private InetSocketAddress parseTargetFromUri(String uri, String transport){
        // 例：sip:fd00:...:250 或 sip:fd00:...:250:56072
        String withoutScheme = uri;
        if (uri.toLowerCase().startsWith("sip:")) withoutScheme = uri.substring(4);
        String host = withoutScheme;
        int port = "UDP".equalsIgnoreCase(transport) ? 5060 : 5060;

        // 支持 IPv6 的冒号很多：若带最后端口，通常在最后一个冒号后且不是 ] 结束（这里简化，用'%'判断不出现）
        if (withoutScheme.startsWith("[")) {
            // 格式 [ipv6]:port
            int rbr = withoutScheme.indexOf(']');
            if (rbr > 0) {
                host = withoutScheme.substring(1, rbr);
                int colon = withoutScheme.indexOf(':', rbr);
                if (colon > 0) port = Integer.parseInt(withoutScheme.substring(colon + 1));
            }
        } else {
            int lastColon = withoutScheme.lastIndexOf(':');
            if (lastColon > 0 && withoutScheme.indexOf(':') == lastColon) {
                host = withoutScheme.substring(0, lastColon);
                port = Integer.parseInt(withoutScheme.substring(lastColon + 1));
            } else {
                host = withoutScheme;
            }
        }
        return new InetSocketAddress(host, port);
    }

    // === 接收 → EventScanner 入队 ===
    // TCP/UDP MessageProcessor 解析后调用
    public void onMessageArrived(SIPMessage m, ChannelRef ref){
        if (m instanceof SipRequest) {
            long cseq = m.getCSeqNumber();
            inboundByCseq.put(cseq, ref);
            scanner.add(new RequestEvent(this, (SipRequest)m, ref));
        } else if (m instanceof SipResponse) {
            scanner.add(new ResponseEvent(this, (SipResponse)m, ref));
        }
    }

    // EventScanner 回调本 Provider 做最终分发
    public void dispatchIncoming(SipEvent ev){
        SipListener l = this.sipListener;
        if (l == null) return;
        if (ev instanceof RequestEvent) {
            l.processRequest((RequestEvent) ev);
        } else if (ev instanceof ResponseEvent) {
            l.processResponse((ResponseEvent) ev);
        } else if (ev instanceof TimeoutEvent) {
            l.processTimeout((TimeoutEvent) ev);
        }
    }
}