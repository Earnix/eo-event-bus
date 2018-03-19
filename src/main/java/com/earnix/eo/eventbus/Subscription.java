package com.earnix.eo.eventbus;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents internal subscription state
 */
class Subscription<T extends Event>
{
	Executor executor;
	Duration delay;
	Instant subscribedAt;
	Consumer<T> listener;
	Predicate<T> condition;
	Consumer<Exception> errorHandler;
}
