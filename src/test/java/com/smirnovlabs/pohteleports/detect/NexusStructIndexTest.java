package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Pins the struct-id -> Destination bridge and guarantees it covers every nexus destination. */
public class NexusStructIndexTest
{
	@Test
	public void resolvesKnownStructs()
	{
		assertEquals(Destination.NEXUS_VARROCK, NexusStructIndex.forStruct(450));
		assertEquals(Destination.NEXUS_GRAND_EXCHANGE, NexusStructIndex.forStruct(451));
		assertEquals(Destination.NEXUS_KHARYRLL, NexusStructIndex.forStruct(461));
		assertEquals(Destination.NEXUS_CIVITAS, NexusStructIndex.forStruct(855));
		assertEquals(Destination.NEXUS_TROLL_STRONGHOLD, NexusStructIndex.forStruct(467));
	}

	@Test
	public void bothMooringStructsMapToOneDestination()
	{
		assertEquals(Destination.NEXUS_MOORING_POINT, NexusStructIndex.forStruct(1187));
		assertEquals(Destination.NEXUS_MOORING_POINT, NexusStructIndex.forStruct(6423));
	}

	@Test
	public void unknownStructIsNull()
	{
		assertNull(NexusStructIndex.forStruct(-1));
		assertNull(NexusStructIndex.forStruct(99999));
	}

	@Test
	public void everyNexusDestinationIsBridged()
	{
		for (Destination d : Destination.values())
		{
			if (d.getTransport() == Transport.NEXUS && !d.getId().endsWith(":unknown"))
			{
				assertTrue("no struct maps to " + d, NexusStructIndex.mappedDestinations().contains(d));
			}
		}
	}

	@Test
	public void unknownBucketIsNotBridged()
	{
		// The catalog resolves real destinations; the Unknown bucket is a fallback, never a struct target.
		assertTrue(!NexusStructIndex.mappedDestinations().contains(Destination.NEXUS_UNKNOWN));
	}
}
