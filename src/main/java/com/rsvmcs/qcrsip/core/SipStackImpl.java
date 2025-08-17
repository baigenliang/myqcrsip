package com.rsvmcs.qcrsip.core;


import com.rsvmcs.qcrsip.core.io.MessageProcessor;
import com.rsvmcs.qcrsip.core.io.TCPMessageProcessor;
import com.rsvmcs.qcrsip.core.io.UDPMessageProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * createListeningPoint / createSipProvider.
 * Starts UDP server or TCP acceptor; UDP/TCP server threads call provider.deliverRawMessage / provider.connManager path.
 */

public class SipStackImpl implements SipStack {
    private final Map<ListeningPoint, Object> processors = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    @Override
    public ListeningPoint createListeningPoint(String ip, int port, String transport) {
        return new ListeningPoint(ip, port, transport);
    }

    @Override
    public SipProvider createSipProvider(ListeningPoint lp) throws Exception {
        if (!running) throw new IllegalStateException("Start SipStack before createSipProvider()");
        if (lp == null) throw new IllegalArgumentException("ListeningPoint is null");
        Object proc = processors.computeIfAbsent(lp, k -> {
            try {
                if ("TCP".equalsIgnoreCase(lp.getTransport())) {
                    TCPMessageProcessor p = new TCPMessageProcessor(lp);
                    p.start();
                    return p;
                } else if ("UDP".equalsIgnoreCase(lp.getTransport())) {
                    UDPMessageProcessor p = new UDPMessageProcessor(lp);
                    p.start();
                    return p;
                } else {
                    throw new IllegalArgumentException("Unsupported transport: " + lp.getTransport());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return new SipProviderImpl(lp, this, proc);
    }

    @Override
    public void start() { running = true; }

    @Override
    public void stop() {
        running = false;
        processors.values().forEach(p -> {
            try {
                if (p instanceof TCPMessageProcessor) ((TCPMessageProcessor) p).stop();
                if (p instanceof UDPMessageProcessor) ((UDPMessageProcessor) p).stop();
            } catch (Exception ignored) {}
        });
        processors.clear();
    }

    /* package */ Object getProcessor(ListeningPoint lp) {
        return processors.get(lp);
    }
}
