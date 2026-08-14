package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.TeleportEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DetectionRouterTest
{
	private static class FakeState implements GameStateView
	{
		int tick = 0;
		boolean inPoh = true;

		public int getVarbit(int id)
		{
			return 0;
		}

		public boolean isInPoh()
		{
			return inPoh;
		}

		public int currentTick()
		{
			return tick;
		}
	}

	private static final TeleportRecognizer ALWAYS = (e, s) -> Optional.of(Destination.NEXUS_VARROCK);

	private MenuInteraction click()
	{
		return new MenuInteraction("Teleport", "Varrock", 0);
	}

	@Test
	public void debouncesMultipleEmitsInSameTick()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		DetectionRouter r = new DetectionRouter(List.of(ALWAYS), st, out::add);
		r.onMenuInteraction(click());
		r.onMenuInteraction(click()); // same tick
		assertEquals(1, out.size());
		st.tick = 1;
		r.onMenuInteraction(click());
		assertEquals(2, out.size());
	}

	@Test
	public void suppressesCountRightAfterDenialMessage()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		DetectionRouter r = new DetectionRouter(List.of(ALWAYS), st, out::add);
		r.onChatMessage("You need a Magic level of 25 to cast this.");
		r.onMenuInteraction(click()); // same tick as denial -> suppressed
		assertEquals(0, out.size());
	}

	@Test
	public void ignoresInteractionsOutsidePoh()
	{
		FakeState st = new FakeState();
		st.inPoh = false;
		List<TeleportEvent> out = new ArrayList<>();
		DetectionRouter r = new DetectionRouter(List.of(ALWAYS), st, out::add);
		r.onMenuInteraction(click());
		assertEquals(0, out.size());
	}

	@Test
	public void firstMatchingRecognizerWins()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		TeleportRecognizer none = (e, s) -> Optional.empty();
		DetectionRouter r = new DetectionRouter(List.of(none, ALWAYS), st, out::add);
		r.onMenuInteraction(click());
		assertEquals(1, out.size());
		assertEquals(Destination.NEXUS_VARROCK, out.get(0).getDestination());
	}
}
