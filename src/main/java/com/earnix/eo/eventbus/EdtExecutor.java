package com.earnix.eo.eventbus;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;

/**
 * Schedules runnable's into EDT. Allows to try or avoid synchronous execution if current thread is EDT.
 */
class EdtExecutor implements Executor
{
	private final boolean synchronous;

	EdtExecutor(boolean synchronous)
	{
		this.synchronous = synchronous;
	}

	@Override
	public void execute(Runnable run)
	{
		if (synchronous)
		{
			if (SwingUtilities.isEventDispatchThread())
			{
				run.run();
			}
			else
			{
				try
				{
					SwingUtilities.invokeAndWait(run);
				}
				catch (InterruptedException | InvocationTargetException e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		else
		{
			SwingUtilities.invokeLater(run);
		}
	}
}
