package com.smirnovlabs.pohteleports.model;

import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class DestinationTableTest
{
	@Test
	public void everyTransportHasAtLeastOneDestination()
	{
		for (Transport t : Transport.values())
		{
			assertTrue("no destinations for " + t,
				EnumSet.allOf(Destination.class).stream().anyMatch(d -> d.getTransport() == t));
		}
	}

	@Test
	public void everyDestinationHasIdAndBasis()
	{
		for (Destination d : Destination.values())
		{
			assertNotNull(d.getId());
			assertFalse(d.getId().isEmpty());
			assertNotNull("null basis for " + d, d.getCostBasis());
		}
	}

	@Test
	public void idsAreUnique()
	{
		long distinct = EnumSet.allOf(Destination.class).stream().map(Destination::getId).distinct().count();
		assertEquals(Destination.values().length, distinct);
	}

	@Test
	public void unknownForReturnsMatchingTransportBucket()
	{
		for (Transport t : Transport.values())
		{
			assertEquals(t, Destination.unknownFor(t).getTransport());
		}
	}
}
