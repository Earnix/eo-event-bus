package com.earnix.eo.eventbus;

import java.util.LinkedList;
import java.util.List;

public class TestListener
{
	List<Object> listen1received = new LinkedList<>();
	static List<Object> listen2received = new LinkedList<>();
	List<Event> listenBothReceived = new LinkedList<>();
	
	@ListenEvent 
	public void listen1(Event1 event1)
	{
		listen1received.add(event1);
	}
	
	@ListenEvent
	public static void listen2(Event1 event1)
	{
		listen2received.add(event1);
	}
	
	@ListenEvent
	public void lisenBoth(Event1 event1, Event2 event2){
		if(event1 != null) listenBothReceived.add(event1);
		if(event2 != null) listenBothReceived.add(event2);
	}
}
