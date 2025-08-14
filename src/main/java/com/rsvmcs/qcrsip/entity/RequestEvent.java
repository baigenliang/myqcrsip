package com.rsvmcs.qcrsip.entity;

import com.rsvmcs.qcrsip.core.SipProviderImpl;

import java.net.InetAddress;
import java.util.EventObject;

/**
 * 不持有 Socket，只有来源地址/端口/transport
 */
public class RequestEvent extends SipEvent {
    private final SipRequest request;
    private final ChannelRef channelRef; // 回送通道
    public RequestEvent(SipProviderImpl provider, SipRequest request, ChannelRef ref){
        super(provider);
        this.request = request;
        this.channelRef = ref;
    }
    public SipRequest getRequest(){ return request; }
    public ChannelRef getChannelRef(){ return channelRef; }
}