package com.smirnovlabs.pohteleports;

import net.runelite.client.plugins.PluginDescriptor;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SmokeTest
{
	@Test
	public void pluginNameIsSet()
	{
		PluginDescriptor d = PohTeleportCounterPlugin.class.getAnnotation(PluginDescriptor.class);
		assertEquals("PoH Teleport Counter", d.name());
	}
}
