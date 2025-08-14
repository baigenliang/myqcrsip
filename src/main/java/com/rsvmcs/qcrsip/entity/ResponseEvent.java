package com.rsvmcs.qcrsip.entity;


import com.rsvmcs.qcrsip.core.SipProviderImpl;

import java.net.InetAddress;
import java.util.EventObject;

public class ResponseEvent extends SipEvent {
    private final SipResponse response;
    private final ChannelRef channelRef;
    public ResponseEvent(SipProviderImpl provider, SipResponse response, ChannelRef ref){
        super(provider);
        this.response = response;
        this.channelRef = ref;
    }
    public SipResponse getResponse(){ return response; }
    public ChannelRef getChannelRef(){ return channelRef; }
}