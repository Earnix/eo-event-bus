package com.earnix.eo.eventbus;

/**
 * Listener handle, which removes links to itself and weak listener wrapper from bus on subscription cancellation.
 */
class WeakListenerHandle implements ListenerHandle
{
	private final ListenerHandle original;
	private final EventBusImpl eventBus;

	WeakListenerHandle(EventBusImpl eventBus, ListenerHandle original)
	{
		this.original = original;
		this.eventBus = eventBus;
	}

	@Override
	public void cancel()
	{
		synchronized (eventBus)
		{
			eventBus.weakListeners.remove(this);
			original.cancel();
		}
	}

	@Override
	public boolean isActive()
	{
		return original.isActive();
	}
}
