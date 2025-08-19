package com.rsvmcs.qcrsip.test;

import com.rsvmcs.qcrsip.core.*;
import com.rsvmcs.qcrsip.core.events.RequestEvent;
import com.rsvmcs.qcrsip.core.events.ResponseEvent;
import com.rsvmcs.qcrsip.core.events.TimeoutEvent;
import com.rsvmcs.qcrsip.core.model.*;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

public class DemoMainPro {
    public static void main(String[] args) throws Exception {
//        SipStack stack = new SipStackImpl();
//
//        // 启动 TCP & UDP 监听（本机 127.0.0.1:5060）
//        ListeningPoint tcpLP = stack.createListeningPoint("127.0.0.1", 5060, "TCP");
//        ListeningPoint udpLP = stack.createListeningPoint("127.0.0.1", 5060, "UDP");
//        SipProvider tcpProvider = stack.createSipProvider(tcpLP);
//        SipProvider udpProvider = stack.createSipProvider(udpLP);
//
//        // 业务层 listener
//        SipListener listener = new SipListener() {
//            @Override
//            public void processRequest(RequestEvent requestEvent) {
//                System.out.println("[Biz] processRequest\n" + requestEvent.getRequest().encode());
//                // 构造 200 OK 响应回发
//                SipRequest req = requestEvent.getRequest();
//                SipResponse resp = new SipResponse();
//                resp.setResponseLine(new StatusLine("SIP/2.0", 200, "OK"));
//                LinkedHashMap<String,String> h = new LinkedHashMap<>();
//                h.put("Via", req.getHeader("Via"));
//                h.put("To", req.getHeader("To"));
//                h.put("From", req.getHeader("From"));
//                h.put("Call-ID", req.getHeader("Call-ID"));
//                h.put("CSeq", req.getHeader("CSeq"));
//                h.put("Content-Type", "RVSS/xml");
//                String body = "<?xml version=\"1.0\" encoding=\"GB2312\" standalone=\"yes\"?>\r\n" +
//                        "<response command=\"Echo\"><result code=\"0\">ok</result><parameters/></response>";
//                resp.setHeaders(h);
//                resp.setBody(body.getBytes(SIPMessage.GB2312));
//                try {
//                    requestEvent.getProvider().sendResponse(resp);
//                } catch (Exception e) { e.printStackTrace(); }
//            }
//            @Override public void processResponse(ResponseEvent responseEvent) {
//                System.out.println("[Biz] processResponse\n" + responseEvent.getResponse().encode());
//            }
//            @Override public void processTimeout(TimeoutEvent timeoutEvent) {
//                System.out.println("[Biz] timeout: " + timeoutEvent);
//            }
//        };
//
//        ((SipProviderImpl) tcpProvider).addSipListener(listener);
//        ((SipProviderImpl) udpProvider).addSipListener(listener);
//
//        // 启动栈
//        ((SipStackImpl) stack).startProvider(tcpProvider);
//        ((SipStackImpl) stack).startProvider(udpProvider);
//
//        // 给自己发一个 TCP INVITE（演示主动发送 + 回收响应）
//        SipRequest invite = new SipRequest();
//        invite.setRequestLine(new RequestLine("INVITE", "sip:dummy SIP/2.0")); // URI 不作约束
//        LinkedHashMap<String,String> headers = new LinkedHashMap<>();
//        headers.put("Via", "SIP/2.0/TCP 127.0.0.1:5060");
//        headers.put("To", "<sip:to@local>");
//        headers.put("From", "<sip:from@local>");
//        headers.put("Max-Forwards", "70");
//        headers.put("Call-ID", "10001");
//        headers.put("CSeq", "101 INVITE");
//        headers.put("Content-Type", "RVSS/xml");
//        invite.setHeaders(headers);
//        String xml = "<?xml version=\"1.0\" encoding=\"GB2312\" standalone=\"yes\"?>\r\n" +
//                "<request command=\"Ping\"><parameters/></request>";
//        invite.setBody(xml.getBytes(SIPMessage.GB2312));
//
//        // 显式指定目标地址，不从 Request-URI 解析
//       InetSocketAddress loop = new InetSocketAddress("10.120.5.185", 5061);
//      ((SipProviderImpl) tcpProvider).sendRequest(invite, loop);
//
//        // 给自己发一个 UDP INVITE
//        SipRequest inviteUdp = invite.cloneShallow();
//        inviteUdp.getHeaders().put("Via", "SIP/2.0/UDP 127.0.0.1:5061");
//         ((SipProviderImpl) udpProvider).sendRequest(inviteUdp, loop);
//
//        System.out.println("Demo running. Use tcp/udp tool to send to 127.0.0.1:5061.");
//        Thread.sleep(20_000);
//        ((SipStackImpl) stack).stopProvider(tcpProvider);
//        ((SipStackImpl) stack).stopProvider(udpProvider);


         // SipProvider tcpProv=  SIPSender.init();
        SipStack stack = new SipStackImpl();
        try {
            stack.start();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        ListeningPoint tcpLP = stack.createListeningPoint("10.120.5.185", 5060, "TCP");
        SipProvider tcpProv = stack.createSipProvider(tcpLP);

        String iplocal=tcpProv.getListeningPoint().getIp();
         // 业务监听器
          tcpProv.addSipListener(new SipListener() {
            @Override public void processRequest(RequestEvent e) {
                System.out.println("[Biz] processRequest:\n" + e.getRequest().encode());

               String aa= e.getRequest().getHost();
               InetSocketAddress inetSocketAddresse=e.getRequest().getLocalAddress();
               InetSocketAddress inetSocketAddressRemote=e.getRequest().getExplicitRemote();

               String transport= getTransportFromMessage(e.getRequest());

                // 构造 200 OK
                SipResponse ok = new SipResponse(new StatusLine("SIP/2.0",200,"OK"));
                ok.setHeader("Via", e.getRequest().getHeader("Via"));
                ok.setHeader("To",   e.getRequest().getHeader("To"));
                ok.setHeader("From", e.getRequest().getHeader("From"));
                ok.setHeader("Call-ID", e.getRequest().getHeader("Call-ID"));
                ok.setHeader("CSeq", e.getRequest().getHeader("CSeq"));
                ok.setHeader("Content-Type", "RVSS/xml");
                String body = "<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n<response command=\"Ack\"><result code=\"0\">ok</result></response>";
                ok.setBody(body.getBytes(SIPMessage.BODY_CHARSET));

                // 沿原通道回发（TCP）
                 if (e.getTcpChannel()!=null) ok.setReplyChannel(e.getTcpChannel());
                // 如果是 UDP：
                 if(e.getUdpPeer()!=null) ok.setUdpPeer(e.getUdpPeer());



                try { tcpProv.sendResponse(ok); } catch (Exception ex) { ex.printStackTrace(); }
            }
            @Override public void processResponse(ResponseEvent responseEvent) {
                System.out.println("[Biz] processResponse:\n " + responseEvent.getResponse().encode());

                System.out.println("[Biz] processResponse: headers\n " + responseEvent.getResponse().getHeaders());
                System.out.println("[Biz] processResponse: bodys\n " + new String(responseEvent.getResponse().getBody()));

            }
            @Override public void processTimeout(TimeoutEvent timeoutEvent) {
                System.out.println("[Biz] timeout: " + timeoutEvent.getInfo());
            }
        });




        // 创建 HttpServer，监听 8080 端口
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        // 注册处理器
        server.createContext("/test", new MyHandler(tcpProv));
        // 启动服务
        server.setExecutor(null); // 使用默认线程池
        server.start();
        System.out.println("HTTP Server started on http://localhost:8080/test");


// 等待对端连接/发包，然后演示主动发一个 INVITE
      //  Thread.sleep(1500);
//        SipRequest req = new SipRequest(new RequestLine("INVITE","sip:127.0.0.1:5060","SIP/2.0"));
//        req.setHeader("Via","SIP/2.0/TCP 127.0.0.1");
//        req.setHeader("To","<sip:to@127.0.0.1>");
//        req.setHeader("From","<sip:from@127.0.0.1>");
//        req.setHeader("Call-ID","abc-123");
//        req.setHeader("CSeq","1 INVITE");
//        req.setHeader("Content-Type","RVSS/xml");
//        String xml = "<?xml version=\"1.0\" encoding=\"GB2312\"?><request command=\"Ping\"><parameters/></request>";
//        req.setBody(xml.getBytes(SIPMessage.BODY_CHARSET));
//        req.setTransport("TCP");
//        req.setHost("127.0.0.1");
//        req.setPort(5061);
//// 或者明确指定远端（不从 URI 解析）：req.setExplicitRemote(new InetSocketAddress("127.0.0.1",5060));
//
//        tcpProv.sendRequest(req);

    }

    public static String getTransportFromMessage(SIPMessage message) {
        String via = message.getHeader("Via");
        if (via == null) {
            return null;
        }
        // Via 格式：SIP/2.0/UDP host:port;params
        //          SIP/2.0/TCP host:port;params
        // 所以只要取 "SIP/2.0/" 后面的部分直到空格即可
        int idx = via.indexOf("SIP/2.0/");
        if (idx >= 0) {
            String after = via.substring(idx + "SIP/2.0/".length()).trim();
            int spaceIdx = after.indexOf(' ');
            if (spaceIdx > 0) {
                return after.substring(0, spaceIdx).toUpperCase(); // 结果是 "UDP" / "TCP" / "TLS" 等
            } else {
                return after.toUpperCase();
            }
        }
        return null;
    }
}
