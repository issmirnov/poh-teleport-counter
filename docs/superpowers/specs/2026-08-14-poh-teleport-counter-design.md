# PoH Teleport Counter — Design Spec

**Status:** Design approved, pending final user review → implementation plan
**Date:** 2026-08-14
**Machine:** hexane (`vania`)
**Project:** `~/Projects/1.Personal/games/runelite-plugin`

---

## Summary

A RuneLite plugin that counts how many times the player uses their **free, pre-paid Player-Owned House teleports** and estimates the **runes/charges saved** (valued at live GE/wiki prices). Nothing on the Plugin Hub currently counts these free PoH-furniture teleports — `tictac7x-charges` tracks charges on *worn* items, not the free furniture; `nexus-map` / `poh-portal-labels` / `better-teleport-menu` are UI/labeling tools. This fills a real, novel gap.

**Target:** build to Plugin Hub standards, prove on the author's account via sideload first, submit the `runelite/plugin-hub` PR as a follow-up.

## Scope

**In scope (v1):**
- Teleport Nexus (all tiers)
- Jewellery box (basic / fancy / ornate)
- Mounted amulet of glory (Quest Hall)
- Mounted Xeric's talisman (Portal Nexus amulet hotspot)
- Mounted Digsite pendant (Portal Nexus amulet hotspot)
- Counts teleports performed **as a guest in another player's POH** too.

**Deferred (future versions):**
- Farming cape teleports, worn-item / tablet teleports.
- **Break-even / net-savings** layer (see Deferred Features).

**Excluded on purpose:** PoH spirit tree / fairy ring / obelisk — these cost nothing to use normally, so there is no rune "saving" to report. Could be added as *count-only* later, but they do not fit the savings story.

## Metric definition

Per `(transport, destination)`: a **use count** and a **gross GP saved**.

Gross GP saved = the GE value of the runes/charges you would otherwise have spent making the same trip with the item/spell the transport replaces, **valued at the current wiki price**.

**Honest caveat (documented in-plugin, not hidden):** this is *face value at GE price*. Glories and jewellery recharge for free at the Fountain of Rune / wells etc., so the true marginal cost of a charge is lower than face value. "Gross saved" is the satisfying, always-accurate headline number the user chose; precise net/ROI is the deferred break-even layer.

`ItemManager.getItemPrice` is RuneLite's periodically-refreshed wiki-price feed, not tick-live — fine for this, and not oversold as "live market".

## Architecture

Small, isolated units, each with one job and a narrow interface:

| Module | Responsibility | Depends on |
|---|---|---|
| **Transport recognizers** (4) | Turn raw game events into a normalized `TeleportEvent`. `NexusRecognizer`, `JewelleryBoxRecognizer`, `MountedGloryRecognizer`, `MountedAmuletRecognizer` (Xeric's + Digsite) | RuneLite events only |
| **`TeleportEvent`** | Value object `{transport, destination, timestamp}` — the one interface between detection and accounting | nothing |
| **`TeleportCostTable`** | Pure static data: `(transport, destination) → CostBasis` | nothing |
| **`SavingsValuator`** | Resolve a `CostBasis` → GP via `ItemManager` | ItemManager |
| **`TeleportSavingsStore`** | Accumulate + persist counts per `(transport, destination)`; source of truth | ConfigManager, CostTable, Valuator |
| **`PohTeleportPanel`** | Side panel: totals header → per-transport sections → (jewellery-box) item sub-groups → destination rows | Store (read-only) |
| **Plugin glue + `Config`** | `@PluginDescriptor`, subscribe events → route to recognizers, wire store→panel, config toggles | all |

**Data flow:**
```
MenuOptionClicked (+ WidgetLoaded / VarbitChanged / ChatMessage)
   → matching Recognizer → TeleportEvent
      → Store.record(): count++ , persist
         → Panel refresh (derives savedRunes = count × basis, gp = Σ × livePrice)
```

Recognizers know nothing about GP or UI; the cost table is data; valuation is a thin API wrapper; the panel is pure presentation. Deferred features drop in without touching the core (new recognizer + cost rows; or a build-cost table for break-even).

## Detection design

**Approach: interaction-based (menu/interface/varbit), no location-jump in shipped code.** Every counted teleport corresponds to an actual interaction with a known PoH transport, so a normal spellbook cast or tablet read *inside* the POH is never miscounted (it triggers no PoH-transport interaction). Accuracy target is a fun lifetime counter, not billing — rare random miscounts (e.g. clicked-then-interrupted) are acceptable; **systematic** bias is not.

**Count trigger:** the destination-selection interaction — a `MenuOptionClicked` on a nexus row / jewellery-box entry / mounted-amulet option, or the glory rub-dialog option. Never count opening the list, nor a **Configure / Build / Set-default** action.

**Destination resolution (graded, most-accurate first):**
1. **Picked from the list interface** → the selected widget row's text / component id (exact).
2. **Left-click default / "Previous destination"** (no interface opens) → **read the configured-default varbit** for that transport (Nexus default slot; mounted Digsite default; mounted Xeric's default) and map it to a destination.
3. **Cached last-explicit pick** for that transport this session.
4. **"Default / Unknown"** bucket — still counts the teleport.

**Supporting reads:**
- On **Nexus interface load** (`WidgetLoaded`), scrape the configured destination list → build/refresh the *slot → destination* map that decodes the default varbit and enumerates unlocked destinations. Refresh on `VarbitChanged` for the relevant varbits.
- **Denial guard:** subscribe to `ChatMessage`; a known denial (level requirement, "you can't teleport here", members-only, etc.) right after an intent → do not count.
- **Debounce:** at most one count per intent within a short tick window.

**Per-transport detection notes:**
- **Nexus / Jewellery box** — scrollable list/grid widget; key off the `MenuEntry` component id + option text.
- **Mounted glory** — object "Rub" → a chat-**dialog** widget whose group id is shared with many NPC dialogs → scope by "just rubbed glory" *state*, not group id alone. Destination = the chosen dialog option (Edgeville / Karamja / Draynor / Al Kharid).
- **Mounted Xeric's / Digsite** — plain right-click options on the object; no widget parsing (lowest-risk case). Left-click default resolved via varbit (per user's setup: a default is usually preconfigured).

**Reference implementations to crib IDs from:** `abextm/better-teleport-menu` (core-RuneLite author; already identifies Nexus / jewellery-box destinations) and `nexus-map` (already reads the nexus config/varbits). Real widget / animation / varbit / option-text ids are captured live with **Developer Tools → Widget Inspector + varbit inspector** on the author's account. Prefer `net.runelite.api.gameval.InterfaceID` / `AnimationID` over the deprecated `WidgetID`.

## Cost model

`TeleportCostTable` maps `(transport, destination) → CostBasis`:

| Basis | Meaning | Applies to |
|---|---|---|
| `RUNES(runeItemId → qty)` | the equivalent spell's rune set | Nexus spell teleports (e.g. Varrock = 3 air + 1 fire + 1 law) |
| `ITEM_FRACTION(itemId, chargesPerItem)` | item GE price ÷ charges = one charge | Jewellery box, mounted glory, mounted Digsite |
| `CONSUMABLE(itemId, qtyPerUse)` | consumable spent per use | Mounted Xeric's (~1 lizardman fang) |
| `NONE` | no priceable basis | any destination we can't value → count only, gp = 0 |

Representative charge counts to **confirm during implementation**: glory 6, ring of dueling 8, games necklace 8, skills necklace 6, combat bracelet 6, ring of wealth (teleport charges), digsite pendant 5; and the exact lizardman-fang-per-teleport for Xeric's. Salt-based Nexus teleports (Troll Stronghold / Weiss) → value salts or `NONE`.

## Data schema / persistence

Because every basis is **static data**, the store persists **only the count** per `(transport, destination)`. Saved runes/charges and GP are derived at render time — so the GP total always reflects *current* prices, and the store stays trivial.

`ConfigManager`, group `pohteleports`, auto-scoped per RuneLite profile (→ per account):
```
counts   → { "<transport>|<destination>": <int>, ... }   // the only real state
firstSeen / lastSeen (optional, per key)
config   → sortMode (used|saved), countGuestPoh (default true), showOverlay (future), …
```

Adding the deferred break-even layer needs only a static `buildCost[transport|destination]` table (and optional user-entered actual costs) — **no schema change**: `net = count × perUseValue − buildCost`, `break-even progress = count ÷ (buildCost ÷ perUseValue)`.

## UI — side panel

Modeled on the Loot Tracker panel:
- **Toolbar:** expand-all / collapse-all, and a **Sort** chip toggling **Most used ⇄ Most saved** — applies to sections, sub-groups, and rows.
- **Totals header:** icon + `Total: N teleports` / `Saved: X gp`.
- **Per-transport sections** (collapsible): name + `×count` + `gp saved`.
- **Jewellery box nests** into item sub-groups (Amulet of glory / Ring of dueling / …) → destination rows; single-item transports (Nexus, Mounted glory, Xeric's, Digsite) are **flat**.
- **Every destination row shows both** use count (yellow) and gp saved (green).

## Config options

- `sortMode` — Most used | Most saved (default: Most used).
- `countGuestPoh` — count teleports in others' POHs (default: **on**).
- (future) `showOverlay`, break-even display toggles.

## Edge cases

- Count only on destination-select; ignore menu-open and Configure/Build/Set-default.
- Denial `ChatMessage` after an intent → suppress the count.
- Nexus left-click default with no named option → varbit-resolved default → cached last-pick → "Default/Unknown".
- Unknown/new destination (unpriced content) → still count (gp = 0) and log; never drop a teleport.
- Debounce one count per intent per short tick window.
- POH is instanced — detection is interaction-based, so absolute `WorldPoint` is not relied upon.
- Profile switch → `ConfigManager` re-reads for the active profile; panel refreshes.
- `ItemManager` returns no price → show count, gp as "—" (never a wrong 0).

## Testing strategy

- **Unit tests (the bulk, offline):**
  - Recognizers: synthetic `MenuOptionClicked` / `WidgetLoaded` / `VarbitChanged` / `ChatMessage` → assert emitted `TeleportEvent` or none. Cover happy path, menu-open (no event), configure action (no event), denial (suppressed), nexus left-click default (varbit-resolved).
  - Cost/Valuator: `CostBasis` resolution against a mocked `ItemManager` with fixed prices (RUNES / ITEM_FRACTION / CONSUMABLE / NONE).
  - Store: record → counts increment; serialize/deserialize round-trip; per-profile isolation.
- **Live QA on the author's account** — doubles as the ID-discovery pass: perform each transport's teleport with the Widget/varbit Inspector open, confirm the panel increments correctly, record the real ids. This is where real-world detection is validated.
- **No in-game automation** in tests (Jagex/RuneLite rules): offline unit tests + manual QA only.

## Deferred features (design-ready, not built)

- **Break-even / net savings:** static build-cost table + optional user input; per-destination "X / N uses until paid for itself". Store already holds the counts.
- **Farming cape / worn-item / tablet teleports:** new recognizers + cost-table rows.
- Optional POH-only on-screen overlay.

## Open items to resolve during implementation

- Exact **varbit ids** for the Nexus default slot + configured-destination map, mounted Digsite default, mounted Xeric's default.
- Real **interface / widget component ids** for the Nexus and jewellery-box lists; the glory rub-dialog scoping state.
- **Animation/graphic** behavior per transport (object spin ≠ player animation; verify whether any player-side confirmation is even needed given interaction-based counting).
- Exact **charge counts** per jewellery item and the **lizardman-fang-per-use** for Xeric's.
- Confirm **guest-POH** interactions fire the same events as owner interactions.

## Plugin Hub submission checklist (follow-up)

- Open source, no obfuscation, no bundled binaries, no automation, no hidden-info advantage — this plugin is passive/read-only, so it qualifies.
- README, config UX, icon.
- Own public GitHub repo → PR to `runelite/plugin-hub` with a manifest pointing at a commit hash → CI build → manual review.

## References

- Portal nexus, Ornate jewellery box, Mounted xeric's talisman, Mounted digsite pendant — OSRS Wiki.
- `runelite/example-plugin` (template), `runelite/plugin-hub` (submission rules), `static.runelite.net` (API javadoc).
- `abextm/better-teleport-menu`, `nexus-map` — interface/varbit reading references.
- RuneLite dev env on hexane: Bolt launcher, RuneLite home `~/.local/share/bolt-launcher/.runelite/` (see project `handoff.md`).

---

## Addendum — Nexus cache catalog (wired 2026-08-15, commits `5699617` + `2946c95`)

Nexus destinations are now resolved from the **game cache** at runtime instead of relying only on hardcoded/guessed names — the user's "stop guessing names, do a set match not an if/contains chain" ask.

**Design — additive & fail-safe.** `NexusRecognizer` resolves in three layers:
1. **Name map** (today's behaviour, authoritative for recognized labels — never regressed).
2. **Cache catalog** (`NexusCatalog`, only when `isLoaded()`): matches the click against the game's own cache names (rename-proof; fixes any guessed label) and resolves the generic left-click "Teleport" default from varbit `6653`. Struct ids bridge back to the `Destination` enum via `NexusStructIndex`.
3. **Legacy varbit → Unknown** bucket.

Because the catalog only loads if the researched cache ids are correct, a wrong id means it silently stays unloaded and the plugin behaves exactly as the name-map-only version. **Zero regression risk.** It also fixes a genuinely-broken path: the generic left-click default previously always logged as `Unknown` (empty varbit map).

**Structure.** `CacheView` (narrow cache window, mirrors `GameStateView`) → `ClientCacheView` (prod adapter) / map-backed fakes (tests). `NexusCatalog` folds alt→primary (param `680`) and scry (`+150`) at load and exposes `structForName / structForDestValue / structForObject / name`. `NexusStructIndex` is the single struct-id→`Destination` bridge; the old `NexusCost` (a duplicate struct→cost table) was **removed** — cost comes from the resolved `Destination`'s own `CostBasis`, so nothing can drift.

**Live-verification checklist** (the one open question — the cache ids `1377 / 660–663 / 680 / 6653` were researched, not confirmed in-game):
1. In the plugin config, enable **Debug: log varbits**.
2. Log in, click any object once, and watch the client log for `[POH-CC] nexus catalog loaded: N names (461=…, 855=…)`.
   - Expect `N` ≈ 45, `461=Kharyrll`, `855=Civitas illa Fortis`. If so, the enum/struct/name ids are correct.
   - If it never logs, or `N=0`/names blank → `NEXUS_DEST_ENUM (1377)` or `STRUCT_PARAM_NAME (660)` is wrong; the plugin still works via the name map, and we adjust `PohGameIds` from the captured values.
3. Set the nexus left-click default to a destination, then left-click the portal (menu-entry-swapper style). It should count that destination (not `Default / Unknown`) → confirms varbit `6653`.
4. Teleport to a Wilderness destination (Dareeyak/Carrallanger/Annakarl/Ghorrock) via the list. Correct bucketing there confirms the cache-name rescue (these were the riskiest guesses).

**Stage B (after the ids are confirmed live):**
- Flip **display names** to cache strings (thread `NexusCatalog.name(structId)` into `PanelModel` rows) so the panel shows Jagex's exact labels, not our best-guess enum text. Resolution is already cache-driven; only the shown string is still the enum's.
- Add a **scry-mode gate** (varbit `6671 == 1` → don't count) — deliberately omitted for now because gating on an unconfirmed varbit could suppress real teleports; safe to add once `6671` is verified.

**Roadmap (unchanged, per user):** skill-cape teleports = v1.1; spirit trees + fairy rings = v2; break-even / net-savings layer deferred (store already holds the counts).
