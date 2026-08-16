package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class NexusRecognizerTest
{
	private static final int NEXUS_OBJ = 100;

	private final Map<String, Destination> nameMap = Map.of(
		"varrock", Destination.NEXUS_VARROCK,
		"camelot", Destination.NEXUS_CAMELOT);
	private final Map<Integer, Destination> varbitDefault = Map.of(2, Destination.NEXUS_CAMELOT);

	private static class St implements GameStateView
	{
		int v = 0;
		public int getVarbit(int id) { return v; }
		public boolean isInPoh() { return true; }
		public int currentTick() { return 0; }
		public int[] playerPos() { return null; }
	}

	private NexusRecognizer rec()
	{
		return new NexusRecognizer(NEXUS_OBJ, varbitDefault, nameMap);
	}

	@Test
	public void listPickResolvesByName()
	{
		assertEquals(Optional.of(Destination.NEXUS_VARROCK),
			rec().onMenuInteraction(new MenuInteraction("Teleport", "Varrock", 0), new St()));
	}

	@Test
	public void genericDefaultOnNexusObjectResolvesByVarbit()
	{
		St st = new St();
		st.v = 2; // encodes Camelot
		assertEquals(Optional.of(Destination.NEXUS_CAMELOT),
			rec().onMenuInteraction(new MenuInteraction("Teleport", "", NEXUS_OBJ), st));
	}

	@Test
	public void openingMenuIsNotCounted()
	{
		assertEquals(Optional.empty(),
			rec().onMenuInteraction(new MenuInteraction("Teleport Menu", "Portal Nexus", NEXUS_OBJ), new St()));
	}

	@Test
	public void configureIsNotCounted()
	{
		assertEquals(Optional.empty(),
			rec().onMenuInteraction(new MenuInteraction("Configure", "Portal Nexus", NEXUS_OBJ), new St()));
	}

	@Test
	public void foreignTeleportIsIgnored()
	{
		// a jewellery-box "Teleport -> Ferox Enclave" (not on the nexus object, not a nexus name)
		assertEquals(Optional.empty(),
			rec().onMenuInteraction(new MenuInteraction("Teleport", "Ferox Enclave", 555), new St()));
	}

	@Test
	public void unknownDefaultFallsBackToBucket()
	{
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, Map.of(), nameMap);
		St st = new St();
		st.v = 99;
		assertEquals(Optional.of(Destination.NEXUS_UNKNOWN),
			r.onMenuInteraction(new MenuInteraction("Teleport", "", NEXUS_OBJ), st));
	}

	@Test
	public void listPickStripsKeyboardShortcutPrefix()
	{
		// Real nexus interface pick: option='Teleport' target='[4] Civitas illa Fortis'
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, varbitDefault,
			Map.of("civitas illa fortis", Destination.NEXUS_CIVITAS));
		assertEquals(Optional.of(Destination.NEXUS_CIVITAS),
			r.onMenuInteraction(new MenuInteraction("Teleport", "[4] Civitas illa Fortis", 1), new St()));
	}

	/** Build the nexus name map exactly like the plugin's byName(NEXUS) does. */
	private static java.util.Map<String, Destination> realNexusNameMap()
	{
		java.util.Map<String, Destination> m = new java.util.HashMap<>();
		for (Destination d : Destination.values())
		{
			if (d.getTransport() == com.smirnovlabs.pohteleports.model.Transport.NEXUS && !d.getId().endsWith(":unknown"))
			{
				m.put(d.getDisplayName().toLowerCase(java.util.Locale.ROOT), d);
			}
		}
		return m;
	}

	@Test
	public void realNameMapMatchesCapturedMenuPicks()
	{
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, Map.of(), realNexusNameMap());
		St st = new St();
		// Exactly as captured live:
		assertEquals("West Ardougne map-pick", Optional.of(Destination.NEXUS_WEST_ARDOUGNE),
			r.onMenuInteraction(new MenuInteraction("Teleport", "[K] West Ardougne", 1), st));
		assertEquals("Civitas list-pick", Optional.of(Destination.NEXUS_CIVITAS),
			r.onMenuInteraction(new MenuInteraction("Teleport", "[4] Civitas illa Fortis", 1), st));
		assertEquals("Grand Exchange direct object click", Optional.of(Destination.NEXUS_GRAND_EXCHANGE),
			r.onMenuInteraction(new MenuInteraction("Grand Exchange", "Portal Nexus", NEXUS_OBJ), st));
	}

	@Test
	public void nonBreakingSpaceSeparatorStillMatches()
	{
		// Live nexus picks separate the "[K]" shortcut from the name with a
		// non-breaking space (U+00A0), not a normal space — stripKey must fold it
		// or the lookup misses (the 2026-08-16 "no match" bug on every map pick).
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, Map.of(), realNexusNameMap());
		assertEquals(Optional.of(Destination.NEXUS_WEST_ARDOUGNE),
			r.onMenuInteraction(new MenuInteraction("Teleport", "[K] West Ardougne", 1), new St()));
		assertEquals(Optional.of(Destination.NEXUS_DAREEYAK),
			r.onMenuInteraction(new MenuInteraction("Teleport", "[R] Dareeyak (Crazy Archaeologist)", 1), new St()));
	}

	@Test
	public void objectDirectOptionNamesDestination()
	{
		// Real nexus object option: option='Grand Exchange' target='Portal Nexus'
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, varbitDefault,
			Map.of("grand exchange", Destination.NEXUS_GRAND_EXCHANGE));
		assertEquals(Optional.of(Destination.NEXUS_GRAND_EXCHANGE),
			r.onMenuInteraction(new MenuInteraction("Grand Exchange", "Portal Nexus", NEXUS_OBJ), new St()));
	}

	// ---- Cache-catalog rescue layer (only fires when the catalog is loaded) ----

	@Test
	public void catalogRescuesUnrecognizedNameViaCache()
	{
		// Name we deliberately do NOT have in the name map, but the cache does (struct 6417 = "Dareeyak").
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, varbitDefault, nameMap, loadedCatalog());
		assertEquals(Optional.of(Destination.NEXUS_DAREEYAK),
			r.onMenuInteraction(new MenuInteraction("Teleport", "[F] Dareeyak", 0), new St()));
	}

	@Test
	public void catalogResolvesGenericDefaultViaVarbit()
	{
		// Left-click "Teleport" on the object; varbit encodes dest 21 -> struct 461 = Kharyrll.
		St st = new St();
		st.v = 21;
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, varbitDefault, nameMap, loadedCatalog());
		assertEquals(Optional.of(Destination.NEXUS_KHARYRLL),
			r.onMenuInteraction(new MenuInteraction("Teleport", "", NEXUS_OBJ), st));
	}

	@Test
	public void unloadedCatalogBehavesLikeToday()
	{
		// Catalog present but never loaded (empty cache) -> generic default falls to the Unknown bucket.
		NexusCatalog cold = new NexusCatalog();
		cold.ensureLoaded(new EmptyCache());
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, Map.of(), nameMap, cold);
		St st = new St();
		st.v = 99;
		assertEquals(Optional.of(Destination.NEXUS_UNKNOWN),
			r.onMenuInteraction(new MenuInteraction("Teleport", "", NEXUS_OBJ), st));
	}

	@Test
	public void catalogDoesNotRescueForeignTeleports()
	{
		// A jewellery-box "Ferox Enclave" (not a nexus cache name, not on the nexus object) stays unclaimed.
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, varbitDefault, nameMap, loadedCatalog());
		assertEquals(Optional.empty(),
			r.onMenuInteraction(new MenuInteraction("Teleport", "Ferox Enclave", 555), new St()));
	}

	private static NexusCatalog loadedCatalog()
	{
		NexusCatalog c = new NexusCatalog();
		c.ensureLoaded(new RecCache());
		return c;
	}

	/** Minimal cache: two destinations (Dareeyak / Kharyrll) with names, no objects/alts. */
	private static final class RecCache implements CacheView
	{
		private final Map<Integer, Integer> dest = Map.of(20, 6417, 21, 461);
		private final Map<Integer, String> names = Map.of(6417, "Dareeyak", 461, "Kharyrll");

		public int[] enumKeys(int id)
		{
			return id == PohGameIds.NEXUS_DEST_ENUM ? new int[]{20, 21} : new int[0];
		}

		public int enumValue(int id, int key)
		{
			return id == PohGameIds.NEXUS_DEST_ENUM ? dest.getOrDefault(key, 0) : 0;
		}

		public int structInt(int structId, int paramId)
		{
			return 0;
		}

		public String structString(int structId, int paramId)
		{
			return paramId == PohGameIds.STRUCT_PARAM_NAME ? names.get(structId) : null;
		}
	}

	/** A cache that never becomes ready (enum absent). */
	private static final class EmptyCache implements CacheView
	{
		public int[] enumKeys(int id)
		{
			return new int[0];
		}

		public int enumValue(int id, int key)
		{
			return 0;
		}

		public int structInt(int structId, int paramId)
		{
			return 0;
		}

		public String structString(int structId, int paramId)
		{
			return null;
		}
	}
}
