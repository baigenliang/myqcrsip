package com.rsvmcs.qcrsip.entity;

import com.rsvmcs.qcrsip.core.SipProviderImpl;

import java.util.EventObject;


public class  TimeoutEvent extends SipEvent {
    private final long cseq;
    public TimeoutEvent(SipProviderImpl provider, long cseq){
        super(provider); this.cseq = cseq;
    }
    public long getCseq(){ return cseq; }
}