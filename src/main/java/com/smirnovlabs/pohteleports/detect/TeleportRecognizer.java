package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Turns a single menu interaction into a resolved teleport {@link Destination}, or nothing. */
public interface TeleportRecognizer
{
	/**
	 * Words that mark a menu option as a UI / configuration action (opening the
	 * list, editing slots, examining, ...) rather than an actual teleport. Note
	 * {@code "teleport"} is deliberately absent, so the generic left-click default
	 * ("Teleport") is kept while "Teleport menu" (→ token "menu") is rejected.
	 */
	Set<String> IGNORE_WORDS = Set.of(
		"menu", "configure", "build", "set", "select", "add",
		"remove", "empty", "examine", "rub", "recharge", "uncharge");

	/** Whole-option strings that are never a teleport (no single meaningful token). */
	Set<String> IGNORE_OPTIONS = Set.of("cancel", "continue", "ok", "close");

	Optional<Destination> onMenuInteraction(MenuInteraction e, GameStateView state);

	/**
	 * True if this (already-lowercased) option is a config/UI action we must not
	 * count as a teleport. Token-set membership, so it correctly rejects
	 * multi-word labels like "teleport menu" / "set default" without the old
	 * substring chains. An empty option (a bare object left-click) is kept.
	 */
	static boolean isIgnoredOption(String optionLower)
	{
		if (optionLower.isEmpty())
		{
			return false;
		}
		if (IGNORE_OPTIONS.contains(optionLower))
		{
			return true;
		}
		for (String word : optionLower.split("\\s+"))
		{
			if (IGNORE_WORDS.contains(word))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Match a destination name against either the option or the target text.
	 * Different transports put the destination in different fields (e.g. a
	 * mounted-amulet right-click names it in the option, a nexus list pick in
	 * the target), so we check both.
	 */
	static Destination matchName(MenuInteraction e, Map<String, Destination> nameMap)
	{
		Destination d = nameMap.get(stripKey(e.optionLower()));
		return d != null ? d : nameMap.get(stripKey(e.targetLower()));
	}

	/**
	 * Normalize a menu label for name matching: strip a leading keyboard-shortcut
	 * prefix like "[4] " / "[K] " that the Nexus UI adds, and fold the whitespace.
	 *
	 * <p>The Nexus interface separates the shortcut from the name with a
	 * <em>non-breaking</em> space (U+00A0, regex {@code \xA0}), which Java's plain
	 * {@code \s} does NOT match. Left as-is, {@code stripKey("[K] West Ardougne")}
	 * keeps the U+00A0 and returns " west ardougne", so the name lookup misses
	 * and every menu/map pick falls through to "no match". Folding {@code \xA0}
	 * together with normal whitespace fixes it. All-ASCII source on purpose — no
	 * literal non-breaking space to be mangled by an editor.
	 */
	static String stripKey(String s)
	{
		return s.replaceAll("[\\s\\xA0]+", " ")      // fold whitespace incl. non-breaking space
			.replaceFirst("^\\[[^\\]]*\\] ?", "")    // drop a leading "[4] "/"[K] " shortcut
			.trim();
	}
}
