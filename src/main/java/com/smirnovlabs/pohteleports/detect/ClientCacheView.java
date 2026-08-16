package com.smirnovlabs.pohteleports.detect;

import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.StructComposition;

/**
 * Production {@link CacheView}: adapts the RuneLite {@link Client}'s cache
 * accessors. Every read is wrapped so an unloaded, missing, or wrong-typed
 * param reads as "absent" ({@code empty} / {@code 0} / {@code null}) instead of
 * throwing — {@code StructComposition.getIntValue} throws
 * {@code IllegalArgumentException} when the param is actually a string param, and
 * the catalog must never let that (or any cache surprise) escape into the click
 * handler. Must be used on the client thread.
 */
public final class ClientCacheView implements CacheView
{
	private final Client client;

	public ClientCacheView(Client client)
	{
		this.client = client;
	}

	@Override
	public int[] enumKeys(int enumId)
	{
		try
		{
			EnumComposition e = client.getEnum(enumId);
			return e == null ? new int[0] : e.getKeys();
		}
		catch (RuntimeException ex)
		{
			return new int[0];
		}
	}

	@Override
	public int enumValue(int enumId, int key)
	{
		try
		{
			EnumComposition e = client.getEnum(enumId);
			return e == null ? 0 : e.getIntValue(key);
		}
		catch (RuntimeException ex)
		{
			return 0;
		}
	}

	@Override
	public int structInt(int structId, int paramId)
	{
		try
		{
			StructComposition s = client.getStructComposition(structId);
			return s == null ? 0 : s.getIntValue(paramId);
		}
		catch (RuntimeException ex)
		{
			return 0; // e.g. paramId is a string param on this struct
		}
	}

	@Override
	public String structString(int structId, int paramId)
	{
		try
		{
			StructComposition s = client.getStructComposition(structId);
			return s == null ? null : s.getStringValue(paramId);
		}
		catch (RuntimeException ex)
		{
			return null; // e.g. paramId is an int param on this struct
		}
	}
}
