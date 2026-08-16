package com.smirnovlabs.pohteleports.detect;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HouseOwnershipTrackerTest
{
	@Test
	public void enteringAfterABoardVisitIsAGuestHouse()
	{
		HouseOwnershipTracker t = new HouseOwnershipTracker();
		t.onBoardVisit();
		t.onTick(true); // enter the instance
		assertTrue(t.inGuestHouse());
	}

	@Test
	public void enteringWithoutABoardVisitIsYourOwnHouse()
	{
		HouseOwnershipTracker t = new HouseOwnershipTracker();
		t.onTick(true); // entered via your own portal/spell — no visit pending
		assertFalse(t.inGuestHouse());
	}

	@Test
	public void guestFlagPersistsWhileInside()
	{
		HouseOwnershipTracker t = new HouseOwnershipTracker();
		t.onBoardVisit();
		t.onTick(true);
		t.onTick(true); // still inside
		assertTrue(t.inGuestHouse());
	}

	@Test
	public void leavingResetsToOwn()
	{
		HouseOwnershipTracker t = new HouseOwnershipTracker();
		t.onBoardVisit();
		t.onTick(true);
		assertTrue(t.inGuestHouse());
		t.onTick(false); // left the instance
		assertFalse(t.inGuestHouse());
		t.onTick(true); // re-enter, this time your own house (no new visit)
		assertFalse(t.inGuestHouse());
	}

	@Test
	public void aCancelledVisitExpiresAndDoesNotTaintTheNextEntry()
	{
		HouseOwnershipTracker t = new HouseOwnershipTracker();
		t.onBoardVisit();
		for (int i = 0; i < 11; i++)
		{
			t.onTick(false); // clicked Visit but never entered (cancelled), stayed outside
		}
		t.onTick(true); // now enter your own house
		assertFalse(t.inGuestHouse());
	}
}
