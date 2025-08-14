package com.rsvmcs.qcrsip.entity;

import com.rsvmcs.qcrsip.core.SipProviderImpl;

public abstract class SipEvent {
    protected final SipProviderImpl sourceProvider;
    public SipEvent(SipProviderImpl provider){ this.sourceProvider = provider; }
    public SipProviderImpl getSourceProvider(){ return sourceProvider; }
}
