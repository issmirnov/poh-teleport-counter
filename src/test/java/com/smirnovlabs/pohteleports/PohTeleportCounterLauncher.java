package com.smirnovlabs.pohteleports;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Dev launcher: starts a RuneLite client with this plugin side-loaded.
 * Run via {@code ./gradlew run}. Not a JUnit test.
 */
public class PohTeleportCounterLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PohTeleportCounterPlugin.class);
		RuneLite.main(args);
	}
}
