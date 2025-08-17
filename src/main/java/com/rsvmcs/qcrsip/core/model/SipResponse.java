package com.rsvmcs.qcrsip.core.model;


import com.rsvmcs.qcrsip.core.io.TCPMessageChannel;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public class SipResponse extends SIPMessage implements Response {
    private StatusLine statusLine;

    // 回应所需（沿原通道/对端回发）
    private TCPMessageChannel replyTcpChannel;
    private InetSocketAddress replyUdpPeer;

    // 可选：业务层显式指定发送目的地/协议
    private String transport = "TCP";
    private InetSocketAddress explicitRemote;

    public SipResponse(StatusLine sl){ this.statusLine=sl; }

    @Override public String firstLine(){ return statusLine.encode(); }

    public StatusLine getStatusLine(){ return statusLine; }

    public void setReplyChannel(TCPMessageChannel ch){ this.replyTcpChannel=ch; }
    public TCPMessageChannel getReplyTcpChannel(){ return replyTcpChannel; }

    public void setUdpPeer(InetSocketAddress peer){ this.replyUdpPeer=peer; }
    public InetSocketAddress getReplyUdpPeer(){ return replyUdpPeer; }

    public void setTransport(String t){ this.transport=t; }
    public String getTransport(){ return transport; }

    public void setExplicitRemote(InetSocketAddress r){ this.explicitRemote=r; }
    public InetSocketAddress getExplicitRemote(){ return explicitRemote; }
}