package com.rsvmcs.qcrsip.core;


import java.util.EventObject;

public class EventWrapper {
    public enum Type { REQUEST, RESPONSE, TIMEOUT, ERROR }
    private final Type type;
    private final EventObject event;
    public EventWrapper(Type type, EventObject event){ this.type = type; this.event = event; }
    public Type getType(){ return type; }
    public EventObject getEvent(){ return event; }
}