package com.smirnovlabs.pohteleports.model;

/** A confirmed teleport: which destination, and the game tick it fired on. */
public final class TeleportEvent
{
	private final Destination destination;
	private final int tick;

	public TeleportEvent(Destination destination, int tick)
	{
		this.destination = destination;
		this.tick = tick;
	}

	public Destination getDestination()
	{
		return destination;
	}

	public int getTick()
	{
		return tick;
	}
}
