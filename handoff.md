# RuneLite Plugin — Brainstorm Handoff

**Status:** ⏸️ Paused mid-brainstorm. We were choosing a *direction* for a plugin to build; no direction picked yet.
**Date:** 2026-07-03 (session started 2026-06-22)
**Machine:** hexane (`vania`)
**Goal:** Build a RuneLite plugin — ideally novel and Plugin-Hub-approvable — that fills a real gap in the user's setup, using Claude Code to make shipping it easy.

---

## TL;DR — how to resume

1. Read the **Candidate directions** section and pick one (Sailing was the recommendation).
2. From there, resume the brainstorming flow at step 4: *propose 2–3 concrete plugin concepts with trade-offs* → design → spec → implementation plan.
3. Building/testing reference lives in **RuneLite environment** and **Plugin dev + Hub approval** below.

Open question that stopped us: **"What area should the plugin target?"** (Sailing / PvM helper / Barbarian Assault / account-wide QoL).

---

## RuneLite environment (hexane)

The user does **not** run stock RuneLite — it's launched via the **Bolt launcher** (open-source Jagex/RuneLite/HDOS launcher), so the RuneLite home is in an unusual place.

| Thing | Value |
|---|---|
| Launcher | Bolt (`~/.local/share/bolt-launcher/`) |
| RuneLite home | `~/.local/share/bolt-launcher/.runelite/` |
| `user.home` override | JVM launched with `-Duser.home=/home/vania/.local/share/bolt-launcher` |
| Client version | `client-1.12.29.1.jar` |
| Launcher version | `2.7.7` |
| JVM | `java-17-openjdk`, `-Xmx768m`, uiScale 2.0 |
| `runelite` on PATH | `/usr/bin/runelite` (also installed via yay; cache at `~/.cache/yay/runelite`) |

Key paths under `~/.local/share/bolt-launcher/.runelite/`:
- `profiles2/*.properties` — **authoritative config**, including the installed-external-plugins list. Read via:
  `grep -rhiE "externalPlugins" ~/.local/share/bolt-launcher/.runelite/profiles2/`
  → key `runelite.externalPlugins=<comma-separated internal names>`
  Profiles present: `lorium-511491031674750` (largest/most-recently-modified → likely active account), `default-2607644338635`, `$rsprofile--1`, plus `profiles.json`.
- `plugins/` — cached external plugin JARs (hundreds of files). **No `plugin-hub/` dir on this install** — the JARs live in `plugins/`.
- `repository2/` — RuneLite client + dependency JARs (incl. `runelite-api-1.12.29.1-runtime.jar`).
- Per-plugin data dirs at top level: `clue-scroll-notifier/`, `ground-item-sounds/`, `location-display/`, `rich-text-notes/`, `spam-filter/`.
- `logs/`, `screenshots/`, `videos/`, `cache/`, `jagexcache/`.

> For **development**, you won't build against this Bolt install — you clone the `example-plugin` template and run via Gradle/IntelliJ against the RuneLite client (from source or the published artifacts). The Bolt install is only relevant for runtime/sideload testing.

---

## Playstyle analysis (from 214 installed Plugin Hub plugins)

The user is a **Plugin Hub power user** — 214 external plugins. Categorized read of what they play:

- **PvM-heavy** — CoX (`cox-scouter-external`, `cox-qol`, `cox-additions`), ToA (`toa-mistake-tracker`), Inferno (`inferno-stats`), Zulrah (`zulrah-helper`, `zulrah-loot-locator`), GWD (`better-godwars-overlay`), NMZ (`ultimate-nmz`, `nmz-optimal-points`, `zom-nmz-util`), plus combat tooling: `dps-helper`, `damage-counter`, `tick-counter`, `attacktimer`, `maxhitplugin`, `cannon-damage`, `attack-ranges`, `line-of-sight`, `boss-health-indicators`, `monster-hp-percentage`.
- **Barbarian Assault superfan** — `ba-call-highlight`, `ba-tiles`, `ba-minigame`, `ba-heal-codes`. Four separate BA plugins.
- **PvP / minigames** — LMS (`lms-start-notifier`, `large-logout`), `pvp-arena-points`, `soul-wars`, `trouble-brewing`, `rogues-den`, GotR (`guardians-of-the-rift-helper`, `-optimizer`), `wilderness-multi-lines`, `wilderness-player-alarm`.
- **Sailing (new skill)** — `sailing`, `boat-upgrades`, `ship-combat`, `ship-renamer`, `wheresmyboat`, `port-tasks`, `barracuda-trials-pathfinder`, `barracuda-trial-helm-lock`. **8 sailing plugins → actively playing the new skill; its plugin ecosystem is young.**
- **Clue fiend** — `clue-scroll-notifier`, `clue-steps`, `emote-clue-items`, `shortest-clue`, `missed-clues`, `clue-teleport-helper`, `tile-packs`.
- **Skilling / efficiency** — `motherlode-profit-tracker`, `easy-blastfurnace`, `advanced-mining`, `gemstone-crab-timer`, `stardust-per-hour`, `shooting-stars-tracking`, `mastering-mixology(-strategy)`, `herblorerecipes`, `birdhouse-overlay/-status`, `lazy-farming`, `daily-agility`, `agility-pyramid-slider-block-timer`, `agilityfc`, `tictac7x-rooftops`, `easy-giantsfoundry`, `totem-fletching`, `drift-net-improved`, `tempoross`, `wintertodt-notifications`, `treecount`, `tree-despawn-timer`, `tictac7x-sulliuscep`.
- **QoL / one-click** — `instant-inventory`, `modified-left-click-dropper`, `zom-leftclick-dropper`, `door-kicker`, `one-click-summer-garden`, `better-teleport-menu`, `teleport-zoom`, `fuzzy-bank-search`, `bank-tag-layouts`, `inventory-setups`, `bank-templates`, `storage-interactions`, `quick-prayer-preview`, `prayer-loadouts`, `autocast-utilities`, `compass-camera-control`, `camera-smoothing`.
- **Quest** — `quest-helper`, `optimal-quest-guide`, `quest-food-guard`.
- **Visual** — `117hd`, `hd-minimap`, `3d-weather`, `resource-packs`, `custom-xp-orbs`, `remastered-xp-globes`, `picture-in-picture`, `animatedicons`, `visual-metronome`.
- **Reminders / tracking** — `time-tracking-reminder`, `hydrate-reminder`, `goggles-reminder`, `unpotted-reminder`, `consumable-cooldowns`, `time-to-level`, `milestone-levels`, `virtual-level-ups`, `dude-wheres-my-stuff`, `banked-experience`, `did-i-compost`, `dryness-tracker`, `tostky-tracker`, `profit-tracker`, `loot-lister`, `loot-lookup`, `valuable-drop-prices`, `itemrarity`, `drop-rate-properties`, `combat-achievements-tracker`, `wasted-bank-space`.

Full raw list in **Appendix A**.

---

## Candidate directions (decision pending)

Presented as 4 options; **Sailing was the recommendation.** User redirected to "archive the session" before choosing.

1. **Sailing tooling (RECOMMENDED).** Newest skill, thinnest plugin coverage → best shot at something genuinely novel, useful daily, and easy to get Hub-approved with no near-duplicate to compete against. User already runs 8 sailing plugins, so they know the gaps firsthand. Lowest "beat the incumbents" bar.
2. **PvM / raid helper.** User lives in CoX/ToA/Inferno/Zulrah — a sharper encounter/rotation/mistake tracker. High personal payoff but high bar (many mature PvM plugins to beat).
3. **Barbarian Assault companion.** Four BA plugins installed → clear passion. Room for a next-gen all-in-one: calls, role swaps, points, heal codes, gambit/wave tracking. Niche but underserved.
4. **Account-wide QoL / tracking.** Cross-cutting tool spanning the whole account — unified goals, efficiency/streak tracking, or something the 214 plugins still don't do together. Broadest Hub appeal.

**Note on framing (from earlier in the session):** the user clarified the "Claude assistant plugin" idea means *using Claude Code to build/ship the plugin faster* — **NOT** putting an LLM in the game's hot loop. Any design must stay a normal passive/QoL RuneLite plugin.

---

## RuneLite plugin dev + Hub approval (primer)

*(Confirm current details against the live repos before building — knowledge is as of early 2026 and RuneLite processes drift.)*

**Two paths:**
- **Core plugin** — bundled in `runelite/runelite`. High bar, selective, slow. Not the target.
- **Plugin Hub** — `runelite/plugin-hub`. Community repo; installable from the in-client Plugin Hub panel. **This is the path.**

**Plugin Hub submission flow:**
1. Write the plugin in **Java** (Gradle), starting from the **`runelite/example-plugin`** template.
2. Push it to its own public GitHub repo.
3. Open a PR to **`runelite/plugin-hub`** adding a **manifest file** that points at your repo + a specific commit hash.
4. CI builds it; a RuneLite developer manually reviews.
5. Merge → it appears in everyone's Plugin Hub.

**Approval requirements / hard rules:**
- **Open source, no obfuscation, no bundled binaries, no sketchy network calls.**
- **Must not break Jagex's rules** — RuneLite runs under a special arrangement with Jagex and protects it hard:
  - ❌ **No automation / botting** (no synthetic input, no clicking for the player).
  - ❌ **No unfair advantage** (no revealing hidden info a normal player can't get).
  - Violations → rejected, and users of such software risk Jagex bans.
- Passive overlays / info / QoL that reorganize *existing* info → generally fine.

**RuneLite API hooks a plugin typically uses:** game-tick events, menu-entry manipulation, overlays (canvas drawing), config panels, chat/event listeners, NPC/object/inventory/varbit queries. Java-typed, well-documented; good fit for AI-assisted development.

**References to verify on resume:**
- `github.com/runelite/plugin-hub` (README has current submission rules)
- `github.com/runelite/example-plugin` (template)
- RuneLite API javadoc (`static.runelite.net`)

---

## Brainstorming process state

Following the `superpowers:brainstorming` flow. Checklist status:

- [x] 1. Explore project context (RuneLite install + installed plugins) — **done**
- [ ] 2. Offer visual companion just-in-time
- [~] 3. Ask clarifying questions to scope the plugin — **in progress; stopped at the direction question**
- [ ] 4. Propose 2–3 plugin approaches with trade-offs ← **resume here after a direction is chosen**
- [ ] 5. Present design, approval per section
- [ ] 6. Write design doc to `docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md` + commit
- [ ] 7. Spec self-review
- [ ] 8. User reviews written spec
- [ ] 9. Transition to implementation (writing-plans skill)

---

## Appendix A — full installed external plugin list (214)

Read from `runelite.externalPlugins` in `profiles2/*.properties`:

```
zom-leftclick-dropper, shooting-stars-tracking, potion-storage-bars, camera-smoothing,
instant-inventory, clan-chat-country-flags, home-improvement, spirit-tree-map, kitten-tracker,
slayer-tag-highlight, tool-required, butler, tictac7x-rooftops, motherlode-profit-tracker,
slayer-task-sorter, remastered-xp-globes, skills-tab-progress-bars, bank-tag-layouts,
inventory-setups, wikitagbankhighlighter, mastering-mixology-strategy, treecount, profit-tracker,
cannon-highlighter, ground-item-organizer, boss-health-indicators, attack-ranges, plank-sack,
time-tracking-reminder, clue-steps, ba-call-highlight, wheresmyboat, startierindicator,
damage-counter, door-kicker, rich-text-notes, multi-lines, loot-lister, npcleveloverlay,
tray-indicators, bank-cleaner, cox-scouter-external, afk-marks-canafis, 3d-weather, betteroverload,
osrs-player-count, guardians-of-the-rift-helper, trouble-brewing, resource-packs,
mouseover-text-disabler, goggles-reminder, port-tasks, drift-net-improved, better-teleport-menu,
attacktimer, zom-nmz-util, food-coloring, recommended-equipment, custom-xp-orbs,
guardians-of-the-rift-optimizer, afk-crab-helper, teleport-zoom, cheapest-food, poh-portal-labels,
optimal-quest-guide, quest-food-guard, ultimate-nmz, tempoross, patch-payment, spawnpredictor,
barrows-potential, dps-helper, potion-storage-customizer, varlamore-house-thieving, target-true-tile,
emote-clue-items, ba-tiles, sailing, daily-agility, consumable-cooldowns, boat-upgrades, ping-grapher,
corner-tile-indicators, marks-of-grace-counter, time-to-level, hydrate-reminder, shortest-clue,
action-progress, wintertodt-notifications, drop-rate-properties, barrows-door-highlighter,
large-logout, easy-blastfurnace, storage-interactions, wikisync, spec-dialogue,
agility-pyramid-slider-block-timer, inventory-summary, hpabovehead, ba-minigame, did-i-compost,
quick-prayer-preview, wilderness-multi-lines, ground-item-notification, projectile-override,
milestone-levels, fuzzy-bank-search, birdhouse-overlay, npc-timer, virtual-level-ups,
barracuda-trials-pathfinder, monsterstats, ship-renamer, random-screenshot, one-click-summer-garden,
lms-start-notifier, loot-lookup, fishing-spot-tracker, animatedicons, ba-heal-codes, spirit-tree-menu,
tick-counter, valuable-drop-prices, improved-tears-of-guthix-interface, easy-tasks, afkspot-finder,
dude-wheres-my-stuff, shortest-path, dryness-tracker, more-status-bars, no-player-use, tostky-tracker,
missed-clues, clue-teleport-helper, hd-minimap, ticktracker, zulrah-loot-locator, easy-giantsfoundry,
spamfilter, effective-level, equipment-inspector, unpotted-reminder, customizable-xp-drops,
frozen-icon-me, menuhp, tog-crowdsourcing, ground-item-sounds, bank-templates, cox-qol,
gemstone-crab-timer, itemrarity, zom-con-qol, better-godwars-overlay, inferno-stats, location-display,
stardust-per-hour, dont-telegrab-npcs, picture-in-picture, ship-combat, totem-fletching,
tictac7x-sulliuscep, tree-despawn-timer, modified-left-click-dropper, autocast-utilities,
slayer-assistant, scrollboxcounter, prayer-loadouts, cox-additions, quest-helper, 117hd, no-examine,
tictac7x-charges, clear-unsent-messages, wilderness-player-alarm, easy-empty, compass-camera-control,
random-event-hider, combat-achievements-tracker, soul-wars, nexus-map, barracuda-trial-helm-lock,
wasted-bank-space, slayer-helper, click-minimap-orbs, monster-hp-percentage, radius-markers,
looting-bag-value, tictac7x-motherlode, toa-mistake-tracker, herblorerecipes, lazy-farming,
compact-orbs, advanced-mining, tile-packs, maxhitplugin, center-skill-icons, cannon-damage,
prayer-regeneration-helper, unresponsive-cursor, zulrah-helper, item-link, better-npc-highlight,
banked-experience, mastering-mixology, catvrat, clue-scroll-notifier, pvp-arena-points, rogues-den,
birdhouse-status, visual-metronome, line-of-sight, nmz-optimal-points, interactable, agilityfc
```
