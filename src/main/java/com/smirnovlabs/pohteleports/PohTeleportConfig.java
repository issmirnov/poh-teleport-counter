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

	@ConfigItem(
		keyName = "debugLogMenus",
		name = "Debug: log menu clicks",
		position = 3,
		description = "Log each menu click's option/target/id and map regions — used to capture PoH ids during setup"
	)
	default boolean debugLogMenus()
	{
		return false;
	}

	@ConfigItem(
		keyName = "debugLogVarbits",
		name = "Debug: log varbit changes",
		position = 4,
		description = "Log varbit/varp changes — used to find the Nexus destination varbit for keyboard selection"
	)
	default boolean debugLogVarbits()
	{
		return false;
	}
}
