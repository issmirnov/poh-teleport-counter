package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MountedGloryRecognizerTest
{
	private static final int GLORY_OBJ = 500;
	private final Map<String, Destination> nameMap = Map.of("edgeville", Destination.MGLORY_EDGEVILLE);

	private static class St implements GameStateView
	{
		public int getVarbit(int id) { return 0; }
		public boolean isInPoh() { return true; }
		public int currentTick() { return 0; }
	}

	@Test
	public void dialogPickAfterRubResolves()
	{
		MountedGloryRecognizer r = new MountedGloryRecognizer(GLORY_OBJ, nameMap);
		assertEquals(Optional.empty(),
			r.onMenuInteraction(new MenuInteraction("Rub", "Mounted glory", GLORY_OBJ), new St()));
		assertEquals(Optional.of(Destination.MGLORY_EDGEVILLE),
			r.onMenuInteraction(new MenuInteraction("Continue", "Edgeville", 0), new St()));
	}

	@Test
	public void dialogPickWithoutRubIgnored()
	{
		MountedGloryRecognizer r = new MountedGloryRecognizer(GLORY_OBJ, nameMap);
		assertEquals(Optional.empty(),
			r.onMenuInteraction(new MenuInteraction("Continue", "Edgeville", 0), new St()));
	}
}
