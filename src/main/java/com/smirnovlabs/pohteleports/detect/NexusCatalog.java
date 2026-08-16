package com.smirnovlabs.pohteleports.detect;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reads the Portal Nexus destination table from the game cache at runtime, so
 * destination names never have to be hardcoded or guessed.
 *
 * <p>enum {@code 1377} maps an int destination-id to a teleport struct; struct
 * param {@code 660} is the display name and params {@code 661/662/663} are the
 * left-click object ids (one per portal skin: marble / gilded / crystalline).
 * The alt&rarr;primary link (param {@code 680}) and the scry variant
 * (destination-id {@code + 150}) are folded in at build time, so every
 * per-teleport lookup is a plain O(1) map get with no further cache call.
 *
 * <p>Built lazily <em>exactly once</em> per session via {@link #ensureLoaded} —
 * nothing is read at plugin start-up, and if the cache is not ready (or the ids
 * are wrong) it stays unloaded and retries next time. All calls must be on the
 * client thread. Reads go through the narrow {@link CacheView} so the folding
 * logic is unit-testable without a live client.
 */
public final class NexusCatalog
{
	private static final int SCRY_OFFSET = 150;

	private boolean loaded;
	private final Map<Integer, Integer> destToStruct = new HashMap<>();   // enum key (+scry) -> primary struct id
	private final Map<Integer, String> structToName = new HashMap<>();    // primary struct id -> game name
	private final Map<String, Integer> nameToStruct = new HashMap<>();    // lowercased game name -> primary struct id
	private final Map<Integer, Integer> objectToStruct = new HashMap<>(); // nexus object id -> primary struct id

	/**
	 * Build the maps on first call; later calls are a cheap boolean check. Safe to
	 * call before the cache is ready — it stays unloaded and retries next time.
	 */
	public void ensureLoaded(CacheView cache)
	{
		if (loaded)
		{
			return;
		}
		int[] keys = cache.enumKeys(PohGameIds.NEXUS_DEST_ENUM);
		if (keys.length == 0)
		{
			return; // cache not ready (or the enum id is wrong) — retry later, stay unloaded
		}
		for (int destId : keys)
		{
			int ownStruct = cache.enumValue(PohGameIds.NEXUS_DEST_ENUM, destId);
			if (ownStruct <= 0)
			{
				continue;
			}
			int primary = cache.structInt(ownStruct, PohGameIds.STRUCT_PARAM_PRIMARY_STRUCT);
			int structId = primary > 0 ? primary : ownStruct;

			destToStruct.put(destId, structId);
			destToStruct.put(destId + SCRY_OFFSET, structId); // scry variant resolves to the same destination

			if (!structToName.containsKey(structId))
			{
				String name = cache.structString(structId, PohGameIds.STRUCT_PARAM_NAME);
				if (name != null && !name.isEmpty())
				{
					structToName.put(structId, name);
					nameToStruct.put(name.toLowerCase(Locale.ROOT), structId);
				}
			}
			for (int p : new int[]{PohGameIds.STRUCT_PARAM_OBJ_MARBLE, PohGameIds.STRUCT_PARAM_OBJ_GILDED,
				PohGameIds.STRUCT_PARAM_OBJ_CRYSTALLINE})
			{
				int obj = cache.structInt(ownStruct, p);
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

	/** Primary struct id for a cache display name (case-insensitive), or null. O(1). */
	public Integer structForName(String name)
	{
		return name == null ? null : nameToStruct.get(name.toLowerCase(Locale.ROOT));
	}

	/** Cache-sourced display name for a struct id, or null if unknown. */
	public String name(int structId)
	{
		return structToName.get(structId);
	}
}
