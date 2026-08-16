package com.smirnovlabs.pohteleports.cost;

import java.util.Map;
import java.util.function.IntUnaryOperator;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CostBasisTest
{
	// price: itemId -> gp
	private final IntUnaryOperator price = id ->
	{
		switch (id)
		{
			case 1: return 5;
			case 2: return 300;
			case 3: return 100;
			default: return 0;
		}
	};

	@Test
	public void runesSumsPriceTimesQty()
	{
		assertEquals(5 * 3 + 300 * 1, CostBasis.runes(Map.of(1, 3, 2, 1)).gpPerUse(price));
	}

	@Test
	public void itemFractionDividesByCharges()
	{
		assertEquals(300 / 6, CostBasis.itemFraction(2, 6).gpPerUse(price)); // 50
	}

	@Test
	public void consumableMultipliesQty()
	{
		assertEquals(100 * 2, CostBasis.consumable(3, 2).gpPerUse(price));
	}

	@Test
	public void noneIsZero()
	{
		assertEquals(0, CostBasis.NONE.gpPerUse(price));
	}

	@Test
	public void craftedFractionAddsMakeRunesThenDividesByCharges()
	{
		// (base 300 + 5*fire@5 + 1*cosmic@100) / 5 = (300 + 25 + 100) / 5 = 85
		assertEquals(85, CostBasis.craftedFraction(2, Map.of(1, 5, 3, 1), 5).gpPerUse(price));
	}

	@Test
	public void craftedFractionZeroChargesIsZero()
	{
		assertEquals(0, CostBasis.craftedFraction(2, Map.of(1, 5), 0).gpPerUse(price));
	}

}
