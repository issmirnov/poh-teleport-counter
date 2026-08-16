package com.smirnovlabs.pohteleports.detect;

/** The slice of live game state detection needs, so it can be faked in tests. */
public interface GameStateView
{
	int getVarbit(int varbitId);

	/** True while the player is in any POH (own or a guest's), by region allowlist. */
	boolean isInPoh();

	int currentTick();

	/**
	 * The player's world position as {@code {x, y, plane}}, or null if unavailable.
	 * Used to confirm a teleport by the large single-tick coordinate jump it causes.
	 */
	int[] playerPos();
}
