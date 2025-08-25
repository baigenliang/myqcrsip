package com.rsvmcs.qcrsip.test.conf;


import com.rsvmcs.qcrsip.core.SipStack;
import com.rsvmcs.qcrsip.core.log.ServerLogger;
import com.rsvmcs.qcrsip.core.log.StackLogger;
import com.rsvmcs.qcrsip.core.model.SIPMessage;

import java.util.Properties;

public class ServerLoggerImpl implements ServerLogger {

    private boolean showLog = true;

    private SipStack sipStack;

    protected StackLogger stackLogger;

    @Override
    public void closeLogFile() {

    }

    @Override
    public void logMessage(SIPMessage message, String from, String to, boolean sender, long time) {
        if (!showLog) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append(!sender? "发送：目标--->" + from:"接收：来自--->" + to)
//                .append("\r\n")
//                        .append(message);
//        this.stackLogger.logInfo(stringBuilder.toString());
        stringBuilder.append(sender ? "发送：目标--->" + to : "接收：来自--->" + from)
                .append("\r\n")
                .append(message);
        this.stackLogger.logInfo(stringBuilder.toString());

    }

    @Override
    public void logMessage(SIPMessage message, String from, String to, String status, boolean sender, long time) {
        if (!showLog) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append(!sender? "发送： 目标->" + from :"接收：来自->" + to)
//                .append("\r\n")
//                .append(message);
//        this.stackLogger.logInfo(stringBuilder.toString());
        stringBuilder.append(sender? "发送： 目标->" + from :"接收：来自->" + to)
                .append("\r\n")
                .append(message);
        this.stackLogger.logInfo(stringBuilder.toString());
    }

    @Override
    public void logMessage(SIPMessage message, String from, String to, String status, boolean sender) {
        if (!showLog) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(sender? "发送： 目标->" + from :"接收：来自->" + to)
                .append("\r\n")
                .append(message);
        this.stackLogger.logInfo(stringBuilder.toString());
    }

    @Override
    public void logException(Exception ex) {
        if (!showLog) {
            return;
        }
        this.stackLogger.logException(ex);
    }

    @Override
    public void setStackProperties(Properties stackProperties) {
        if (!showLog) {
            return;
        }
        String TRACE_LEVEL = stackProperties.getProperty("com.rsvmcs.qcrsip.TRACE_LEVEL");
        if (TRACE_LEVEL != null) {
            showLog = true;
        }
    }

    @Override
    public void setSipStack(SipStack sipStack) {
        if (!showLog) {
            return;
        }
        this.sipStack=sipStack;
        this.stackLogger = new StackLoggerImpl();
//      if(sipStack instanceof SIPTransactionStack) {
//          this.sipStack = (SIPTransactionStack)sipStack;
//          this.stackLogger = this.sipStack.getStackLogger();
//      }
    }


}
