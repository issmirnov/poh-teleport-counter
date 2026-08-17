package com.smirnovlabs.pohteleports.detect;

/**
 * Tracks whether the player is currently in <em>their own</em> POH or a guest's,
 * for the "count guest POHs" config toggle. Map regions can't tell them apart
 * (POH regions follow the house's chosen location, not its owner), so we use the
 * entry method instead: you reach a guest's house by clicking "Visit" on a House
 * Advertisement board; you reach your own by any other means (portal, spell,
 * tablet). A pending board-visit is "consumed" by the next instance entry, and
 * everything resets on leaving the instance.
 *
 * <p>Pure and client-free (driven by two booleans) so it is unit-testable.
 */
public final class HouseOwnershipTracker
{
	/** Ticks a pending board-visit stays valid (covers browsing the board) before we assume it was cancelled. */
	private static final int PENDING_TTL = 100;

	private boolean inGuestHouse;
	private int pendingVisitTicks; // > 0 while a board-visit awaits the house entry
	private boolean wasInstanced;

	/** Player clicked "Visit" on a House Advertisement board — the next house entered is a guest's. */
	public void onBoardVisit()
	{
		pendingVisitTicks = PENDING_TTL;
	}

	/** Call every game tick with whether the player is currently in an instanced region. */
	public void onTick(boolean instanced)
	{
		if (instanced && !wasInstanced)
		{
			inGuestHouse = pendingVisitTicks > 0; // entered a POH: guest iff a board-visit was pending
			pendingVisitTicks = 0;
		}
		else if (!instanced)
		{
			inGuestHouse = false; // outside any POH
			if (pendingVisitTicks > 0)
			{
				pendingVisitTicks--; // expire a visit that never led into a house (cancelled)
			}
		}
		wasInstanced = instanced;
	}

	/** True while the player is in another player's house (not their own). */
	public boolean inGuestHouse()
	{
		return inGuestHouse;
	}
}
