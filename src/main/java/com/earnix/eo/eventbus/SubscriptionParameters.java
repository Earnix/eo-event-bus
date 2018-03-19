package com.earnix.eo.eventbus;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Subscription parameters state
 */
class SubscriptionParameters <T extends Event, K extends Event> implements Cloneable
{
	boolean edt;
	Executor executor;
	Thread afterThread;
	boolean asyncEdt;
	boolean async;
	boolean weak;
	Class<T> eventClass;
	Predicate<T> eventCondition;
	Class<K> cancellationEventClass;
	Predicate<K> cancellationEventCondition;
	Consumer<T> listener;
	Integer projectPk;
	Object objectWithListeningMethods;
	Class<?> classWithListeningMethods;
	Duration delay;
	final HashMap<Class<Exception>, Consumer<Exception>> errorConsumers;

	SubscriptionParameters()
	{
		errorConsumers = new HashMap<>();
	}
	
	SubscriptionParameters(SubscriptionParameters<T, K> other)
	{
		this.edt = other.edt;
		this.executor = other.executor;
		this.afterThread = other.afterThread;
		this.asyncEdt = other.asyncEdt;
		this.async = other.async;
		this.weak = other.weak;
		this.eventClass = other.eventClass;
		this.eventCondition = other.eventCondition;
		this.cancellationEventClass = other.cancellationEventClass;
		this.cancellationEventCondition = other.cancellationEventCondition;
		this.listener = other.listener;
		this.projectPk = other.projectPk;
		this.objectWithListeningMethods = other.objectWithListeningMethods;
		this.delay = other.delay;
		this.objectWithListeningMethods = other.objectWithListeningMethods;
		this.classWithListeningMethods = other.classWithListeningMethods;
		this.errorConsumers = other.errorConsumers;
	}
}
