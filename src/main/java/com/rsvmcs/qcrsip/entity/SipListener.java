package com.rsvmcs.qcrsip.entity;


public interface SipListener {
    void processRequest(RequestEvent requestEvent);
    void processResponse(ResponseEvent responseEvent);
    void processTimeout(TimeoutEvent timeoutEvent);
}