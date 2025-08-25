package com.rsvmcs.qcrsip.test.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * 获取sip默认配置
 * @author lin
 */
public class DefaultProperties {
    private final static Logger logger = LoggerFactory.getLogger(com.rsvmcs.qcrsip.test.conf.DefaultProperties.class);


    public static Properties getProperties(String name, boolean sipLog) {
        Properties properties = new Properties();
        properties.setProperty("com.rsvmcs.qcrsip.STACK_NAME", name);
//        properties.setProperty("com.rsvmcs.qcrsip.IP_ADDRESS", sipConfig.getIp());
        /**
         * 完整配置参考 com.rsvmcs.qcrsip.SipStackImpl，需要下载源码
         * gov/nist/javax/sip/SipStackImpl.class
         */
    //    properties.setProperty("com.rsvmcs.qcrsip.LOG_MESSAGE_CONTENT", "true");
     //   properties.setProperty("com.rsvmcs.qcrsip.RELIABLE_CONNECTION_KEEP_ALIVE_TIMEOUT", "10");

        /**
         * sip_server_log.log 和 sip_debug_log.log public static final int TRACE_NONE =
         * 0; public static final int TRACE_MESSAGES = 16; public static final int
         * TRACE_EXCEPTION = 17; public static final int TRACE_DEBUG = 32;
         */
//		properties.setProperty("com.rsvmcs.qcrsip.TRACE_LEVEL", "0");
//		properties.setProperty("com.rsvmcs.qcrsip.SERVER_LOG", "sip_server_log");
//		properties.setProperty("com.rsvmcs.qcrsip.DEBUG_LOG", "sip_debug_log");
        if (sipLog) {
            properties.setProperty("com.rsvmcs.qcrsip.STACK_LOGGER", "com.rsvmcs.qcrsip.test.conf.StackLoggerImpl");
            properties.setProperty("com.rsvmcs.qcrsip.SERVER_LOGGER", "com.rsvmcs.qcrsip.test.conf.ServerLoggerImpl");
            properties.setProperty("com.rsvmcs.qcrsip.LOG_MESSAGE_CONTENT", "true");
            logger.info("[SIP日志]已开启");
        }else {
            logger.info("[SIP日志]已关闭");
        }
        return properties;
    }
}
