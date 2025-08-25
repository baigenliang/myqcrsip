package com.rsvmcs.qcrsip.core.io;

import com.rsvmcs.qcrsip.core.EventScanner;
import com.rsvmcs.qcrsip.core.SipProvider;

import java.nio.channels.DatagramChannel;

public abstract class MessageProcessor {
    protected final EventScanner scanner;
    protected SipProvider provider;

    protected MessageProcessor() {
        this.scanner = new EventScanner();
        new Thread(scanner, "EventScanner").start();
    }
    public void setProvider(SipProvider provider){ this.provider = provider; }

    public abstract void start() throws Exception;
    public abstract void stop() throws Exception;
}
