package com.rsvmcs.qcrsip.entity;

public class RequestLine {
    private final String method; // INVITE
    private final String uri;    // sip:host[:port]
    private final String version; // SIP/2.0

    public RequestLine(String method, String uri, String version){
        this.method = method; this.uri = uri; this.version = version;
    }
    public String getMethod(){ return method; }
    public String getUri(){ return uri; }
    public String getVersion(){ return version; }
    public String encode(){ return method + " " + uri + " " + version; }
}
