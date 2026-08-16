package com.smirnovlabs.pohteleports.detect;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Locks the token-set ignore helper that replaced the old contains(...) chains. */
public class TeleportRecognizerTest
{
	@Test
	public void multiWordConfigLabelsAreIgnored()
	{
		assertTrue(TeleportRecognizer.isIgnoredOption("teleport menu")); // token "menu"
		assertTrue(TeleportRecognizer.isIgnoredOption("set default"));   // token "set"
		assertTrue(TeleportRecognizer.isIgnoredOption("add teleport"));  // token "add"
	}

	@Test
	public void singleWordConfigActionsAreIgnored()
	{
		assertTrue(TeleportRecognizer.isIgnoredOption("configure"));
		assertTrue(TeleportRecognizer.isIgnoredOption("examine"));
		assertTrue(TeleportRecognizer.isIgnoredOption("continue")); // whole-option ignore
	}

	@Test
	public void bareTeleportDefaultIsKept()
	{
		// The generic left-click default must NOT be ignored (it is a real teleport).
		assertFalse(TeleportRecognizer.isIgnoredOption("teleport"));
	}

	@Test
	public void emptyOptionIsKept()
	{
		// A bare object left-click surfaces an empty option; keep it.
		assertFalse(TeleportRecognizer.isIgnoredOption(""));
	}

	@Test
	public void destinationNamesAreKept()
	{
		assertFalse(TeleportRecognizer.isIgnoredOption("grand exchange"));
		assertFalse(TeleportRecognizer.isIgnoredOption("civitas illa fortis"));
		assertFalse(TeleportRecognizer.isIgnoredOption("edgeville"));
	}
}
