package com.earnix.eo.eventbus.examples;


import com.earnix.eo.eventbus.Event;

class MyEvent implements Event{
    private final boolean eventFlag;

    MyEvent() {
        this.eventFlag = false;
    }
    
    MyEvent(boolean eventFlag) {
        this.eventFlag = eventFlag;
    }

    boolean isEventFlag() {
        return eventFlag;
    }

    @Override
    public String toString() {
        return "MyEvent{" +
                "eventFlag=" + eventFlag +
                '}';
    }
}
