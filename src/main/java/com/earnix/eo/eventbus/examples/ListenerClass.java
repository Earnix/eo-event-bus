package com.earnix.eo.eventbus.examples;

import com.earnix.eo.eventbus.ListenEvent;

public class ListenerClass {
    
    @ListenEvent
    public static void onMyEventStatic(MyEvent myEvent){
        System.out.println("Received: " + myEvent);    
    }
    
    @ListenEvent
    public void onMyEvent(MyEvent myEvent){
        System.out.println("Received: " + myEvent);    
    }
}
