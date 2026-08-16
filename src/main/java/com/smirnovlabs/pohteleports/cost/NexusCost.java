package com.smirnovlabs.pohteleports.cost;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * The per-use rune cost of the equivalent spellbook teleport for each Portal
 * Nexus destination, keyed by the destination's cache <em>struct id</em>
 * (stable across game updates, unlike the display name — a Jagex rename of
 * "Kharyrll" to "Kharyrll (Canifis)" does not touch struct 461).
 *
 * <p>This is the one thing the cache does not give us: the nexus struct stores
 * the one-time <em>unlock</em> items, not the equivalent spell's per-cast runes.
 * Names come from the cache (struct param 660); only these costs are hardcoded.
 * Struct ids are the keys of enum 1377 (cross-referenced with nexus-map's
 * RegionDef and the current wiki rune costs). Destinations with no priceable
 * spellbook equivalent map to {@link CostBasis#NONE} (count-only).
 */
public final class NexusCost
{
	private NexusCost()
	{
	}

	private static final Map<Integer, CostBasis> BY_STRUCT = build();

	/** Equivalent-spell cost for a nexus destination struct id; {@link CostBasis#NONE} if unpriced/unknown. */
	public static CostBasis of(int structId)
	{
		return BY_STRUCT.getOrDefault(structId, CostBasis.NONE);
	}

	/** True if we hold an explicit entry (priced or explicitly NONE) for this struct id. */
	public static boolean isKnown(int structId)
	{
		return BY_STRUCT.containsKey(structId);
	}

	/** Number of nexus destinations we have a cost entry for (should track enum 1377's size). */
	public static int size()
	{
		return BY_STRUCT.size();
	}

	private static Map<Integer, CostBasis> build()
	{
		Map<Integer, CostBasis> m = new HashMap<>();

		// --- Standard spellbook ---
		m.put(450, runes(ItemID.AIRRUNE, 3, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1));   // Varrock
		m.put(451, runes(ItemID.AIRRUNE, 3, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1));   // Grand Exchange (alt of Varrock)
		m.put(452, runes(ItemID.AIRRUNE, 3, ItemID.EARTHRUNE, 1, ItemID.LAWRUNE, 1));  // Lumbridge
		m.put(453, runes(ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1));  // Falador
		m.put(454, runes(ItemID.AIRRUNE, 5, ItemID.LAWRUNE, 1));                       // Camelot
		m.put(455, runes(ItemID.AIRRUNE, 5, ItemID.LAWRUNE, 1));                       // Seers' Village (alt of Camelot)
		m.put(463, runes(ItemID.FIRERUNE, 1, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 2)); // Kourend Castle
		m.put(456, runes(ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 2));                     // (East) Ardougne
		m.put(855, runes(ItemID.EARTHRUNE, 1, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 2)); // Civitas illa Fortis
		m.put(457, runes(ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 2));                     // Watchtower
		m.put(458, runes(ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 2));                     // Yanille (alt of Watchtower)
		m.put(6414, runes(ItemID.FIRERUNE, 2, ItemID.LAWRUNE, 2));                     // Trollheim
		m.put(460, runes(ItemID.FIRERUNE, 2, ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 2)); // Marim (Ape Atoll)

		// --- Arceuus spellbook ---
		m.put(2887, runes(ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 1));                    // Arceuus Library
		m.put(1246, runes(ItemID.EARTHRUNE, 1, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1)); // Draynor Manor
		m.put(1247, runes(ItemID.EARTHRUNE, 1, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1)); // Battlefront
		m.put(1248, runes(ItemID.LAWRUNE, 1, ItemID.MINDRUNE, 2));                     // Mind Altar
		m.put(6422, runes(ItemID.LAWRUNE, 1, ItemID.SOULRUNE, 1));                     // Respawn Point
		m.put(1249, runes(ItemID.LAWRUNE, 1, ItemID.SOULRUNE, 2));                     // Salve Graveyard
		m.put(1250, runes(ItemID.EARTHRUNE, 1, ItemID.LAWRUNE, 1, ItemID.SOULRUNE, 1)); // Fenkenstrain's Castle
		m.put(1251, runes(ItemID.LAWRUNE, 2, ItemID.SOULRUNE, 2));                     // West Ardougne
		m.put(1252, runes(ItemID.LAWRUNE, 1, ItemID.NATURERUNE, 1, ItemID.SOULRUNE, 1)); // Harmony Island
		m.put(1253, runes(ItemID.BLOODRUNE, 1, ItemID.LAWRUNE, 1, ItemID.SOULRUNE, 1)); // (Forgotten) Cemetery
		m.put(1254, runes(ItemID.BLOODRUNE, 1, ItemID.LAWRUNE, 2, ItemID.SOULRUNE, 2)); // Barrows
		m.put(1255, runes(ItemID.BLOODRUNE, 2, ItemID.LAWRUNE, 2, ItemID.SOULRUNE, 2)); // Ape Atoll Dungeon

		// --- Ancient Magicks ---
		m.put(6415, runes(ItemID.AIRRUNE, 1, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 2));  // Paddewwa (Edgeville Dungeon)
		m.put(459, runes(ItemID.LAWRUNE, 2, ItemID.SOULRUNE, 1));                      // Senntisten (Digsite)
		m.put(461, runes(ItemID.BLOODRUNE, 1, ItemID.LAWRUNE, 2));                     // Kharyrll (Canifis)
		m.put(6416, runes(ItemID.WATERRUNE, 4, ItemID.LAWRUNE, 2));                    // Lassar (Ice Mountain)
		m.put(6417, runes(ItemID.AIRRUNE, 2, ItemID.FIRERUNE, 3, ItemID.LAWRUNE, 2));  // Dareeyak (Crazy Archaeologist)
		m.put(470, runes(ItemID.LAWRUNE, 2, ItemID.SOULRUNE, 2));                      // Carrallanger (Graveyard of Shadows)
		m.put(466, runes(ItemID.BLOODRUNE, 2, ItemID.LAWRUNE, 2));                     // Annakarl (Demonic Ruins)
		m.put(469, runes(ItemID.WATERRUNE, 8, ItemID.LAWRUNE, 2));                     // Ghorrock (Frozen Waste Plateau)

		// --- Lunar spellbook ---
		m.put(462, runes(ItemID.EARTHRUNE, 2, ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 1)); // Lunar Isle
		m.put(6418, runes(ItemID.EARTHRUNE, 6, ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 1)); // Ourania Cave
		m.put(464, runes(ItemID.WATERRUNE, 1, ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 1)); // Waterbirth Island
		m.put(6419, runes(ItemID.FIRERUNE, 3, ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 2)); // Barbarian Outpost
		m.put(6420, runes(ItemID.WATERRUNE, 4, ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 2)); // Port Khazard
		m.put(465, runes(ItemID.WATERRUNE, 10, ItemID.ASTRALRUNE, 3, ItemID.LAWRUNE, 3)); // Fishing Guild
		m.put(468, runes(ItemID.WATERRUNE, 10, ItemID.ASTRALRUNE, 3, ItemID.LAWRUNE, 3)); // Catherby
		m.put(6421, runes(ItemID.WATERRUNE, 8, ItemID.ASTRALRUNE, 3, ItemID.LAWRUNE, 3)); // Ice Plateau

		// --- No priceable spellbook equivalent (count-only) ---
		m.put(467, CostBasis.NONE);   // Troll Stronghold
		m.put(592, CostBasis.NONE);   // Weiss
		m.put(1187, CostBasis.NONE);  // Last Boat (Mooring Point)
		m.put(6423, CostBasis.NONE);  // Boat (Mooring Point)

		return m;
	}

	private static CostBasis runes(int id1, int q1)
	{
		return CostBasis.runes(Map.of(id1, q1));
	}

	private static CostBasis runes(int id1, int q1, int id2, int q2)
	{
		return CostBasis.runes(Map.of(id1, q1, id2, q2));
	}

	private static CostBasis runes(int id1, int q1, int id2, int q2, int id3, int q3)
	{
		return CostBasis.runes(Map.of(id1, q1, id2, q2, id3, q3));
	}
}
