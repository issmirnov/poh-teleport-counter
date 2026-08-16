package com.smirnovlabs.pohteleports.detect;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.StructComposition;

/**
 * Reads the Portal Nexus destination table from the game cache at runtime, so
 * destination names never have to be hardcoded or guessed.
 *
 * <p>enum {@code 1377} maps an int destination-id to a teleport struct; struct
 * param {@code 660} is the display name and params {@code 661/662/663} are the
 * left-click object ids (one per portal skin: marble / gilded / crystalline).
 * The alt&rarr;primary link (param {@code 680}) and the scry variant
 * (destination-id {@code + 150}) are folded in at build time, so the per-teleport
 * lookup is a plain O(1) map get with no further cache call.
 *
 * <p>Built lazily <em>exactly once</em> per session via {@link #ensureLoaded} —
 * nothing is read at plugin start-up. All calls must be on the client thread.
 */
public final class NexusCatalog
{
	private static final int SCRY_OFFSET = 150;

	private boolean loaded;
	private final Map<Integer, Integer> destToStruct = new HashMap<>();   // enum key (+scry) -> primary struct id
	private final Map<Integer, String> structToName = new HashMap<>();    // struct id -> game name
	private final Map<Integer, Integer> objectToStruct = new HashMap<>(); // nexus object id -> struct id

	/**
	 * Build the maps on first call; later calls are a cheap boolean check. Safe to
	 * call before the cache is ready — it stays unloaded and retries next time.
	 */
	public void ensureLoaded(Client client)
	{
		if (loaded)
		{
			return;
		}
		EnumComposition e = client.getEnum(PohGameIds.NEXUS_DEST_ENUM);
		if (e == null)
		{
			return;
		}
		for (int destId : e.getKeys())
		{
			StructComposition s = client.getStructComposition(e.getIntValue(destId));
			if (s == null)
			{
				continue;
			}
			int primary = s.getIntValue(PohGameIds.STRUCT_PARAM_PRIMARY_STRUCT);
			int structId = primary > 0 ? primary : s.getId();
			destToStruct.put(destId, structId);
			destToStruct.put(destId + SCRY_OFFSET, structId); // scry variant resolves to the same destination
			structToName.computeIfAbsent(structId, id ->
			{
				StructComposition ps = id == s.getId() ? s : client.getStructComposition(id);
				return ps == null ? null : ps.getStringValue(PohGameIds.STRUCT_PARAM_NAME);
			});
			for (int p : new int[]{PohGameIds.STRUCT_PARAM_OBJ_MARBLE, PohGameIds.STRUCT_PARAM_OBJ_GILDED,
				PohGameIds.STRUCT_PARAM_OBJ_CRYSTALLINE})
			{
				int obj = s.getIntValue(p);
				if (obj > 0)
				{
					objectToStruct.put(obj, structId);
				}
			}
		}
		loaded = !structToName.isEmpty();
	}

	public boolean isLoaded()
	{
		return loaded;
	}

	/** Primary struct id for a raw slot / left-click varbit value, or null. O(1), no cache call. */
	public Integer structForDestValue(int destValue)
	{
		return destToStruct.get(destValue);
	}

	/** Primary struct id for a clicked nexus object id, or null. O(1), no cache call. */
	public Integer structForObject(int objectId)
	{
		return objectToStruct.get(objectId);
	}

	/** Cache-sourced display name for a struct id, or null if unknown. */
	public String name(int structId)
	{
		return structToName.get(structId);
	}
}
