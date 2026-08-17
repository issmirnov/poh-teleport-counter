package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import java.util.Map;
import java.util.Optional;

/**
 * Mounted amulets that expose destinations as right-click options directly on
 * the object: mounted glory, mounted Xeric's talisman, mounted Digsite pendant.
 * Scoped by object id. A named option resolves by name; a generic "Teleport"
 * (with no named destination) falls to the count-only Unknown bucket; other
 * options (Examine, Rub, Configure, "Teleport menu", ...) are ignored. One
 * instance per transport.
 */
public class MountedAmuletRecognizer implements TeleportRecognizer
{
	private final int objectId;
	private final Transport transport;
	private final Map<String, Destination> nameMap;

	public MountedAmuletRecognizer(int objectId, Transport transport, Map<String, Destination> nameMap)
	{
		this.objectId = objectId;
		this.transport = transport;
		this.nameMap = nameMap;
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
		// A bare "Teleport" on the object (no named destination — mounted amulets have no
		// configurable left-click default) counts, as the transport's Unknown bucket.
		String option = e.optionLower();
		if (!TeleportRecognizer.isIgnoredOption(option) && option.contains("teleport"))
		{
			return Optional.of(Destination.unknownFor(transport));
		}
		return Optional.empty();
	}
}
