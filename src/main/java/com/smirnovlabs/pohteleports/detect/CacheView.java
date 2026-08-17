package com.smirnovlabs.pohteleports.detect;

/**
 * Narrow, read-only window onto the game cache — the two enum accessors and two
 * struct-param accessors {@link NexusCatalog} needs, and nothing else. Mirrors
 * the {@link GameStateView} pattern: production adapts the RuneLite {@code Client}
 * to this interface, tests supply a plain map-backed fake, so the catalog's
 * folding logic is unit-testable without mocking the whole client.
 *
 * <p>All methods must be called on the client thread in production. Accessors
 * return an "absent" value ({@code empty array} / {@code 0} / {@code null})
 * rather than throwing when the cache is not yet loaded, so a caller can probe
 * readiness by checking {@link #enumKeys}.
 */
public interface CacheView
{
	/** Keys of an {@code int->int} enum; an empty array if the enum is not loaded yet. */
	int[] enumKeys(int enumId);

	/** Value of an {@code int->int} enum at {@code key} (0 if absent). */
	int enumValue(int enumId, int key);

	/** Int-valued struct parameter (0 if the struct or the param is absent). */
	int structInt(int structId, int paramId);

	/** String-valued struct parameter (null if the struct or the param is absent). */
	String structString(int structId, int paramId);
}
