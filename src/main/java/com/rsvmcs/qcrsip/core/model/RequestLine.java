package com.rsvmcs.qcrsip.core.model;

public class RequestLine {
    private String method;   // 例如 INVITE，也允许其他任意字符串
    private String uri;      // 不约束形态，可为空
    private String version;  // 默认 SIP/2.0

    public RequestLine(String method, String uri, String version) {
        this.method = method;
        this.uri = uri;
        this.version = version == null ? "SIP/2.0" : version;
    }

    public String getMethod() { return method; }
    public String getUri() { return uri; }
    public String getVersion() { return version; }
    public void setUri(String uri) { this.uri = uri; }
    public void setMethod(String method) { this.method = method; }
    public void setVersion(String version) { this.version = version; }

    public String encode() {
        String m = method == null ? "" : method;
        String u = uri == null ? "" : uri;
        String v = version == null ? "SIP/2.0" : version;
        // 尽量还原三段形式；若 uri 为空，按两段输出也可被对端接受
        return (u.isEmpty() ? (m + " " + v) : (m + " " + u + " " + v)).trim();
    }

    /** 宽松解析：至少 1 个 token（method）；其余字段缺省也不抛异常 */
    public static RequestLine parse(String line) {
        if (line == null) line = "";
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            // 返回一个“空请求行”，上层可根据需要继续处理（不抛异常）
            return new RequestLine("", "", "SIP/2.0");
        }
        String[] p = trimmed.split("\\s+");
        String method = p.length >= 1 ? p[0] : "";
        String uri    = p.length >= 2 ? p[1] : "";
        String ver    = p.length >= 3 ? p[2] : "SIP/2.0";
        return new RequestLine(method, uri, ver);
    }
}
