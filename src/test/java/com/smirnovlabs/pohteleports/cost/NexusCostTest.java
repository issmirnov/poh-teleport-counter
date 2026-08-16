package com.smirnovlabs.pohteleports.cost;

import java.util.function.IntUnaryOperator;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Spot-checks the struct-id -> cost table that replaces name-keyed nexus costs. */
public class NexusCostTest
{
	@Test
	public void coversAll45NexusStructs()
	{
		assertEquals(45, NexusCost.size());
	}

	@Test
	public void kharyrllStruct461IsBloodAndLaw()
	{
		// struct 461 = "Kharyrll": 1 blood + 2 law
		IntUnaryOperator price = id -> id == ItemID.BLOODRUNE ? 400 : id == ItemID.LAWRUNE ? 100 : 0;
		assertEquals(400L + 200L, NexusCost.of(461).gpPerUse(price));
	}

	@Test
	public void grandExchangeAltMatchesVarrock()
	{
		IntUnaryOperator price = id -> id == ItemID.AIRRUNE ? 5 : id == ItemID.FIRERUNE ? 4 : id == ItemID.LAWRUNE ? 100 : 0;
		assertEquals(NexusCost.of(450).gpPerUse(price), NexusCost.of(451).gpPerUse(price)); // Varrock == Grand Exchange
	}

	@Test
	public void countOnlyDestinationsAreKnownButFree()
	{
		assertTrue(NexusCost.isKnown(467));                       // Troll Stronghold
		assertEquals(0L, NexusCost.of(467).gpPerUse(id -> 999));
	}

	@Test
	public void unknownStructIsNoneAndNotKnown()
	{
		assertFalse(NexusCost.isKnown(-12345));
		assertEquals(0L, NexusCost.of(-12345).gpPerUse(id -> 999));
	}
}
