package com.rsvmcs.qcrsip.core;


import com.rsvmcs.qcrsip.entity.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * 单一线程事件调度器，把事件发送给 SipListener
 */
public class EventScanner implements Runnable {
    private final BlockingQueue<SipEvent> q = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public void add(SipEvent e){ q.offer(e); }
    public void stop(){ running = false; }

    @Override public void run() {
        while(running){
            try {
                SipEvent ev = q.take();
                // provider 层做最终分发（保持你“transport→scanner→provider→listeners”的路径）
                ev.getSourceProvider().dispatchIncoming(ev);
            } catch (InterruptedException ie){
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t){
                t.printStackTrace();
            }
        }
    }
}
