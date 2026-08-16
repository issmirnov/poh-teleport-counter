package com.smirnovlabs.pohteleports.cost;

import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * The runes/charges a single use of a teleport would otherwise have cost.
 * Valuation is decoupled from RuneLite: callers supply a price function
 * (itemId -&gt; gp), so this class has no dependency on ItemManager and is
 * trivially unit-testable with a lambda.
 */
public abstract class CostBasis
{
	/** Gross GP saved for one use, given a price lookup (itemId -&gt; gp). */
	public abstract long gpPerUse(IntUnaryOperator priceFn);

	/** Runes/charges consumed per use (itemId -&gt; qty); empty for NONE. Used for display/tooltips. */
	public abstract Map<Integer, Integer> unitsPerUse();

	public static CostBasis runes(Map<Integer, Integer> runes)
	{
		final Map<Integer, Integer> copy = Map.copyOf(runes);
		return new CostBasis()
		{
			@Override
			public long gpPerUse(IntUnaryOperator priceFn)
			{
				long sum = 0;
				for (Map.Entry<Integer, Integer> e : copy.entrySet())
				{
					sum += (long) priceFn.applyAsInt(e.getKey()) * e.getValue();
				}
				return sum;
			}

			@Override
			public Map<Integer, Integer> unitsPerUse()
			{
				return copy;
			}
		};
	}

	public static CostBasis itemFraction(int itemId, int charges)
	{
		return new CostBasis()
		{
			@Override
			public long gpPerUse(IntUnaryOperator priceFn)
			{
				return charges <= 0 ? 0 : (long) priceFn.applyAsInt(itemId) / charges;
			}

			@Override
			public Map<Integer, Integer> unitsPerUse()
			{
				return Map.of(itemId, 1);
			}
		};
	}

	public static CostBasis consumable(int itemId, int qtyPerUse)
	{
		return new CostBasis()
		{
			@Override
			public long gpPerUse(IntUnaryOperator priceFn)
			{
				return (long) priceFn.applyAsInt(itemId) * qtyPerUse;
			}

			@Override
			public Map<Integer, Integer> unitsPerUse()
			{
				return Map.of(itemId, qtyPerUse);
			}
		};
	}

	/**
	 * A single use of an item you <em>craft</em> rather than buy: the base item's GE
	 * price plus the runes to make it, divided across its charges. The mounted
	 * Digsite pendant is a Ruby necklace enchanted with Enchant Ruby Jewellery, so
	 * its true per-use cost is (ruby necklace + 5 fire + 1 cosmic) / 5.
	 */
	public static CostBasis craftedFraction(int baseItemId, Map<Integer, Integer> makeRunes, int charges)
	{
		final Map<Integer, Integer> runes = Map.copyOf(makeRunes);
		return new CostBasis()
		{
			@Override
			public long gpPerUse(IntUnaryOperator priceFn)
			{
				if (charges <= 0)
				{
					return 0;
				}
				long total = priceFn.applyAsInt(baseItemId);
				for (Map.Entry<Integer, Integer> e : runes.entrySet())
				{
					total += (long) priceFn.applyAsInt(e.getKey()) * e.getValue();
				}
				return total / charges;
			}

			@Override
			public Map<Integer, Integer> unitsPerUse()
			{
				return Map.of(baseItemId, 1);
			}
		};
	}

	public static final CostBasis NONE = new CostBasis()
	{
		@Override
		public long gpPerUse(IntUnaryOperator priceFn)
		{
			return 0;
		}

		@Override
		public Map<Integer, Integer> unitsPerUse()
		{
			return Map.of();
		}
	};
}
