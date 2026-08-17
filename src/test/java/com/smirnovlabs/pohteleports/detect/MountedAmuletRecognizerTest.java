package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MountedAmuletRecognizerTest
{
	private static class St implements GameStateView
	{
		public int getVarbit(int id)
		{
			return 0;
		}

		public boolean isInPoh()
		{
			return true;
		}

		public int currentTick()
		{
			return 0;
		}

		public int[] playerPos()
		{
			return null;
		}
	}

	@Test
	public void namedRightClickResolves()
	{
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, Transport.MOUNTED_DIGSITE,
			Map.of("digsite", Destination.MDIG_DIGSITE));
		assertEquals(Optional.of(Destination.MDIG_DIGSITE),
			r.onMenuInteraction(new MenuInteraction("Digsite", "Mounted digsite pendant", 500), new St()));
	}

	@Test
	public void menuNewNumberedPickResolves()
	{
		// The mounted "Teleport menu" pick is synthesized from MENU_NEW widget text like
		// "1: Digsite" (numbered option); stripKey must drop the "1: " so it matches by name.
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, Transport.MOUNTED_DIGSITE,
			Map.of("digsite", Destination.MDIG_DIGSITE, "fossil island", Destination.MDIG_FOSSIL));
		assertEquals(Optional.of(Destination.MDIG_DIGSITE),
			r.onMenuInteraction(new MenuInteraction("1: Digsite", "Digsite Pendant", 500), new St()));
		assertEquals(Optional.of(Destination.MDIG_FOSSIL),
			r.onMenuInteraction(new MenuInteraction("2: Fossil Island", "Digsite Pendant", 500), new St()));
	}

	@Test
	public void xericsMenuLabelsResolveViaRealNames()
	{
		// The Xeric's "Teleport menu" (MENU_NEW) labels are "Xeric's Lookout/Glade/Inferno/
		// Heart/Honour" (live capture) — the real Destination names must match them.
		Map<String, Destination> names = new HashMap<>();
		for (Destination d : Destination.values())
		{
			if (d.getTransport() == Transport.MOUNTED_XERICS && !d.getId().endsWith(":unknown"))
			{
				names.put(d.getDisplayName().toLowerCase(Locale.ROOT), d);
			}
		}
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(33412, Transport.MOUNTED_XERICS, names);
		assertEquals(Optional.of(Destination.MXERIC_INFERNO),
			r.onMenuInteraction(new MenuInteraction("3: Xeric's Inferno", "", 33412), new St()));
		assertEquals(Optional.of(Destination.MXERIC_HEART),
			r.onMenuInteraction(new MenuInteraction("4: Xeric's Heart", "", 33412), new St()));
	}

	@Test
	public void nonTeleportOptionIgnored()
	{
		// Examine / Rub on the object must NOT count.
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, Transport.MOUNTED_DIGSITE, Map.of());
		assertEquals(Optional.empty(),
			r.onMenuInteraction(new MenuInteraction("Examine", "Mounted digsite pendant", 500), new St()));
	}

	@Test
	public void teleportMenuOpenNotCounted()
	{
		// "Teleport menu" (opens the dialog) contains "teleport" but must NOT count.
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, Transport.MOUNTED_DIGSITE, Map.of());
		assertEquals(Optional.empty(),
			r.onMenuInteraction(new MenuInteraction("Teleport menu", "Digsite Pendant", 500), new St()));
	}

	@Test
	public void otherObjectIgnored()
	{
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, Transport.MOUNTED_DIGSITE, Map.of());
		assertEquals(Optional.empty(),
			r.onMenuInteraction(new MenuInteraction("Digsite", "x", 999), new St()));
	}

	@Test
	public void genericTeleportFallsToUnknownBucket()
	{
		// A bare "Teleport" (no named destination) counts as the transport's Unknown bucket.
		MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, Transport.MOUNTED_DIGSITE, Map.of());
		assertEquals(Optional.of(Destination.MDIG_UNKNOWN),
			r.onMenuInteraction(new MenuInteraction("Teleport", "Mounted digsite pendant", 500), new St()));
	}
}
