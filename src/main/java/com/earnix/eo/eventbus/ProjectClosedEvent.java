package com.earnix.eo.eventbus;

/**
 * Event, which must be published after project closure.
 */
public class ProjectClosedEvent implements Event
{
	private final int projectPK;

	public ProjectClosedEvent(int projectPK)
	{
		this.projectPK = projectPK;
	}

	public int getProjectPK()
	{
		return projectPK;
	}
}