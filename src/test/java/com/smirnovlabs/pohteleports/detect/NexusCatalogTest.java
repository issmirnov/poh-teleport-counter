package com.smirnovlabs.pohteleports.detect;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Exercises the cache-folding logic (alt-&gt;primary, scry offset, name lookups) with a map-backed fake. */
public class NexusCatalogTest
{
	/** Map-backed {@link CacheView}: one enum (1377) plus per-struct int/string params. */
	private static final class FakeCache implements CacheView
	{
		final Map<Integer, Integer> dest = new LinkedHashMap<>();          // destId -> ownStruct
		final Map<Integer, Map<Integer, Integer>> ints = new HashMap<>();  // struct -> (param -> int)
		final Map<Integer, Map<Integer, String>> strs = new HashMap<>();   // struct -> (param -> string)

		FakeCache put(int destId, int struct, String name, int primary)
		{
			dest.put(destId, struct);
			if (name != null)
			{
				strs.computeIfAbsent(struct, k -> new HashMap<>()).put(PohGameIds.STRUCT_PARAM_NAME, name);
			}
			if (primary > 0)
			{
				ints.computeIfAbsent(struct, k -> new HashMap<>()).put(PohGameIds.STRUCT_PARAM_PRIMARY_STRUCT, primary);
			}
			return this;
		}

		@Override
		public int[] enumKeys(int enumId)
		{
			return enumId == PohGameIds.NEXUS_DEST_ENUM ? dest.keySet().stream().mapToInt(Integer::intValue).toArray() : new int[0];
		}

		@Override
		public int enumValue(int enumId, int key)
		{
			return enumId == PohGameIds.NEXUS_DEST_ENUM ? dest.getOrDefault(key, 0) : 0;
		}

		@Override
		public int structInt(int structId, int paramId)
		{
			return ints.getOrDefault(structId, Map.of()).getOrDefault(paramId, 0);
		}

		@Override
		public String structString(int structId, int paramId)
		{
			return strs.getOrDefault(structId, Map.of()).get(paramId);
		}
	}

	private NexusCatalog loaded()
	{
		FakeCache c = new FakeCache()
			.put(10, 450, "Varrock", 0)       // primary
			.put(11, 451, null, 450)          // Grand Exchange: alt of Varrock (name comes from primary)
			.put(12, 461, "Kharyrll", 0);     // primary
		NexusCatalog cat = new NexusCatalog();
		cat.ensureLoaded(c);
		return cat;
	}

	@Test
	public void loadsWhenCacheReady()
	{
		assertTrue(loaded().isLoaded());
	}

	@Test
	public void emptyCacheStaysUnloadedAndSafe()
	{
		NexusCatalog cat = new NexusCatalog();
		cat.ensureLoaded(new FakeCache()); // no dests => enumKeys empty
		assertFalse(cat.isLoaded());
		assertNull(cat.structForDestValue(10));
		assertNull(cat.structForName("varrock"));
	}

	@Test
	public void namesComeFromTheStruct()
	{
		assertEquals("Varrock", loaded().name(450));
		assertEquals("Kharyrll", loaded().name(461));
	}

	@Test
	public void sizeCountsDistinctNames()
	{
		// 450 (Varrock) + 461 (Kharyrll); 451 is an alt of 450 with no own name.
		assertEquals(2, loaded().size());
	}

	@Test
	public void altDestFoldsToPrimaryStruct()
	{
		assertEquals(Integer.valueOf(450), loaded().structForDestValue(10));
		assertEquals(Integer.valueOf(450), loaded().structForDestValue(11)); // GE alt -> Varrock primary
		assertEquals(Integer.valueOf(461), loaded().structForDestValue(12));
	}

	@Test
	public void scryVariantResolvesToSameDestination()
	{
		assertEquals(Integer.valueOf(450), loaded().structForDestValue(10 + 150));
		assertEquals(Integer.valueOf(461), loaded().structForDestValue(12 + 150));
	}

	@Test
	public void nameLookupIsCaseInsensitive()
	{
		assertEquals(Integer.valueOf(450), loaded().structForName("VARROCK"));
		assertEquals(Integer.valueOf(461), loaded().structForName("kharyrll"));
		assertNull(loaded().structForName("nowhere"));
	}

	// ---- Crash-safety: a cache read that throws (e.g. a wrong-typed param, the 2026-08-15
	// ---- getIntValue-on-string-param crash) must never propagate into the click handler. ----

	@Test
	public void survivesAThrowingStructReadAndStillLoadsOtherNames()
	{
		CacheView oneBad = new CacheView()
		{
			public int[] enumKeys(int id)
			{
				return id == PohGameIds.NEXUS_DEST_ENUM ? new int[]{10, 11} : new int[0];
			}

			public int enumValue(int id, int key)
			{
				return key == 10 ? 450 : 461;
			}

			public int structInt(int structId, int paramId)
			{
				if (structId == 450)
				{
					throw new IllegalArgumentException("trying to get int from string param");
				}
				return 0;
			}

			public String structString(int structId, int paramId)
			{
				return paramId == PohGameIds.STRUCT_PARAM_NAME && structId == 461 ? "Kharyrll" : null;
			}
		};

		NexusCatalog cat = new NexusCatalog();
		cat.ensureLoaded(oneBad); // must NOT throw
		assertTrue(cat.isLoaded());
		assertEquals("Kharyrll", cat.name(461));  // good destination loaded
		assertNull(cat.structForName("varrock")); // throwing destination skipped
	}

	@Test
	public void neverPropagatesWhenEveryStructReadThrows()
	{
		CacheView allThrow = new CacheView()
		{
			public int[] enumKeys(int id)
			{
				return id == PohGameIds.NEXUS_DEST_ENUM ? new int[]{10, 11} : new int[0];
			}

			public int enumValue(int id, int key)
			{
				return 450;
			}

			public int structInt(int structId, int paramId)
			{
				throw new IllegalArgumentException("boom");
			}

			public String structString(int structId, int paramId)
			{
				throw new IllegalStateException("boom");
			}
		};

		NexusCatalog cat = new NexusCatalog();
		cat.ensureLoaded(allThrow); // must NOT throw
		assertFalse(cat.isLoaded()); // nothing loaded, but the client handler is safe
	}
}
