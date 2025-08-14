package com.rsvmcs.qcrsip.entity;


public class StatusLine {
    private final String version; // SIP/2.0
    private final int statusCode; // 200
    private final String reason;  // OK

    public StatusLine(String version, int statusCode, String reason){
        this.version = version; this.statusCode = statusCode; this.reason = reason;
    }
    public String getVersion(){ return version; }
    public int getStatusCode(){ return statusCode; }
    public String getReason(){ return reason; }
    public String encode(){ return version + " " + statusCode + " " + reason; }
}
