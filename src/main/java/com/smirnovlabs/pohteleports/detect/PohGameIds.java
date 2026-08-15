package com.smirnovlabs.pohteleports.detect;

/**
 * Real client ids for the PoH transports. Sentinel {@code -1} / empty until
 * captured live with the RuneLite Widget + Varbit inspectors (see the
 * implementation plan, Task 12). Recognizers take their ids by constructor so
 * they stay unit-testable; production wiring reads these constants.
 */
public final class PohGameIds
{
	private PohGameIds()
	{
	}

	// Interface group ids (WidgetLoaded#getGroupId)
	public static final int NEXUS_INTERFACE_GROUP = -1;
	public static final int JEWELLERY_BOX_INTERFACE_GROUP = -1;

	// Varbits: the currently-configured default destination per transport
	public static final int NEXUS_DEFAULT_DEST_VARBIT = -1;
	public static final int MOUNTED_XERICS_DEFAULT_VARBIT = -1;
	public static final int MOUNTED_DIGSITE_DEFAULT_VARBIT = -1;

	// Scenery object ids (MenuOptionClicked#getId on the object)
	public static final int NEXUS_OBJECT = 33410;
	public static final int MOUNTED_GLORY_OBJECT = -1;
	public static final int MOUNTED_XERICS_OBJECT = 33412;
	public static final int MOUNTED_DIGSITE_OBJECT = 33417;

	// POH region ids allowlist (Client#getMapRegions) — captured live.
	public static final int[] POH_REGIONS = {8046, 8047};
}
