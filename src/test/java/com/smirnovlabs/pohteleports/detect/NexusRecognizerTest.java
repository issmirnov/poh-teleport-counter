package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class NexusRecognizerTest
{
	private static final int NEXUS_OBJ = 100;

	private final Map<String, Destination> nameMap = Map.of(
		"varrock", Destination.NEXUS_VARROCK,
		"camelot", Destination.NEXUS_CAMELOT);
	private final Map<Integer, Destination> varbitDefault = Map.of(2, Destination.NEXUS_CAMELOT);

	private static class St implements GameStateView
	{
		int v = 0;
		public int getVarbit(int id) { return v; }
		public boolean isInPoh() { return true; }
		public int currentTick() { return 0; }
	}

	private NexusRecognizer rec()
	{
		return new NexusRecognizer(NEXUS_OBJ, varbitDefault, nameMap);
	}

	@Test
	public void listPickResolvesByName()
	{
		assertEquals(Optional.of(Destination.NEXUS_VARROCK),
			rec().onMenuInteraction(new MenuInteraction("Teleport", "Varrock", 0), new St()));
	}

	@Test
	public void genericDefaultOnNexusObjectResolvesByVarbit()
	{
		St st = new St();
		st.v = 2; // encodes Camelot
		assertEquals(Optional.of(Destination.NEXUS_CAMELOT),
			rec().onMenuInteraction(new MenuInteraction("Teleport", "", NEXUS_OBJ), st));
	}

	@Test
	public void openingMenuIsNotCounted()
	{
		assertEquals(Optional.empty(),
			rec().onMenuInteraction(new MenuInteraction("Teleport Menu", "Portal Nexus", NEXUS_OBJ), new St()));
	}

	@Test
	public void configureIsNotCounted()
	{
		assertEquals(Optional.empty(),
			rec().onMenuInteraction(new MenuInteraction("Configure", "Portal Nexus", NEXUS_OBJ), new St()));
	}

	@Test
	public void foreignTeleportIsIgnored()
	{
		// a jewellery-box "Teleport -> Ferox Enclave" (not on the nexus object, not a nexus name)
		assertEquals(Optional.empty(),
			rec().onMenuInteraction(new MenuInteraction("Teleport", "Ferox Enclave", 555), new St()));
	}

	@Test
	public void unknownDefaultFallsBackToBucket()
	{
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, Map.of(), nameMap);
		St st = new St();
		st.v = 99;
		assertEquals(Optional.of(Destination.NEXUS_UNKNOWN),
			r.onMenuInteraction(new MenuInteraction("Teleport", "", NEXUS_OBJ), st));
	}

	@Test
	public void listPickStripsKeyboardShortcutPrefix()
	{
		// Real nexus interface pick: option='Teleport' target='[4] Civitas illa Fortis'
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, varbitDefault,
			Map.of("civitas illa fortis", Destination.NEXUS_CIVITAS));
		assertEquals(Optional.of(Destination.NEXUS_CIVITAS),
			r.onMenuInteraction(new MenuInteraction("Teleport", "[4] Civitas illa Fortis", 1), new St()));
	}

	@Test
	public void objectDirectOptionNamesDestination()
	{
		// Real nexus object option: option='Grand Exchange' target='Portal Nexus'
		NexusRecognizer r = new NexusRecognizer(NEXUS_OBJ, varbitDefault,
			Map.of("grand exchange", Destination.NEXUS_GRAND_EXCHANGE));
		assertEquals(Optional.of(Destination.NEXUS_GRAND_EXCHANGE),
			r.onMenuInteraction(new MenuInteraction("Grand Exchange", "Portal Nexus", NEXUS_OBJ), new St()));
	}
}
