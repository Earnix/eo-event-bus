package com.earnix.eo.eventbus;

import javax.swing.SwingUtilities;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event bus implementation.
 * {@inheritDoc}
 */
class EventBusImpl implements EventBus
{
	// TM TBD: error handling in builder, optionals, unsubscribe after timeout, listeners exec timeout,
	// responding events, ID-based events filtering, memory leaks detection, stats calculation.

	private static final Logger log = LoggerFactory.getLogger(EventBusImpl.class);
	
	private static final Duration MAINTENANCE_INTERVAL = Duration.ofSeconds(10);
	
	/**
	 * A bridge that acts both as a Subscriber and as an Observable. Because it is a Subscriber, it can subscribe to one
	 * or more Observables, and because it is an Observable, it can pass through the items it observes by re-emitting
	 * them, and it can also emit new items.
	 */
	private final String name;
	private EdtExecutor syncEdtScheduler = new EdtExecutor(true);
	private EdtExecutor asyncEdtScheduler = new EdtExecutor(false);
	private Instant lastMaintenance = Instant.now();
	
	final HashMap<ListenerHandle, WeakConsumer<Event>> weakListeners = new HashMap<>();
	
	private final HashMap<Class<Event>, List<Subscription<Event>>> data = new HashMap<>();

	EventBusImpl(String name)
	{
		Validator.notNull(name);
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void publish(Event... events)
	{
		Stream.of(events).forEach(event -> {
			List<Map.Entry<Class<Event>, List<Subscription<Event>>>> assignable = data.entrySet().stream()
					.filter(entry -> entry.getKey().isAssignableFrom(event.getClass()))
					.collect(Collectors.toList());
			
			assignable.forEach(entry -> {
				// creating another list to avoid ConcurrentModificationException
				new ArrayList<>(entry.getValue()).forEach(subscription -> tryExecuteListener(event, subscription));
			});
			
			log.trace("EventBus[{}] Published an event of type {}", name, event.getClass().getSimpleName());
		});
		attemptMaintenance();
	}

	private void tryExecuteListener(Event event, Subscription<Event> subscription)
	{
		// checking event condition
		if (subscription.condition != null && !subscription.condition.test(event))
		{
			return;
		}

		// checking subscription delay
		if (subscription.delay != null && subscription.subscribedAt.plus(subscription.delay).isAfter(Instant.now()))
		{
			return;
		}

		// executing
		if (subscription.executor != null)
		{
			subscription.executor.execute(() -> {
				try
				{
					subscription.listener.accept(event);
				}
				catch (Exception t)
				{
					subscription.errorHandler.accept(t);
				}
			});
		}
		else
		{
			try
			{
				subscription.listener.accept(event);
			}
			catch (Exception t)
			{
				subscription.errorHandler.accept(t);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Event, K extends Event> SubscriptionBuilder<T ,K> builder(Class<T> eventClass, Consumer<T> listener)
	{
		Validator.notNull(eventClass);
        Validator.notNull(listener);
		return new SubscriptionBuilder<T, K>(eventClass, listener)
		{
			@Override
			public ListenerHandle subscribe()
			{
				validate();
				return EventBusImpl.this.subscribeImpl(params);
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Event, K extends Event> SubscriptionBuilder<T, K> builder()
	{
		return new SubscriptionBuilder<T, K>()
		{
			@Override
			public ListenerHandle subscribe()
			{
				validate();
				return EventBusImpl.this.subscribeImpl(params);
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Event, K extends Event> SubscriptionBuilder<T, K> builder(Object objectWithListeners)
	{
		return new SubscriptionBuilder<T, K>(objectWithListeners)
		{
			@Override
			public ListenerHandle subscribe()
			{
				validate();
				return EventBusImpl.this.subscribeImpl(params);
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Event, K extends Event> SubscriptionBuilder<T, K> builder(Class<?> classWithListeners)
	{
		return new SubscriptionBuilder<T, K>(classWithListeners)
		{
			@Override
			public ListenerHandle subscribe()
			{
				validate();
				return EventBusImpl.this.subscribeImpl(params);
			}
		};	
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListenerHandle subscribeMethods(final Object objectWithListeners)
	{
        Validator.notNull(objectWithListeners);
		return subscribeMethods(null, objectWithListeners);
	}

	@Override
	public ListenerHandle subscribeMethods(Class<?> classWithListeners)
	{
        Validator.notNull(classWithListeners);
		return subscribeMethods(classWithListeners, null);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Event> ListenerHandle subscribe(final Class<T> eventClass, final Consumer<T> listener)
	{
        Validator.notNull(eventClass);
        Validator.notNull(listener);

		return subscribe(eventClass, listener, null, null, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Event> ListenerHandle subscribe(final Class<T> eventClass, final Consumer<T> listener, Predicate<T> eventCondition)
	{
        Validator.notNull(eventClass);
        Validator.notNull(eventCondition);
        Validator.notNull(listener);

		return subscribe(eventClass, listener, eventCondition, null, null);
	}

	/**
	 * Analyzes class or object and class: locates method parameters annotated with {@link ListenEvent} and wraps them into consumers
	 */
	private Map<Consumer<Event>, Class<Event>> annotatedMethodsToConsumers(@Nullable final Class<?> clazz, @Nullable Object object)
	{
        Validator.isTrue(clazz != null ^ object != null);
		final Class<?> targetClass = object != null ? object.getClass() : clazz;
		final HashMap<Consumer<Event>, Class<Event>> result = new HashMap<>();
		final List<Method> methods = getMethodsListWithAnnotation(targetClass, ListenEvent.class);
		for (final Method method : methods)
		{
			if(object == null && !Modifier.isStatic(method.getModifiers())){
				continue;
			}
			Class[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++)
			{
				Validator.isTrue(Event.class.isAssignableFrom(parameterTypes[i]), "Event listening method parameter class must implement Event marker");
				//noinspection unchecked
				result.put(new MethodConsumer<>(i, method, object), parameterTypes[i]);
			}
		}
		Validator.isTrue(result.size() > 0, "Passed object doesn't have event listening methods");
		return result;
	}
	
	@Override
	public <T extends Event, K extends Event> ListenerHandle subscribe(
			final Class<T> eventClass,
			final Consumer<T> listener,
			@Nullable final Predicate<T> eventCondition,
			@Nullable final Class<K> cancellationEventClass, 
			@Nullable final Predicate<K> cancellationEventCondition)
	{
		Validator.notNull(eventClass);
		Validator.notNull(listener);
		SubscriptionParameters<T, K> parameters = new SubscriptionParameters<>();
		parameters.eventClass = eventClass;
		parameters.listener = listener;
		parameters.eventCondition = eventCondition;
		parameters.cancellationEventClass = cancellationEventClass;
		parameters.cancellationEventCondition = cancellationEventCondition;
		return subscribeImpl(parameters);
	}

	private ListenerHandle subscribeMethods(Class<?> classWithListeners, final Object objectWithListeners)
	{
		final Map<Consumer<Event>, Class<Event>> consumers = annotatedMethodsToConsumers(classWithListeners, objectWithListeners);
		final ArrayList<ListenerHandle> allSubscriptions = new ArrayList<>();
		for (Map.Entry<Consumer<Event>, Class<Event>> consumerClassEntry : consumers.entrySet())
		{
			ListenerHandle handle = this.subscribe(
					consumerClassEntry.getValue(),
					consumerClassEntry.getKey(), 
					null, 
					null, 
					null
			);
			allSubscriptions.add(handle);
		}
		return mergeHandles(allSubscriptions);
	}

	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	private synchronized <T extends Event, K extends Event> ListenerHandle subscribeImpl(
			SubscriptionParameters<T, K> params)
	{
		if (params.objectWithListeningMethods != null || params.classWithListeningMethods != null)
		{
			// recursive call to handle each annotated method
			final Map<Consumer<Event>, Class<Event>> consumers = annotatedMethodsToConsumers(params.classWithListeningMethods, params.objectWithListeningMethods);
			params.objectWithListeningMethods = null;
			params.classWithListeningMethods = null;
			final ArrayList<ListenerHandle> allHandles = new ArrayList<>();
			for (Map.Entry<Consumer<Event>, Class<Event>> consumerClassEntry : consumers.entrySet())
			{
				SubscriptionParameters<Event, Event> concreteParameters = new SubscriptionParameters(params);
				concreteParameters.eventClass = consumerClassEntry.getValue();
				concreteParameters.listener = consumerClassEntry.getKey();
				ListenerHandle handle = this.subscribeImpl(params);
				allHandles.add(handle);
			}
			return mergeHandles(allHandles);
		}
		else
		{
			// single subscription

			final Subscription subscription = new Subscription();
			
			Consumer<T> listener = params.listener;
			
			if (params.weak)
			{
				listener = new WeakConsumer<>(this, listener);
			}
			subscription.listener = listener;

			if (params.eventCondition != null)
			{
				subscription.condition = params.eventCondition;
			}
			
			configureThreading(subscription, params);

			subscription.errorHandler = new CompositeErrorConsumer(params.errorConsumers);
			
			subscription.subscribedAt = Instant.now();
			
			List<Subscription<Event>> perEvent = data.computeIfAbsent((Class<Event>) params.eventClass, aClass -> new ArrayList<>());
			perEvent.add(subscription);
			ListenerHandle handle = new DefaultListenerHandle<>(params.eventClass, subscription);

			// cancelling on cancellation event
			if (params.cancellationEventClass != null)
			{
				final UnsubscribingListener<K> cancelListener = new UnsubscribingListener<>();
				final ListenerHandle cancelListenerHandle = subscribe(
						params.cancellationEventClass, 
						cancelListener,
						params.cancellationEventCondition, 
						null, 
						null
				);
				handle = mergeHandles(handle, cancelListenerHandle);
				cancelListener.handleToCancel = handle;
			}

			// storing handle for cleanup after weak reference removal
			if (params.weak)
			{
				handle = new WeakListenerHandle(this, handle);
				weakListeners.put(handle, (WeakConsumer<Event>) listener);
			}

			return handle;
		}
	}

	private <T extends Event> void configureThreading(Subscription subscription, SubscriptionParameters<T, ?> params)
	{
		final boolean isEdt = SwingUtilities.isEventDispatchThread();

		if (params.afterThread != null)
		{
			subscription.executor = new AfterThreadExecutor(params.afterThread, params.edt || params.asyncEdt);
		}
		else if (params.asyncEdt || params.edt && params.async)
		{
			subscription.executor = asyncEdtScheduler;
		}
		else if (params.edt)
		{
			subscription.executor = syncEdtScheduler;
		}
		else if (params.executor != null)
		{
			subscription.executor = params.executor;
		}
		else if (params.async)
		{
			subscription.executor = Executors.newSingleThreadExecutor();
		}
		else if (isEdt)
		{
			// if subscribe was called from EDT - sync EDT scheduler
			subscription.executor = syncEdtScheduler;
		}
		if (params.delay != null)
		{
			subscription.delay = params.delay;
		}
	}

	/**
	 * Performs periodical maintenance of this event bus. 
	 * Now it's cleaning of dead weak listeners wrappers.
	 * In future may be used to implement memory leaks detection and to calculate stats.
	 */
	void attemptMaintenance()
	{
		Instant now = Instant.now();
		if (now.isAfter(lastMaintenance.plus(MAINTENANCE_INTERVAL)))
		{
			lastMaintenance = now;
			if (!weakListeners.isEmpty())
			{
				weakListeners.entrySet()
						.stream()
						.filter(e -> e.getValue().isRemoved())
						.map(Map.Entry::getKey)
						// creating another list to avoid ConcurrentModificationException
						.collect(toList())
						.forEach(ListenerHandle::cancel);
			}
		}
	}

	private ListenerHandle mergeHandles(ListenerHandle... handles)
	{
		List<ListenerHandle> handlesList = Arrays.asList(handles);
		return mergeHandles(handlesList);
	}


	private ListenerHandle mergeHandles(final List<ListenerHandle> handles)
	{
		return new CompositeListenerHandle(this, handles);
	}
	
	private class DefaultListenerHandle<T extends Event, K extends Subscription<T>> implements ListenerHandle {
		
		private Class<T> eventClass;
		private Subscription<T> subscription;


		DefaultListenerHandle(Class<T> eventClass, Subscription<T> subscription)
		{
			this.eventClass = eventClass;
			this.subscription = subscription;
		}

		@SuppressWarnings("SuspiciousMethodCalls")
		@Override
		public void cancel()
		{
			synchronized (EventBusImpl.this)
			{
				final List<Subscription<Event>> perEvent = data.get(eventClass);
				if (perEvent != null)
				{
					perEvent.remove(subscription);
					if (perEvent.isEmpty())
					{
						data.remove(eventClass);
					}
				}
			}
		}

		@SuppressWarnings("SuspiciousMethodCalls")
		@Override
		public boolean isActive()
		{
			synchronized (EventBusImpl.class)
			{
				List<Subscription<Event>> perEvent = data.get(eventClass);
				return perEvent != null && perEvent.contains(subscription);
			}
		}
	}
	
	private static class MethodConsumer<T extends Event> implements Consumer<T>
	{
		private int argIndex;
		private Method method;
		private Object target;

		MethodConsumer(int argIndex, Method method, Object target)
		{
			this.argIndex = argIndex;
			this.method = method;
			this.target = target;
		}

		@Override
		public void accept(T o)
		{
			Object[] args = new Object[method.getParameterTypes().length];
			args[argIndex] = o;
			try
			{
				method.invoke(target, args);
			}
			catch (IllegalAccessException | InvocationTargetException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	private static class UnsubscribingListener<T extends Event> implements Consumer<T> 
	{
		private ListenerHandle handleToCancel;

		@Override
		public void accept(T event)
		{
			handleToCancel.cancel();
		}
	}

    private static List<Method> getMethodsListWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        Validator.isTrue(cls != null, "The class must not be null");
        Validator.isTrue(annotationCls != null, "The annotation class must not be null");
        final Method[] allMethods = cls.getMethods();
        final List<Method> annotatedMethods = new ArrayList<>();
        for (final Method method : allMethods) {
            if (method.getAnnotation(annotationCls) != null) {
                annotatedMethods.add(method);
            }
        }
        return annotatedMethods;
    }
}
