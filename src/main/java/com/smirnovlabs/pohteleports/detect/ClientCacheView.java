package com.smirnovlabs.pohteleports.detect;

import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.StructComposition;

/**
 * Production {@link CacheView}: adapts the RuneLite {@link Client}'s cache
 * accessors, null-guarding each so an unloaded enum/struct reads as "absent"
 * ({@code empty} / {@code 0} / {@code null}) instead of throwing. Must be used
 * on the client thread. Enum/struct compositions are cached by the client, so
 * the repeated {@code getEnum}/{@code getStructComposition} calls are cheap.
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
		EnumComposition e = client.getEnum(enumId);
		return e == null ? new int[0] : e.getKeys();
	}

	@Override
	public int enumValue(int enumId, int key)
	{
		EnumComposition e = client.getEnum(enumId);
		return e == null ? 0 : e.getIntValue(key);
	}

	@Override
	public int structInt(int structId, int paramId)
	{
		StructComposition s = client.getStructComposition(structId);
		return s == null ? 0 : s.getIntValue(paramId);
	}

	@Override
	public String structString(int structId, int paramId)
	{
		StructComposition s = client.getStructComposition(structId);
		return s == null ? null : s.getStringValue(paramId);
	}
}
