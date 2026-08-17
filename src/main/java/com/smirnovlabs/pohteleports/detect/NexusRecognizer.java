package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import java.util.Map;
import java.util.Optional;

/**
 * Nexus teleports, resolved in three fail-safe layers:
 *
 * <ol>
 *   <li><b>Name map</b> — the clicked option/target matched against our known
 *       destination names. Authoritative for every recognized label.</li>
 *   <li><b>Cache catalog</b> — only when {@link NexusCatalog#isLoaded()}. Rescues
 *       an unrecognised name (matched against the game's own cache names, so it is
 *       rename-proof) and resolves the generic left-click <em>default</em> from the
 *       configured-default varbit ({@code 6653}). Struct ids bridge back to a
 *       {@link Destination} via {@link NexusStructIndex}.</li>
 *   <li><b>Unknown</b> — a generic default on the nexus object that nothing above
 *       resolved falls to the count-only Unknown bucket.</li>
 * </ol>
 *
 * <p>The catalog is optional (null = disabled) and only ever <em>adds</em>
 * resolution: if the cache ids are wrong it never loads and this behaves
 * identically to the name-map-only version.
 */
public class NexusRecognizer implements TeleportRecognizer
{
	private final int nexusObjectId;
	private final Map<String, Destination> nameMap;     // lowercased name -> Destination
	private final NexusCatalog catalog;                 // nullable: cache-backed rescue + left-click default

	public NexusRecognizer(int nexusObjectId, Map<String, Destination> nameMap)
	{
		this(nexusObjectId, nameMap, null);
	}

	public NexusRecognizer(int nexusObjectId, Map<String, Destination> nameMap, NexusCatalog catalog)
	{
		this.nexusObjectId = nexusObjectId;
		this.nameMap = nameMap;
		this.catalog = catalog;
	}

	@Override
	public Optional<Destination> onMenuInteraction(MenuInteraction e, GameStateView state)
	{
		String option = e.optionLower();
		if (TeleportRecognizer.isIgnoredOption(option))
		{
			return Optional.empty(); // opening the list / config / build — never a teleport
		}

		// 1) Known name — authoritative.
		Destination byName = TeleportRecognizer.matchName(e, nameMap);
		if (byName != null)
		{
			return Optional.of(byName);
		}

		// 2) Cache-backed rescue + left-click default — only when the catalog loaded.
		if (catalog != null && catalog.isLoaded())
		{
			Destination byCatalog = resolveViaCatalog(e, option, state);
			if (byCatalog != null)
			{
				return Optional.of(byCatalog);
			}
		}

		// 3) A generic default on the nexus object that stayed unresolved -> Unknown bucket.
		if (e.getId() == nexusObjectId && (option.contains("teleport") || option.isEmpty()))
		{
			return Optional.of(Destination.unknownFor(Transport.NEXUS));
		}
		return Optional.empty();
	}

	/** Resolve a nexus struct id from the cache (by name, then the left-click default varbit) and bridge to a Destination. */
	private Destination resolveViaCatalog(MenuInteraction e, String option, GameStateView state)
	{
		Integer struct = catalog.structForName(TeleportRecognizer.stripKey(e.targetLower()));
		if (struct == null)
		{
			struct = catalog.structForName(TeleportRecognizer.stripKey(option));
		}
		if (struct == null && e.getId() == nexusObjectId && (option.contains("teleport") || option.isEmpty()))
		{
			struct = catalog.structForDestValue(state.getVarbit(PohGameIds.NEXUS_DEFAULT_DEST_VARBIT));
		}
		return struct == null ? null : NexusStructIndex.forStruct(struct);
	}
}
