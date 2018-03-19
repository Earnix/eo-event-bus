package com.earnix.eo.eventbus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Contains error handlers (consumers) registry (per Exception class) and calls 
 * corresponding (the closest in the hierarchy) consumer with passed Exception instance.
 */
class CompositeErrorConsumer implements Consumer<Exception>
{
	private final Map<Class<Exception>, Consumer<Exception>> consumers;

	CompositeErrorConsumer(Map<Class<Exception>, Consumer<Exception>> consumers)
	{
		// Sorting consumers by class hierarchy (concrete first) to allow error handlers "overrides".
		this.consumers = consumers.entrySet().stream().sorted((entry1, entry2) -> {
			final Class<Exception> class1 = entry1.getKey();
			final Class<Exception> class2 = entry2.getKey();

			if (class1.equals(class2))
			{
				return 0;
			}

			boolean firstLower = class2.isAssignableFrom(class1);
			boolean secondLower = class1.isAssignableFrom(class2);

			if (firstLower && !secondLower)
			{
				return -1;
			}
			else if (secondLower && !firstLower)
			{
				return 1;
			}
			return class1.getName().compareTo(class2.getName());
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> {
			throw new IllegalStateException();
		}, LinkedHashMap::new));
	}


	@Override
	public void accept(final Exception exception)
	{
		final Optional<Consumer<Exception>> consumer = consumers.entrySet().stream()
				.filter(entry -> entry.getKey().isAssignableFrom(exception.getClass()))
				.map(Map.Entry::getValue)
				.findFirst();

		if (consumer.isPresent())
		{
			try
			{
				consumer.get().accept(exception);
			}
			catch (Exception handlingException)
			{
				throwUncaught(handlingException);
			}
		}
		else
		{
			throwUncaught(exception);
		}
	}

	private void throwUncaught(Exception exception)
	{ 
		if (exception instanceof RuntimeException)
		{
			throw (RuntimeException) exception;
		}
		else
		{
			throw new RuntimeException(exception);
		}
	}
}
