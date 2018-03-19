package com.earnix.eo.eventbus;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
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
public interface EventBus 
{
	/**
	 * Publishes event(s) to event bus.
	 * 
	 * @param events events to publish
	 */
	void publish(Event... events);

	// region - Single-Call Subscription -
	
	/**
	 * Subscribes to specific event class.
	 *
	 * @param eventClass event class to listen
	 * @param listener listener, which will be called
	 * @param <T> event type
	 * @return {@link ListenerHandle}, which allows listening cancellation
	 */
	<T extends Event> ListenerHandle subscribe(
			final Class<T> eventClass,
			final Consumer<T> listener
	);
	
	/**
	 * Subscribes to specific event class. Listener will receive event only in case of positive condition evaluation on it.
	 *
	 * @param eventClass event class to listen
	 * @param eventCondition condition to filter events
	 * @param listener listener, which will be called in case of success condition
	 * @param <T> event type
	 * @return {@link ListenerHandle}, which allows listening cancellation
	 */
	<T extends Event> ListenerHandle subscribe(
			final Class<T> eventClass,
			final Consumer<T> listener,
			Predicate<T> eventCondition
	);

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
	<T extends Event, K extends Event> ListenerHandle subscribe(
			Class<T> eventClass,
			Consumer<T> listener,
			@Nullable Predicate<T> eventCondition,
			@Nullable Class<K> cancellationEventClass,
			@Nullable Predicate<K> cancellationEventCondition
	);

	/**
	 * Looks for methods of passed object's class, annotated with {@link ListenEvent}, and register all of them to events.
	 * Event class is detected from method's parameter class. In case of several parameters: method will be called for 
	 * each event with <code>null</code> value of other parameters.
	 *
	 * @param objectWithListeners object with {@link ListenEvent}-annotated method(s)
	 * @return {@link ListenerHandle}, which allows listening cancellation
	 */
	ListenerHandle subscribeMethods(final Object objectWithListeners);

	/**
	 * Looks for methods of passed class, annotated with {@link ListenEvent}, and register all of them to events.
	 * Event class is detected from method's parameter class. In case of several parameters: method will be called for 
	 * each event with <code>null</code> value of other parameters.
	 *
	 * @param classWithListeners class with {@link ListenEvent}-annotated method(s)
	 * @return {@link ListenerHandle}, which allows listening cancellation
	 */
	ListenerHandle subscribeMethods(final Class<?> classWithListeners);
	
	// endregion
	
	// region - Subscription Builder -

	/**
	 * Returns subscription builder which allows to customize event handling parameters.
	 */
	<T extends Event, K extends Event> SubscriptionBuilder<T, K> builder();

	/**
	 * Returns subscription builder which allows to customize event handling parameters
	 * 
	 * @param eventClass event class to listen
	 * @param listener listener, which will be called
	 * @param <T> event type
	 * @param <K> cancel event type
	 */
	<T extends Event, K extends Event> SubscriptionBuilder<T, K> builder(Class<T> eventClass, Consumer<T> listener);
	
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
	<T extends Event, K extends Event> SubscriptionBuilder<T, K> builder(final Object objectWithListeners);
	
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
	<T extends Event, K extends Event> SubscriptionBuilder<T, K> builder(final Class<?> classWithListeners);
	
	// endregion 
}