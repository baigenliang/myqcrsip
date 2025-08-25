package com.rsvmcs.qcrsip.core.model;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

    private InetSocketAddress explicitRemote; //只设置接收的请求和响应，主动向外发送的请求和响应未设置(需要手动设置与设置host和port是一样的)

    public void setExplicitRemote(InetSocketAddress r){ this.explicitRemote=r; }
    public InetSocketAddress getExplicitRemote(){ return explicitRemote; }

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

//    public String encode(){
//        StringBuilder sb = new StringBuilder();
//        sb.append(firstLine()).append(CRLF);
//        if (body != null) headers.put("Content-Length", String.valueOf(body.length));
//        for (Map.Entry<String,String> e: headers.entrySet()) {
//            sb.append(e.getKey()).append(": ").append(e.getValue()).append(CRLF);
//        }
//        sb.append(CRLF);
//        if (body == null) return sb.toString();
//        return sb.toString() + new String(body, BODY_CHARSET);
//    }

    /**
     * 统一发送内容中的换行符，目前手动拼接的可能只有\n（应该是采用的linux的方式）
     * @return
     */
    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(firstLine()).append(CRLF);

        // 计算包含所有换行符的完整消息体长度
        if (body != null) {
            // 将body中的换行符统一为CRLF
            String bodyStr = new String(body, BODY_CHARSET);
            bodyStr = bodyStr.replace("\r\n", "\n").replace("\n", CRLF);
            byte[] normalizedBody = bodyStr.getBytes(BODY_CHARSET);
            headers.put("Content-Length", String.valueOf(normalizedBody.length));
//          System.out.println("Body bytes length: " + body.length);
//          System.out.println("Body string re-encoded length: " + bodyStr.getBytes(BODY_CHARSET).length);

            for (Map.Entry<String,String> e: headers.entrySet()) {
                sb.append(e.getKey()).append(": ").append(e.getValue()).append(CRLF);
            }
            sb.append(CRLF);

            return sb.toString() + bodyStr;
        } else {
            for (Map.Entry<String,String> e: headers.entrySet()) {
                sb.append(e.getKey()).append(": ").append(e.getValue()).append(CRLF);
            }
            sb.append(CRLF);
            return sb.toString();
        }
    }

    public static class Frame { public final byte[] bytes; public Frame(byte[] b){this.bytes=b;} }

    /** 宽松帧提取：无 Content-Length → 视为 0；允许只出现 CRLFCRLF（keepalive），由 parse() 处理为 null */
    //public static Frame tryExtractFrame(ByteBuffer buf) {
//        int start = buf.position();
//        int end = -1;
//        for (int i = start; i + 3 < buf.limit(); i++) {
//            if (buf.get(i) == '\r' && buf.get(i+1) == '\n' && buf.get(i+2) == '\r' && buf.get(i+3) == '\n') {
//                end = i; break;
//            }
//        }
//        if (end < 0) return null;
//
//       byte[] headBytes = new byte[end - start];
//       buf.get(headBytes);
//       buf.position(buf.position() + 4); // skip CRLFCRLF
//
//        String head = new String(headBytes, java.nio.charset.StandardCharsets.US_ASCII);
//        int contentLen = 0;
//        for (String line : head.split("\r\n")) {
//            int idx = line.indexOf(':');
//            if (idx > 0 && line.substring(0, idx).trim().equalsIgnoreCase("Content-Length")) {
//                try { contentLen = Integer.parseInt(line.substring(idx + 1).trim()); } catch (Exception ignored) {}
//                break;
//            }
//        }
//        if (buf.remaining() < contentLen) {
//            // 回退，等待更多字节
//            buf.position(start);
//            return null;
//        }
//        byte[] body = new byte[contentLen];
//        buf.get(body);
//
//        byte[] all = new byte[headBytes.length + 4 + contentLen];
//        System.arraycopy(headBytes, 0, all, 0, headBytes.length);
//        all[headBytes.length] = '\r'; all[headBytes.length+1] = '\n';
//        all[headBytes.length+2] = '\r'; all[headBytes.length+3] = '\n';
//        System.arraycopy(body, 0, all, headBytes.length + 4, contentLen);
//        return new Frame(all);
//*******************************************方法2****************************************
//        int start = buf.position();
//        int limit = buf.limit();
//        int endOfHeader = -1;
//
//        // 找到 CRLFCRLF
//        for (int i = start; i + 3 < limit; i++) {
//            if (buf.get(i) == '\r' && buf.get(i + 1) == '\n' &&
//                    buf.get(i + 2) == '\r' && buf.get(i + 3) == '\n') {
//                endOfHeader = i + 4; // 包含 CRLFCRLF
//                break;
//            }
//        }
//
//        if (endOfHeader < 0) {
//            // header 不完整，等待更多字节
//            return null;
//        }
//
//        // 提取 header
//        byte[] headerBytes = new byte[endOfHeader - start];
//        for (int j = 0; j < headerBytes.length; j++) {
//            headerBytes[j] = buf.get(start + j);
//        }
//        String headerText = new String(headerBytes, StandardCharsets.US_ASCII);
//
//        // 解析 Content-Length
//        int contentLen = 0;
//        for (String line : headerText.split("\r\n")) {
//            int idx = line.indexOf(':');
//            if (idx > 0 && line.substring(0, idx).trim().equalsIgnoreCase("Content-Length")) {
//                try {
//                    contentLen = Integer.parseInt(line.substring(idx + 1).trim());
//                } catch (Exception ignored) {}
//                break;
//            }
//        }
//
//        // 总帧长度
//        int frameLen = (endOfHeader - start) + contentLen;
//
//        if (limit - start < frameLen) {
//            // body 还没收全
//            return null;
//        }
//
//        // === 到这里说明完整帧已经收齐 ===
//        byte[] all = new byte[frameLen];
//        buf.position(start);
//        buf.get(all); // 消耗这个 frame
//
//        return new Frame(all);
//    }

    /**
     * 修复方法3（粗暴+20获取真实body内容值后再返回frame
     */
//
//    public static String peekLine(ByteBuffer buf, int start) {
//        int pos = start;
//        StringBuilder sb = new StringBuilder();
//
//        while (pos < buf.limit()) {
//            byte b = buf.get(pos++);
//            if (b == '\r') {
//                if (pos < buf.limit() && buf.get(pos) == '\n') {
//                    pos++; // 跳过 \n
//                }
//                break;
//            } else if (b == '\n') {
//                break;
//            } else {
//                sb.append((char) b);
//            }
//        }
//
//        return sb.toString();
//    }
//
//    // 查找头部结束符 \r\n\r\n
//    private static int indexOfHeaderEnd(ByteBuffer buf, int start) {
//        for (int i = start; i < buf.limit() - 3; i++) {
//            if (buf.get(i) == '\r' && buf.get(i + 1) == '\n'
//                    && buf.get(i + 2) == '\r' && buf.get(i + 3) == '\n') {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    // 提取指定范围的字符串
//    private static String extractString(ByteBuffer buf, int start, int end, java.nio.charset.Charset cs) {
//        byte[] tmp = new byte[end - start];
//        int oldPos = buf.position();
//        buf.position(start);
//        buf.get(tmp);
//        buf.position(oldPos);
//        return new String(tmp, cs);
//    }
//
//    // 解析 Content-Length
//    private static int parseContentLength(String headerStr) {
//        for (String line : headerStr.split("\r\n")) {
//            if (line.toLowerCase().startsWith("content-length")) {
//                try {
//                    return Integer.parseInt(line.split(":")[1].trim());
//                } catch (Exception ignore) {}
//            }
//        }
//        return 0;
//    }
//
//    // 修正 Content-Length (按字符数计算)
//    private static int getFixedContentLength(String body, String charset) {
//        try {
//            return body.getBytes(charset).length;
//        } catch (Exception e) {
//            return body.length(); // fallback
//        }
//    }
//
//    public static Frame tryExtractFrame(ByteBuffer buf) {
//        int start = buf.position();
//
//        // 1. 找起始行
//        String line = peekLine(buf, start);
//        if (!(line.startsWith("INVITE sip:") || line.startsWith("SIP/2.0") || line.startsWith("REGISTER sip:"))) {
//            // 不是合法SIP起始行，丢弃
//            return null;
//        }
//
//        // 2. 找头部结束位置
//        int endOfHeader = indexOfHeaderEnd(buf, start);
//        if (endOfHeader == -1) {
//            return null; // 头还没接收完整
//        }
//
//        // 3. 解析头部，找到 Content-Length
//        String headerStr = extractString(buf, start, endOfHeader, StandardCharsets.US_ASCII);
//        int contentLen = parseContentLength(headerStr);
//
//        // 4. 修正 Content-Length (按字符实际长度算)
//        if (contentLen > 0) {
//            // 提取 body 部分 (注意 header 结束后跳过 \r\n\r\n)
//            int bodyStart = endOfHeader + 4;
//            if (buf.limit() > bodyStart) {
//                int available = buf.limit() - bodyStart;
//                int checkLen = Math.min(available, contentLen + 15); // 防御：多拿一点防止丢结尾,只有返回available的值才能确保返回读取的数据是完整的
//                byte[] tmp = new byte[checkLen];
//                buf.position(bodyStart);
//                buf.get(tmp);
//                String bodyStr = new String(tmp, StandardCharsets.UTF_8); // 先尝试 UTF-8，后面可以换 GB2312
//                int fixedLen = getFixedContentLength(bodyStr, "GB2312");
//
//                if (fixedLen != contentLen) {
//                    System.out.println("修正 Content-Length: 原=" + contentLen + " 实际=" + fixedLen);
//                    contentLen = fixedLen;
//                }
//                buf.position(start); // 回退
//            }
//        }
//
//        // 5. 计算整帧长度
//        int frameLen = (endOfHeader - start) + 4 + contentLen;
//        if (buf.limit() - start < frameLen) {
//            return null; // 包还没接收完整
//        }
//
//        // 6. 提取一个完整 Frame
//        byte[] frameBytes = new byte[frameLen];
//        buf.position(start);
//        buf.get(frameBytes);
//
////        Frame frame = new Frame();
////        frame.data = frameBytes;
////        frame.start = start;
////        frame.length = frameLen;
//
//        return new Frame(frameBytes);
//    }

    //支持粘包拆分处理和半包等待拼接处理
    public static Frame tryExtractFrame(ByteBuffer buf) {
        int start = buf.position();

        try {
            // 查找SIP报文头结束标记 "\r\n\r\n"
            int headerEndPos = -1;
            for (int i = start; i + 3 < buf.limit(); i++) {
                if (buf.get(i) == '\r' && buf.get(i + 1) == '\n'
                        && buf.get(i + 2) == '\r' && buf.get(i + 3) == '\n') {
                    headerEndPos = i;  // 找到第一个CR的位置
                    break;
                }
            }

            if (headerEndPos < 0) {
                return null; // 没有找到完整的头部
            }

            // 计算头部总长度（包括CRLFCRLF）
            int headerLength = headerEndPos - start + 4;

            // 解析Content-Length
            byte[] headerBytes = new byte[headerLength - 4]; // 不包含CRLFCRLF
            buf.get(headerBytes); // 读取头部内容
            buf.position(buf.position() + 4); // 跳过CRLFCRLF

            String headerStr = new String(headerBytes, StandardCharsets.US_ASCII);
            int contentLen = 0;

            // 使用正则表达式解析Content-Length
            Matcher m = Pattern.compile("Content-Length:\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
                    .matcher(headerStr);
            if (m.find()) {
                try {
                    contentLen = Integer.parseInt(m.group(1));
                } catch (NumberFormatException e) {
                    contentLen = 0;
                }
            }

            //如果发送端Content-Length正确下面逻辑是完全ok的
            // 检查是否有足够的消息体内容
//            if (buf.remaining() < contentLen) {
//                buf.position(start); // 回退到起始位置
//                return null;
//            }

            // 提取消息体
//            byte[] body = new byte[contentLen];
//            if (contentLen > 0) {
//                buf.get(body);
//            }

            // 构建完整报文
//            byte[] all = new byte[headerBytes.length + 4 + contentLen];
//            System.arraycopy(headerBytes, 0, all, 0, headerBytes.length);
//            all[headerBytes.length] = '\r';
//            all[headerBytes.length + 1] = '\n';
//            all[headerBytes.length + 2] = '\r';
//            all[headerBytes.length + 3] = '\n';
//            if (contentLen > 0) {
//                System.arraycopy(body, 0, all, headerBytes.length + 4, contentLen);
//            }
//
//            return new Frame(all);

             //这个逻辑是为了兼容客户端Content-Length不正确而适配的（windows下和linux下的换行符是不同的）
            if (buf.remaining() < contentLen) { //包含传入的不对和本事数据半包就少两种情况
//                // 正常半包情况，等待更多数据
//                buf.position(start);
//                return null;
                int originalContentLen=contentLen;
                // 可能是换行符不一致导致的长度差异，尝试小范围容错
                int tolerance = 20; // 允许10字节的差异
                if (buf.remaining() >= contentLen - tolerance && buf.remaining() <= contentLen + tolerance) {
                    // 调整contentLen以匹配实际数据
                    contentLen = buf.remaining();
                    System.out.println("WARN: Adjusted Content-Length from " +
                            originalContentLen + " to " + contentLen);

                    // 提取实际的消息体
                    byte[] body = new byte[contentLen];
                    buf.get(body);

                    // 构建完整报文（使用实际长度）
                    byte[] all = new byte[headerBytes.length + 4 + contentLen];
                    System.arraycopy(headerBytes, 0, all, 0, headerBytes.length);
                    all[headerBytes.length] = '\r';
                    all[headerBytes.length + 1] = '\n';
                    all[headerBytes.length + 2] = '\r';
                    all[headerBytes.length + 3] = '\n';
                    System.arraycopy(body, 0, all, headerBytes.length + 4, contentLen);

                    return new Frame(all);
                } else {
                    buf.position(start);
                    return null;
                }
            } else if (buf.remaining() > contentLen) {
                // 可能是换行符不一致导致的长度差异
                int extraBytes = buf.remaining() - contentLen;

                // 检查多余的字节是否是合理的换行符差异（通常是消息体行数 * 1）
                // 消息体通常有6-10行，所以差异在20字节内是合理的,当然如果粘包太多了这里也处理不了了，只能是针对单个完整包的适配，
                //所以最好还是从根源上保证发送的Content-Length就是对的吧
                if (extraBytes > 0 && extraBytes <= 20) {
                    // 使用实际的数据长度而不是Content-Length
                    int actualBodyLength = buf.remaining();
//                    System.out.println("WARN: Content-Length mismatch. Header: " + contentLen +
//                            ", Actual: " + actualBodyLength + ", Using actual length.");

                    // 提取实际的消息体
                    byte[] body = new byte[actualBodyLength];
                    buf.get(body);

                    // 构建完整报文（使用实际长度）
                    byte[] all = new byte[headerBytes.length + 4 + actualBodyLength];
                    System.arraycopy(headerBytes, 0, all, 0, headerBytes.length);
                    all[headerBytes.length] = '\r';
                    all[headerBytes.length + 1] = '\n';
                    all[headerBytes.length + 2] = '\r';
                    all[headerBytes.length + 3] = '\n';
                    System.arraycopy(body, 0, all, headerBytes.length + 4, actualBodyLength);

                    return new Frame(all);
                } else {
                    // 差异太大，可能是协议错误，按正常逻辑处理
                    byte[] body = new byte[contentLen];
                    buf.get(body);

                    // 构建完整报文
                    byte[] all = new byte[headerBytes.length + 4 + contentLen];
                    System.arraycopy(headerBytes, 0, all, 0, headerBytes.length);
                    all[headerBytes.length] = '\r';
                    all[headerBytes.length + 1] = '\n';
                    all[headerBytes.length + 2] = '\r';
                    all[headerBytes.length + 3] = '\n';
                    System.arraycopy(body, 0, all, headerBytes.length + 4, contentLen);

                    return new Frame(all);
                }
            }
            return  null;
        } catch (Exception e) {
            buf.position(start); // 发生异常时回退
            return null;
        }

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