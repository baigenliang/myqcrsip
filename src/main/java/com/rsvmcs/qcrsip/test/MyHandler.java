package com.rsvmcs.qcrsip.test;

import com.rsvmcs.qcrsip.core.ListeningPoint;
import com.rsvmcs.qcrsip.core.SipProvider;
import com.rsvmcs.qcrsip.core.SipStack;
import com.rsvmcs.qcrsip.core.SipStackImpl;
import com.rsvmcs.qcrsip.core.model.RequestLine;
import com.rsvmcs.qcrsip.core.model.SIPMessage;
import com.rsvmcs.qcrsip.core.model.SipRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class MyHandler implements HttpHandler {

    private final SipProvider sipProvider;

    // 构造函数注入额外参数
    public MyHandler(SipProvider sipProvider) {
        this.sipProvider = sipProvider;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) query = "default";

        // 调用业务逻辑
        try {
            SipRequest req = new SipRequest(new RequestLine("INVITE", "sip:127.0.0.1:5060", "SIP/2.0"));
            req.setHeader("Via", "SIP/2.0/TCP 127.0.0.1");
            req.setHeader("To", "<sip:to@127.0.0.1>");
            req.setHeader("From", "<sip:from@127.0.0.1>");
            req.setHeader("Call-ID", "abc-123");
            req.setHeader("CSeq", "1 INVITE");
            req.setHeader("Content-Type", "RVSS/xml");
            // String xml = "<?xml version=\"1.0\" encoding=\"GB2312\"?><request command=\"Ping\"><parameters/></request>";
//            String xml="<?xml version=\"1.0\" encoding=\"gb2312\"?>\n"+
//                    "<request command=\"SaRegister\">\n" +
//                    "    <parameters>\n" +
//                    "        <expires>300</expires>\n" +
//                    "        <manufacturerId>QCR-1.33-1.38</manufacturerId>\n" +
//                    "        <manufacturerName>下级QCR</manufacturerName>\n" +
//                    "        <productVersion>1.33</productVersion>\n" +
//                    "        <saId>6400003010000030</saId>\n" +
//                    "        <saIp>10.120.5.30</saIp>\n" +
//                    "        <saName>6400003010000030</saName>\n" +
//                    "        <saPassword>d41d8cd98f00b204e9800998ecf8427e</saPassword>\n" +
//                    "        <saSipPort>5068</saSipPort>\n" +
//                    "        <softwareVersion>1.38</softwareVersion>\n" +
//                    "    </parameters>\n" +
//                    "</request>\n";
            String xml = "<?xml version=\"1.0\" encoding=\"GB2312\" standalone=\"yes\"?>\n" +
                    "<request command=\"GetCuUserId\">\n" +
                    "    <parameters>\n" +
                    "        <cuUserName>sbqx</cuUserName>\n" +
                    "        <cuMac></cuMac>\n" +
                    "    </parameters>\n" +
                    "</request>\n";
            req.setBody(xml.getBytes(SIPMessage.BODY_CHARSET));
            int lenghtStr = xml.length();
            Integer lengthStr2 = req.getBody().length;
            req.setTransport("TCP");
            req.setHost("10.120.5.190");
            req.setPort(5061);
            // 或者明确指定远端（不从 URI 解析）：req.setExplicitRemote(new InetSocketAddress("127.0.0.1",5060));
            sipProvider.sendRequest(req);
        } catch (Exception e) {

        }

        String response = "sip消息发送完成";
        // 返回响应
        exchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}