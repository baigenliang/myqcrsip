package com.rsvmcs.qcrsip.core;


import com.rsvmcs.qcrsip.core.events.RequestEvent;
import com.rsvmcs.qcrsip.core.events.ResponseEvent;
import com.rsvmcs.qcrsip.core.events.TimeoutEvent;
import com.rsvmcs.qcrsip.core.model.Request;
import com.rsvmcs.qcrsip.core.model.Response;
import com.rsvmcs.qcrsip.core.model.SipRequest;
import com.rsvmcs.qcrsip.core.model.SipResponse;

import java.net.InetSocketAddress;
public interface SipProvider {
    void addSipListener(SipListener l);
    void removeSipListener(SipListener l);
    SipListener getSipListener();

    ListeningPoint getListeningPoint();
    void sendRequest(SipRequest request) throws Exception;
    void sendResponse(SipResponse response) throws Exception;

    // 供处理器投递事件
    void fireRequest(RequestEvent ev);
    void fireResponse(ResponseEvent ev);
    void fireTimeout(TimeoutEvent ev);
}