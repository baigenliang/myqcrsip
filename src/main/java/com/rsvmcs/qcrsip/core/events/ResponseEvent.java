package com.rsvmcs.qcrsip.core.events;


import com.rsvmcs.qcrsip.core.SipProvider;
import com.rsvmcs.qcrsip.core.model.SipResponse;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class ResponseEvent  {
    private final SipResponse response;
    public ResponseEvent(SipResponse response){ this.response = response; }
    public SipResponse getResponse(){ return response; }
}