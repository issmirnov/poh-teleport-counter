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
 *       destination names. Authoritative for every recognized label; this is
 *       exactly today's behaviour and is never regressed.</li>
 *   <li><b>Cache catalog</b> — only when {@link NexusCatalog#isLoaded()}. Rescues
 *       a name we didn't recognise (matched against the game's own cache names,
 *       so it is rename-proof and fixes any guessed label) and resolves the
 *       generic left-click "Teleport" default from the configured-default varbit.
 *       Struct ids bridge back to a {@link Destination} via {@link NexusStructIndex}.</li>
 *   <li><b>Legacy varbit / Unknown</b> — the generic default falls back to the
 *       injected varbit map, then the count-only Unknown bucket.</li>
 * </ol>
 *
 * <p>The catalog is optional (null = disabled) and only ever <em>adds</em>
 * resolution: if the cache ids are wrong the catalog never loads and this
 * behaves identically to the name-map-only version.
 */
public class NexusRecognizer implements TeleportRecognizer
{
	private final int nexusObjectId;
	private final Map<Integer, Destination> varbitDefaultMap; // varbit value -> Destination (legacy fallback)
	private final Map<String, Destination> nameMap;           // lowercased name -> Destination
	private final NexusCatalog catalog;                       // nullable: cache-backed rescue

	public NexusRecognizer(int nexusObjectId, Map<Integer, Destination> varbitDefaultMap, Map<String, Destination> nameMap)
	{
		this(nexusObjectId, varbitDefaultMap, nameMap, null);
	}

	public NexusRecognizer(int nexusObjectId, Map<Integer, Destination> varbitDefaultMap,
		Map<String, Destination> nameMap, NexusCatalog catalog)
	{
		this.nexusObjectId = nexusObjectId;
		this.varbitDefaultMap = varbitDefaultMap;
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

		// 1) Known name — authoritative, unchanged behaviour.
		Destination byName = TeleportRecognizer.matchName(e, nameMap);
		if (byName != null)
		{
			return Optional.of(byName);
		}

		// 2) Cache-backed rescue — only when the catalog actually loaded.
		if (catalog != null && catalog.isLoaded())
		{
			Destination byCatalog = resolveViaCatalog(e, option, state);
			if (byCatalog != null)
			{
				return Optional.of(byCatalog);
			}
		}

		// 3) Generic left-click default on the nexus object -> legacy varbit map, else Unknown bucket.
		if (e.getId() == nexusObjectId && (option.contains("teleport") || option.isEmpty()))
		{
			Destination byVarbit = varbitDefaultMap.get(state.getVarbit(PohGameIds.NEXUS_DEFAULT_DEST_VARBIT));
			return Optional.of(byVarbit != null ? byVarbit : Destination.unknownFor(Transport.NEXUS));
		}
		return Optional.empty();
	}

	/** Resolve a nexus struct id from the cache (by name, then left-click default, then object) and bridge to a Destination. */
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
		if (struct == null)
		{
			struct = catalog.structForObject(e.getId());
		}
		return struct == null ? null : NexusStructIndex.forStruct(struct);
	}
}
