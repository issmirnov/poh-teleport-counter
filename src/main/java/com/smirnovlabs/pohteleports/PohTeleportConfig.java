package com.smirnovlabs.pohteleports;

import com.smirnovlabs.pohteleports.model.SortMode;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pohteleports")
public interface PohTeleportConfig extends Config
{
	@ConfigItem(
		keyName = "sortMode",
		name = "Sort by",
		position = 1,
		description = "Order sections and rows by use count or by GP saved"
	)
	default SortMode sortMode()
	{
		return SortMode.MOST_USED;
	}

	@ConfigItem(
		keyName = "countGuestPoh",
		name = "Count guest POHs",
		position = 2,
		description = "Also count teleports you make in another player's house"
	)
	default boolean countGuestPoh()
	{
		return true;
	}
}
