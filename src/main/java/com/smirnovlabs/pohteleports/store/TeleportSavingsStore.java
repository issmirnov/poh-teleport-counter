package com.smirnovlabs.pohteleports.store;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.TeleportEvent;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.config.ConfigManager;

/**
 * Source of truth for per-destination use counts. Persists ONLY the counts
 * (GP is derived live from a static basis), keyed by stable
 * {@link Destination#getId()} strings so renames never crash a load.
 *
 * <p>Serialization is a pure method ({@link #toJson()} / {@link #loadJson})
 * so it is unit-testable without {@code ConfigManager}; the two
 * {@code ConfigManager} wrappers are thin one-liners.
 */
public class TeleportSavingsStore
{
	public static final String GROUP = "pohteleports";
	public static final String KEY_COUNTS = "counts";

	private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>()
	{
	}.getType();

	// The Plugin Hub forbids constructing fresh Gson instances; this is the client's injected Gson.
	private final Gson gson;
	private final EnumMap<Destination, Integer> counts = new EnumMap<>(Destination.class);

	public TeleportSavingsStore(Gson gson)
	{
		this.gson = gson;
	}

	public void record(TeleportEvent e)
	{
		counts.merge(e.getDestination(), 1, Integer::sum);
	}

	public int count(Destination d)
	{
		return counts.getOrDefault(d, 0);
	}

	public int totalCount()
	{
		int total = 0;
		for (int v : counts.values())
		{
			total += v;
		}
		return total;
	}

	public Map<Destination, Integer> snapshot()
	{
		return Collections.unmodifiableMap(new EnumMap<>(counts));
	}

	// --- pure serialization (testable without ConfigManager) ---

	String toJson()
	{
		Map<String, Integer> byId = new HashMap<>();
		for (Map.Entry<Destination, Integer> e : counts.entrySet())
		{
			byId.put(e.getKey().getId(), e.getValue());
		}
		return gson.toJson(byId, MAP_TYPE);
	}

	void loadJson(String json)
	{
		counts.clear();
		if (json == null || json.isEmpty())
		{
			return;
		}
		Map<String, Integer> byId = gson.fromJson(json, MAP_TYPE);
		if (byId == null)
		{
			return;
		}
		Map<String, Destination> index = new HashMap<>();
		for (Destination d : Destination.values())
		{
			index.put(d.getId(), d);
		}
		for (Map.Entry<String, Integer> e : byId.entrySet())
		{
			Destination d = index.get(e.getKey());
			if (d != null && e.getValue() != null)
			{
				counts.put(d, e.getValue());
			}
		}
	}

	// --- thin ConfigManager I/O ---

	public void persist(ConfigManager cm)
	{
		cm.setConfiguration(GROUP, KEY_COUNTS, toJson());
	}

	public void load(ConfigManager cm)
	{
		loadJson(cm.getConfiguration(GROUP, KEY_COUNTS));
	}
}
