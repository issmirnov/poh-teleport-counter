package com.smirnovlabs.pohteleports.cost;

import com.smirnovlabs.pohteleports.model.Destination;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SavingsValuatorTest
{
	@Test
	public void gpTotalIsPerUseTimesCount()
	{
		SavingsValuator v = new SavingsValuator(id -> 60); // every rune = 60
		long per = v.gpPerUse(Destination.NEXUS_CAMELOT);  // 5 air + 1 law = 6 * 60
		assertEquals(360, per);
		assertEquals(3600, v.gpTotal(Destination.NEXUS_CAMELOT, 10));
	}

	@Test
	public void itemFractionUsesPriceOverCharges()
	{
		SavingsValuator v = new SavingsValuator(id -> 6000); // glory(6) = 6000
		assertEquals(1000, v.gpPerUse(Destination.MGLORY_EDGEVILLE)); // 6000 / 6
	}

	@Test
	public void noneValuesToZero()
	{
		assertEquals(0, new SavingsValuator(id -> 999).gpPerUse(Destination.NEXUS_UNKNOWN));
	}
}
