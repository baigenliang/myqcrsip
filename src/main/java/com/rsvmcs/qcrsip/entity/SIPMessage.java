package com.rsvmcs.qcrsip.entity;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基本的 SIP 消息序列化/反序列化（GB2312 body）
 */
public abstract class SIPMessage {
    public static final Charset BODY_CHARSET = Charset.forName("GB2312");

    protected final LinkedHashMap<String,String> headers = new LinkedHashMap<>();
    protected byte[] rawContent; // 原始 body 字节
    public abstract String encodeStartLine();

    public void setHeader(String name, String value){ headers.put(name, value); }
    public String getHeader(String name){ return headers.get(name); }
    public Map<String,String> getHeaders(){ return headers; }

    public void setRawContent(byte[] body){
        this.rawContent = body == null ? null : Arrays.copyOf(body, body.length);
        if (this.rawContent != null) setHeader("Content-Length", String.valueOf(this.rawContent.length));
        else setHeader("Content-Length","0");
    }
    public byte[] getRawContent(){ return rawContent; }

    public String encode(){
        StringBuilder sb = new StringBuilder();
        sb.append(encodeStartLine()).append("\r\n");
        for (Map.Entry<String,String> e : headers.entrySet()){
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        byte[] headBytes = sb.toString().getBytes(StandardCharsets.US_ASCII);
        if (rawContent == null || rawContent.length == 0) {
            return new String(headBytes, StandardCharsets.US_ASCII);
        } else {
            byte[] all = new byte[headBytes.length + rawContent.length];
            System.arraycopy(headBytes,0,all,0,headBytes.length);
            System.arraycopy(rawContent,0,all,headBytes.length,rawContent.length);
            return new String(all, StandardCharsets.ISO_8859_1); // 不丢字节（透明拼接）
        }
    }

    public long getCSeqNumber(){
        String cseq = headers.get("CSeq");
        if (cseq == null) return -1;
        String num = cseq.trim().split("\\s+")[0];
        try { return Long.parseLong(num); } catch (Exception e){ return -1; }
    }

    // === 解析 ===
    public static SIPMessage parse(byte[] data){
        try{
            int headerEnd = indexOfCrlfCrlf(data);
            if (headerEnd < 0) return null;

            String head = new String(data, 0, headerEnd, StandardCharsets.US_ASCII);
            Map<String,String> hdrs = new LinkedHashMap<>();
            String[] lines = head.split("\r\n");
            String startLine = lines[0];

            for (int i=1;i<lines.length;i++){
                String ln = lines[i];
                int idx = ln.indexOf(':');
                if (idx>0){
                    String k = ln.substring(0,idx).trim();
                    String v = ln.substring(idx+1).trim();
                    hdrs.put(k, v);
                }
            }
            int contentLen = 0;
            if (hdrs.containsKey("Content-Length")){
                try { contentLen = Integer.parseInt(hdrs.get("Content-Length")); } catch (Exception ignore){}
            }
            byte[] body = null;
            if (contentLen>0 && data.length >= headerEnd+4+contentLen){
                body = Arrays.copyOfRange(data, headerEnd+4, headerEnd+4+contentLen);
            }

            if (startLine.startsWith("SIP/2.0")) {
                // Response
                String[] parts = startLine.split("\\s+",3);
                int code = Integer.parseInt(parts[1]);
                String reason = parts.length>=3? parts[2] : "";
                SipResponse resp = new SipResponse(new StatusLine("SIP/2.0", code, reason));
                resp.headers.putAll(hdrs);
                resp.setRawContent(body);
                return resp;
            } else {
                // Request
                String[] parts = startLine.split("\\s+");
                String method = parts[0];
                String uri = parts[1];
                String ver = parts[2];
                SipRequest req = new SipRequest(new RequestLine(method, uri, ver));
                req.headers.putAll(hdrs);
                req.setRawContent(body);
                return req;
            }
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static int indexOfCrlfCrlf(byte[] a){
        for (int i=0;i<a.length-3;i++){
            if (a[i]==13 && a[i+1]==10 && a[i+2]==13 && a[i+3]==10) return i;
        }
        return -1;
    }
}