package com.earnix.eo.eventbus;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;


/**
 * Consumer, which holds a weak reference to original.
 */
class WeakConsumer<T extends Event> implements Consumer<T>
{
	private final WeakReference<Consumer<T>> target;
	private final EventBusImpl eventBus;

	WeakConsumer(EventBusImpl eventBus, Consumer<T> consumer)
	{
		this.eventBus = eventBus;
		this.target = new WeakReference<>(consumer);
	}

	@Override
	public void accept(T o)
	{
		final Consumer<T> consumer = target.get();
		if(consumer != null) {
			consumer.accept(o);
		} else {
			eventBus.attemptMaintenance();
		}
	}
	
	boolean isRemoved()
	{
		return target.get() == null;
	}
	
}
