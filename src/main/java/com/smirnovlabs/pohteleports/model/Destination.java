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
 * A single teleport destination reachable from a PoH transport, with the
 * {@link CostBasis} describing what one use would otherwise have cost.
 *
 * <p>This is a representative starter set. The full destination list is
 * populated as data entry from the OSRS Wiki (see the implementation plan,
 * Task 2 Step 6); {@code DestinationTableTest} enforces the coverage
 * invariants after entry. Item ids are the real {@code gameval.ItemID}
 * constants (e.g. the digsite pendant is {@code NECKLACE_OF_DIGSITE_5}).
 */
public enum Destination
{
	// --- Nexus: RUNES basis = the equivalent standard-spellbook teleport ---
	NEXUS_VARROCK("nexus:varrock", "Varrock", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 3, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1))),
	NEXUS_CAMELOT("nexus:camelot", "Camelot", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 5, ItemID.LAWRUNE, 1))),
	NEXUS_FALADOR("nexus:falador", "Falador", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1))),
	NEXUS_GRAND_EXCHANGE("nexus:grand_exchange", "Grand Exchange", NEXUS, null,
		CostBasis.runes(Map.of(ItemID.AIRRUNE, 3, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1))),
	NEXUS_CIVITAS("nexus:civitas", "Civitas illa Fortis", NEXUS, null, CostBasis.NONE),

	// --- Jewellery box: ITEM_FRACTION = item GE price / charges; subGroup = the item ---
	JBOX_GLORY_EDGEVILLE("jbox:glory:edgeville", "Edgeville", JEWELLERY_BOX, "Amulet of glory",
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
	JBOX_DUEL_FEROX("jbox:duel:ferox", "Ferox Enclave", JEWELLERY_BOX, "Ring of dueling",
		CostBasis.itemFraction(ItemID.RING_OF_DUELING_8, 8)),

	// --- Mounted glory (Quest Hall): ITEM_FRACTION ---
	MGLORY_EDGEVILLE("mglory:edgeville", "Edgeville", MOUNTED_GLORY, null,
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
	MGLORY_KARAMJA("mglory:karamja", "Karamja", MOUNTED_GLORY, null,
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
	MGLORY_DRAYNOR("mglory:draynor", "Draynor Village", MOUNTED_GLORY, null,
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
	MGLORY_AL_KHARID("mglory:al_kharid", "Al Kharid", MOUNTED_GLORY, null,
		CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),

	// --- Mounted Xeric's talisman: CONSUMABLE (lizardman fangs per use; qty TBD in live QA) ---
	MXERIC_LOOKOUT("mxeric:lookout", "Look-out", MOUNTED_XERICS, null,
		CostBasis.consumable(ItemID.LIZARDMAN_FANG, 1)),
	MXERIC_GLADE("mxeric:glade", "Glade", MOUNTED_XERICS, null,
		CostBasis.consumable(ItemID.LIZARDMAN_FANG, 1)),

	// --- Mounted Digsite pendant: ITEM_FRACTION = digsite pendant (5) / 5 ---
	MDIG_DIGSITE("mdig:digsite", "Digsite", MOUNTED_DIGSITE, null,
		CostBasis.itemFraction(ItemID.NECKLACE_OF_DIGSITE_5, 5)),
	MDIG_FOSSIL("mdig:fossil", "Fossil Island", MOUNTED_DIGSITE, null,
		CostBasis.itemFraction(ItemID.NECKLACE_OF_DIGSITE_5, 5)),

	// --- Per-transport unknown/default buckets (count-only, no gp) ---
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
