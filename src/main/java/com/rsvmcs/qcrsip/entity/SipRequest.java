package com.rsvmcs.qcrsip.entity;


public class SipRequest extends SIPMessage implements Request {
    private final RequestLine requestLine;
    public SipRequest(RequestLine rl){ this.requestLine = rl; }
    public RequestLine getRequestLine(){ return requestLine; }
    @Override public String encodeStartLine(){ return requestLine.encode(); }
}
