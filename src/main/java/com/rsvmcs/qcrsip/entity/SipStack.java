package com.rsvmcs.qcrsip.entity;

public interface SipStack {
    ListeningPoint createListeningPoint(String ip, int port, String transport) throws Exception;
    SipProvider createSipProvider(ListeningPoint lp) throws Exception;
    void start() throws Exception;
    void stop();
}