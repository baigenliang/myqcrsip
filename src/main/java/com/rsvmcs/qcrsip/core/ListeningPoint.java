package com.rsvmcs.qcrsip.core;


import java.util.Objects;

public class ListeningPoint {
    private final String ip;
    private final int port;
    private final String transport;

    public ListeningPoint(String ip, int port, String transport) {
        this.ip = ip; this.port = port; this.transport = transport.toUpperCase();
    }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public String getTransport() { return transport; }
    @Override public String toString(){return ip+":"+port+"/"+transport;}
}