# Roadmap

Where **PoH Teleport Counter** is and where it's headed. No dates — it's a hobby plugin and
priorities move around. Suggestions welcome.

## Shipped — v1.0

- Counts free Player-Owned House teleports across all five sources: **Teleport Nexus**,
  **jewellery box**, and the mounted **amulet of glory** / **Xeric's talisman** /
  **digsite pendant**.
- **GP saved** per teleport, priced live from the Grand Exchange.
- Detection is confirmed by the large world-coordinate jump a real teleport produces, so a
  cancelled Wilderness warning, a denied teleport, or just browsing a menu never miscounts.
- Collapsible, sortable side panel (**Most used** / **Most saved**) with a nested
  transport → item → destination breakdown.
- **Count guest POHs** toggle (own house vs a friend's, detected by entry method).

## Planned

### v1.1 — Skill cape teleports
Count the free teleports from skill / max / quest-cape sources.
> These are worn-item teleports rather than a house feature, so this widens the plugin's
> scope beyond the POH — it may warrant a broader name at that point.

### v2.0 — More POH transports
- **Spirit tree** built in the house.
- **Fairy ring** built in the house.

## Under consideration

- Per-session totals alongside all-time, with a reset.
- Configurable price basis (live GE vs a fixed reference).
- Copy / export a savings summary.

## Explicitly not planned

- Anything that automates play or reads more than your menu clicks and position. The plugin
  stays **passive and read-only**.

## Distribution

- [x] Feature-complete, unit-tested, self-reviewed
- [x] CI — GitHub Actions runs build + tests + RuneLite Checkstyle on JDK 11
- [x] Repository review — merged and made public
- [ ] Plugin Hub submission — PR to `runelite/plugin-hub` ← *we are here*
