package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MountedAmuletRecognizerTest
{
	private static class St implements GameStateView
	{
		int v = 0;
		public int getVarbit(int id) { return v; }
		public boolean isInPoh() { return true; }
		public int currentTick() { return 0; }
		public int[] playerPos() { return null; }
	}

	@Test
	public void namedRightClickResolves()
	{
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, 700, Transport.MOUNTED_DIGSITE,
			Map.of("digsite", Destination.MDIG_DIGSITE), Map.of());
		assertEquals(Optional.of(Destination.MDIG_DIGSITE),
			r.onMenuInteraction(new MenuInteraction("Digsite", "Mounted digsite pendant", 500), new St()));
	}

	@Test
	public void genericDefaultUsesVarbit()
	{
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, 700, Transport.MOUNTED_DIGSITE,
			Map.of(), Map.of(3, Destination.MDIG_DIGSITE));
		St st = new St();
		st.v = 3;
		assertEquals(Optional.of(Destination.MDIG_DIGSITE),
			r.onMenuInteraction(new MenuInteraction("Teleport", "Mounted digsite pendant", 500), st));
	}

	@Test
	public void nonTeleportOptionIgnored()
	{
		// Examine / Rub on the object must NOT count (was falling through to the Unknown bucket)
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, 700, Transport.MOUNTED_DIGSITE, Map.of(), Map.of());
		assertEquals(Optional.empty(),
			r.onMenuInteraction(new MenuInteraction("Examine", "Mounted digsite pendant", 500), new St()));
	}

	@Test
	public void teleportMenuOpenNotCounted()
	{
		// "Teleport menu" (opens the dialog) contains "teleport" but must NOT count
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, 700, Transport.MOUNTED_DIGSITE, Map.of(), Map.of());
		assertEquals(Optional.empty(),
			r.onMenuInteraction(new MenuInteraction("Teleport menu", "Digsite Pendant", 500), new St()));
	}

	@Test
	public void otherObjectIgnored()
	{
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, 700, Transport.MOUNTED_DIGSITE, Map.of(), Map.of());
		assertEquals(Optional.empty(),
			r.onMenuInteraction(new MenuInteraction("Digsite", "x", 999), new St()));
	}

	@Test
	public void unknownDefaultFallsBackToBucket()
	{
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, 700, Transport.MOUNTED_DIGSITE, Map.of(), Map.of());
		St st = new St();
		st.v = 42;
		assertEquals(Optional.of(Destination.MDIG_UNKNOWN),
			r.onMenuInteraction(new MenuInteraction("Teleport", "Mounted digsite pendant", 500), st));
	}
}
