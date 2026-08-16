package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.TeleportEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DetectionRouterTest
{
	private static final int[] POH = {1984, 7040, 0};       // inside a POH instance
	private static final int[] VARROCK = {3212, 3424, 0};   // far away — a teleport

	private static class FakeState implements GameStateView
	{
		int tick = 0;
		boolean inPoh = true;
		int[] pos = POH;

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

		public int[] playerPos()
		{
			return pos;
		}
	}

	private static final TeleportRecognizer ALWAYS = (e, s) -> Optional.of(Destination.NEXUS_VARROCK);

	private MenuInteraction click()
	{
		return new MenuInteraction("Teleport", "Varrock", 0);
	}

	@Test
	public void countsOnCoordJumpAfterArming()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		DetectionRouter r = new DetectionRouter(List.of(ALWAYS), st, out::add);
		r.onGameTick();               // establish lastPos = POH
		r.onMenuInteraction(click()); // arm — no count yet
		assertEquals(0, out.size());
		st.tick = 1;
		st.pos = VARROCK;             // teleported
		r.onGameTick();               // jump -> count
		assertEquals(1, out.size());
		assertEquals(Destination.NEXUS_VARROCK, out.get(0).getDestination());
	}

	@Test
	public void doesNotCountUntilTheJump()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		DetectionRouter r = new DetectionRouter(List.of(ALWAYS), st, out::add);
		r.onGameTick();
		r.onMenuInteraction(click());
		st.tick = 1;
		r.onGameTick();               // still in POH, no jump
		assertEquals(0, out.size());
	}

	@Test
	public void walkingNeverCounts()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		DetectionRouter r = new DetectionRouter(List.of(ALWAYS), st, out::add);
		r.onGameTick();
		r.onMenuInteraction(click());
		for (int t = 1; t <= 5; t++)
		{
			st.tick = t;
			st.pos = new int[]{POH[0] + 2 * t, POH[1], 0}; // 2 tiles/tick — running
			r.onGameTick();
		}
		assertEquals(0, out.size());
	}

	@Test
	public void wildernessWarningDelayStillCounts()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		DetectionRouter r = new DetectionRouter(List.of(ALWAYS), st, out::add);
		r.onGameTick();
		r.onMenuInteraction(click()); // armed at tick 0
		st.tick = 1;
		r.onGameTick();               // warning showing, no jump
		st.tick = 2;
		r.onGameTick();               // clicking "Enter Wilderness", no jump
		st.tick = 3;
		st.pos = VARROCK;             // now it fires
		r.onGameTick();
		assertEquals(1, out.size());
	}

	@Test
	public void discardsAfterWindowThenAJumpDoesNotCount()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		DetectionRouter r = new DetectionRouter(List.of(ALWAYS), st, out::add);
		r.onGameTick();
		r.onMenuInteraction(click()); // armed at tick 0
		for (int t = 1; t <= 10; t++) // 10 ticks with no jump -> expires
		{
			st.tick = t;
			r.onGameTick();
		}
		st.tick = 11;
		st.pos = VARROCK;             // a jump, but the arm is long gone
		r.onGameTick();
		assertEquals(0, out.size());
	}

	@Test
	public void ignoresInteractionsOutsidePoh()
	{
		FakeState st = new FakeState();
		st.inPoh = false;
		List<TeleportEvent> out = new ArrayList<>();
		DetectionRouter r = new DetectionRouter(List.of(ALWAYS), st, out::add);
		r.onGameTick();
		r.onMenuInteraction(click()); // not armed
		st.tick = 1;
		st.pos = VARROCK;
		r.onGameTick();
		assertEquals(0, out.size());
	}

	@Test
	public void firstMatchingRecognizerWins()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		TeleportRecognizer none = (e, s) -> Optional.empty();
		DetectionRouter r = new DetectionRouter(List.of(none, ALWAYS), st, out::add);
		r.onGameTick();
		r.onMenuInteraction(click());
		st.tick = 1;
		st.pos = VARROCK;
		r.onGameTick();
		assertEquals(1, out.size());
		assertEquals(Destination.NEXUS_VARROCK, out.get(0).getDestination());
	}

	@Test
	public void traceReportsArmAndCount()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		List<String> traces = new ArrayList<>();
		DetectionRouter r = new DetectionRouter(List.of(ALWAYS), st, out::add, traces::add);
		r.onGameTick();
		r.onMenuInteraction(click());
		st.tick = 1;
		st.pos = VARROCK;
		r.onGameTick();
		assertTrue("expected an 'armed' trace", traces.stream().anyMatch(t -> t.startsWith("armed")));
		assertTrue("expected a 'counted' trace", traces.stream().anyMatch(t -> t.startsWith("counted")));
	}

	@Test
	public void traceReportsNoMatch()
	{
		FakeState st = new FakeState();
		List<TeleportEvent> out = new ArrayList<>();
		List<String> traces = new ArrayList<>();
		TeleportRecognizer none = (e, s) -> Optional.empty();
		DetectionRouter r = new DetectionRouter(List.of(none), st, out::add, traces::add);
		r.onMenuInteraction(click());
		assertTrue("expected a 'no match' trace", traces.stream().anyMatch(t -> t.startsWith("no match")));
	}
}
