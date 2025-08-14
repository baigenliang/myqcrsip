package com.rsvmcs.qcrsip.test;

import com.rsvmcs.qcrsip.core.EventScanner;
import com.rsvmcs.qcrsip.core.SipProviderImpl;
import com.rsvmcs.qcrsip.core.SipStackImpl;
import com.rsvmcs.qcrsip.entity.*;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class DemoMainPro {
    public static void main(String[] args) throws Exception {
        EventScanner scanner = new EventScanner();
        Thread es = new Thread(scanner, "EventScanner"); es.start();

        SipStack stack = new SipStackImpl(scanner);


        // 建立 TCP & UDP 监听
        ListeningPoint tcpLP = stack.createListeningPoint("0.0.0.0", 5060, "TCP");
        ListeningPoint udpLP = stack.createListeningPoint("0.0.0.0", 5060, "UDP");

        SipProviderImpl tcpProvider = (SipProviderImpl) stack.createSipProvider(tcpLP);
        SipProviderImpl udpProvider = (SipProviderImpl) stack.createSipProvider(udpLP);

        // 业务层 listener
        SipListener appListener = new SipListener() {
            @Override
            public void processRequest(RequestEvent requestEvent) {
                SipRequest req = requestEvent.getRequest();
                System.out.println("<<< Request received:\n" + req.encode());

                // 自动返回 200 OK（保持相同 CSeq）
                try {
                    SipResponse ok = create200OkFrom(req, "GetCuUserId");
                    requestEvent.getSourceProvider().sendResponse(ok);
                    System.out.println(">>> 200 OK sent for CSeq=" + ok.getCSeqNumber());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void processResponse(ResponseEvent responseEvent) {
                SipResponse resp = responseEvent.getResponse();
                System.out.println("<<< Response received:\n" + resp.encode());
            }

            @Override
            public void processTimeout(TimeoutEvent timeoutEvent) {
                System.out.println("!!! Timeout CSeq=" + timeoutEvent.getCseq());
            }

            private SipResponse create200OkFrom(SipRequest req, String cmd){
                long cseq = req.getCSeqNumber();
                SipResponse resp = new SipResponse(new StatusLine("SIP/2.0", 200, "OK"));
                resp.setHeader("Via", req.getHeader("Via"));
                resp.setHeader("To", req.getHeader("To"));
                resp.setHeader("From", req.getHeader("From"));
                resp.setHeader("Call-ID", req.getHeader("Call-ID"));
                resp.setHeader("CSeq", cseq + " INVITE");
                resp.setHeader("Content-Type", "RVSS/xml");
                String xml = "<?xml version=\"1.0\" encoding=\"GB2312\" standalone=\"yes\"?>\r\n"
                        + "<response command=\"" + cmd + "\">\r\n"
                        + "  <result code=\"0\">success</result>\r\n"
                        + "  <parameters/>\r\n"
                        + "</response>";
                resp.setRawContent(xml.getBytes(SIPMessage.BODY_CHARSET));
                return resp;
            }
        };

        tcpProvider.addSipListener(appListener);
        udpProvider.addSipListener(appListener);

        // ==== 主动发送一个 TCP INVITE（目标按你设备改 host/port）====
        InetSocketAddress remoteAddr = new InetSocketAddress("10.120.5.185", 5061);
        SipRequest inviteTcp = createInvite("sip:10.120.5.185:5061", 101, "GetCuUserId");
       // tcpProvider.sendRequest(inviteTcp);
        tcpProvider.sendRequest(inviteTcp,remoteAddr);

        // ==== 主动发送一个 UDP INVITE ====
        SipRequest inviteUdp = createInviteUDP("sip:10.120.5.185:5062", 102, "CuRegister");
        InetSocketAddress remoteAddr2 = new InetSocketAddress("10.120.5.185", 5062);
        udpProvider.sendRequest(inviteUdp,remoteAddr2);

        // 运行一段时间观察
        TimeUnit.MINUTES.sleep(10);
//        ((SipStackImpl)stack).stop();
//        scanner.stop();
    }

    private static SipRequest createInvite(String reqUri, long cseq, String cmd){
        RequestLine rl = new RequestLine("INVITE", reqUri, "SIP/2.0");
        SipRequest req = new SipRequest(rl);
        req.setHeader("Via", reqUri.contains("UDP") ? "SIP/2.0/UDP 127.0.0.1" : "SIP/2.0/TCP 127.0.0.1");
        req.setHeader("To", "<sip:to@local>");
        req.setHeader("From", "<sip:from@local>");
        req.setHeader("Max-Forwards", "70");
        req.setHeader("Call-ID", "call-12345");
        req.setHeader("CSeq", cseq + " INVITE");
        req.setHeader("Content-Type", "RVSS/xml");
        String xml = "<?xml version=\"1.0\" encoding=\"GB2312\" standalone=\"yes\"?>\r\n"
                + "<request command=\"" + cmd + "\">\r\n"
                + "  <parameters>\r\n"
                + "    <cuUserName>sbqx</cuUserName>\r\n"
                + "  </parameters>\r\n"
                + "</request>";
        req.setRawContent(xml.getBytes(SIPMessage.BODY_CHARSET));
        return req;
    }
    private static SipRequest createInviteUDP(String reqUri, long cseq, String cmd){
        RequestLine rl = new RequestLine("INVITE", reqUri, "SIP/2.0");
        SipRequest req = new SipRequest(rl);
        req.setHeader("Via", "SIP/2.0/UDP 127.0.0.1");
        req.setHeader("To", "<sip:to@local>");
        req.setHeader("From", "<sip:from@local>");
        req.setHeader("Max-Forwards", "70");
        req.setHeader("Call-ID", "call-12345");
        req.setHeader("CSeq", cseq + " INVITE");
        req.setHeader("Content-Type", "RVSS/xml");
        String xml = "<?xml version=\"1.0\" encoding=\"GB2312\" standalone=\"yes\"?>\r\n"
                + "<request command=\"" + cmd + "\">\r\n"
                + "  <parameters>\r\n"
                + "    <cuUserName>sbqx</cuUserName>\r\n"
                + "  </parameters>\r\n"
                + "</request>";
        req.setRawContent(xml.getBytes(SIPMessage.BODY_CHARSET));
        return req;
    }
}
