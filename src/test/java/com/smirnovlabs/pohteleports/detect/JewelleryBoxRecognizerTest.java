package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class JewelleryBoxRecognizerTest
{
	private final Map<String, Destination> nameMap = Map.of(
		"ferox enclave", Destination.JBOX_DUEL_FEROX,
		"edgeville", Destination.JBOX_GLORY_EDGEVILLE);

	private static class St implements GameStateView
	{
		public int getVarbit(int id) { return 0; }
		public boolean isInPoh() { return true; }
		public int currentTick() { return 0; }
		public int[] playerPos() { return null; }
	}

	@Test
	public void namedEntryResolves()
	{
		JewelleryBoxRecognizer r = new JewelleryBoxRecognizer(nameMap);
		assertEquals(Optional.of(Destination.JBOX_DUEL_FEROX),
			r.onMenuInteraction(new MenuInteraction("Teleport", "Ferox Enclave", 0), new St()));
	}

	@Test
	public void unrelatedClickIgnored()
	{
		JewelleryBoxRecognizer r = new JewelleryBoxRecognizer(nameMap);
		assertEquals(Optional.empty(),
			r.onMenuInteraction(new MenuInteraction("Walk here", "", 0), new St()));
	}
}
