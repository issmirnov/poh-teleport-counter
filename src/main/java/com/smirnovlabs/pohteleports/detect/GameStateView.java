package com.smirnovlabs.pohteleports.detect;

/** The slice of live game state detection needs, so it can be faked in tests. */
public interface GameStateView
{
	int getVarbit(int varbitId);

	/** True while the player is in any POH (own or a guest's), by region allowlist. */
	boolean isInPoh();

	int currentTick();
}
