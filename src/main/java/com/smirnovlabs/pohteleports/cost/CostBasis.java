package com.smirnovlabs.pohteleports.cost;

import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * The gross GP a single teleport use would otherwise have cost. Valuation is
 * decoupled from RuneLite: callers supply a price function (itemId -&gt; gp), so
 * this has no dependency on ItemManager and is trivially unit-testable with a
 * lambda. A single-method functional interface with static factory constructors.
 */
@FunctionalInterface
public interface CostBasis
{
	/** Gross GP saved for one use, given a price lookup (itemId -&gt; gp). */
	long gpPerUse(IntUnaryOperator priceFn);

	/** Sum of runes consumed by the equivalent spellbook teleport. */
	static CostBasis runes(Map<Integer, Integer> runes)
	{
		final Map<Integer, Integer> copy = Map.copyOf(runes);
		return priceFn ->
		{
			long sum = 0;
			for (Map.Entry<Integer, Integer> e : copy.entrySet())
			{
				sum += (long) priceFn.applyAsInt(e.getKey()) * e.getValue();
			}
			return sum;
		};
	}

	/** One use of a bought, charged item: its GE price divided by its charges. */
	static CostBasis itemFraction(int itemId, int charges)
	{
		return priceFn -> charges <= 0 ? 0 : (long) priceFn.applyAsInt(itemId) / charges;
	}

	/** A quantity of a consumable spent per use (e.g. lizardman fangs). */
	static CostBasis consumable(int itemId, int qtyPerUse)
	{
		return priceFn -> (long) priceFn.applyAsInt(itemId) * qtyPerUse;
	}

	/**
	 * One use of an item you <em>craft</em> rather than buy: the base item's GE
	 * price plus the runes to make it, divided across its charges. The mounted
	 * Digsite pendant is a Ruby necklace enchanted with Enchant Ruby Jewellery, so
	 * its true per-use cost is (ruby necklace + 5 fire + 1 cosmic) / 5.
	 */
	static CostBasis craftedFraction(int baseItemId, Map<Integer, Integer> makeRunes, int charges)
	{
		final Map<Integer, Integer> runes = Map.copyOf(makeRunes);
		return priceFn ->
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
		};
	}

	/** No priceable equivalent — count-only, zero GP. */
	CostBasis NONE = priceFn -> 0;
}
