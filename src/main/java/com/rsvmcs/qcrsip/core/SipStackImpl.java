package com.rsvmcs.qcrsip.core;


import com.rsvmcs.qcrsip.core.io.MessageProcessor;
import com.rsvmcs.qcrsip.core.io.TCPMessageProcessor;
import com.rsvmcs.qcrsip.core.io.UDPMessageProcessor;
import com.rsvmcs.qcrsip.core.log.CommonLogger;
import com.rsvmcs.qcrsip.core.log.ServerLogger;
import com.rsvmcs.qcrsip.core.log.StackLogger;
import com.rsvmcs.qcrsip.core.log.logRecord.DefaultMessageLogFactory;
import com.rsvmcs.qcrsip.core.log.logRecord.LogRecordFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * createListeningPoint / createSipProvider.
 * Starts UDP server or TCP acceptor; UDP/TCP server threads call provider.deliverRawMessage / provider.connManager path.
 */

public class SipStackImpl implements SipStack {
    private final Map<ListeningPoint, Object> processors = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    private static StackLogger logger = CommonLogger.getLogger(SipStackImpl.class);

    public LogRecordFactory logRecordFactory;

    private Properties configurationProperties;

    /*
     * ServerLog is used just for logging stack message tracecs.
     */
    protected ServerLogger serverLogger;

    /*
     * Name of the stack.
     */
    protected String stackName;

    /*
     * IP address of stack -- this can be re-written by stun.
     *
     * @deprecated
     */
    protected String stackAddress;

    /*
     * INET address of stack (cached to avoid repeated lookup)
     *
     * @deprecated
     */
    protected InetAddress stackInetAddress;


    /**
     * Set the logger factory.
     *
     * @param logRecordFactory
     *            -- the log record factory to set.
     */
    public void setLogRecordFactory(LogRecordFactory logRecordFactory) {
        this.logRecordFactory = logRecordFactory;
    }


    /**
     * Set the descriptive name of the stack.
     *
     * @param stackName
     *            -- descriptive name of the stack.
     */
    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    /**
     * Set my address.
     *
     * @param stackAddress
     *            -- A string containing the stack address.
     */
    protected void setHostAddress(String stackAddress)
            throws UnknownHostException {
        if (stackAddress.indexOf(':') != stackAddress.lastIndexOf(':')
                && stackAddress.trim().charAt(0) != '[')
            this.stackAddress = '[' + stackAddress + ']';
        else
            this.stackAddress = stackAddress;
        this.stackInetAddress = InetAddress.getByName(stackAddress);
    }

    /**
     * Get my address.
     *
     * @return hostAddress - my host address or null if no host address is
     *         defined.
     * @deprecated
     */
    public String getHostAddress() {

        // JvB: for 1.2 this may return null...
        return this.stackAddress;
    }


    public SipStackImpl(){}

    public SipStackImpl(Properties configurationProperties) throws Exception{

        configurationProperties = new MergedSystemProperties(configurationProperties);
        this.configurationProperties = configurationProperties;

        String address = configurationProperties
                .getProperty("com.rsvmcs.qcrsip.IP_ADDRESS");
        try {
            /** Retrieve the stack IP address */
            if (address != null) {
                // In version 1.2 of the spec the IP address is
                // associated with the listening point and
                // is not madatory.
                setHostAddress(address);

            }
        } catch (java.net.UnknownHostException ex) {
            throw new Exception("bad address " + address);
        }

        /** Retrieve the stack name */
        String name = configurationProperties
                .getProperty("com.rsvmcs.qcrsip.STACK_NAME");
        if (name == null)
            throw new Exception("stack name is missing");
        setStackName(name);
        String stackLoggerClassName = configurationProperties
                .getProperty("com.rsvmcs.qcrsip.STACK_LOGGER");
        // To log debug messages.
        if (stackLoggerClassName == null)
            stackLoggerClassName = "gov.nist.core.LogWriter";
        try {
            Class<?> stackLoggerClass = Class.forName(stackLoggerClassName);
            Class<?>[] constructorArgs = new Class[0];
            Constructor<?> cons = stackLoggerClass
                    .getConstructor(constructorArgs);
            Object[] args = new Object[0];
            StackLogger stackLogger = (StackLogger) cons.newInstance(args);
            CommonLogger.legacyLogger = stackLogger;
            stackLogger.setStackProperties(configurationProperties);
        } catch (InvocationTargetException ex1) {
            throw new IllegalArgumentException(
                    "Cound not instantiate stack logger "
                            + stackLoggerClassName
                            + "- check that it is present on the classpath and that there is a no-args constructor defined",
                    ex1);
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Cound not instantiate stack logger "
                            + stackLoggerClassName
                            + "- check that it is present on the classpath and that there is a no-args constructor defined",
                    ex);
        }



        String serverLoggerClassName = configurationProperties
                .getProperty("com.rsvmcs.qcrsip.SERVER_LOGGER");
        // To log debug messages.
        if (serverLoggerClassName == null)
            serverLoggerClassName = "com.rsvmcs.qcrsip.stack.ServerLog";
        try {
            Class<?> serverLoggerClass = Class
                    .forName(serverLoggerClassName);
            Class<?>[] constructorArgs = new Class[0];
            Constructor<?> cons = serverLoggerClass
                    .getConstructor(constructorArgs);
            Object[] args = new Object[0];
            this.serverLogger = (ServerLogger) cons.newInstance(args);
            serverLogger.setSipStack(this);
            serverLogger.setStackProperties(configurationProperties);
        } catch (InvocationTargetException ex1) {
            throw new IllegalArgumentException(
                    "Cound not instantiate server logger "
                            + stackLoggerClassName
                            + "- check that it is present on the classpath and that there is a no-args constructor defined",
                    ex1);
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Cound not instantiate server logger "
                            + stackLoggerClassName
                            + "- check that it is present on the classpath and that there is a no-args constructor defined",
                    ex);
        }


        String messageLogFactoryClasspath = configurationProperties
                .getProperty("com.rsvmcs.qcrsip.LOG_FACTORY");
        if (messageLogFactoryClasspath != null) {
            try {
                Class<?> clazz = Class.forName(messageLogFactoryClasspath);
                Constructor<?> c = clazz.getConstructor(new Class[0]);
                this.logRecordFactory = (LogRecordFactory) c
                        .newInstance(new Object[0]);
            } catch (Exception ex) {
                if (logger.isLoggingEnabled())
                    logger
                            .logError(
                                    "Bad configuration value for LOG_FACTORY -- using default logger");
                this.logRecordFactory = new DefaultMessageLogFactory();
            }

        } else {
            this.logRecordFactory = new DefaultMessageLogFactory();
        }
    }


    @Override
    public ListeningPoint createListeningPoint(String ip, int port, String transport) {
        return new ListeningPoint(ip, port, transport);
    }

    @Override
    public SipProvider createSipProvider(ListeningPoint lp) throws Exception {
        if (!running) throw new IllegalStateException("Start SipStack before createSipProvider()");
        if (lp == null) throw new IllegalArgumentException("ListeningPoint is null");
        Object proc = processors.computeIfAbsent(lp, k -> {
            try {
                if ("TCP".equalsIgnoreCase(lp.getTransport())) {
                    TCPMessageProcessor p = new TCPMessageProcessor(lp);
                    p.start();
                    return p;
                } else if ("UDP".equalsIgnoreCase(lp.getTransport())) {
                    UDPMessageProcessor p = new UDPMessageProcessor(lp);
                    p.start();
                    return p;
                } else {
                    throw new IllegalArgumentException("Unsupported transport: " + lp.getTransport());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return new SipProviderImpl(lp, this, proc);
    }



    /**
     * Return true if logging is enabled for this stack.
     * Deprecated. Use StackLogger.isLoggingEnabled instead
     *
     * @return true if logging is enabled for this stack instance.
     */
    @Deprecated
    public boolean isLoggingEnabled() {
        return logger == null ? false : logger
                .isLoggingEnabled();
    }

    /**
     * Deprecated. Use StackLogger.isLoggingEnabled instead
     * @param level
     * @return
     */
    @Deprecated
    public boolean isLoggingEnabled( int level ) {
        return logger == null ? false
                : logger.isLoggingEnabled( level );
    }

    /**
     * Get the logger. This method should be deprected.
     * Use static logger = CommonLogger.getLogger() instead
     *
     * @return --the logger for the sip stack. Each stack has its own logger
     *         instance.
     */
    @Deprecated
    public StackLogger getStackLogger() {
        return logger;
    }


    @Override
    public void start() { running = true; }

    @Override
    public void stop() {
        running = false;
        processors.values().forEach(p -> {
            try {
                if (p instanceof TCPMessageProcessor) ((TCPMessageProcessor) p).stop();
                if (p instanceof UDPMessageProcessor) ((UDPMessageProcessor) p).stop();
            } catch (Exception ignored) {}
        });
        processors.clear();
    }

    /* package */ Object getProcessor(ListeningPoint lp) {
        return processors.get(lp);
    }
}
