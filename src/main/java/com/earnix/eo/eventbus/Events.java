package com.earnix.eo.eventbus;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Global {@link EventBus} accessor and factory for local event buses.
 * <br/>
 * Event bus for common usage. All events must implement {@link Event} marker.
 * Subscription approaches:
 * <ul>
 * <li>Single-call subscription ({@link #subscribe} and its overloads) <b>or</b> subscription builder (most customizable, 
 * includes functionality of previous).</li>
 * <li>Annotated methods subscription <b>or</b> subscription providing event class and listener.</li>
 * </ul>
 * Default threading behavior: listeners will be executed synchronously in the same thread where {@link #publish(Event...)}
 * was called, but if subscription was created in EDT - listeners will be called in EDT, synchronously.
 */
public class Events
{
	public static final EventBus bus = new EventBusImpl("global");

	/**
	 * Crates new event bus for local usage.
	 * @param name preferably unique event bus ID
	 */
	public static EventBus createBus(String name)
	{
		return new EventBusImpl(name);
	}

	/**
	 * Publishes event(s) to event bus.
	 *
	 * @param events events to publish
	 */
	public static void publish(Event... events)
	{
		bus.publish(events);
	}

	// region - Single-Call Subscription -

	/**
	 * Subscribes to specific event class.
	 *
	 * @param eventClass event class to listen
	 * @param listener listener, which will be called
	 * @param <T> event type
	 * @return {@link ListenerHandle}, which allows listening cancellation
	 */
	public static <T extends Event> ListenerHandle subscribe(
			final Class<T> eventClass,
			final Consumer<T> listener
	)
	{
		return bus.subscribe(eventClass, listener);
	}

	/**
	 * Subscribes to specific event class. Listener will receive event only in case of positive condition evaluation on it.
	 *
	 * @param eventClass event class to listen
	 * @param eventCondition condition to filter events
	 * @param listener listener, which will be called in case of success condition
	 * @param <T> event type
	 * @return {@link ListenerHandle}, which allows listening cancellation
	 */
	public static <T extends Event> ListenerHandle subscribe(
			final Class<T> eventClass,
			final Consumer<T> listener,
			Predicate<T> eventCondition
	)
	{
		return bus.subscribe(eventClass, listener, eventCondition);
	}

	/**
	 * Subscribes to specific event class. Listener will receive event only in case of positive condition evaluation on it.
	 * As option, subscription wil be cancelled after cancellation event publishing in case of null or positive cancel event condition evaluation.
	 *
	 * @param eventClass event class to listen
	 * @param listener listener, which will be called in case of success condition
	 * @param eventCondition condition to filter events
	 * @param cancellationEventClass event which should cancel current subscription
	 * @param cancellationEventCondition condition to filter cancel events
	 * @param <T> event type
	 * @param <K> cancel event type
	 * @return {@link ListenerHandle}, which allows listening cancellation
	 */
	public static <T extends Event, K extends Event> ListenerHandle subscribe(
			Class<T> eventClass,
			Consumer<T> listener,
			@Nullable Predicate<T> eventCondition,
			@Nullable Class<K> cancellationEventClass,
			@Nullable Predicate<K> cancellationEventCondition
	)
	{
		return bus.subscribe(eventClass, listener, eventCondition, cancellationEventClass, cancellationEventCondition);
	}

	/**
	 * Looks for methods of passed object's class, annotated with {@link ListenEvent}, and register all of them to events.
	 * Event class is detected from method's parameter class. In case of several parameters: method will be called for 
	 * each event with <code>null</code> value of other parameters.
	 *
	 * @param objectWithListeners object with {@link ListenEvent}-annotated method(s)
	 * @return {@link ListenerHandle}, which allows listening cancellation
	 */
	public static ListenerHandle subscribeMethods(final Object objectWithListeners)
	{
		return bus.subscribeMethods(objectWithListeners);
	}

	/**
	 * Looks for methods of passed class, annotated with {@link ListenEvent}, and register all of them to events.
	 * Event class is detected from method's parameter class. In case of several parameters: method will be called for 
	 * each event with <code>null</code> value of other parameters.
	 *
	 * @param classWithListeners class with {@link ListenEvent}-annotated method(s)
	 * @return {@link ListenerHandle}, which allows listening cancellation
	 */
	public static ListenerHandle subscribeMethods(final Class<?> classWithListeners)
	{
		return bus.subscribeMethods(classWithListeners);
	}

	// endregion

	// region - Subscription Builder -

	/**
	 * Returns subscription builder which allows to customize event handling parameters.
	 */
	public static <T extends Event, K extends Event> SubscriptionBuilder<T, K> builder()
	{
		return bus.builder();
	}

	/**
	 * Returns subscription builder which allows to customize event handling parameters
	 *
	 * @param eventClass event class to listen
	 * @param listener listener, which will be called
	 * @param <T> event type
	 * @param <K> cancel event type
	 */
	public static <T extends Event, K extends Event> SubscriptionBuilder<T, K> builder(Class<T> eventClass, Consumer<T> listener)
	{
		return bus.builder(eventClass, listener);
	}

	/**
	 * Looks for methods of passed object's class, annotated with {@link ListenEvent}, and register all of them to events.
	 * Event class is detected from method's parameter class. In case of several parameters: method will be called for 
	 * each event with <code>null</code> value of other parameters.
	 * <br/>
	 * Returns subscription builder which allows to customize event handling parameters.
	 *
	 * @param objectWithListeners object with {@link ListenEvent}-annotated method(s)
	 * @param <T> event type
	 * @param <K> cancel event type
	 */
	public static <T extends Event, K extends Event> SubscriptionBuilder<T, K> builder(final Object objectWithListeners)
	{
		return bus.builder(objectWithListeners);
	}

	/**
	 * Looks for methods of passed object's class, annotated with {@link ListenEvent}, and register all of them to events.
	 * Event class is detected from method's parameter class. In case of several parameters: method will be called for 
	 * each event with <code>null</code> other parameters.
	 * <br/>
	 * Returns subscription builder which allows to customize event handling parameters.
	 *
	 * @param classWithListeners class with {@link ListenEvent}-annotated static method(s)
	 * @param <T> event type
	 * @param <K> cancel event type
	 */
	public static <T extends Event, K extends Event> SubscriptionBuilder<T, K> builder(final Class<?> classWithListeners)
	{
		return bus.builder(classWithListeners);
	}

	// endregion 
}
