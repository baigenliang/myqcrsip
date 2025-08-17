package com.rsvmcs.qcrsip.core.events;

import com.rsvmcs.qcrsip.core.SipProvider;
import com.rsvmcs.qcrsip.core.io.TCPMessageChannel;
import com.rsvmcs.qcrsip.core.model.SipRequest;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;


public class RequestEvent {
    private final SipRequest request;
    private final TCPMessageChannel tcpChannel;     // 收到请求的 TCP 通道（用于回 200 OK）
    private final InetSocketAddress udpPeer;        // 收到请求的 UDP 源（用于回 200 OK）

    public RequestEvent(SipRequest request, TCPMessageChannel tcpChannel, InetSocketAddress udpPeer){
        this.request=request; this.tcpChannel=tcpChannel; this.udpPeer=udpPeer;
    }
    public SipRequest getRequest(){ return request; }
    public TCPMessageChannel getTcpChannel(){ return tcpChannel; }
    public InetSocketAddress getUdpPeer(){ return udpPeer; }
}