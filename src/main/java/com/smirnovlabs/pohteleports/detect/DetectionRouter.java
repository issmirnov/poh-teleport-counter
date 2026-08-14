package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.TeleportEvent;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Routes menu interactions to the recognizers and emits at most one
 * {@link TeleportEvent} per tick. Gates on being in a POH (so worn-jewellery
 * teleports done while adventuring are never counted) and suppresses a count
 * on the same tick as a known teleport-denial chat message.
 */
public class DetectionRouter
{
	// Substrings of common teleport-denial messages (extend during live QA).
	private static final String[] DENIALS = {
		"you need a magic level",
		"you can't reach that",
		"you can't teleport",
		"a mysterious force",
		"you have not unlocked",
		"you must be a member"
	};

	private final List<TeleportRecognizer> recognizers;
	private final GameStateView state;
	private final Consumer<TeleportEvent> sink;

	private int lastEmitTick = Integer.MIN_VALUE;
	private int lastDenialTick = Integer.MIN_VALUE;

	public DetectionRouter(List<TeleportRecognizer> recognizers, GameStateView state, Consumer<TeleportEvent> sink)
	{
		this.recognizers = recognizers;
		this.state = state;
		this.sink = sink;
	}

	public void onChatMessage(String message)
	{
		if (message == null)
		{
			return;
		}
		String m = message.toLowerCase(Locale.ROOT);
		for (String d : DENIALS)
		{
			if (m.contains(d))
			{
				lastDenialTick = state.currentTick();
				return;
			}
		}
	}

	public void onMenuInteraction(MenuInteraction e)
	{
		if (!state.isInPoh())
		{
			return;
		}
		int tick = state.currentTick();
		if (tick == lastEmitTick || tick == lastDenialTick)
		{
			return; // debounce + denial guard
		}
		for (TeleportRecognizer r : recognizers)
		{
			Optional<Destination> d = r.onMenuInteraction(e, state);
			if (d.isPresent())
			{
				lastEmitTick = tick;
				sink.accept(new TeleportEvent(d.get(), tick));
				return;
			}
		}
	}
}
