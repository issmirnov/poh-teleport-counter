package com.smirnovlabs.pohteleports.detect;

/**
 * Real client ids for the PoH transports. Values verified against
 * {@code net.runelite.api.gameval} (VarbitID / InterfaceID / ObjectID) and the
 * decompiled {@code telenexus_*} / {@code poh_jewellery_box_*} clientscripts,
 * except the three nexus/mounted scenery object ids (see notes) which have no
 * symbolic gameval name and must be confirmed in live QA.
 */
public final class PohGameIds
{
	private PohGameIds()
	{
	}

	// ---- Nexus cache data (enum 1377 -> struct; struct params) ----
	/** enum(int destination-id -&gt; struct). {@code getEnum(1377)}. */
	public static final int NEXUS_DEST_ENUM = 1377;
	/** Struct param: destination display name (string). */
	public static final int STRUCT_PARAM_NAME = 660;
	/** Struct params: left-click object id per portal skin (marble / gilded / crystalline). */
	public static final int STRUCT_PARAM_OBJ_MARBLE = 661;
	public static final int STRUCT_PARAM_OBJ_GILDED = 662;
	public static final int STRUCT_PARAM_OBJ_CRYSTALLINE = 663;
	/** Struct param: primary struct id for an "alt" teleport (e.g. Grand Exchange -&gt; Varrock). */
	public static final int STRUCT_PARAM_PRIMARY_STRUCT = 680;

	// Interface group ids (WidgetLoaded#getGroupId)
	public static final int NEXUS_INTERFACE_GROUP = 17;        // InterfaceID.TELENEXUS_TELEPORT
	public static final int JEWELLERY_BOX_INTERFACE_GROUP = 590; // InterfaceID.POH_JEWELLERY_BOX

	// Clientscripts that fire for a jewellery-box teleport (click + keyboard both reach these).
	public static final int JBOX_OP_SCRIPT = 1690;       // poh_jewellery_box_op
	public static final int JBOX_KEYPRESS_SCRIPT = 1689; // poh_jewellery_box_keypress

	// Varbits
	/** POH_NEXUS_LEFT_CLICK: the configured left-click default's destination-id (enum-1377 key). */
	public static final int NEXUS_DEFAULT_DEST_VARBIT = 6653;
	/** POH_NEXUS_TELE_SCRY_MODE: 0 = teleport mode, 1 = scry mode (do not count a scry). */
	public static final int NEXUS_SCRY_MODE_VARBIT = 6671;
	// No configurable-default varbit exists for the mounted amulets (they have a fixed
	// left-click op); left at -1 so getVarbit() short-circuits to the Unknown bucket.
	public static final int MOUNTED_XERICS_DEFAULT_VARBIT = -1;
	public static final int MOUNTED_DIGSITE_DEFAULT_VARBIT = -1;

	// Scenery object ids (MenuOptionClicked#getId on the object)
	// NOTE: the Portal Nexus and mounted Xeric's/Digsite objects have no symbolic
	// gameval ObjectID name (nothing in the 33xxx range) — confirm these live.
	public static final int NEXUS_OBJECT = 33410;
	public static final int MOUNTED_GLORY_OBJECT = 13523; // ObjectID.POH_TROPHY_AMULETOFGLORY_4 (confirmed)
	public static final int MOUNTED_XERICS_OBJECT = 33412;
	public static final int MOUNTED_DIGSITE_OBJECT = 33417;

	// POH region ids allowlist (Client#getMapRegions) — captured live.
	public static final int[] POH_REGIONS = {8046, 8047};
}
