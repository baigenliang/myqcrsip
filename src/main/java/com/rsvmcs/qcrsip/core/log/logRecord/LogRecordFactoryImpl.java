package com.rsvmcs.qcrsip.core.log.logRecord;

public class LogRecordFactoryImpl implements LogRecordFactory {

    public LogRecord createLogRecord(String message, String source,
                                     String destination, long timeStamp, boolean isSender,
                                     String firstLine, String tid, String callId, long timestampVal) {

        return new LogRecordImpl(message,source,destination,timeStamp,isSender,firstLine,tid,callId,timestampVal);
    }

}
