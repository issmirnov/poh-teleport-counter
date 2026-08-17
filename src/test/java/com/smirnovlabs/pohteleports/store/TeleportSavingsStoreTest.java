package com.smirnovlabs.pohteleports.store;

import com.google.gson.Gson;
import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.TeleportEvent;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TeleportSavingsStoreTest
{
	@Test
	public void recordIncrementsCount()
	{
		TeleportSavingsStore s = new TeleportSavingsStore(new Gson());
		s.record(new TeleportEvent(Destination.NEXUS_VARROCK, 1));
		s.record(new TeleportEvent(Destination.NEXUS_VARROCK, 2));
		assertEquals(2, s.count(Destination.NEXUS_VARROCK));
		assertEquals(0, s.count(Destination.NEXUS_CAMELOT));
		assertEquals(2, s.totalCount());
	}

	@Test
	public void jsonRoundTrips()
	{
		TeleportSavingsStore a = new TeleportSavingsStore(new Gson());
		a.record(new TeleportEvent(Destination.MGLORY_EDGEVILLE, 5));
		String json = a.toJson();

		TeleportSavingsStore b = new TeleportSavingsStore(new Gson());
		b.loadJson(json);
		assertEquals(1, b.count(Destination.MGLORY_EDGEVILLE));
	}

	@Test
	public void loadJsonNullOrEmptyIsEmpty()
	{
		TeleportSavingsStore s = new TeleportSavingsStore(new Gson());
		s.loadJson(null);
		assertEquals(0, s.totalCount());
		s.loadJson("");
		assertEquals(0, s.totalCount());
	}

	@Test
	public void loadJsonSkipsUnknownIds()
	{
		TeleportSavingsStore s = new TeleportSavingsStore(new Gson());
		s.loadJson("{\"bogus:removed:destination\":7}");
		assertEquals(0, s.totalCount());
	}
}
