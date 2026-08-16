package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The single bridge from a cache <em>struct id</em> (what {@link NexusCatalog}
 * resolves a click to) back to the {@link Destination} enum the store and panel
 * are keyed on. Struct ids are stable across game updates, so this binding is
 * rename-proof: a Jagex rename of "Kharyrll" changes the cache <em>name</em> but
 * never struct {@code 461}, which still maps to {@link Destination#NEXUS_KHARYRLL}.
 *
 * <p>This is the only place nexus struct ids are hardcoded. Per-use cost is not
 * duplicated here — it comes from the resolved {@code Destination}'s own
 * {@link Destination#getCostBasis()}, so there is no second cost table to drift.
 * A couple of destinations own two structs (an "alt" label sharing one teleport,
 * e.g. Mooring Point's boat variants); both map to the same {@code Destination}.
 */
public final class NexusStructIndex
{
	private NexusStructIndex()
	{
	}

	private static final Map<Integer, Destination> BY_STRUCT = build();
	private static final Set<Destination> MAPPED = Collections.unmodifiableSet(new HashSet<>(BY_STRUCT.values()));
	private static final Map<Destination, Integer> STRUCT_FOR_DEST = reverse();

	/** The {@link Destination} for a nexus struct id, or null if we have no binding for it. */
	public static Destination forStruct(int structId)
	{
		return BY_STRUCT.get(structId);
	}

	/** Every {@link Destination} that at least one struct maps to (for completeness checks). */
	public static Set<Destination> mappedDestinations()
	{
		return MAPPED;
	}

	/** A struct id that maps to this destination (any one, for fetching its cache name), or null. */
	public static Integer structFor(Destination d)
	{
		return STRUCT_FOR_DEST.get(d);
	}

	/** Number of struct bindings (a couple of destinations own two structs). */
	public static int size()
	{
		return BY_STRUCT.size();
	}

	private static Map<Destination, Integer> reverse()
	{
		Map<Destination, Integer> m = new HashMap<>();
		for (Map.Entry<Integer, Destination> e : BY_STRUCT.entrySet())
		{
			m.putIfAbsent(e.getValue(), e.getKey()); // first struct wins; alts share the same name
		}
		return m;
	}

	private static Map<Integer, Destination> build()
	{
		Map<Integer, Destination> m = new HashMap<>();

		// --- Standard spellbook ---
		m.put(450, Destination.NEXUS_VARROCK);
		m.put(451, Destination.NEXUS_GRAND_EXCHANGE);
		m.put(452, Destination.NEXUS_LUMBRIDGE);
		m.put(453, Destination.NEXUS_FALADOR);
		m.put(454, Destination.NEXUS_CAMELOT);
		m.put(455, Destination.NEXUS_SEERS_VILLAGE);
		m.put(463, Destination.NEXUS_KOUREND);
		m.put(456, Destination.NEXUS_ARDOUGNE);
		m.put(855, Destination.NEXUS_CIVITAS);
		m.put(457, Destination.NEXUS_WATCHTOWER);
		m.put(458, Destination.NEXUS_YANILLE);
		m.put(6414, Destination.NEXUS_TROLLHEIM);
		m.put(460, Destination.NEXUS_MARIM);

		// --- Arceuus spellbook ---
		m.put(2887, Destination.NEXUS_ARCEUUS_LIBRARY);
		m.put(1246, Destination.NEXUS_DRAYNOR_MANOR);
		m.put(1247, Destination.NEXUS_BATTLEFRONT);
		m.put(1248, Destination.NEXUS_MIND_ALTAR);
		m.put(6422, Destination.NEXUS_RESPAWN);
		m.put(1249, Destination.NEXUS_SALVE_GRAVEYARD);
		m.put(1250, Destination.NEXUS_FENKENSTRAIN);
		m.put(1251, Destination.NEXUS_WEST_ARDOUGNE);
		m.put(1252, Destination.NEXUS_HARMONY);
		m.put(1253, Destination.NEXUS_CEMETERY);
		m.put(1254, Destination.NEXUS_BARROWS);
		m.put(1255, Destination.NEXUS_APE_ATOLL_DUNGEON);

		// --- Ancient Magicks ---
		m.put(6415, Destination.NEXUS_PADDEWWA);
		m.put(459, Destination.NEXUS_SENNTISTEN);
		m.put(461, Destination.NEXUS_KHARYRLL);
		m.put(6416, Destination.NEXUS_LASSAR);
		m.put(6417, Destination.NEXUS_DAREEYAK);
		m.put(470, Destination.NEXUS_CARRALLANGER);
		m.put(466, Destination.NEXUS_ANNAKARL);
		m.put(469, Destination.NEXUS_GHORROCK);

		// --- Lunar spellbook ---
		m.put(462, Destination.NEXUS_LUNAR_ISLE);
		m.put(6418, Destination.NEXUS_OURANIA);
		m.put(464, Destination.NEXUS_WATERBIRTH);
		m.put(6419, Destination.NEXUS_BARBARIAN);
		m.put(6420, Destination.NEXUS_KHAZARD);
		m.put(465, Destination.NEXUS_FISHING_GUILD);
		m.put(468, Destination.NEXUS_CATHERBY);
		m.put(6421, Destination.NEXUS_ICE_PLATEAU);

		// --- No priceable spellbook equivalent (count-only) ---
		m.put(467, Destination.NEXUS_TROLL_STRONGHOLD);
		m.put(592, Destination.NEXUS_WEISS);
		m.put(1187, Destination.NEXUS_MOORING_POINT); // "Last Boat" variant
		m.put(6423, Destination.NEXUS_MOORING_POINT); // "Boat" variant — same destination

		return m;
	}
}
