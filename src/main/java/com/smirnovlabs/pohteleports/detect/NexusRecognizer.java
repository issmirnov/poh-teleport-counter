package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import java.util.Map;
import java.util.Optional;

/**
 * Nexus teleports. A list pick (or a named left-click default) is resolved by
 * the destination name; a generic left-click default on the nexus object is
 * resolved from the configured-default varbit. The varbit branch is scoped to
 * the nexus object id so other transports' unrecognized "Teleport" clicks are
 * never mis-claimed.
 */
public class NexusRecognizer implements TeleportRecognizer
{
	private final int nexusObjectId;
	private final Map<Integer, Destination> varbitDefaultMap; // varbit value -> Destination
	private final Map<String, Destination> nameMap;           // lowercased name -> Destination

	public NexusRecognizer(int nexusObjectId, Map<Integer, Destination> varbitDefaultMap, Map<String, Destination> nameMap)
	{
		this.nexusObjectId = nexusObjectId;
		this.varbitDefaultMap = varbitDefaultMap;
		this.nameMap = nameMap;
	}

	@Override
	public Optional<Destination> onMenuInteraction(MenuInteraction e, GameStateView state)
	{
		String option = e.optionLower();
		if (option.contains("menu") || option.contains("configure") || option.contains("build") || option.contains("set ") || option.contains("add "))
		{
			return Optional.empty(); // opening the list / config / build — never a teleport
		}
		Destination byName = TeleportRecognizer.matchName(e, nameMap);
		if (byName != null)
		{
			return Optional.of(byName);
		}
		// generic default: only on the nexus object itself
		if (e.getId() == nexusObjectId && (option.contains("teleport") || option.isEmpty()))
		{
			Destination byVarbit = varbitDefaultMap.get(state.getVarbit(PohGameIds.NEXUS_DEFAULT_DEST_VARBIT));
			return Optional.of(byVarbit != null ? byVarbit : Destination.unknownFor(Transport.NEXUS));
		}
		return Optional.empty();
	}
}
