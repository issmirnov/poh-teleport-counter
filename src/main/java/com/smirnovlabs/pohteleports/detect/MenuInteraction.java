package com.smirnovlabs.pohteleports.detect;

import java.util.Locale;

/**
 * A plain, RuneLite-free view of a menu click: the option text, the target
 * text (colour tags already stripped at the plugin boundary), and the id
 * (object/NPC id for object clicks). Lets recognizers be unit-tested with
 * plain objects instead of mocking {@code MenuOptionClicked}.
 */
public final class MenuInteraction
{
	private final String option;
	private final String target;
	private final int id;

	public MenuInteraction(String option, String target, int id)
	{
		this.option = option == null ? "" : option;
		this.target = target == null ? "" : target;
		this.id = id;
	}

	public String getOption()
	{
		return option;
	}

	public String getTarget()
	{
		return target;
	}

	public int getId()
	{
		return id;
	}

	public String optionLower()
	{
		return option.toLowerCase(Locale.ROOT);
	}

	public String targetLower()
	{
		return target.toLowerCase(Locale.ROOT);
	}
}
