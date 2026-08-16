package com.smirnovlabs.pohteleports.detect;

/**
 * Real client ids for the PoH transports. Nexus cache ids are verified against a
 * live capture; the three nexus/mounted scenery object ids have no symbolic
 * {@code gameval.ObjectID} name and were confirmed in live QA.
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
	/** Struct param: primary struct id for an "alt" teleport (e.g. Grand Exchange -&gt; Varrock). */
	public static final int STRUCT_PARAM_PRIMARY_STRUCT = 680;

	/** POH_NEXUS_LEFT_CLICK: the configured left-click default's destination-id (enum-1377 key). */
	public static final int NEXUS_DEFAULT_DEST_VARBIT = 6653;

	// Scenery object ids (MenuOptionClicked#getId on the object), confirmed in live QA.
	public static final int NEXUS_OBJECT = 33410;
	public static final int MOUNTED_GLORY_OBJECT = 13523; // ObjectID.POH_TROPHY_AMULETOFGLORY_4
	public static final int MOUNTED_XERICS_OBJECT = 33412;
	public static final int MOUNTED_DIGSITE_OBJECT = 33417;

	// Your own POH's map regions (Client#getMapRegions) — a fallback to isInInstancedRegion().
	public static final int[] POH_REGIONS = {8046, 8047};
}
