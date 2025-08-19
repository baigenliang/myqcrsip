package com.rsvmcs.qcrsip.core;


import com.rsvmcs.qcrsip.core.events.RequestEvent;
import com.rsvmcs.qcrsip.core.events.ResponseEvent;
import com.rsvmcs.qcrsip.core.events.TimeoutEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 单一线程事件调度器，把事件发送给 SipListener
 */
public class EventScanner implements Runnable {
    public enum Kind { REQUEST, RESPONSE, TIMEOUT }

    public static class Item {
        public final Kind kind;
        public final Object event;
        public final SipProvider provider;
        public Item(Kind k, Object e, SipProvider p){kind=k;event=e;provider=p;}
    }

    private final BlockingQueue<Item> q = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public void offer(Item it){ q.offer(it); }
    public void stop(){ running=false; }

    @Override
    public void run() {
        while (running) {
            try {
                Item it = q.take();
                SipProvider p = it.provider;
                switch (it.kind) {
                    case REQUEST:  p.fireRequest((RequestEvent) it.event);  break;
                    case RESPONSE: p.fireResponse((ResponseEvent) it.event); break;
                    case TIMEOUT:  p.fireTimeout((TimeoutEvent) it.event);  break;
                }
            } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            catch (Throwable t) { t.printStackTrace(); }
        }
    }
}
