package com.rsvmcs.qcrsip.core.events;

import com.rsvmcs.qcrsip.core.SipProviderImpl;
import java.util.EventObject;


public class  TimeoutEvent {
    private final String info;
    public TimeoutEvent(String info){ this.info=info; }
    public String getInfo(){ return info; }
}