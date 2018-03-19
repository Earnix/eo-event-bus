package com.earnix.eo.eventbus.examples;

import com.earnix.eo.eventbus.Event;

public class CancelEvent implements Event {
    private final boolean eventFlag;

    CancelEvent() {
        this.eventFlag = false;
    }

    CancelEvent(boolean eventFlag) {
        this.eventFlag = eventFlag;
    }

    boolean isEventFlag() {
        return eventFlag;
    }

    @Override
    public String toString() {
        return "CancelEvent{" +
                "eventFlag=" + eventFlag +
                '}';
    }
}
