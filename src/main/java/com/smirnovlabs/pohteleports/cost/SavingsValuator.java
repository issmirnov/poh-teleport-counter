package com.smirnovlabs.pohteleports.cost;

import com.smirnovlabs.pohteleports.model.Destination;
import java.util.function.IntUnaryOperator;

/**
 * Values a destination's saving in GP. Takes a price function (itemId -&gt; gp)
 * rather than {@code ItemManager} directly, so it is pure and unit-testable
 * with a lambda; the plugin passes {@code itemManager::getItemPrice}.
 */
public class SavingsValuator
{
	private final IntUnaryOperator priceFn;

	public SavingsValuator(IntUnaryOperator priceFn)
	{
		this.priceFn = priceFn;
	}

	public long gpPerUse(Destination d)
	{
		return d.getCostBasis().gpPerUse(priceFn);
	}

	public long gpTotal(Destination d, int count)
	{
		return gpPerUse(d) * count;
	}
}
