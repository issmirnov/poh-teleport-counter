package com.smirnovlabs.pohteleports.model;

import com.smirnovlabs.pohteleports.cost.CostBasis;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import static com.smirnovlabs.pohteleports.model.Transport.JEWELLERY_BOX;
import static com.smirnovlabs.pohteleports.model.Transport.MOUNTED_DIGSITE;
import static com.smirnovlabs.pohteleports.model.Transport.MOUNTED_GLORY;
import static com.smirnovlabs.pohteleports.model.Transport.MOUNTED_XERICS;
import static com.smirnovlabs.pohteleports.model.Transport.NEXUS;

/**
 * Every teleport destination reachable from a PoH transport, with the
 * {@link CostBasis} describing what one use would otherwise have cost.
 *
 * <p>Display names match the in-game menu text (the Nexus UI's "[N] " shortcut
 * prefix is stripped before matching). Costs are wiki-sourced: Nexus =
 * equivalent spellbook teleport runes; jewellery box / mounted glory = item GE
 * price ÷ charges; mounted Digsite = (ruby necklace + Enchant Ruby Jewellery
 * runes) ÷ 5 (a crafted item, not bought); mounted Xeric's = lizardman fangs. Item
 * ids are real {@code gameval.ItemID} constants. A destination whose exact menu
 * label differs from the name here simply falls into the transport's
 * {@code *_UNKNOWN} bucket (still counted) until corrected.
 */
public enum Destination
{
	// ===================== NEXUS: runes = the equivalent spellbook teleport =====================
	// --- Standard spellbook ---
	NEXUS_VARROCK("nexus:varrock", "Varrock", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 3, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1))),
	NEXUS_GRAND_EXCHANGE("nexus:grand_exchange", "Grand Exchange", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 3, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1))),
	NEXUS_LUMBRIDGE("nexus:lumbridge", "Lumbridge", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 3, ItemID.EARTHRUNE, 1, ItemID.LAWRUNE, 1))),
	NEXUS_FALADOR("nexus:falador", "Falador", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1))),
	NEXUS_CAMELOT("nexus:camelot", "Camelot", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 5, ItemID.LAWRUNE, 1))),
	NEXUS_SEERS_VILLAGE("nexus:seers_village", "Seers' Village", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 5, ItemID.LAWRUNE, 1))),
	NEXUS_KOUREND("nexus:kourend", "Kourend Castle", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.FIRERUNE, 1, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 2))),
	NEXUS_ARDOUGNE("nexus:ardougne", "East Ardougne", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 2))),
	NEXUS_CIVITAS("nexus:civitas", "Civitas illa Fortis", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.EARTHRUNE, 1, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 2))),
	NEXUS_WATCHTOWER("nexus:watchtower", "Watchtower", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 2))),
	NEXUS_YANILLE("nexus:yanille", "Yanille", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 2))),
	NEXUS_TROLLHEIM("nexus:trollheim", "Trollheim", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.FIRERUNE, 2, ItemID.LAWRUNE, 2))),
	NEXUS_MARIM("nexus:marim", "Marim (Ape Atoll)", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.FIRERUNE, 2, ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 2))),

	// --- Arceuus spellbook ---
	NEXUS_ARCEUUS_LIBRARY("nexus:arceuus_library", "Arceuus Library", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 1))),
	NEXUS_DRAYNOR_MANOR("nexus:draynor_manor", "Draynor Manor", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.EARTHRUNE, 1, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1))),
	NEXUS_BATTLEFRONT("nexus:battlefront", "Battlefront", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.EARTHRUNE, 1, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1))),
	NEXUS_MIND_ALTAR("nexus:mind_altar", "Mind Altar", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.LAWRUNE, 1, ItemID.MINDRUNE, 2))),
	NEXUS_RESPAWN("nexus:respawn", "Respawn point", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.LAWRUNE, 1, ItemID.SOULRUNE, 1))),
	NEXUS_SALVE_GRAVEYARD("nexus:salve_graveyard", "Salve Graveyard", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.LAWRUNE, 1, ItemID.SOULRUNE, 2))),
	NEXUS_FENKENSTRAIN("nexus:fenkenstrain", "Fenkenstrain's Castle", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.EARTHRUNE, 1, ItemID.LAWRUNE, 1, ItemID.SOULRUNE, 1))),
	NEXUS_WEST_ARDOUGNE("nexus:west_ardougne", "West Ardougne", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.LAWRUNE, 2, ItemID.SOULRUNE, 2))),
	NEXUS_HARMONY("nexus:harmony", "Harmony Island", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.LAWRUNE, 1, ItemID.NATURERUNE, 1, ItemID.SOULRUNE, 1))),
	NEXUS_CEMETERY("nexus:cemetery", "The Forgotten Cemetery", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.BLOODRUNE, 1, ItemID.LAWRUNE, 1, ItemID.SOULRUNE, 1))),
	NEXUS_BARROWS("nexus:barrows", "Barrows", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.BLOODRUNE, 1, ItemID.LAWRUNE, 2, ItemID.SOULRUNE, 2))),
	NEXUS_APE_ATOLL_DUNGEON("nexus:ape_atoll_dungeon", "Ape Atoll Dungeon", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.BLOODRUNE, 2, ItemID.LAWRUNE, 2, ItemID.SOULRUNE, 2))),

	// --- Ancient Magicks (menu shows the Ancient spell name) ---
	NEXUS_PADDEWWA("nexus:paddewwa", "Paddewwa (Edgeville Dungeon)", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 1, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 2))),
	NEXUS_SENNTISTEN("nexus:senntisten", "Senntisten (Digsite)", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.LAWRUNE, 2, ItemID.SOULRUNE, 1))),
	NEXUS_KHARYRLL("nexus:kharyrll", "Kharyrll (Canifis)", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.BLOODRUNE, 1, ItemID.LAWRUNE, 2))),
	NEXUS_LASSAR("nexus:lassar", "Lassar (Ice Mountain)", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.WATERRUNE, 4, ItemID.LAWRUNE, 2))),
	NEXUS_DAREEYAK("nexus:dareeyak", "Dareeyak (Crazy Archaeologist)", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 2, ItemID.FIRERUNE, 3, ItemID.LAWRUNE, 2))),
	NEXUS_CARRALLANGER("nexus:carrallanger", "Carrallanger (Graveyard of Shadows)", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.LAWRUNE, 2, ItemID.SOULRUNE, 2))),
	NEXUS_ANNAKARL("nexus:annakarl", "Annakarl (Demonic Ruins)", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.BLOODRUNE, 2, ItemID.LAWRUNE, 2))),
	NEXUS_GHORROCK("nexus:ghorrock", "Ghorrock (Frozen Waste Plateau)", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.WATERRUNE, 8, ItemID.LAWRUNE, 2))),

	// --- Lunar spellbook ---
	NEXUS_LUNAR_ISLE("nexus:lunar_isle", "Lunar Isle", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.EARTHRUNE, 2, ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 1))),
	NEXUS_OURANIA("nexus:ourania", "Ourania Cave", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.EARTHRUNE, 6, ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 1))),
	NEXUS_WATERBIRTH("nexus:waterbirth", "Waterbirth Island", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.WATERRUNE, 1, ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 1))),
	NEXUS_BARBARIAN("nexus:barbarian", "Barbarian Outpost", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.FIRERUNE, 3, ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 2))),
	NEXUS_KHAZARD("nexus:khazard", "Port Khazard", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.WATERRUNE, 4, ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 2))),
	NEXUS_FISHING_GUILD("nexus:fishing_guild", "Fishing Guild", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.WATERRUNE, 10, ItemID.ASTRALRUNE, 3, ItemID.LAWRUNE, 3))),
	NEXUS_CATHERBY("nexus:catherby", "Catherby", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.WATERRUNE, 10, ItemID.ASTRALRUNE, 3, ItemID.LAWRUNE, 3))),
	NEXUS_ICE_PLATEAU("nexus:ice_plateau", "Ice Plateau", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.WATERRUNE, 8, ItemID.ASTRALRUNE, 3, ItemID.LAWRUNE, 3))),

	// --- Nexus destinations with no priceable spellbook equivalent (count only) ---
	NEXUS_TROLL_STRONGHOLD("nexus:troll_stronghold", "Troll Stronghold", NEXUS, null, CostBasis.NONE),
	NEXUS_WEISS("nexus:weiss", "Weiss", NEXUS, null, CostBasis.NONE),
	NEXUS_MOORING_POINT("nexus:mooring_point", "Mooring Point", NEXUS, null, CostBasis.NONE),

	// ===================== JEWELLERY BOX: itemFraction = item GE price / charges =====================
	// --- Ring of dueling (8 charges) ---
	JBOX_DUEL_FEROX("jbox:duel:ferox", "Ferox Enclave", JEWELLERY_BOX, "Ring of dueling",
		CostBasis.itemFraction(ItemID.RING_OF_DUELING_8, 8)),
	JBOX_DUEL_CASTLE_WARS("jbox:duel:castle_wars", "Castle Wars", JEWELLERY_BOX, "Ring of dueling",
		CostBasis.itemFraction(ItemID.RING_OF_DUELING_8, 8)),
	JBOX_DUEL_EMIRS("jbox:duel:emirs", "Emir's Arena", JEWELLERY_BOX, "Ring of dueling",
		CostBasis.itemFraction(ItemID.RING_OF_DUELING_8, 8)),
	JBOX_DUEL_COLOSSEUM("jbox:duel:colosseum", "Fortis Colosseum", JEWELLERY_BOX, "Ring of dueling",
		CostBasis.itemFraction(ItemID.RING_OF_DUELING_8, 8)),

	// --- Games necklace (8 charges) ---
	JBOX_GAMES_BURTHORPE("jbox:games:burthorpe", "Burthorpe", JEWELLERY_BOX, "Games necklace",
		CostBasis.itemFraction(ItemID.NECKLACE_OF_MINIGAMES_8, 8)),
	JBOX_GAMES_BARBARIAN("jbox:games:barbarian", "Barbarian Outpost", JEWELLERY_BOX, "Games necklace",
		CostBasis.itemFraction(ItemID.NECKLACE_OF_MINIGAMES_8, 8)),
	JBOX_GAMES_CORP("jbox:games:corp", "Corporeal Beast", JEWELLERY_BOX, "Games necklace",
		CostBasis.itemFraction(ItemID.NECKLACE_OF_MINIGAMES_8, 8)),
	JBOX_GAMES_TEARS("jbox:games:tears", "Tears of Guthix", JEWELLERY_BOX, "Games necklace",
		CostBasis.itemFraction(ItemID.NECKLACE_OF_MINIGAMES_8, 8)),
	JBOX_GAMES_WINTERTODT("jbox:games:wintertodt", "Wintertodt Camp", JEWELLERY_BOX, "Games necklace",
		CostBasis.itemFraction(ItemID.NECKLACE_OF_MINIGAMES_8, 8)),

	// --- Combat bracelet (6 charges) ---
	JBOX_COMBAT_WARRIORS("jbox:combat:warriors", "Warriors' Guild", JEWELLERY_BOX, "Combat bracelet",
		CostBasis.itemFraction(ItemID.JEWL_BRACELET_OF_COMBAT_6, 6)),
	JBOX_COMBAT_CHAMPIONS("jbox:combat:champions", "Champions' Guild", JEWELLERY_BOX, "Combat bracelet",
		CostBasis.itemFraction(ItemID.JEWL_BRACELET_OF_COMBAT_6, 6)),
	JBOX_COMBAT_MONASTERY("jbox:combat:monastery", "Monastery", JEWELLERY_BOX, "Combat bracelet",
		CostBasis.itemFraction(ItemID.JEWL_BRACELET_OF_COMBAT_6, 6)),
	JBOX_COMBAT_RANGING("jbox:combat:ranging", "Ranging Guild", JEWELLERY_BOX, "Combat bracelet",
		CostBasis.itemFraction(ItemID.JEWL_BRACELET_OF_COMBAT_6, 6)),

	// --- Skills necklace (6 charges) ---
	JBOX_SKILLS_FISHING("jbox:skills:fishing", "Fishing Guild", JEWELLERY_BOX, "Skills necklace",
		CostBasis.itemFraction(ItemID.JEWL_NECKLACE_OF_SKILLS_6, 6)),
	JBOX_SKILLS_MINING("jbox:skills:mining", "Mining Guild", JEWELLERY_BOX, "Skills necklace",
		CostBasis.itemFraction(ItemID.JEWL_NECKLACE_OF_SKILLS_6, 6)),
	JBOX_SKILLS_CRAFTING("jbox:skills:crafting", "Crafting Guild", JEWELLERY_BOX, "Skills necklace",
		CostBasis.itemFraction(ItemID.JEWL_NECKLACE_OF_SKILLS_6, 6)),
	JBOX_SKILLS_COOKING("jbox:skills:cooking", "Cooking Guild", JEWELLERY_BOX, "Skills necklace",
		CostBasis.itemFraction(ItemID.JEWL_NECKLACE_OF_SKILLS_6, 6)),
	JBOX_SKILLS_WOODCUTTING("jbox:skills:woodcutting", "Woodcutting Guild", JEWELLERY_BOX, "Skills necklace",
		CostBasis.itemFraction(ItemID.JEWL_NECKLACE_OF_SKILLS_6, 6)),
	JBOX_SKILLS_FARMING("jbox:skills:farming", "Farming Guild", JEWELLERY_BOX, "Skills necklace",
		CostBasis.itemFraction(ItemID.JEWL_NECKLACE_OF_SKILLS_6, 6)),

	// --- Amulet of glory (6 charges) ---
	JBOX_GLORY_EDGEVILLE("jbox:glory:edgeville", "Edgeville", JEWELLERY_BOX, "Amulet of glory",
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
	JBOX_GLORY_KARAMJA("jbox:glory:karamja", "Karamja", JEWELLERY_BOX, "Amulet of glory",
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
	JBOX_GLORY_DRAYNOR("jbox:glory:draynor", "Draynor Village", JEWELLERY_BOX, "Amulet of glory",
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
	JBOX_GLORY_AL_KHARID("jbox:glory:al_kharid", "Al Kharid", JEWELLERY_BOX, "Amulet of glory",
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),

	// --- Ring of wealth (5 charges) ---
	JBOX_WEALTH_MISCELLANIA("jbox:wealth:miscellania", "Miscellania", JEWELLERY_BOX, "Ring of wealth",
		CostBasis.itemFraction(ItemID.RING_OF_WEALTH_5, 5)),
	JBOX_WEALTH_GE("jbox:wealth:ge", "Grand Exchange", JEWELLERY_BOX, "Ring of wealth",
		CostBasis.itemFraction(ItemID.RING_OF_WEALTH_5, 5)),
	JBOX_WEALTH_FALADOR_PARK("jbox:wealth:falador_park", "Falador Park", JEWELLERY_BOX, "Ring of wealth",
		CostBasis.itemFraction(ItemID.RING_OF_WEALTH_5, 5)),
	JBOX_WEALTH_DONDAKAN("jbox:wealth:dondakan", "Dondakan's Rock", JEWELLERY_BOX, "Ring of wealth",
		CostBasis.itemFraction(ItemID.RING_OF_WEALTH_5, 5)),

	// ===================== MOUNTED GLORY (Quest Hall): itemFraction =====================
	MGLORY_EDGEVILLE("mglory:edgeville", "Edgeville", MOUNTED_GLORY, null,
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
	MGLORY_KARAMJA("mglory:karamja", "Karamja", MOUNTED_GLORY, null,
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
	MGLORY_DRAYNOR("mglory:draynor", "Draynor Village", MOUNTED_GLORY, null,
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
	MGLORY_AL_KHARID("mglory:al_kharid", "Al Kharid", MOUNTED_GLORY, null,
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),

	// ===================== MOUNTED XERIC'S TALISMAN: consumable (1 lizardman fang/use) =====================
	MXERIC_LOOKOUT("mxeric:lookout", "Xeric's Lookout", MOUNTED_XERICS, null,
		CostBasis.consumable(ItemID.LIZARDMAN_FANG, 1)),
	MXERIC_GLADE("mxeric:glade", "Xeric's Glade", MOUNTED_XERICS, null,
		CostBasis.consumable(ItemID.LIZARDMAN_FANG, 1)),
	MXERIC_INFERNO("mxeric:inferno", "Xeric's Inferno", MOUNTED_XERICS, null,
		CostBasis.consumable(ItemID.LIZARDMAN_FANG, 1)),
	MXERIC_HEART("mxeric:heart", "Xeric's Heart", MOUNTED_XERICS, null,
		CostBasis.consumable(ItemID.LIZARDMAN_FANG, 1)),
	MXERIC_HONOUR("mxeric:honour", "Xeric's Honour", MOUNTED_XERICS, null,
		CostBasis.consumable(ItemID.LIZARDMAN_FANG, 1)),

	// ============ MOUNTED DIGSITE PENDANT: craftedFraction = (ruby necklace + enchant runes) / 5 ============
	MDIG_DIGSITE("mdig:digsite", "Digsite", MOUNTED_DIGSITE, null,
		CostBasis.craftedFraction(ItemID.RUBY_NECKLACE, Map.of(ItemID.FIRERUNE, 5, ItemID.COSMICRUNE, 1), 5)),
	MDIG_FOSSIL("mdig:fossil", "Fossil Island", MOUNTED_DIGSITE, null,
		CostBasis.craftedFraction(ItemID.RUBY_NECKLACE, Map.of(ItemID.FIRERUNE, 5, ItemID.COSMICRUNE, 1), 5)),
	MDIG_LITHKREN("mdig:lithkren", "Lithkren", MOUNTED_DIGSITE, null,
		CostBasis.craftedFraction(ItemID.RUBY_NECKLACE, Map.of(ItemID.FIRERUNE, 5, ItemID.COSMICRUNE, 1), 5)),

	// ===================== Per-transport unknown/default buckets (count-only, no gp) =====================
	NEXUS_UNKNOWN("nexus:unknown", "Default / Unknown", NEXUS, null, CostBasis.NONE),
	JBOX_UNKNOWN("jbox:unknown", "Default / Unknown", JEWELLERY_BOX, null, CostBasis.NONE),
	MGLORY_UNKNOWN("mglory:unknown", "Default / Unknown", MOUNTED_GLORY, null, CostBasis.NONE),
	MXERIC_UNKNOWN("mxeric:unknown", "Default / Unknown", MOUNTED_XERICS, null, CostBasis.NONE),
	MDIG_UNKNOWN("mdig:unknown", "Default / Unknown", MOUNTED_DIGSITE, null, CostBasis.NONE);

	private final String id;
	private final String displayName;
	private final Transport transport;
	private final String subGroup;
	private final CostBasis costBasis;

	Destination(String id, String displayName, Transport transport, String subGroup, CostBasis costBasis)
	{
		this.id = id;
		this.displayName = displayName;
		this.transport = transport;
		this.subGroup = subGroup;
		this.costBasis = costBasis;
	}

	public String getId()
	{
		return id;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public Transport getTransport()
	{
		return transport;
	}

	/** Item sub-group for nested display (e.g. "Amulet of glory"); null = flat. */
	public String getSubGroup()
	{
		return subGroup;
	}

	public CostBasis getCostBasis()
	{
		return costBasis;
	}

	/** The count-only "Default / Unknown" bucket for a transport. */
	public static Destination unknownFor(Transport t)
	{
		switch (t)
		{
			case NEXUS: return NEXUS_UNKNOWN;
			case JEWELLERY_BOX: return JBOX_UNKNOWN;
			case MOUNTED_GLORY: return MGLORY_UNKNOWN;
			case MOUNTED_XERICS: return MXERIC_UNKNOWN;
			case MOUNTED_DIGSITE: return MDIG_UNKNOWN;
			default: throw new IllegalArgumentException(t.name());
		}
	}
}
