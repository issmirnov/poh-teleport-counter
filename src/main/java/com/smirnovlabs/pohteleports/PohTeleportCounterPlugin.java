package com.smirnovlabs.pohteleports;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "PoH Teleport Counter",
	description = "Counts your free Player-Owned House teleports and the runes/charges they save",
	tags = {"poh", "teleport", "nexus", "jewellery", "construction"}
)
public class PohTeleportCounterPlugin extends Plugin
{
	@Override
	protected void startUp() throws Exception
	{
		log.debug("PoH Teleport Counter started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("PoH Teleport Counter stopped");
	}
}
