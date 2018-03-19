package com.earnix.eo.eventbus;

import java.util.List;

/**
 * Cancels and represents state of multiple handles.
 */
class CompositeListenerHandle implements ListenerHandle
{
	private final List<ListenerHandle> original;
	private final EventBusImpl eventBus;

	CompositeListenerHandle(EventBusImpl eventBus, List<ListenerHandle> original)
	{
		this.original = original;
		this.eventBus = eventBus;
	}
	
	@Override
	public void cancel()
	{
		synchronized (eventBus){
			original.forEach(ListenerHandle::cancel);
		}
	}

	@Override
	public boolean isActive()
	{
		synchronized (eventBus)
		{
			return original.stream().anyMatch(ListenerHandle::isActive);
		}
	}
}
