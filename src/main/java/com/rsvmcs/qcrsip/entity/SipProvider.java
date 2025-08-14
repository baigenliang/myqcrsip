package com.rsvmcs.qcrsip.entity;


import java.io.IOException;
import java.util.List;

public interface SipProvider {
    void addSipListener(SipListener listener);
    void removeSipListener(SipListener listener);
    SipListener getSipListener();

    ListeningPoint getListeningPoint();
    void sendRequest(Request request) throws Exception;
    void sendResponse(Response response) throws Exception;
}