package com.rsvmcs.qcrsip.core.model;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基本的 SIP 消息序列化/反序列化（GB2312 body）
 */
public abstract class SIPMessage {
    public static final String CRLF = "\r\n";
    public static final Charset BODY_CHARSET = Charset.forName("GB2312");

    protected final LinkedHashMap<String,String> headers = new LinkedHashMap<>();
    protected byte[] body;

    public Map<String,String> getHeaders(){ return headers; }
    public void setHeader(String k, String v){ headers.put(k,v); }
    public String getHeader(String k){ return headers.get(k); }
    public byte[] getBody(){ return body; }
    public void setBody(byte[] body){ this.body = body; }

    private InetSocketAddress localAddress; // 与客户端发送或者接收通信消息时本地地址

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

//    private InetSocketAddress remoteAddress;  // 来源地址
//
//    public void setRemoteAddress(InetSocketAddress addr) {
//        this.remoteAddress = addr;
//    }
//    public InetSocketAddress getRemoteAddress() {
//        return remoteAddress;
//    }

    public abstract String firstLine();

    public String encode(){
        StringBuilder sb = new StringBuilder();
        sb.append(firstLine()).append(CRLF);
        if (body != null) headers.put("Content-Length", String.valueOf(body.length));
        for (Map.Entry<String,String> e: headers.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append(CRLF);
        }
        sb.append(CRLF);
        if (body == null) return sb.toString();
        return sb.toString() + new String(body, BODY_CHARSET);
    }

    public static class Frame { public final byte[] bytes; public Frame(byte[] b){this.bytes=b;} }

    /** 宽松帧提取：无 Content-Length → 视为 0；允许只出现 CRLFCRLF（keepalive），由 parse() 处理为 null */
    public static Frame tryExtractFrame(ByteBuffer buf) {
        int start = buf.position();
        int end = -1;
        for (int i = start; i + 3 < buf.limit(); i++) {
            if (buf.get(i) == '\r' && buf.get(i+1) == '\n' && buf.get(i+2) == '\r' && buf.get(i+3) == '\n') {
                end = i; break;
            }
        }
        if (end < 0) return null;

        byte[] headBytes = new byte[end - start];
        buf.get(headBytes);
        buf.position(buf.position() + 4); // skip CRLFCRLF

        String head = new String(headBytes, java.nio.charset.StandardCharsets.US_ASCII);
        int contentLen = 0;
        for (String line : head.split("\r\n")) {
            int idx = line.indexOf(':');
            if (idx > 0 && line.substring(0, idx).trim().equalsIgnoreCase("Content-Length")) {
                try { contentLen = Integer.parseInt(line.substring(idx + 1).trim()); } catch (Exception ignored) {}
                break;
            }
        }
        if (buf.remaining() < contentLen) {
            // 回退，等待更多字节
            buf.position(start);
            return null;
        }
        byte[] body = new byte[contentLen];
        buf.get(body);

        byte[] all = new byte[headBytes.length + 4 + contentLen];
        System.arraycopy(headBytes, 0, all, 0, headBytes.length);
        all[headBytes.length] = '\r'; all[headBytes.length+1] = '\n';
        all[headBytes.length+2] = '\r'; all[headBytes.length+3] = '\n';
        System.arraycopy(body, 0, all, headBytes.length + 4, contentLen);
        return new Frame(all);
    }

    /** 宽松解析：允许首部前出现空行；若首行为空（keepalive）返回 null；不对 URI 施加任何约束 */
    public static SIPMessage parse(String text){
        if (text == null) return null;
        String[] parts = text.split("\r\n\r\n", 2);
        String head = parts.length > 0 ? parts[0] : "";
        String bodyStr = parts.length > 1 ? parts[1] : "";

        // 找到第一个非空的首行
        String[] rawLines = head.split("\r\n");
        String first = null;
        int firstIdx = -1;
        for (int i = 0; i < rawLines.length; i++) {
            if (rawLines[i] != null && !rawLines[i].trim().isEmpty()) { first = rawLines[i].trim(); firstIdx = i; break; }
        }
        if (first == null) {
            // 纯 CRLFCRLF / keepalive，忽略
            return null;
        }


        // 响应：首行以 SIP/ 开头；否则按请求处理（不校验 token 数量）
        if (first.toUpperCase().startsWith("SIP/")) {
            StatusLine sl = StatusLine.parse(first);
            SipResponse r = new SipResponse(sl);
            for (int i = firstIdx + 1; i < rawLines.length; i++) {
                String line = rawLines[i];
                if (line == null || line.isEmpty()) continue;
                int idx = line.indexOf(':'); if (idx <= 0) continue;
                r.setHeader(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
            if (!bodyStr.isEmpty()) r.setBody(bodyStr.getBytes(BODY_CHARSET));
            return r;
        } else {
            RequestLine rl = RequestLine.parse(first);
            SipRequest req = new SipRequest(rl);
            for (int i = firstIdx + 1; i < rawLines.length; i++) {
                String line = rawLines[i];
                if (line == null || line.isEmpty()) continue;
                int idx = line.indexOf(':'); if (idx <= 0) continue;
                req.setHeader(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
            if (!bodyStr.isEmpty()) req.setBody(bodyStr.getBytes(BODY_CHARSET));
            // 不从 URI 推导 host/port/transport，完全交由业务层决定
            return req;
        }
    }
}