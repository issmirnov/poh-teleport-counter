package com.smirnovlabs.pohteleports.model;

/** A free, pre-paid Player-Owned House teleport source. */
public enum Transport
{
	NEXUS("Teleport Nexus"),
	JEWELLERY_BOX("Jewellery box"),
	MOUNTED_GLORY("Mounted glory"),
	MOUNTED_XERICS("Mounted Xeric's talisman"),
	MOUNTED_DIGSITE("Mounted Digsite pendant");

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
