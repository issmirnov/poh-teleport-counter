package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.TeleportEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Recognizes a teleport destination from a menu interaction, then counts it only
 * once the player <em>actually</em> teleports — confirmed by the large single-tick
 * world-coordinate jump a teleport causes.
 *
 * <p>This is accurate by construction: a cancelled Wilderness warning, a denied
 * teleport, or just opening the menu never produces a jump, so it never counts.
 * It also replaces the brittle same-tick / denial-message guards the old
 * count-on-click model needed. Walking or running never trips it — those move a
 * couple of tiles per tick, far below {@link #JUMP_TILES}; a teleport moves
 * hundreds in one tick.
 *
 * <p>Flow: {@link #onMenuInteraction} arms a pending destination; {@link #onGameTick}
 * (called every tick) confirms it on a jump, or discards it after
 * {@link #ARM_WINDOW_TICKS} without one (covers the delay of a Wilderness warning).
 */
public class DetectionRouter
{
	/** Minimum single-tick move (tiles) that counts as a teleport, not walking. Compared squared. */
	private static final int JUMP_TILES = 15;
	/** Ticks a recognized teleport stays armed awaiting the jump (covers the Wilderness warning click). */
	private static final int ARM_WINDOW_TICKS = 10;

	private final List<TeleportRecognizer> recognizers;
	private final GameStateView state;
	private final Consumer<TeleportEvent> sink;
	private final Consumer<String> trace; // nullable diagnostics

	private Destination pending;
	private int pendingArmTick;
	private int[] lastPos;

	public DetectionRouter(List<TeleportRecognizer> recognizers, GameStateView state, Consumer<TeleportEvent> sink)
	{
		this(recognizers, state, sink, null);
	}

	public DetectionRouter(List<TeleportRecognizer> recognizers, GameStateView state,
		Consumer<TeleportEvent> sink, Consumer<String> trace)
	{
		this.recognizers = recognizers;
		this.state = state;
		this.sink = sink;
		this.trace = trace;
	}

	/** A recognized teleport arms a pending count; the count itself waits for the coord jump. */
	public void onMenuInteraction(MenuInteraction e)
	{
		if (!state.isInPoh())
		{
			trace("skip (not in POH): " + desc(e));
			return;
		}
		for (TeleportRecognizer r : recognizers)
		{
			Optional<Destination> d = r.onMenuInteraction(e, state);
			if (d.isPresent())
			{
				pending = d.get();
				pendingArmTick = state.currentTick();
				trace("armed " + pending + " (awaiting teleport): " + desc(e));
				return;
			}
		}
		trace("no match: " + desc(e));
	}

	/** Every game tick: confirm a pending teleport on a big coord jump, else expire it. */
	public void onGameTick()
	{
		int[] pos = state.playerPos();
		if (pending != null)
		{
			if (jumped(lastPos, pos))
			{
				trace("counted " + pending + " (teleport confirmed by coord jump)");
				sink.accept(new TeleportEvent(pending, state.currentTick()));
				pending = null;
			}
			else if (state.currentTick() - pendingArmTick >= ARM_WINDOW_TICKS)
			{
				trace("discarded " + pending + " (no teleport within window)");
				pending = null;
			}
		}
		lastPos = pos;
	}

	/** A large single-tick move in the horizontal plane — a teleport, not walking. */
	private static boolean jumped(int[] from, int[] to)
	{
		if (from == null || to == null)
		{
			return false;
		}
		long dx = from[0] - to[0];
		long dy = from[1] - to[1];
		return dx * dx + dy * dy > (long) JUMP_TILES * JUMP_TILES;
	}

	private void trace(String msg)
	{
		if (trace != null)
		{
			trace.accept(msg);
		}
	}

	private static String desc(MenuInteraction e)
	{
		return "option='" + e.getOption() + "' target='" + e.getTarget() + "' id=" + e.getId();
	}
}
