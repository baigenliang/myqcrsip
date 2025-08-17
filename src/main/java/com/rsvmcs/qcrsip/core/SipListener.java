package com.rsvmcs.qcrsip.core;


import com.rsvmcs.qcrsip.core.events.RequestEvent;
import com.rsvmcs.qcrsip.core.events.ResponseEvent;
import com.rsvmcs.qcrsip.core.events.TimeoutEvent;

public interface SipListener {
    void processRequest(RequestEvent requestEvent);
    void processResponse(ResponseEvent responseEvent);
    void processTimeout(TimeoutEvent timeoutEvent);
}