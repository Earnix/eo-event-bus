package com.earnix.eo.eventbus;

/**
 * Event(s) subscription(s) result, which provides API to cancel subscription and to check whether it is still subscribed.
 */
public interface ListenerHandle
{
	/**
	 * Cancels subscription(s)
	 */
	void cancel();

	/**
	 * Checks whether subscription(s) is/are still active
	 * @return <code>true</code> if subscription(s) is/are still active
	 */
	boolean isActive();
}