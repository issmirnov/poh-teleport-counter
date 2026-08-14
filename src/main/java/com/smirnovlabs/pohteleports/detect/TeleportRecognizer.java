package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import java.util.Map;
import java.util.Optional;

/** Turns a single menu interaction into a resolved teleport {@link Destination}, or nothing. */
public interface TeleportRecognizer
{
	Optional<Destination> onMenuInteraction(MenuInteraction e, GameStateView state);

	/**
	 * Match a destination name against either the option or the target text.
	 * Different transports put the destination in different fields (e.g. a
	 * mounted-amulet right-click names it in the option, a nexus list pick in
	 * the target), so we check both.
	 */
	static Destination matchName(MenuInteraction e, Map<String, Destination> nameMap)
	{
		Destination d = nameMap.get(e.optionLower());
		return d != null ? d : nameMap.get(e.targetLower());
	}
}
