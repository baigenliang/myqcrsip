package com.rsvmcs.qcrsip.core;


import com.rsvmcs.qcrsip.core.stack.TCPMessageProcessor;
import com.rsvmcs.qcrsip.core.stack.UDPMessageProcessor;
import com.rsvmcs.qcrsip.entity.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * createListeningPoint / createSipProvider.
 * Starts UDP server or TCP acceptor; UDP/TCP server threads call provider.deliverRawMessage / provider.connManager path.
 */

public class SipStackImpl implements SipStack {

    private final Map<String, ListeningPoint> lps = new ConcurrentHashMap<>();
    private final Map<String, SipProviderImpl> providers = new ConcurrentHashMap<>();
    private final List<TCPMessageProcessor> tcpProcessors = Collections.synchronizedList(new ArrayList<>());
    private final List<UDPMessageProcessor> udpProcessors = Collections.synchronizedList(new ArrayList<>());
    private final EventScanner scanner;

    public SipStackImpl(EventScanner scanner){ this.scanner = scanner; }

    @Override
    public ListeningPoint createListeningPoint(String ip, int port, String transport){
        ListeningPoint lp = new ListeningPoint(ip, port, transport);
        lps.put(lp.key(), lp);
        return lp;
    }

    @Override
    public SipProvider createSipProvider(ListeningPoint lp) throws Exception {
        SipProviderImpl p = new SipProviderImpl(this, lp, scanner);
        providers.put(lp.key(), p);
        // 立即创建对应的接收器
        if ("TCP".equalsIgnoreCase(lp.getTransport())) {
            TCPMessageProcessor proc = new TCPMessageProcessor(p);
            tcpProcessors.add(proc);
            proc.start(lp.getIp(), lp.getPort());
        } else {
            UDPMessageProcessor proc = new UDPMessageProcessor(p);
            udpProcessors.add(proc);
            proc.start(lp.getIp(), lp.getPort());
        }
        return p;
    }

    @Override
    public void start(){ /* 处理器在 createSipProvider 时已经启动 */ }

    @Override
    public void stop() {
        tcpProcessors.forEach(TCPMessageProcessor::stop);
        udpProcessors.forEach(UDPMessageProcessor::stop);
        providers.values().forEach(SipProviderImpl::shutdown);
    }
}