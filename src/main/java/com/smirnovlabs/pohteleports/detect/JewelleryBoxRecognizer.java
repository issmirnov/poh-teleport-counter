package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import java.util.Map;
import java.util.Optional;

/**
 * Jewellery box teleports. Box entries are named, so resolve by the clicked
 * target text. The router's in-POH gate keeps worn-jewellery teleports (done
 * while adventuring) from reaching here.
 */
public class JewelleryBoxRecognizer implements TeleportRecognizer
{
	private final Map<String, Destination> nameMap;

	public JewelleryBoxRecognizer(Map<String, Destination> nameMap)
	{
		this.nameMap = nameMap;
	}

	@Override
	public Optional<Destination> onMenuInteraction(MenuInteraction e, GameStateView state)
	{
		if (TeleportRecognizer.isIgnoredOption(e.optionLower()))
		{
			return Optional.empty();
		}
		return Optional.ofNullable(TeleportRecognizer.matchName(e, nameMap));
	}
}
