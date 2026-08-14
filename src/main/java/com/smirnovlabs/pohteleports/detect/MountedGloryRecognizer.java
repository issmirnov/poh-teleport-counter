package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import java.util.Map;
import java.util.Optional;

/**
 * Mounted glory: a two-step interaction (Rub the object, then pick a
 * destination in a dialog whose group id is shared with unrelated NPC
 * dialogs). We arm on the Rub and only then resolve the next destination
 * pick, so we never match a stray dialog. Must be tried before the jewellery
 * box recognizer (they share destination names like "Edgeville").
 */
public class MountedGloryRecognizer implements TeleportRecognizer
{
	private final int gloryObjectId;
	private final Map<String, Destination> nameMap;
	private boolean armed = false;

	public MountedGloryRecognizer(int gloryObjectId, Map<String, Destination> nameMap)
	{
		this.gloryObjectId = gloryObjectId;
		this.nameMap = nameMap;
	}

	@Override
	public Optional<Destination> onMenuInteraction(MenuInteraction e, GameStateView state)
	{
		if (e.getId() == gloryObjectId && e.optionLower().contains("rub"))
		{
			armed = true;
			return Optional.empty();
		}
		if (armed)
		{
			Destination d = TeleportRecognizer.matchName(e, nameMap);
			if (d != null)
			{
				armed = false;
				return Optional.of(d);
			}
		}
		return Optional.empty();
	}
}
