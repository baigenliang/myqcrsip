package com.rsvmcs.qcrsip.core;

import com.rsvmcs.qcrsip.core.io.MessageProcessor;

import java.util.Map;

public interface SipStack {
    ListeningPoint createListeningPoint(String ip, int port, String transport) throws Exception;
    SipProvider createSipProvider(ListeningPoint lp) throws Exception;
    void start() throws Exception;
    void stop() throws Exception;
}