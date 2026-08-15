package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import java.util.Map;
import java.util.Optional;

/**
 * Mounted amulets that expose destinations as right-click options directly on
 * the object: mounted glory, mounted Xeric's talisman, mounted Digsite pendant.
 * Scoped by object id. A named option resolves by name; a generic "Teleport"
 * default resolves from the configured-default varbit; other options (Examine,
 * Rub, Configure, ...) are ignored. One instance per transport.
 */
public class MountedAmuletRecognizer implements TeleportRecognizer
{
	private final int objectId;
	private final int defaultVarbit;
	private final Transport transport;
	private final Map<String, Destination> nameMap;
	private final Map<Integer, Destination> varbitDefaultMap;

	public MountedAmuletRecognizer(int objectId, int defaultVarbit, Transport transport,
		Map<String, Destination> nameMap, Map<Integer, Destination> varbitDefaultMap)
	{
		this.objectId = objectId;
		this.defaultVarbit = defaultVarbit;
		this.transport = transport;
		this.nameMap = nameMap;
		this.varbitDefaultMap = varbitDefaultMap;
	}

	@Override
	public Optional<Destination> onMenuInteraction(MenuInteraction e, GameStateView state)
	{
		if (e.getId() != objectId)
		{
			return Optional.empty();
		}
		Destination byName = TeleportRecognizer.matchName(e, nameMap);
		if (byName != null)
		{
			return Optional.of(byName);
		}
		// Generic left-click default (e.g. "Teleport") -> configured-default varbit.
		// Non-teleport options (Examine, Rub, Configure, Set-default, ...) are ignored.
		if (e.optionLower().contains("teleport"))
		{
			Destination byVarbit = varbitDefaultMap.get(state.getVarbit(defaultVarbit));
			return Optional.of(byVarbit != null ? byVarbit : Destination.unknownFor(transport));
		}
		return Optional.empty();
	}
}
