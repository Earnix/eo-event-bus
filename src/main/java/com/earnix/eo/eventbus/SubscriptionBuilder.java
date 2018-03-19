package com.earnix.eo.eventbus;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * API for event(s) subscription customization.
 *  
 * @param <T> event class
 * @param <K> cancellation event class
 */
public abstract class SubscriptionBuilder<T extends Event, K extends Event>
{
	final SubscriptionParameters<T, K> params = new SubscriptionParameters<>();

	SubscriptionBuilder()
	{
	}
	
	SubscriptionBuilder(Class<T> eventClass, Consumer<T> listener)
	{
		eventClass(eventClass);
		listener(listener);
	}
	
	SubscriptionBuilder(final Class<?> classWithListeners)
	{
		objectWithListeners(classWithListeners);
	}
	
	SubscriptionBuilder(final Object objectWithListeners)
	{
		objectWithListeners(objectWithListeners);
	}

	/**
	 * Sets event class to listen for.
	 * 
	 * @param eventClass event class
	 */
	public <L extends Event> SubscriptionBuilder<L, K> eventClass(Class<L> eventClass)
	{
		Validator.notNull(eventClass);
		ensureEventClassNotSet();
		@SuppressWarnings("unchecked")
		final SubscriptionBuilder<L, K> result = (SubscriptionBuilder<L, K>) this;
		result.params.eventClass = eventClass;
		return result;
	}

	/**
	 * Sets listener, which will be called in case of success condition.
	 * 
	 * @param listener listener
	 */
	public SubscriptionBuilder<T, K> listener(Consumer<T> listener)
	{
		Validator.notNull(listener);
		ensureListenerNotSet();
		params.listener = listener;
		return this;
	}
	
	/**
	 * Looks for methods of passed object's class, annotated with {@link ListenEvent}, and register all of them to events.
	 * Events classes are detected from method's parameters classes. In case of several parameters: method will be called for 
	 * each event with <code>null</code> value of other parameters.
	 * 
	 * @param object object with {@link ListenEvent}-annotated method(s)
	 */
	public SubscriptionBuilder<T, K> objectWithListeners(Object object)
	{
		Validator.notNull(object);
		ensureEventClassNotSet();
		ensureListenerNotSet();
		params.objectWithListeningMethods = object;
		return this;
	}

	/**
	 * Looks for methods of passed class, annotated with {@link ListenEvent}, and register all of them to events.
	 * Events classes are detected from method's parameters classes. In case of several parameters: method will be called for 
	 * each event with <code>null</code> value of other parameters.
	 * 
	 * @param clazz class with {@link ListenEvent}-annotated static method(s)
	 */
	public void classWithListeners(Class<?> clazz)
	{
		Validator.notNull(clazz);
		ensureEventClassNotSet();
		ensureListenerNotSet();
		params.classWithListeningMethods = clazz;
	}

	/**
	 * Executes listener asynchronously after finish or interruption of given thread.
	 * 
	 * @param thread thread
	 */
	public SubscriptionBuilder<T, K> afterThread(Thread thread)
	{
		Validator.notNull(thread);
		Validator.isTrue(params.afterThread == null, "Prior thread already set");
		Validator.isTrue(params.executor == null, "Can not set prior thread with executor");
		params.afterThread = thread;
		return this;
	}

	/**
	 * Executes listener on given executor.
	 * 
	 * @param executor executor
	 */
	public SubscriptionBuilder<T, K> executor(Executor executor)
	{
		Validator.notNull(executor);
		Validator.isTrue(!isEdt(), "Can not specify executor for EDT listeners");
		params.executor = executor;
		return this;
	}

	/**
	 * Sets listener execution to be async. If EDT execution is enabled - listener will be invoked later on EDT.
	 */
	public SubscriptionBuilder<T, K> async()
	{
		params.async = true;
		return this;
	}

	/**
	 * Makes listener weak. Allows to avoid resources leaks in some cases. Is not preferred way since may cause un-deterministic behavior.
	 * It's better to unsubscribe explicitly.
	 */
	public SubscriptionBuilder<T, K> weak()
	{
		params.weak = true;
		return this;
	}

	/**
	 * Set current listener to be executed on EDT. Listener will be executed synchronously if event will be published from EDT.
	 */
	public SubscriptionBuilder<T, K> edt()
	{
		Validator.isTrue(params.executor == null, "Cannot enable EDT if executor is specified");
		params.edt = true;
		return this;
	}

	/**
	 * Set current listener to be later on EDT. Listener will never be synchronous.
	 */
	public SubscriptionBuilder<T, K> asyncEdt()
	{
		Validator.isTrue(params.executor == null, "Cannot enable EDT if executor is specified");
		params.asyncEdt = true;
		return this;
	}

	/**
	 * Condition the event must correspond to achieve listener.
	 */
	public SubscriptionBuilder<T, K> condition(Predicate<T> eventCondition)
	{
		Validator.notNull(eventCondition);
		Validator.isTrue(params.eventCondition == null, "Event condition already set");
		params.eventCondition = eventCondition;
		return this;
	}

	/**
	 * Delays actual subscription.
	 */
	public SubscriptionBuilder<T, K> delay(Duration duration)
	{
		Validator.notNull(duration);
		Validator.isTrue(!duration.isNegative(), "Delay duration is negative");
		params.delay = duration;
		return this;
	}

	/**
	 * After this event original event subscription will be cancelled.
	 */
	public <L extends Event> SubscriptionBuilder<T, L> cancelOn(Class<L> cancelEventClass)
	{
		Validator.notNull(cancelEventClass);
		Validator.isTrue(params.cancellationEventClass == null, "Cancel event class already set");
		@SuppressWarnings("unchecked")
		final SubscriptionBuilder<T, L> result = (SubscriptionBuilder<T, L>) this;
		result.params.cancellationEventClass = cancelEventClass;
		return result;
	}

	/**
	 * Condition to be applied on cancel event.
	 */
	public SubscriptionBuilder<T, K> cancelOnCondition(Predicate<K> cancelEventCondition)
	{
		Validator.notNull(cancelEventCondition);
		Validator.isTrue(params.cancellationEventCondition == null, "Cancel event condition already set");
		params.cancellationEventCondition = cancelEventCondition;
		return this;
	}

	/**
	 * Error handler for all exceptions, throw'ed within listener.
	 */
	public SubscriptionBuilder<T, K> onError(Consumer<Exception> exceptionConsumer)
	{
		Validator.notNull(exceptionConsumer);
		params.errorConsumers.put(Exception.class, exceptionConsumer);
		return this;
	}
	
	/**
	 * Error handler for given exception class, throw'ed within listener.
	 */
	@SuppressWarnings({"SuspiciousMethodCalls", "unchecked"})
	public <L extends Exception> SubscriptionBuilder<T, K> onError(Class<L> errorClass, Consumer<L> errorConsumer)
	{
		Validator.notNull(errorClass);
		Validator.notNull(errorClass);
		Validator.isTrue(!params.errorConsumers.containsKey(errorClass), "Error handler for this exception class is already set");
		params.errorConsumers.put((Class<Exception>) errorClass, (Consumer<Exception>) errorConsumer);
		return this;
	}

	// region Earnix-specific

	/**
	 * Listener will be cancelled after {@link ProjectClosedEvent} published for given project PK.
	 */
	public SubscriptionBuilder<T, K> withinProject(int projectPk)
	{
		Validator.isTrue(projectPk > 0, "Project PK must be positive");
		Validator.isTrue(params.projectPk == null, "Project already set");
		params.projectPk = Integer.valueOf(projectPk);
		return this;
	}

	// endregion

	void validate()
	{
		Validator.isTrue(isListenerSet() && isEventClassSet(), "Event class and listener must be set");
		Validator.isTrue(
				params.cancellationEventClass == null ^ params.cancellationEventCondition != null, 
				"Cancel event condition set without cancel event class"
		);
	}

	public abstract ListenerHandle subscribe();
	
	private void ensureListenerNotSet()
	{
		if (isListenerSet())
		{
			throw new IllegalArgumentException("Listener already set");
		}
	}
	
	private void ensureEventClassNotSet()
	{
		if (isEventClassSet()) 
		{
			throw new IllegalArgumentException("Event class already set");
		}
	}
	
	private boolean isListenerSet()
	{
		return params.listener != null ||
			   params.objectWithListeningMethods != null || 
			   params.classWithListeningMethods != null;
	}
	
	private boolean isEventClassSet()
	{
		return params.eventClass != null || 
		       params.objectWithListeningMethods != null || 
		       params.classWithListeningMethods != null;
	}
	
	private boolean isEdt()
	{
		return params.edt || params.asyncEdt;
	}
}
