package com.rsvmcs.qcrsip.entity;


public class ListeningPoint {
    private final String ip;
    private final int port;
    private final String transport; // "TCP" or "UDP"

    public ListeningPoint(String ip, int port, String transport){
        this.ip = ip; this.port = port; this.transport = transport.toUpperCase();
    }
    public String getIp(){ return ip; }
    public int getPort(){ return port; }
    public String getTransport(){ return transport; }
    public String key(){ return transport + "|" + ip + "|" + port; }
}