package com.rsvmcs.qcrsip.core.model;


public class StatusLine {
    private String version; // 默认 SIP/2.0
    private int status;     // 允许 0（对端没给也不抛）
    private String reason;  // 允许为空

    public StatusLine(String version, int status, String reason) {
        this.version = (version == null || version.isEmpty()) ? "SIP/2.0" : version;
        this.status = status;
        this.reason = reason == null ? "" : reason;
    }

    public int getStatus(){ return status; }
    public String getReason(){ return reason; }
    public String getVersion(){ return version; }

    public String encode(){ return version + " " + status + (reason.isEmpty() ? "" : " " + reason); }

    /** 宽松解析：少字段也不抛异常 */
    public static StatusLine parse(String line){
        if (line == null) line = "";
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return new StatusLine("SIP/2.0", 0, "");
        String[] p = trimmed.split("\\s+", 3);
        String ver = p.length >= 1 ? p[0] : "SIP/2.0";
        int st = 0;
        if (p.length >= 2) {
            try { st = Integer.parseInt(p[1]); } catch (Exception ignored) {}
        }
        String rs = p.length >= 3 ? p[2] : "";
        return new StatusLine(ver, st, rs);
    }
}
