package com.rsvmcs.qcrsip.entity;


public class SipResponse extends SIPMessage implements Response {
    private final StatusLine statusLine;
    public SipResponse(StatusLine sl){ this.statusLine = sl; }
    public StatusLine getStatusLine(){ return statusLine; }
    @Override public String encodeStartLine(){ return statusLine.encode(); }
}