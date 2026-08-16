package com.smirnovlabs.pohteleports.detect;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Exercises the cache-folding logic (alt->primary, scry offset, reverse name/object maps) with a map-backed fake. */
public class NexusCatalogTest
{
	/** Map-backed {@link CacheView}: one enum (1377) plus per-struct int/string params. */
	private static final class FakeCache implements CacheView
	{
		final Map<Integer, Integer> dest = new LinkedHashMap<>();               // destId -> ownStruct
		final Map<Integer, Map<Integer, Integer>> ints = new HashMap<>();       // struct -> (param -> int)
		final Map<Integer, Map<Integer, String>> strs = new HashMap<>();        // struct -> (param -> string)

		FakeCache put(int destId, int struct, String name, int marbleObj, int primary)
		{
			dest.put(destId, struct);
			if (name != null)
			{
				strs.computeIfAbsent(struct, k -> new HashMap<>()).put(PohGameIds.STRUCT_PARAM_NAME, name);
			}
			if (marbleObj > 0)
			{
				ints.computeIfAbsent(struct, k -> new HashMap<>()).put(PohGameIds.STRUCT_PARAM_OBJ_MARBLE, marbleObj);
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
			.put(10, 450, "Varrock", 1000, 0)     // primary
			.put(11, 451, null, 1001, 450)        // Grand Exchange: alt of Varrock (name comes from primary)
			.put(12, 461, "Kharyrll", 1002, 0);   // primary
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
		assertNull(cat.structForObject(1000));
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
	public void objectMapsFoldToPrimaryStruct()
	{
		assertEquals(Integer.valueOf(450), loaded().structForObject(1000));
		assertEquals(Integer.valueOf(450), loaded().structForObject(1001)); // GE skin object -> Varrock primary
		assertEquals(Integer.valueOf(461), loaded().structForObject(1002));
	}

	@Test
	public void nameLookupIsCaseInsensitive()
	{
		assertEquals(Integer.valueOf(450), loaded().structForName("VARROCK"));
		assertEquals(Integer.valueOf(461), loaded().structForName("kharyrll"));
		assertNull(loaded().structForName("nowhere"));
	}

	// ---- Regression guards for the 2026-08-15 crash: a struct object-param is a
	// ---- STRING param, so reading it as int throws IllegalArgumentException. The
	// ---- catalog must survive that (and still load names), never propagating it
	// ---- into the click handler where it stopped all counting.

	@Test
	public void survivesObjectParamThatThrowsAndStillLoadsNames()
	{
		CacheView objThrows = new CacheView()
		{
			public int[] enumKeys(int id)
			{
				return id == PohGameIds.NEXUS_DEST_ENUM ? new int[]{10} : new int[0];
			}

			public int enumValue(int id, int key)
			{
				return id == PohGameIds.NEXUS_DEST_ENUM && key == 10 ? 450 : 0;
			}

			public int structInt(int structId, int paramId)
			{
				if (paramId == PohGameIds.STRUCT_PARAM_PRIMARY_STRUCT)
				{
					return 0; // primary reads fine (absent), like the real cache
				}
				throw new IllegalArgumentException("trying to get int from string param"); // object params
			}

			public String structString(int structId, int paramId)
			{
				return paramId == PohGameIds.STRUCT_PARAM_NAME ? "Varrock" : null;
			}
		};

		NexusCatalog cat = new NexusCatalog();
		cat.ensureLoaded(objThrows); // must NOT throw
		assertTrue(cat.isLoaded());
		assertEquals("Varrock", cat.name(450));
		assertEquals(Integer.valueOf(450), cat.structForName("varrock"));
		assertNull(cat.structForObject(12345)); // objects never mapped, but no crash
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
