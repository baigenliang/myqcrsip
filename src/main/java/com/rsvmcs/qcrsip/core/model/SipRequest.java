package com.rsvmcs.qcrsip.core.model;


import java.net.InetSocketAddress;

public class SipRequest extends SIPMessage implements Request {
    private RequestLine requestLine;

    // 发送所需（不从头里解析时可显式指定）
    private String transport = "TCP";
    private String host;
    private int port = 5060;

    public SipRequest(RequestLine rl){ this.requestLine=rl; }

    @Override public String firstLine(){ return requestLine.encode(); }

    public RequestLine getRequestLine(){ return requestLine; }

    public void setTransport(String t){ this.transport=t; }
    public String getTransport(){ return transport; }

    public void setHost(String h){ this.host=h; }
    public String getHost(){ return host; }

    public void setPort(int p){ this.port=p; }
    public int getPort(){ return port; }

}
