package com.rsvmcs.qcrsip.test;


import com.rsvmcs.qcrsip.core.SipStack;
import com.rsvmcs.qcrsip.core.SipStackImpl;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;

/**
 * 自定义的 SIP 工厂类，提供单例 getInstance 和 createSipStack 方法
 */
public class SipFactory {
    // 单例实例
    private static SipFactory instance;

    private Hashtable sipStackByName;

    public final LinkedList sipStackList = new LinkedList();


    // default domain to locate Reference Implementation
    private String pathName = "com.rsvmcs.qcrsip";


    // 私有构造方法，避免外部直接 new
    private SipFactory() {
        this.sipStackByName = new Hashtable();
    }

    /**
     * 获取单例实例
     */
    public static synchronized SipFactory getInstance() {
        if (instance == null) {
            instance = new SipFactory();
        }
        return instance;
    }

    /**
     * 根据传入的配置参数创建 SipStack 实例
     *
     * @param properties SIP 协议栈属性
     * @return SipStackImpl 实例
     */
//    public SipStackImpl createSipStack(Properties properties) {
//        if (properties == null) {
//            throw new IllegalArgumentException("Properties 参数不能为空");
//        }
//        return new SipStackImpl(properties);
//    }

    private SipStack createStack(Properties properties)
            throws Exception {
        try {
            // create parameters argument to identify constructor
            Class[] paramTypes = new Class[1];
            paramTypes[0] = Class.forName("java.util.Properties");
            // get constructor of SipStack in order to instantiate
            Constructor sipStackConstructor = Class.forName(
                    getPathName() + ".com.rsvmcs.qcrsip.SipStackImpl").getConstructor(
                    paramTypes);
            // Wrap properties object in order to pass to constructor of
            // SipSatck
            Object[] conArgs = new Object[1];
            conArgs[0] = properties;
            // Creates a new instance of SipStack Class with the supplied
            // properties.
            SipStack sipStack = (SipStack) sipStackConstructor.newInstance(conArgs);
            sipStackList.add(sipStack);
            String name = properties.getProperty("com.rsvmcs.qcrsip.STACK_NAME");
            this.sipStackByName.put(name, sipStack);
            return sipStack;
        } catch (Exception e) {
            String errmsg = "The Peer SIP Stack: "
                    + getPathName()
                    + ".com.rsvmcs.qcrsip.SipStackImpl"
                    + " could not be instantiated. Ensure the Path Name has been set.";
            throw new Exception(errmsg, e);
        }
    }


    /**
     * Sets the <var>pathname</var> that identifies the location of a
     * particular vendor's implementation of this specification. The
     * <var>pathname</var> must be the reverse domain name assigned to the
     * vendor providing the implementation. An application must call
     * {@link SipFactory#resetFactory()} before changing between different
     * implementations of this specification.
     *
     * @param pathName -
     *                 the reverse domain name of the vendor, e.g. Sun Microsystem's
     *                 would be 'com.sun'
     */
    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    /**
     * Returns the current <var>pathname</var> of the SipFactory. The
     * <var>pathname</var> identifies the location of a particular vendor's
     * implementation of this specification as defined the naming convention.
     * The pathname must be the reverse domain name assigned to the vendor
     * providing this implementation. This value is defaulted to
     * <code>gov.nist</code> the location of the Reference Implementation.
     *
     * @return the string identifying the current vendor implementation.
     */
    public String getPathName() {
        return pathName;
    }

    public synchronized SipStack createSipStack(Properties properties)
            throws Exception {

        String ipAddress = properties.getProperty("com.rsvmcs.qcrsip.IP_ADDRESS");
        String name = properties.getProperty("com.rsvmcs.qcrsip.STACK_NAME");
        if (name == null) throw new Exception("Missing com.rsvmcs.qcrsip.STACK_NAME property");
        // IP address was not specified in the properties.
        // This means that the architecture supports a single sip stack
        // instance per stack name
        // and each listening point is assinged its own IP address.
//        if ( ipAddress == null) {
        SipStack mySipStack = (SipStack) this.sipStackByName.get(name);
        if (mySipStack == null) {
            mySipStack = createStack(properties);
        }
        return mySipStack;
//        }
//        else {
//            // Check to see if a stack with that IP Address is already
//            // created, if so select it to be returned. In this case the
//            // the name is not used.
//            int i = 0;
//            for (i = 0; i < sipStackList.size(); i++) {
//                if (((SipStack) sipStackList.get(i)).getIPAddress().equals( ipAddress )) {
//                    return (SipStack) sipStackList.get(i);
//                }
//            }
//            return createStack(properties);
//        }

    }
}
