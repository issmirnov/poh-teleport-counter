package com.smirnovlabs.pohteleports.model;

/** A free, pre-paid Player-Owned House teleport source. */
public enum Transport
{
	NEXUS("Teleport Nexus"),
	JEWELLERY_BOX("Jewellery box"),
	// All three are the mounted (POH) variants — "Mounted" is dropped from the label since
	// there is no other way to use them here, and the shorter names fit the panel width.
	MOUNTED_GLORY("Amulet of glory"),
	MOUNTED_XERICS("Xeric's talisman"),
	MOUNTED_DIGSITE("Digsite pendant");

	private final String displayName;

	Transport(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
