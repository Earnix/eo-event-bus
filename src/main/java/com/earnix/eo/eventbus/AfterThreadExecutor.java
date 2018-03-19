package com.earnix.eo.eventbus;

import javax.swing.SwingUtilities;
import java.util.concurrent.Executor;

/**
 * Schedules runnable's to be executed after another thread's completion or interruption.
 */
class AfterThreadExecutor implements Executor
{
	private final Thread thread;
	private final boolean edt;

	AfterThreadExecutor(Thread thread, boolean edt)
	{
		this.thread = thread;
		this.edt = edt;
	}

	@Override
	public void execute(Runnable runnable)
	{
		new Thread(() -> {
			try
			{
				thread.join();
			}
			catch (InterruptedException ignored)
			{
			}
			if (edt)
			{
				SwingUtilities.invokeLater(runnable);
			}
			else
			{
				runnable.run();
			}
		}).start();
	}
}