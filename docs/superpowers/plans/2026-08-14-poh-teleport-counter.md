# PoH Teleport Counter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A RuneLite plugin that counts free Player-Owned House teleports (Teleport Nexus, jewellery box, mounted glory/Xeric's/Digsite) and shows use-counts plus gross GP saved in a sortable, nested side panel.

**Architecture:** Interaction-based detection — per-transport recognizers turn RuneLite menu/widget/varbit events into a normalized `TeleportEvent`; a store keeps only per-destination **counts** (GP is derived live from a static cost basis × current GE price); a pure `PanelModel` builds the sorted tree the Swing panel renders. No location-jump detection ships. Detection IDs that can only be read from a running client are injected into recognizers and captured in a final live-QA task.

**Tech Stack:** Java (RuneLite plugin API, `runelite/example-plugin` template), Gradle, JUnit 4 + Mockito, Gson (bundled by RuneLite), Swing (PluginPanel).

## Global Constraints

- Built from the `runelite/example-plugin` template; match that template's Java + Gradle versions exactly (do not bump).
- **Jagex/RuneLite hard rules:** passive/read-only only — no automation, no synthetic input, no revealing hidden info. No obfuscation, no bundled binaries, no network calls. Open source.
- Java package: `com.smirnovlabs.pohteleports`. Plugin name: `PoH Teleport Counter`.
- Persistence: `ConfigManager`, config group `pohteleports`, auto-scoped per profile.
- Metric: per-`(transport, destination)` **count** + **gross GP saved** valued at live wiki price via `ItemManager.getItemPrice`. **Persist counts only**; derive saved-runes and GP at render time.
- Detection is interaction-based. Count only a **destination-selection**; never count menu-open, Configure/Build/Set-default, or a denied teleport. Guest-POH teleports are counted (`countGuestPoh` default `true`).
- Every RuneLite API signature in this plan is idiomatic but version-sensitive: verify each against the local javadoc/jar `~/.local/share/bolt-launcher/.runelite/repository2/runelite-api-1.12.29.1-runtime.jar` when it first appears, and prefer `net.runelite.api.gameval.*` id classes over deprecated ones.

---

## File Structure

```
runelite-plugin.properties
build.gradle · settings.gradle · gradle/ (wrapper)          # from example-plugin
src/main/resources/poh_teleport_counter_icon.png            # nav-button icon (16x16-ish)
src/main/java/com/smirnovlabs/pohteleports/
  PohTeleportCounterPlugin.java   # @PluginDescriptor; subscribes events; wires router→store→panel
  PohTeleportConfig.java          # @ConfigGroup("pohteleports"): sortMode, countGuestPoh
  model/
    Transport.java                # enum of the 5 transports
    Destination.java              # enum: id, displayName, transport, subGroup?, CostBasis
    SortMode.java                 # enum: MOST_USED, MOST_SAVED
    TeleportEvent.java            # {Destination destination, int tick}
  cost/
    CostBasis.java                # abstract: runes / itemFraction / consumable / NONE ; gpPerUse(priceFn)
    SavingsValuator.java          # wraps ItemManager; gpPerUse(Destination), gpTotal(Destination,count)
  store/
    TeleportSavingsStore.java     # Map<String,Integer> counts; record/load/persist/totals
  detect/
    GameStateView.java            # interface: getVarbit(id), isInPoh(), consumeGloryRubbed(), etc.
    TeleportRecognizer.java       # interface: Optional<Destination> onMenuOptionClicked(e, state)
    DetectionRouter.java          # routes events to recognizers; debounce; denial guard
    PohGameIds.java               # widget/varbit/object/anim/menu-option constants (filled in Task 12)
    NexusRecognizer.java
    JewelleryBoxRecognizer.java
    MountedGloryRecognizer.java
    MountedAmuletRecognizer.java  # Xeric's + Digsite
  ui/
    PanelModel.java               # pure: (store, valuator, sortMode) -> sorted SectionRow tree
    PohTeleportPanel.java         # Swing PluginPanel rendering PanelModel
src/test/java/com/smirnovlabs/pohteleports/
  cost/CostBasisTest.java · cost/SavingsValuatorTest.java
  model/DestinationTableTest.java
  store/TeleportSavingsStoreTest.java
  ui/PanelModelTest.java
  detect/NexusRecognizerTest.java · detect/JewelleryBoxRecognizerTest.java
  detect/MountedGloryRecognizerTest.java · detect/MountedAmuletRecognizerTest.java
```

**Consolidation note:** the spec's separate `TeleportCostTable` is folded into the `Destination` enum (each destination carries its own `CostBasis`) — DRYer, same behavior. `SavingsValuator` still isolates all `ItemManager` contact.

---

### Task 1: Scaffold the plugin and get a green build

**Files:**
- Create (from template): whole `runelite/example-plugin` tree
- Modify: `build.gradle`, `settings.gradle`, `runelite-plugin.properties`
- Rename: `ExamplePlugin`/`ExampleConfig` → package `com.smirnovlabs.pohteleports`
- Test: `src/test/java/com/smirnovlabs/pohteleports/SmokeTest.java`

**Interfaces:**
- Produces: a buildable Gradle project with package `com.smirnovlabs.pohteleports` and a runnable JUnit setup.

- [ ] **Step 1: Fetch the template** into the repo root (it already contains `docs/`, `handoff.md`, `.gitignore`):

```bash
cd ~/Projects/1.Personal/games/runelite-plugin
tmp=$(mktemp -d) && git clone --depth 1 https://github.com/runelite/example-plugin "$tmp"
# copy everything except the template's own .git / .gitignore / README
rsync -a --exclude='.git' --exclude='.gitignore' --exclude='README.md' "$tmp"/ ./
rm -rf "$tmp"
```

- [ ] **Step 2: Set identity** — edit `settings.gradle` `rootProject.name = 'poh-teleport-counter'`; edit `runelite-plugin.properties`:

```properties
displayName=PoH Teleport Counter
author=vania
description=Counts your free Player-Owned House teleports and the runes/charges they save
tags=poh,teleport,nexus,jewellery,construction
plugins=com.smirnovlabs.pohteleports.PohTeleportCounterPlugin
```

- [ ] **Step 3: Move the sources** from the template package (e.g. `com/example`) to `src/main/java/com/smirnovlabs/pohteleports/`, rename `ExamplePlugin`→`PohTeleportCounterPlugin`, `ExampleConfig`→`PohTeleportConfig`, fix `package` lines and the `@PluginDescriptor(name = "PoH Teleport Counter")`. Delete leftover template test/example files.

- [ ] **Step 4: Write a smoke test**

```java
package com.smirnovlabs.pohteleports;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SmokeTest {
    @Test public void pluginNameIsSet() {
        assertEquals("PoH Teleport Counter",
            PohTeleportCounterPlugin.class.getAnnotation(net.runelite.client.plugins.PluginDescriptor.class).name());
    }
}
```

- [ ] **Step 5: Build & test**

Run: `./gradlew clean build`
Expected: `BUILD SUCCESSFUL`; `SmokeTest` passes. If JUnit/Mockito are absent, add to `build.gradle` dependencies: `testImplementation 'junit:junit:4.13.2'` and `testImplementation 'org.mockito:mockito-core:5.+'`, then rerun.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "chore: scaffold PoH Teleport Counter from example-plugin"
```

---

### Task 2: Model types + destination data table

**Files:**
- Create: `model/Transport.java`, `model/SortMode.java`, `model/TeleportEvent.java`, `model/Destination.java`
- Test: `model/DestinationTableTest.java`

**Interfaces:**
- Produces: `Transport{NEXUS,JEWELLERY_BOX,MOUNTED_GLORY,MOUNTED_XERICS,MOUNTED_DIGSITE}` each with `getDisplayName()`; `SortMode{MOST_USED,MOST_SAVED}`; `Destination` enum with `getId()`, `getDisplayName()`, `getTransport()`, `getSubGroup()` (nullable), `getCostBasis()`; `TeleportEvent{getDestination(), getTick()}`. Consumed by every later task.

- [ ] **Step 1: Write the failing test** (coverage invariants — cheap and catches data-entry mistakes)

```java
package com.smirnovlabs.pohteleports.model;

import org.junit.Test;
import java.util.EnumSet;
import static org.junit.Assert.*;

public class DestinationTableTest {
    @Test public void everyTransportHasAtLeastOneDestination() {
        for (Transport t : Transport.values()) {
            assertTrue("no destinations for " + t,
                EnumSet.allOf(Destination.class).stream().anyMatch(d -> d.getTransport() == t));
        }
    }
    @Test public void everyDestinationHasIdAndBasis() {
        for (Destination d : Destination.values()) {
            assertNotNull(d.getId());
            assertFalse(d.getId().isEmpty());
            assertNotNull("null basis for " + d, d.getCostBasis());
        }
    }
    @Test public void idsAreUnique() {
        long distinct = EnumSet.allOf(Destination.class).stream().map(Destination::getId).distinct().count();
        assertEquals(Destination.values().length, distinct);
    }
}
```

- [ ] **Step 2: Run to verify it fails** — `./gradlew test --tests "*DestinationTableTest"` → FAIL (`Destination` not found).

- [ ] **Step 3: Implement `Transport`, `SortMode`, `TeleportEvent`**

```java
// Transport.java
package com.smirnovlabs.pohteleports.model;
public enum Transport {
    NEXUS("Teleport Nexus"), JEWELLERY_BOX("Jewellery box"),
    MOUNTED_GLORY("Mounted glory"), MOUNTED_XERICS("Mounted Xeric's talisman"),
    MOUNTED_DIGSITE("Mounted Digsite pendant");
    private final String displayName;
    Transport(String d) { this.displayName = d; }
    public String getDisplayName() { return displayName; }
}
```
```java
// SortMode.java
package com.smirnovlabs.pohteleports.model;
public enum SortMode { MOST_USED, MOST_SAVED }
```
```java
// TeleportEvent.java
package com.smirnovlabs.pohteleports.model;
public final class TeleportEvent {
    private final Destination destination; private final int tick;
    public TeleportEvent(Destination destination, int tick) { this.destination = destination; this.tick = tick; }
    public Destination getDestination() { return destination; }
    public int getTick() { return tick; }
}
```

- [ ] **Step 4: Implement `Destination`** with a representative-but-real starter set (pattern shown for every basis kind). Rune/charge item ids use `net.runelite.api.gameval.ItemID` (verify the exact constant names against the jar):

```java
package com.smirnovlabs.pohteleports.model;

import com.smirnovlabs.pohteleports.cost.CostBasis;
import java.util.Map;
import static com.smirnovlabs.pohteleports.model.Transport.*;
import net.runelite.api.gameval.ItemID;

public enum Destination {
    // --- Nexus (RUNES basis: the equivalent standard-spellbook teleport) ---
    NEXUS_VARROCK("nexus:varrock", "Varrock", NEXUS, null,
        CostBasis.runes(Map.of(ItemID.AIRRUNE, 3, ItemID.FIRERUNE, 1, ItemID.LAWRUNE, 1))),
    NEXUS_CAMELOT("nexus:camelot", "Camelot", NEXUS, null,
        CostBasis.runes(Map.of(ItemID.AIRRUNE, 5, ItemID.LAWRUNE, 1))),
    NEXUS_FALADOR("nexus:falador", "Falador", NEXUS, null,
        CostBasis.runes(Map.of(ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1))),

    // --- Jewellery box (ITEM_FRACTION: item GE price / charges), subGroup = the item ---
    JBOX_GLORY_EDGEVILLE("jbox:glory:edgeville", "Edgeville", JEWELLERY_BOX, "Amulet of glory",
        CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),
    JBOX_DUEL_FEROX("jbox:duel:ferox", "Ferox Enclave", JEWELLERY_BOX, "Ring of dueling",
        CostBasis.itemFraction(ItemID.RING_OF_DUELING_8, 8)),

    // --- Mounted glory (ITEM_FRACTION), 4 destinations, flat ---
    MGLORY_EDGEVILLE("mglory:edgeville", "Edgeville", MOUNTED_GLORY, null,
        CostBasis.itemFraction(ItemID.AMULET_OF_GLORY_6, 6)),

    // --- Mounted Xeric's (CONSUMABLE: lizardman fangs per use — confirm qty in Task 12) ---
    MXERIC_LOOKOUT("mxeric:lookout", "Xeric's Look-out", MOUNTED_XERICS, null,
        CostBasis.consumable(ItemID.LIZARDMAN_FANG, 1)),

    // --- Mounted Digsite (ITEM_FRACTION: pendant(5) / 5) ---
    MDIG_DIGSITE("mdig:digsite", "Digsite", MOUNTED_DIGSITE, null,
        CostBasis.itemFraction(ItemID.DIGSITE_PENDANT_5, 5)),

    // --- Per-transport unknown/default buckets (count-only) ---
    NEXUS_UNKNOWN("nexus:unknown", "Default / Unknown", NEXUS, null, CostBasis.NONE),
    JBOX_UNKNOWN("jbox:unknown", "Default / Unknown", JEWELLERY_BOX, null, CostBasis.NONE),
    MGLORY_UNKNOWN("mglory:unknown", "Default / Unknown", MOUNTED_GLORY, null, CostBasis.NONE),
    MXERIC_UNKNOWN("mxeric:unknown", "Default / Unknown", MOUNTED_XERICS, null, CostBasis.NONE),
    MDIG_UNKNOWN("mdig:unknown", "Default / Unknown", MOUNTED_DIGSITE, null, CostBasis.NONE);

    private final String id, displayName, subGroup; private final Transport transport; private final CostBasis costBasis;
    Destination(String id, String displayName, Transport t, String subGroup, CostBasis basis) {
        this.id = id; this.displayName = displayName; this.transport = t; this.subGroup = subGroup; this.costBasis = basis;
    }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Transport getTransport() { return transport; }
    public String getSubGroup() { return subGroup; }
    public CostBasis getCostBasis() { return costBasis; }

    public static Destination unknownFor(Transport t) {
        switch (t) {
            case NEXUS: return NEXUS_UNKNOWN; case JEWELLERY_BOX: return JBOX_UNKNOWN;
            case MOUNTED_GLORY: return MGLORY_UNKNOWN; case MOUNTED_XERICS: return MXERIC_UNKNOWN;
            case MOUNTED_DIGSITE: return MDIG_UNKNOWN; default: throw new IllegalArgumentException(t.name());
        }
    }
}
```

- [ ] **Step 5: Run to verify pass** — `./gradlew test --tests "*DestinationTableTest"` → PASS.

- [ ] **Step 6: Populate the full destination set (data entry, not placeholder).** Source: the OSRS Wiki pages *Portal nexus*, *Ornate jewellery box*, *Mounted amulet of glory*, *Mounted xeric's talisman*, *Mounted digsite pendant*. For **each** teleport destination add one enum constant following the exact patterns above: Nexus → `CostBasis.runes(...)` with the standard-spellbook rune set; jewellery box → `CostBasis.itemFraction(itemId, charges)` with `subGroup` = the item name; mounted glory/Digsite → `itemFraction`; mounted Xeric's → `consumable(LIZARDMAN_FANG, qty)`; any destination with no priceable standard equivalent → `CostBasis.NONE`. The `DestinationTableTest` invariants (unique ids, non-null basis, every transport covered) verify completeness after entry.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: model types and destination cost table"
```

---

### Task 3: CostBasis + SavingsValuator

**Files:**
- Create: `cost/CostBasis.java`, `cost/SavingsValuator.java`
- Test: `cost/CostBasisTest.java`, `cost/SavingsValuatorTest.java`

**Interfaces:**
- Consumes: `Destination.getCostBasis()`.
- Produces: `CostBasis.runes(Map<Integer,Integer>)`, `CostBasis.itemFraction(int itemId,int charges)`, `CostBasis.consumable(int itemId,int qty)`, `CostBasis.NONE`; instance `long gpPerUse(IntUnaryOperator priceFn)`. `SavingsValuator(ItemManager)` with `long gpPerUse(Destination)` and `long gpTotal(Destination, int count)`.

- [ ] **Step 1: Write the failing test**

```java
package com.smirnovlabs.pohteleports.cost;

import org.junit.Test;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import static org.junit.Assert.assertEquals;

public class CostBasisTest {
    // price: itemId -> gp
    private final IntUnaryOperator price = id -> { switch (id) { case 1: return 5; case 2: return 300; case 3: return 100; default: return 0; } };

    @Test public void runesSumsPriceTimesQty() {
        assertEquals(5*3 + 300*1, CostBasis.runes(Map.of(1, 3, 2, 1)).gpPerUse(price));
    }
    @Test public void itemFractionDividesByCharges() {
        assertEquals(300 / 6, CostBasis.itemFraction(2, 6).gpPerUse(price)); // 50
    }
    @Test public void consumableMultipliesQty() {
        assertEquals(100 * 2, CostBasis.consumable(3, 2).gpPerUse(price));
    }
    @Test public void noneIsZero() { assertEquals(0, CostBasis.NONE.gpPerUse(price)); }
}
```

- [ ] **Step 2: Run to verify it fails** — `./gradlew test --tests "*CostBasisTest"` → FAIL.

- [ ] **Step 3: Implement `CostBasis`**

```java
package com.smirnovlabs.pohteleports.cost;

import java.util.Map;
import java.util.function.IntUnaryOperator;

public abstract class CostBasis {
    public abstract long gpPerUse(IntUnaryOperator priceFn);

    public static CostBasis runes(Map<Integer,Integer> runes) {
        return new CostBasis() {
            public long gpPerUse(IntUnaryOperator p) {
                long sum = 0; for (Map.Entry<Integer,Integer> e : runes.entrySet()) sum += (long) p.applyAsInt(e.getKey()) * e.getValue();
                return sum;
            }
        };
    }
    public static CostBasis itemFraction(int itemId, int charges) {
        return new CostBasis() { public long gpPerUse(IntUnaryOperator p) { return charges <= 0 ? 0 : (long) p.applyAsInt(itemId) / charges; } };
    }
    public static CostBasis consumable(int itemId, int qty) {
        return new CostBasis() { public long gpPerUse(IntUnaryOperator p) { return (long) p.applyAsInt(itemId) * qty; } };
    }
    public static final CostBasis NONE = new CostBasis() { public long gpPerUse(IntUnaryOperator p) { return 0; } };
}
```

- [ ] **Step 4: Run to verify pass** — `./gradlew test --tests "*CostBasisTest"` → PASS.

- [ ] **Step 5: Write `SavingsValuator` test** (mock `ItemManager`)

```java
package com.smirnovlabs.pohteleports.cost;

import com.smirnovlabs.pohteleports.model.Destination;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SavingsValuatorTest {
    @Test public void gpTotalIsPerUseTimesCount() {
        ItemManager im = mock(ItemManager.class);
        when(im.getItemPrice(anyInt())).thenReturn(60); // every rune = 60
        SavingsValuator v = new SavingsValuator(im);
        long per = v.gpPerUse(Destination.NEXUS_CAMELOT);      // 5 air + 1 law = 6 * 60 = 360
        assertEquals(360, per);
        assertEquals(360 * 10, v.gpTotal(Destination.NEXUS_CAMELOT, 10));
    }
    @Test public void noneValuesToZero() {
        ItemManager im = mock(ItemManager.class);
        assertEquals(0, new SavingsValuator(im).gpPerUse(Destination.NEXUS_UNKNOWN));
    }
}
```

- [ ] **Step 6: Implement `SavingsValuator`**

```java
package com.smirnovlabs.pohteleports.cost;

import com.smirnovlabs.pohteleports.model.Destination;
import net.runelite.client.game.ItemManager;

public class SavingsValuator {
    private final ItemManager itemManager;
    public SavingsValuator(ItemManager itemManager) { this.itemManager = itemManager; }
    public long gpPerUse(Destination d) { return d.getCostBasis().gpPerUse(itemManager::getItemPrice); }
    public long gpTotal(Destination d, int count) { return gpPerUse(d) * count; }
}
```

- [ ] **Step 7: Run all cost tests & commit** — `./gradlew test --tests "*cost*"` → PASS.

```bash
git add -A && git commit -m "feat: cost basis and live GE valuation"
```

---

### Task 4: TeleportSavingsStore (counts + persistence)

**Files:**
- Create: `store/TeleportSavingsStore.java`
- Test: `store/TeleportSavingsStoreTest.java`

**Interfaces:**
- Consumes: `TeleportEvent`, `Destination`, `ConfigManager`.
- Produces: `record(TeleportEvent)`; `int count(Destination)`; `Map<Destination,Integer> snapshot()`; `int totalCount()`; `load(ConfigManager)`; `persist(ConfigManager)`. Config group constant `TeleportSavingsStore.GROUP = "pohteleports"`, key `"counts"`.

- [ ] **Step 1: Write the failing test** (in-memory logic + Gson round-trip via mocked ConfigManager)

```java
package com.smirnovlabs.pohteleports.store;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.TeleportEvent;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TeleportSavingsStoreTest {
    @Test public void recordIncrementsCount() {
        TeleportSavingsStore s = new TeleportSavingsStore();
        s.record(new TeleportEvent(Destination.NEXUS_VARROCK, 1));
        s.record(new TeleportEvent(Destination.NEXUS_VARROCK, 2));
        assertEquals(2, s.count(Destination.NEXUS_VARROCK));
        assertEquals(0, s.count(Destination.NEXUS_CAMELOT));
        assertEquals(2, s.totalCount());
    }
    @Test public void persistThenLoadRoundTrips() {
        ConfigManager cm = mock(ConfigManager.class);
        TeleportSavingsStore a = new TeleportSavingsStore();
        a.record(new TeleportEvent(Destination.MGLORY_EDGEVILLE, 5));
        a.persist(cm);
        // capture what was written and feed it to a fresh store's load
        org.mockito.ArgumentCaptor<String> cap = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(cm).setConfiguration(eq("pohteleports"), eq("counts"), cap.capture());
        when(cm.getConfiguration("pohteleports", "counts")).thenReturn(cap.getValue());
        TeleportSavingsStore b = new TeleportSavingsStore();
        b.load(cm);
        assertEquals(1, b.count(Destination.MGLORY_EDGEVILLE));
    }
    @Test public void loadWithNoSavedDataIsEmpty() {
        ConfigManager cm = mock(ConfigManager.class);
        when(cm.getConfiguration("pohteleports", "counts")).thenReturn(null);
        TeleportSavingsStore s = new TeleportSavingsStore(); s.load(cm);
        assertEquals(0, s.totalCount());
    }
}
```

- [ ] **Step 2: Run to verify it fails** — `./gradlew test --tests "*TeleportSavingsStoreTest"` → FAIL.

- [ ] **Step 3: Implement `TeleportSavingsStore`** (keys are stable `Destination.getId()` strings; unknown ids on load are skipped so renames never crash)

```java
package com.smirnovlabs.pohteleports.store;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.TeleportEvent;
import net.runelite.client.config.ConfigManager;
import java.lang.reflect.Type;
import java.util.*;

public class TeleportSavingsStore {
    public static final String GROUP = "pohteleports";
    public static final String KEY_COUNTS = "counts";
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String,Integer>>(){}.getType();

    private final EnumMap<Destination,Integer> counts = new EnumMap<>(Destination.class);

    public void record(TeleportEvent e) { counts.merge(e.getDestination(), 1, Integer::sum); }
    public int count(Destination d) { return counts.getOrDefault(d, 0); }
    public int totalCount() { int t = 0; for (int v : counts.values()) t += v; return t; }
    public Map<Destination,Integer> snapshot() { return Collections.unmodifiableMap(new EnumMap<>(counts)); }

    public void persist(ConfigManager cm) {
        Map<String,Integer> byId = new HashMap<>();
        for (Map.Entry<Destination,Integer> e : counts.entrySet()) byId.put(e.getKey().getId(), e.getValue());
        cm.setConfiguration(GROUP, KEY_COUNTS, GSON.toJson(byId, MAP_TYPE));
    }
    public void load(ConfigManager cm) {
        counts.clear();
        String json = cm.getConfiguration(GROUP, KEY_COUNTS);
        if (json == null || json.isEmpty()) return;
        Map<String,Integer> byId = GSON.fromJson(json, MAP_TYPE);
        if (byId == null) return;
        Map<String,Destination> index = new HashMap<>();
        for (Destination d : Destination.values()) index.put(d.getId(), d);
        for (Map.Entry<String,Integer> e : byId.entrySet()) {
            Destination d = index.get(e.getKey());
            if (d != null && e.getValue() != null) counts.put(d, e.getValue());
        }
    }
}
```

- [ ] **Step 4: Run to verify pass** — `./gradlew test --tests "*TeleportSavingsStoreTest"` → PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: count store with per-profile persistence"
```

---

### Task 5: PanelModel (pure sorted view-model)

**Files:**
- Create: `ui/PanelModel.java`
- Test: `ui/PanelModelTest.java`

**Interfaces:**
- Consumes: `TeleportSavingsStore.snapshot()`, `SavingsValuator`, `SortMode`.
- Produces: `PanelModel.build(snapshot, valuator, sortMode)` → `PanelModel` with `long getTotalGp()`, `int getTotalCount()`, `List<Section> getSections()`; `Section{Transport transport, int count, long gp, List<SubGroup> subGroups}`; `SubGroup{String name /*null = flat*/, int count, long gp, List<Row> rows}`; `Row{Destination destination, int count, long gp}`. Sorting by count or gp per `SortMode`, descending, at section/subgroup/row levels.

- [ ] **Step 1: Write the failing test**

```java
package com.smirnovlabs.pohteleports.ui;

import com.smirnovlabs.pohteleports.cost.SavingsValuator;
import com.smirnovlabs.pohteleports.model.*;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PanelModelTest {
    private SavingsValuator valuator() {
        ItemManager im = mock(ItemManager.class);
        when(im.getItemPrice(anyInt())).thenReturn(60);
        return new SavingsValuator(im);
    }
    @Test public void sectionsSortByGpWhenMostSaved() {
        Map<Destination,Integer> snap = new EnumMap<>(Destination.class);
        snap.put(Destination.NEXUS_VARROCK, 3);     // 5 runes*60=300/use -> wait: Varrock=5 runes
        snap.put(Destination.MGLORY_EDGEVILLE, 1);  // glory 6-charge / at 60 -> small
        PanelModel m = PanelModel.build(snap, valuator(), SortMode.MOST_SAVED);
        assertEquals(Transport.NEXUS, m.getSections().get(0).getTransport()); // highest gp first
        assertEquals(4, m.getTotalCount());
    }
    @Test public void jewelleryBoxNestsBySubGroup() {
        Map<Destination,Integer> snap = new EnumMap<>(Destination.class);
        snap.put(Destination.JBOX_GLORY_EDGEVILLE, 2);
        snap.put(Destination.JBOX_DUEL_FEROX, 5);
        PanelModel m = PanelModel.build(snap, valuator(), SortMode.MOST_USED);
        PanelModel.Section jbox = m.getSections().stream()
            .filter(s -> s.getTransport() == Transport.JEWELLERY_BOX).findFirst().orElseThrow(AssertionError::new);
        assertEquals(2, jbox.getSubGroups().size());               // Glory, Ring of dueling
        assertEquals("Ring of dueling", jbox.getSubGroups().get(0).getName()); // 5 > 2, most-used first
    }
    @Test public void flatTransportsHaveSingleAnonymousSubGroup() {
        Map<Destination,Integer> snap = new EnumMap<>(Destination.class);
        snap.put(Destination.MGLORY_EDGEVILLE, 1);
        PanelModel m = PanelModel.build(snap, valuator(), SortMode.MOST_USED);
        PanelModel.Section g = m.getSections().get(0);
        assertEquals(1, g.getSubGroups().size());
        assertNull(g.getSubGroups().get(0).getName());
        assertEquals(Destination.MGLORY_EDGEVILLE, g.getSubGroups().get(0).getRows().get(0).getDestination());
    }
}
```

- [ ] **Step 2: Run to verify it fails** — `./gradlew test --tests "*PanelModelTest"` → FAIL.

- [ ] **Step 3: Implement `PanelModel`**

```java
package com.smirnovlabs.pohteleports.ui;

import com.smirnovlabs.pohteleports.cost.SavingsValuator;
import com.smirnovlabs.pohteleports.model.*;
import java.util.*;
import java.util.stream.Collectors;

public final class PanelModel {
    public static final class Row {
        private final Destination destination; private final int count; private final long gp;
        Row(Destination d, int c, long g) { destination = d; count = c; gp = g; }
        public Destination getDestination() { return destination; } public int getCount() { return count; } public long getGp() { return gp; }
    }
    public static final class SubGroup {
        private final String name; private final int count; private final long gp; private final List<Row> rows;
        SubGroup(String n, int c, long g, List<Row> r) { name = n; count = c; gp = g; rows = r; }
        public String getName() { return name; } public int getCount() { return count; } public long getGp() { return gp; } public List<Row> getRows() { return rows; }
    }
    public static final class Section {
        private final Transport transport; private final int count; private final long gp; private final List<SubGroup> subGroups;
        Section(Transport t, int c, long g, List<SubGroup> s) { transport = t; count = c; gp = g; subGroups = s; }
        public Transport getTransport() { return transport; } public int getCount() { return count; } public long getGp() { return gp; } public List<SubGroup> getSubGroups() { return subGroups; }
    }

    private final int totalCount; private final long totalGp; private final List<Section> sections;
    private PanelModel(int tc, long tg, List<Section> s) { totalCount = tc; totalGp = tg; sections = s; }
    public int getTotalCount() { return totalCount; } public long getTotalGp() { return totalGp; } public List<Section> getSections() { return sections; }

    public static PanelModel build(Map<Destination,Integer> snapshot, SavingsValuator valuator, SortMode sort) {
        Comparator<long[]> byMode = (a, b) -> sort == SortMode.MOST_SAVED
            ? Long.compare(b[1], a[1]) : Long.compare(b[0], a[0]); // [count, gp], descending
        int totalCount = 0; long totalGp = 0;
        List<Section> sections = new ArrayList<>();

        for (Transport t : Transport.values()) {
            Map<String,List<Row>> bySub = new LinkedHashMap<>(); // subGroup name (or "") -> rows
            int secCount = 0; long secGp = 0;
            for (Map.Entry<Destination,Integer> e : snapshot.entrySet()) {
                Destination d = e.getKey(); int c = e.getValue(); if (d.getTransport() != t || c == 0) continue;
                long gp = valuator.gpTotal(d, c);
                String sub = d.getSubGroup() == null ? "" : d.getSubGroup();
                bySub.computeIfAbsent(sub, k -> new ArrayList<>()).add(new Row(d, c, gp));
                secCount += c; secGp += gp;
            }
            if (secCount == 0) continue;
            List<SubGroup> groups = new ArrayList<>();
            for (Map.Entry<String,List<Row>> g : bySub.entrySet()) {
                List<Row> rows = g.getValue();
                rows.sort((r1, r2) -> byMode.compare(new long[]{r1.count, r1.gp}, new long[]{r2.count, r2.gp}));
                int gc = rows.stream().mapToInt(Row::getCount).sum();
                long gg = rows.stream().mapToLong(Row::getGp).sum();
                groups.add(new SubGroup(g.getKey().isEmpty() ? null : g.getKey(), gc, gg, rows));
            }
            groups.sort((s1, s2) -> byMode.compare(new long[]{s1.count, s1.gp}, new long[]{s2.count, s2.gp}));
            sections.add(new Section(t, secCount, secGp, groups));
            totalCount += secCount; totalGp += secGp;
        }
        sections.sort((s1, s2) -> byMode.compare(new long[]{s1.count, s1.gp}, new long[]{s2.count, s2.gp}));
        return new PanelModel(totalCount, totalGp, sections);
    }
}
```

- [ ] **Step 4: Run to verify pass** — `./gradlew test --tests "*PanelModelTest"` → PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: sorted nested panel view-model"
```

---

### Task 6: Detection core — recognizer interface, router, game-state view, id constants

**Files:**
- Create: `detect/TeleportRecognizer.java`, `detect/GameStateView.java`, `detect/DetectionRouter.java`, `detect/PohGameIds.java`
- Test: `detect/DetectionRouterTest.java` (created here; per-recognizer tests follow)

**Interfaces:**
- Consumes: `MenuOptionClicked`, `ChatMessage`, `TeleportEvent`, `Destination`, `Transport`.
- Produces:
  - `interface TeleportRecognizer { Optional<Destination> onMenuOptionClicked(MenuOptionClicked e, GameStateView state); }`
  - `interface GameStateView { int getVarbit(int id); boolean isInPoh(); int currentTick(); }` (grows per recognizer)
  - `DetectionRouter(List<TeleportRecognizer> recognizers, GameStateView state, java.util.function.Consumer<TeleportEvent> sink)` with `void onMenuOptionClicked(MenuOptionClicked)`, `void onChatMessage(ChatMessage)`; internal debounce (one emit per tick) and denial suppression.
- `PohGameIds` public static final int constants, sentinel `-1` until Task 12.

- [ ] **Step 1: Write the failing test** (router debounces within a tick and suppresses after a denial message)

```java
package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.TeleportEvent;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ChatMessage;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DetectionRouterTest {
    static class FakeState implements GameStateView {
        int tick; public int getVarbit(int id){return 0;} public boolean isInPoh(){return true;} public int currentTick(){return tick;}
    }
    @Test public void debouncesMultipleEmitsInSameTick() {
        FakeState st = new FakeState();
        List<TeleportEvent> out = new ArrayList<>();
        TeleportRecognizer always = (e, s) -> Optional.of(Destination.NEXUS_VARROCK);
        DetectionRouter r = new DetectionRouter(List.of(always), st, out::add);
        r.onMenuOptionClicked(mock(MenuOptionClicked.class));
        r.onMenuOptionClicked(mock(MenuOptionClicked.class)); // same tick
        assertEquals(1, out.size());
        st.tick = 1;
        r.onMenuOptionClicked(mock(MenuOptionClicked.class));
        assertEquals(2, out.size());
    }
    @Test public void suppressesCountRightAfterDenialMessage() {
        FakeState st = new FakeState();
        List<TeleportEvent> out = new ArrayList<>();
        DetectionRouter r = new DetectionRouter(List.of((e,s)->Optional.of(Destination.NEXUS_VARROCK)), st, out::add);
        ChatMessage denial = mock(ChatMessage.class);
        when(denial.getMessage()).thenReturn("You need a Magic level of 25 to cast this.");
        r.onChatMessage(denial);
        r.onMenuOptionClicked(mock(MenuOptionClicked.class)); // same tick as denial -> suppressed
        assertEquals(0, out.size());
    }
}
```

- [ ] **Step 2: Run to verify it fails** — `./gradlew test --tests "*DetectionRouterTest"` → FAIL.

- [ ] **Step 3: Implement the interfaces + router**

```java
// TeleportRecognizer.java
package com.smirnovlabs.pohteleports.detect;
import com.smirnovlabs.pohteleports.model.Destination;
import net.runelite.api.events.MenuOptionClicked;
import java.util.Optional;
public interface TeleportRecognizer {
    Optional<Destination> onMenuOptionClicked(MenuOptionClicked e, GameStateView state);
}
```
```java
// GameStateView.java
package com.smirnovlabs.pohteleports.detect;
public interface GameStateView {
    int getVarbit(int varbitId);
    boolean isInPoh();
    int currentTick();
}
```
```java
// DetectionRouter.java
package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.TeleportEvent;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class DetectionRouter {
    // Substrings of common teleport-denial game messages (extend during live QA).
    private static final String[] DENIALS = {
        "you need a magic level", "you can't reach that", "you can't teleport",
        "a mysterious force", "you have not unlocked", "members"
    };
    private final List<TeleportRecognizer> recognizers;
    private final GameStateView state;
    private final Consumer<TeleportEvent> sink;
    private int lastEmitTick = Integer.MIN_VALUE;
    private int lastDenialTick = Integer.MIN_VALUE;

    public DetectionRouter(List<TeleportRecognizer> recognizers, GameStateView state, Consumer<TeleportEvent> sink) {
        this.recognizers = recognizers; this.state = state; this.sink = sink;
    }
    public void onChatMessage(ChatMessage e) {
        String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
        for (String d : DENIALS) if (m.contains(d)) { lastDenialTick = state.currentTick(); return; }
    }
    public void onMenuOptionClicked(MenuOptionClicked e) {
        int tick = state.currentTick();
        if (tick == lastEmitTick || tick == lastDenialTick) return; // debounce + denial guard
        for (TeleportRecognizer r : recognizers) {
            Optional<Destination> d = r.onMenuOptionClicked(e, state);
            if (d.isPresent()) { lastEmitTick = tick; sink.accept(new TeleportEvent(d.get(), tick)); return; }
        }
    }
}
```
```java
// PohGameIds.java  — sentinel values; real ids captured in Task 12 live QA.
package com.smirnovlabs.pohteleports.detect;
public final class PohGameIds {
    private PohGameIds() {}
    // Interface group ids (WidgetLoaded.getGroupId())
    public static final int NEXUS_INTERFACE_GROUP = -1;
    public static final int JEWELLERY_BOX_INTERFACE_GROUP = -1;
    public static final int DIALOG_OPTION_GROUP = -1;      // shared NPC/dialog group used by glory rub
    // Varbits
    public static final int NEXUS_DEFAULT_DEST_VARBIT = -1;
    public static final int MOUNTED_XERICS_DEFAULT_VARBIT = -1;
    public static final int MOUNTED_DIGSITE_DEFAULT_VARBIT = -1;
    // Object ids (MenuOptionClicked target objects)
    public static final int MOUNTED_GLORY_OBJECT = -1;
    public static final int MOUNTED_XERICS_OBJECT = -1;
    public static final int MOUNTED_DIGSITE_OBJECT = -1;
    public static final int NEXUS_OBJECT = -1;
    // POH region ids (allowlist for isInPoh); populate in Task 12
    public static final int[] POH_REGIONS = { /* filled in live QA */ };
}
```

- [ ] **Step 4: Run to verify pass** — `./gradlew test --tests "*DetectionRouterTest"` → PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: detection router with debounce and denial guard"
```

---

### Task 7: NexusRecognizer

**Files:**
- Create: `detect/NexusRecognizer.java`
- Test: `detect/NexusRecognizerTest.java`

**Interfaces:**
- Consumes: `MenuOptionClicked`, `GameStateView`, `PohGameIds`, `Destination`.
- Produces: `NexusRecognizer(Map<Integer,Destination> varbitDefaultMap, Map<String,Destination> nameMap)` implementing `TeleportRecognizer`; static helper `resolveFromOptionText(String option, Map<String,Destination> nameMap)`.

Detection logic: a menu click whose option/target names a nexus destination (list-row pick, or a named left-click default) → that `Destination`. A generic default ("Teleport" with no name) while at the nexus object → the destination the `NEXUS_DEFAULT_DEST_VARBIT` currently encodes (via `varbitDefaultMap`). Opening the list ("Teleport Menu") or "Configure" → no event.

- [ ] **Step 1: Write the failing test**

```java
package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import net.runelite.api.events.MenuOptionClicked;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NexusRecognizerTest {
    private final Map<String,Destination> nameMap = Map.of(
        "varrock", Destination.NEXUS_VARROCK, "camelot", Destination.NEXUS_CAMELOT);
    private final Map<Integer,Destination> varbitDefault = Map.of(2, Destination.NEXUS_CAMELOT); // varbit value 2 = Camelot

    private MenuOptionClicked click(String option, String target) {
        MenuOptionClicked e = mock(MenuOptionClicked.class);
        when(e.getMenuOption()).thenReturn(option);
        when(e.getMenuTarget()).thenReturn(target);
        return e;
    }
    static class St implements GameStateView { int v; public int getVarbit(int id){return v;} public boolean isInPoh(){return true;} public int currentTick(){return 0;} }

    @Test public void listPickResolvesByName() {
        NexusRecognizer r = new NexusRecognizer(varbitDefault, nameMap);
        assertEquals(Optional.of(Destination.NEXUS_VARROCK), r.onMenuOptionClicked(click("Teleport", "Varrock"), new St()));
    }
    @Test public void genericDefaultResolvesByVarbit() {
        NexusRecognizer r = new NexusRecognizer(varbitDefault, nameMap);
        St st = new St(); st.v = 2; // encodes Camelot
        assertEquals(Optional.of(Destination.NEXUS_CAMELOT), r.onMenuOptionClicked(click("Teleport", ""), st));
    }
    @Test public void openingMenuIsNotCounted() {
        NexusRecognizer r = new NexusRecognizer(varbitDefault, nameMap);
        assertEquals(Optional.empty(), r.onMenuOptionClicked(click("Teleport Menu", "Portal Nexus"), new St()));
    }
    @Test public void configureIsNotCounted() {
        NexusRecognizer r = new NexusRecognizer(varbitDefault, nameMap);
        assertEquals(Optional.empty(), r.onMenuOptionClicked(click("Configure", "Portal Nexus"), new St()));
    }
    @Test public void unknownDefaultFallsBackToBucket() {
        NexusRecognizer r = new NexusRecognizer(Map.of(), nameMap);
        St st = new St(); st.v = 99;
        assertEquals(Optional.of(Destination.NEXUS_UNKNOWN), r.onMenuOptionClicked(click("Teleport", ""), st));
    }
}
```

- [ ] **Step 2: Run to verify it fails** — `./gradlew test --tests "*NexusRecognizerTest"` → FAIL.

- [ ] **Step 3: Implement `NexusRecognizer`**

```java
package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import net.runelite.api.events.MenuOptionClicked;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class NexusRecognizer implements TeleportRecognizer {
    private final Map<Integer,Destination> varbitDefaultMap; // varbit value -> Destination
    private final Map<String,Destination> nameMap;           // lowercased destination name -> Destination

    public NexusRecognizer(Map<Integer,Destination> varbitDefaultMap, Map<String,Destination> nameMap) {
        this.varbitDefaultMap = varbitDefaultMap; this.nameMap = nameMap;
    }
    @Override public Optional<Destination> onMenuOptionClicked(MenuOptionClicked e, GameStateView state) {
        String option = norm(e.getMenuOption());
        if (option.contains("menu") || option.contains("configure") || option.contains("set ") || option.contains("build")) {
            return Optional.empty(); // opening the list / config / build — never a teleport
        }
        if (!option.contains("teleport") && !nameMap.containsKey(norm(e.getMenuTarget()))) {
            return Optional.empty();
        }
        // 1) named destination in the target text (list pick or named default)
        Destination byName = nameMap.get(norm(e.getMenuTarget()));
        if (byName != null) return Optional.of(byName);
        // 2) generic default -> read the configured-default varbit
        Destination byVarbit = varbitDefaultMap.get(state.getVarbit(PohGameIds.NEXUS_DEFAULT_DEST_VARBIT));
        return Optional.of(byVarbit != null ? byVarbit : Destination.unknownFor(Transport.NEXUS));
    }
    private static String norm(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT).trim(); }
}
```

- [ ] **Step 4: Run to verify pass** — `./gradlew test --tests "*NexusRecognizerTest"` → PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: nexus recognizer with varbit-default resolution"
```

---

### Task 8: JewelleryBoxRecognizer

**Files:**
- Create: `detect/JewelleryBoxRecognizer.java`
- Test: `detect/JewelleryBoxRecognizerTest.java`

**Interfaces:**
- Produces: `JewelleryBoxRecognizer(Map<String,Destination> nameMap)` implementing `TeleportRecognizer`. Resolves a destination by the clicked entry's text (jewellery-box rows are named, e.g. "Ferox Enclave", "Edgeville"). Configure/menu-open → empty.

- [ ] **Step 1: Write the failing test**

```java
package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import net.runelite.api.events.MenuOptionClicked;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JewelleryBoxRecognizerTest {
    private final Map<String,Destination> nameMap = Map.of(
        "ferox enclave", Destination.JBOX_DUEL_FEROX, "edgeville", Destination.JBOX_GLORY_EDGEVILLE);
    private MenuOptionClicked click(String option, String target) {
        MenuOptionClicked e = mock(MenuOptionClicked.class);
        when(e.getMenuOption()).thenReturn(option); when(e.getMenuTarget()).thenReturn(target); return e;
    }
    static class St implements GameStateView { public int getVarbit(int id){return 0;} public boolean isInPoh(){return true;} public int currentTick(){return 0;} }

    @Test public void namedEntryResolves() {
        JewelleryBoxRecognizer r = new JewelleryBoxRecognizer(nameMap);
        assertEquals(Optional.of(Destination.JBOX_DUEL_FEROX), r.onMenuOptionClicked(click("Teleport", "Ferox Enclave"), new St()));
    }
    @Test public void unrelatedClickIgnored() {
        JewelleryBoxRecognizer r = new JewelleryBoxRecognizer(nameMap);
        assertEquals(Optional.empty(), r.onMenuOptionClicked(click("Walk here", ""), new St()));
    }
}
```

- [ ] **Step 2: Run to verify it fails** — FAIL.

- [ ] **Step 3: Implement `JewelleryBoxRecognizer`**

```java
package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import net.runelite.api.events.MenuOptionClicked;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class JewelleryBoxRecognizer implements TeleportRecognizer {
    private final Map<String,Destination> nameMap;
    public JewelleryBoxRecognizer(Map<String,Destination> nameMap) { this.nameMap = nameMap; }
    @Override public Optional<Destination> onMenuOptionClicked(MenuOptionClicked e, GameStateView state) {
        String option = norm(e.getMenuOption());
        if (option.contains("menu") || option.contains("configure")) return Optional.empty();
        Destination d = nameMap.get(norm(e.getMenuTarget()));
        return Optional.ofNullable(d);
    }
    private static String norm(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT).trim(); }
}
```

- [ ] **Step 4: Run to verify pass** — PASS.

- [ ] **Step 5: Commit** — `git commit -m "feat: jewellery box recognizer"`

---

### Task 9: MountedGloryRecognizer

**Files:**
- Create: `detect/MountedGloryRecognizer.java`
- Test: `detect/MountedGloryRecognizerTest.java`

**Interfaces:**
- Produces: `MountedGloryRecognizer(Map<String,Destination> nameMap)` implementing `TeleportRecognizer`, plus `boolean armFromObjectClick(MenuOptionClicked)` — because glory is a two-step interaction (Rub the object → pick a destination in a shared dialog group). The recognizer arms when the glory object is rubbed, and only then resolves the next dialog-option click; this scoping avoids matching unrelated NPC dialogs.

- [ ] **Step 1: Write the failing test**

```java
package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import net.runelite.api.events.MenuOptionClicked;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MountedGloryRecognizerTest {
    private final Map<String,Destination> nameMap = Map.of("edgeville", Destination.MGLORY_EDGEVILLE);
    private MenuOptionClicked click(String option, String target, int objId) {
        MenuOptionClicked e = mock(MenuOptionClicked.class);
        when(e.getMenuOption()).thenReturn(option); when(e.getMenuTarget()).thenReturn(target); when(e.getId()).thenReturn(objId); return e;
    }
    static class St implements GameStateView { public int getVarbit(int id){return 0;} public boolean isInPoh(){return true;} public int currentTick(){return 0;} }

    @Test public void dialogPickAfterRubResolves() {
        MountedGloryRecognizer r = new MountedGloryRecognizer(nameMap);
        r.armFromObjectClick(click("Rub", "Mounted glory", PohGameIds.MOUNTED_GLORY_OBJECT));
        assertEquals(Optional.of(Destination.MGLORY_EDGEVILLE), r.onMenuOptionClicked(click("Continue", "Edgeville", 0), new St()));
    }
    @Test public void dialogPickWithoutRubIgnored() {
        MountedGloryRecognizer r = new MountedGloryRecognizer(nameMap);
        assertEquals(Optional.empty(), r.onMenuOptionClicked(click("Continue", "Edgeville", 0), new St()));
    }
}
```

- [ ] **Step 2: Run to verify it fails** — FAIL.

- [ ] **Step 3: Implement `MountedGloryRecognizer`**

```java
package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import net.runelite.api.events.MenuOptionClicked;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MountedGloryRecognizer implements TeleportRecognizer {
    private final Map<String,Destination> nameMap;
    private boolean armed = false;
    public MountedGloryRecognizer(Map<String,Destination> nameMap) { this.nameMap = nameMap; }

    /** Call from the plugin when any menu click is a "Rub" on the mounted glory object. */
    public boolean armFromObjectClick(MenuOptionClicked e) {
        if (e.getId() == PohGameIds.MOUNTED_GLORY_OBJECT && norm(e.getMenuOption()).contains("rub")) { armed = true; return true; }
        return false;
    }
    @Override public Optional<Destination> onMenuOptionClicked(MenuOptionClicked e, GameStateView state) {
        if (!armed) return Optional.empty();
        Destination d = nameMap.get(norm(e.getMenuTarget()));
        if (d != null) { armed = false; return Optional.of(d); }
        return Optional.empty();
    }
    private static String norm(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT).trim(); }
}
```

Wiring note (Task 11): the plugin calls `armFromObjectClick(e)` on every `MenuOptionClicked` **before** handing the event to the router, so the "Rub" and the later dialog pick are both seen.

- [ ] **Step 4: Run to verify pass** — PASS.

- [ ] **Step 5: Commit** — `git commit -m "feat: mounted glory recognizer (rub-then-pick scoping)"`

---

### Task 10: MountedAmuletRecognizer (Xeric's + Digsite)

**Files:**
- Create: `detect/MountedAmuletRecognizer.java`
- Test: `detect/MountedAmuletRecognizerTest.java`

**Interfaces:**
- Produces: `MountedAmuletRecognizer(int objectId, int defaultVarbit, Transport transport, Map<String,Destination> nameMap, Map<Integer,Destination> varbitDefaultMap)` implementing `TeleportRecognizer`. Two instances are created (one Xeric's, one Digsite). Right-click named option → by name; generic left-click default → by `defaultVarbit`; else the transport's unknown bucket.

- [ ] **Step 1: Write the failing test**

```java
package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import net.runelite.api.events.MenuOptionClicked;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MountedAmuletRecognizerTest {
    private MenuOptionClicked click(String option, String target, int id) {
        MenuOptionClicked e = mock(MenuOptionClicked.class);
        when(e.getMenuOption()).thenReturn(option); when(e.getMenuTarget()).thenReturn(target); when(e.getId()).thenReturn(id); return e;
    }
    static class St implements GameStateView { int v; public int getVarbit(int id){return v;} public boolean isInPoh(){return true;} public int currentTick(){return 0;} }

    @Test public void namedRightClickResolves() {
        MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, 700, Transport.MOUNTED_DIGSITE,
            Map.of("digsite", Destination.MDIG_DIGSITE), Map.of());
        assertEquals(Optional.of(Destination.MDIG_DIGSITE), r.onMenuOptionClicked(click("Digsite", "Mounted digsite pendant", 500), new St()));
    }
    @Test public void genericDefaultUsesVarbit() {
        MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, 700, Transport.MOUNTED_DIGSITE,
            Map.of(), Map.of(3, Destination.MDIG_DIGSITE));
        St st = new St(); st.v = 3;
        assertEquals(Optional.of(Destination.MDIG_DIGSITE), r.onMenuOptionClicked(click("Teleport", "Mounted digsite pendant", 500), st));
    }
    @Test public void otherObjectIgnored() {
        MountedAmuletRecognizer r = new MountedAmuletRecognizer(500, 700, Transport.MOUNTED_DIGSITE, Map.of(), Map.of());
        assertEquals(Optional.empty(), r.onMenuOptionClicked(click("Digsite", "x", 999), new St()));
    }
}
```

- [ ] **Step 2: Run to verify it fails** — FAIL.

- [ ] **Step 3: Implement `MountedAmuletRecognizer`**

```java
package com.smirnovlabs.pohteleports.detect;

import com.smirnovlabs.pohteleports.model.Destination;
import com.smirnovlabs.pohteleports.model.Transport;
import net.runelite.api.events.MenuOptionClicked;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MountedAmuletRecognizer implements TeleportRecognizer {
    private final int objectId, defaultVarbit; private final Transport transport;
    private final Map<String,Destination> nameMap; private final Map<Integer,Destination> varbitDefaultMap;
    public MountedAmuletRecognizer(int objectId, int defaultVarbit, Transport transport,
                                   Map<String,Destination> nameMap, Map<Integer,Destination> varbitDefaultMap) {
        this.objectId = objectId; this.defaultVarbit = defaultVarbit; this.transport = transport;
        this.nameMap = nameMap; this.varbitDefaultMap = varbitDefaultMap;
    }
    @Override public Optional<Destination> onMenuOptionClicked(MenuOptionClicked e, GameStateView state) {
        if (e.getId() != objectId) return Optional.empty();
        String option = norm(e.getMenuOption());
        if (option.contains("configure") || option.contains("build") || option.contains("set ")) return Optional.empty();
        Destination byName = nameMap.get(norm(e.getMenuTarget()));
        if (byName != null) return Optional.of(byName);
        Destination byVarbit = varbitDefaultMap.get(state.getVarbit(defaultVarbit));
        return Optional.of(byVarbit != null ? byVarbit : Destination.unknownFor(transport));
    }
    private static String norm(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT).trim(); }
}
```

- [ ] **Step 4: Run to verify pass** — PASS.

- [ ] **Step 5: Commit** — `git commit -m "feat: mounted xeric's/digsite recognizer"`

---

### Task 11: Config, Swing panel, and plugin glue (assembly)

**Files:**
- Create: `PohTeleportConfig.java`, `ui/PohTeleportPanel.java`, `src/main/resources/poh_teleport_counter_icon.png`
- Modify: `PohTeleportCounterPlugin.java`
- Test: reuse existing unit tests; this task's deliverable is verified by a manual client load (Step 7).

**Interfaces:**
- Consumes: everything above.
- Produces: a running plugin with a nav-button side panel; `RealGameState implements GameStateView` (backs `getVarbit` with `Client.getVarbitValue`, `isInPoh` with a `PohGameIds.POH_REGIONS` check, `currentTick` with `Client.getTickCount`).

- [ ] **Step 1: Config**

```java
package com.smirnovlabs.pohteleports;

import com.smirnovlabs.pohteleports.model.SortMode;
import net.runelite.client.config.*;

@ConfigGroup("pohteleports")
public interface PohTeleportConfig extends Config {
    @ConfigItem(keyName = "sortMode", name = "Sort by", position = 1,
        description = "Order sections and rows by use count or by GP saved")
    default SortMode sortMode() { return SortMode.MOST_USED; }

    @ConfigItem(keyName = "countGuestPoh", name = "Count guest POHs", position = 2,
        description = "Also count teleports you make in another player's house")
    default boolean countGuestPoh() { return true; }
}
```

- [ ] **Step 2: Panel** — render `PanelModel` (collapsible sections, sub-groups, count + gp rows) with a sort toggle at the top. Swing rendering is mechanical; keep it a straightforward `JPanel` rebuilt on `rebuild(PanelModel)`.

```java
package com.smirnovlabs.pohteleports.ui;

import com.smirnovlabs.pohteleports.model.SortMode;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class PohTeleportPanel extends PluginPanel {
    private final JPanel body = new JPanel();
    private final Consumer<SortMode> onSort;
    public PohTeleportPanel(Consumer<SortMode> onSort) {
        this.onSort = onSort;
        setLayout(new BorderLayout());
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        tools.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton mostUsed = new JButton("Most used"); mostUsed.addActionListener(a -> onSort.accept(SortMode.MOST_USED));
        JButton mostSaved = new JButton("Most saved"); mostSaved.addActionListener(a -> onSort.accept(SortMode.MOST_SAVED));
        tools.add(mostUsed); tools.add(mostSaved);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(tools, BorderLayout.NORTH); add(body, BorderLayout.CENTER);
    }
    public void rebuild(PanelModel model) {
        SwingUtilities.invokeLater(() -> {
            body.removeAll();
            JLabel total = new JLabel(String.format("Total: %s teleports  ·  Saved: %s gp",
                QuantityFormatter.quantityToStackSize(model.getTotalCount()),
                QuantityFormatter.quantityToStackSize(model.getTotalGp())));
            total.setForeground(Color.WHITE); body.add(total);
            for (PanelModel.Section sec : model.getSections()) {
                JLabel h = new JLabel(String.format("%s  ×%d   %s gp",
                    sec.getTransport().getDisplayName(), sec.getCount(),
                    QuantityFormatter.quantityToStackSize(sec.getGp())));
                h.setForeground(new Color(0xE2CD92)); body.add(h);
                for (PanelModel.SubGroup g : sec.getSubGroups()) {
                    if (g.getName() != null) {
                        JLabel sg = new JLabel("  " + g.getName() + " · " + g.getCount());
                        sg.setForeground(new Color(0x9FB6D6)); body.add(sg);
                    }
                    for (PanelModel.Row row : g.getRows()) {
                        JLabel r = new JLabel(String.format("    %s   %d   %s",
                            row.getDestination().getDisplayName(), row.getCount(),
                            QuantityFormatter.quantityToStackSize(row.getGp())));
                        r.setForeground(Color.LIGHT_GRAY); body.add(r);
                    }
                }
            }
            body.revalidate(); body.repaint();
        });
    }
}
```

- [ ] **Step 3: Add an `icon.png`** (16×16 or 24×24 PNG) at `src/main/resources/poh_teleport_counter_icon.png`. Any simple original teleport/house glyph; must be original art (no ripped game sprites) for Hub compliance.

- [ ] **Step 4: Plugin glue** — subscribe events, build the recognizer list with the id/name maps, back `GameStateView`, load/persist the store, refresh the panel.

```java
package com.smirnovlabs.pohteleports;

import com.google.inject.Provides;
import com.smirnovlabs.pohteleports.cost.SavingsValuator;
import com.smirnovlabs.pohteleports.detect.*;
import com.smirnovlabs.pohteleports.model.*;
import com.smirnovlabs.pohteleports.store.TeleportSavingsStore;
import com.smirnovlabs.pohteleports.ui.PanelModel;
import com.smirnovlabs.pohteleports.ui.PohTeleportPanel;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@PluginDescriptor(name = "PoH Teleport Counter", description = "Counts free PoH teleports and the runes/charges they save", tags = {"poh","teleport","nexus"})
public class PohTeleportCounterPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ConfigManager configManager;
    @Inject private ItemManager itemManager;
    @Inject private PohTeleportConfig config;

    private final TeleportSavingsStore store = new TeleportSavingsStore();
    private SavingsValuator valuator;
    private DetectionRouter router;
    private MountedGloryRecognizer glory;
    private PohTeleportPanel panel;
    private NavigationButton navButton;

    @Provides PohTeleportConfig provideConfig(ConfigManager cm) { return cm.getConfig(PohTeleportConfig.class); }

    @Override protected void startUp() {
        store.load(configManager);
        valuator = new SavingsValuator(itemManager);
        GameStateView state = new GameStateView() {
            public int getVarbit(int id) { return id < 0 ? 0 : client.getVarbitValue(id); }
            public boolean isInPoh() {
                int[] regions = client.getMapRegions();
                for (int r : PohGameIds.POH_REGIONS) for (int cur : regions) if (cur == r) return true;
                return false;
            }
            public int currentTick() { return client.getTickCount(); }
        };
        glory = new MountedGloryRecognizer(byName(Transport.MOUNTED_GLORY));
        List<TeleportRecognizer> recognizers = List.of(
            new NexusRecognizer(varbitDefault(Transport.NEXUS), byName(Transport.NEXUS)),
            new JewelleryBoxRecognizer(byName(Transport.JEWELLERY_BOX)),
            glory,
            new MountedAmuletRecognizer(PohGameIds.MOUNTED_XERICS_OBJECT, PohGameIds.MOUNTED_XERICS_DEFAULT_VARBIT,
                Transport.MOUNTED_XERICS, byName(Transport.MOUNTED_XERICS), varbitDefault(Transport.MOUNTED_XERICS)),
            new MountedAmuletRecognizer(PohGameIds.MOUNTED_DIGSITE_OBJECT, PohGameIds.MOUNTED_DIGSITE_DEFAULT_VARBIT,
                Transport.MOUNTED_DIGSITE, byName(Transport.MOUNTED_DIGSITE), varbitDefault(Transport.MOUNTED_DIGSITE)));
        router = new DetectionRouter(recognizers, state, ev -> { store.record(ev); store.persist(configManager); refresh(); });

        panel = new PohTeleportPanel(mode -> {
            configManager.setConfiguration("pohteleports", "sortMode", mode.name()); refresh();
        });
        navButton = NavigationButton.builder().tooltip("PoH Teleport Counter")
            .icon(ImageUtil.loadImageResource(getClass(), "/poh_teleport_counter_icon.png")).priority(7).panel(panel).build();
        clientToolbar.addNavigation(navButton);
        refresh();
    }

    @Override protected void shutDown() {
        store.persist(configManager);
        if (navButton != null) clientToolbar.removeNavigation(navButton);
    }

    @Subscribe public void onMenuOptionClicked(MenuOptionClicked e) {
        glory.armFromObjectClick(e);              // let glory see the "Rub" first
        router.onMenuOptionClicked(e);
    }
    @Subscribe public void onChatMessage(ChatMessage e) { router.onChatMessage(e); }

    private void refresh() {
        panel.rebuild(PanelModel.build(store.snapshot(), valuator, config.sortMode()));
    }

    // Build a lowercased-name -> Destination map for one transport.
    private static Map<String,Destination> byName(Transport t) {
        Map<String,Destination> m = new HashMap<>();
        for (Destination d : Destination.values())
            if (d.getTransport() == t && !d.getId().endsWith(":unknown")) m.put(d.getDisplayName().toLowerCase(Locale.ROOT), d);
        return m;
    }
    // varbit value -> Destination. Populated in Task 12 once real varbit encodings are known; empty is safe (falls back to Unknown bucket).
    private static Map<Integer,Destination> varbitDefault(Transport t) { return Collections.emptyMap(); }
}
```

- [ ] **Step 5: Build** — `./gradlew build` → `BUILD SUCCESSFUL`.

- [ ] **Step 6: Run the client with the plugin** (RuneLite dev run). If the template provides a runner main class, use it: `./gradlew runClient` (or the template's documented run task). Confirm the nav button + empty panel appear, and no exceptions on load.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: config, side panel, and plugin wiring"
```

---

### Task 12: Live QA — capture real IDs and verify counting in-game (human-in-the-loop)

> This task cannot be automated and **must not** be (synthetic input violates Jagex rules). The author performs it live on their own account via Bolt-launched RuneLite. It resolves every `-1` in `PohGameIds`, the `varbitDefault(...)` maps, and confirms real-world counting.

**Files:**
- Modify: `detect/PohGameIds.java` (real ids), `PohTeleportCounterPlugin.varbitDefault(...)` (real varbit→Destination maps), `detect/DetectionRouter.DENIALS` (any missed denial strings), `model/Destination.java` (fix any name mismatches found)

- [ ] **Step 1: Enable Developer Tools** in RuneLite (config) → open the **Widget Inspector** and **Varbit Inspector**.
- [ ] **Step 2: Region ids** — stand in your POH; read `client.getMapRegions()` (log it) and add the values to `PohGameIds.POH_REGIONS`.
- [ ] **Step 3: Per transport**, with inspectors open, perform each teleport path and record: the interface **group id** (WidgetLoaded), the menu **option/target strings** (should match `Destination.getDisplayName()` — fix mismatches), the **object ids** for mounted glory/Xeric's/Digsite, and the **default varbit id** + the value→destination encoding when you change the configured default. Cross-check ids against `abextm/better-teleport-menu` and `nexus-map` sources.
- [ ] **Step 4: Fill** `PohGameIds` and the `varbitDefault(...)` maps with the captured values; rebuild.
- [ ] **Step 5: Verify counting** — do one teleport of each type (owner POH), confirm the panel increments the correct destination with a sensible gp; open a friend's POH and confirm guest counting; open a teleport list and **close it without picking** → confirm no increment; trigger a denial (e.g. insufficient level) → confirm no increment.
- [ ] **Step 6: Regression** — `./gradlew test` still green.
- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: real game ids and live-verified detection"
```

---

### Task 13 (phase 2, optional): Plugin Hub submission prep

- [ ] Write `README.md` (what it does, screenshot, config).
- [ ] Confirm Hub rules: passive/read-only ✓, open source ✓, no bundled binaries ✓, original icon ✓, no network calls ✓.
- [ ] Push to a public GitHub repo; open a PR to `runelite/plugin-hub` adding a manifest pointing at the repo + commit hash; respond to CI + reviewer.

---

## Self-Review

**Spec coverage:** Scope (5 transports + guest-POH) → Tasks 2/7–11; metric counts+gross-gp-live → Tasks 2–5; interaction-based detection + graded destination resolution + denial guard + no location-jump → Tasks 6–10; count-only persistence per profile → Task 4; nested sortable count+gp panel → Tasks 5/11; edge cases (menu-open/configure/unknown/denial/debounce) → Tasks 6–10 tests; testing strategy → every task's unit tests + Task 12 live QA; deferred break-even/farming-cape/item-teleports → not built, and the model/store carry counts so they add without schema change; Hub target → Task 13. No uncovered spec sections.

**Placeholder scan:** No "TBD/handle appropriately". The only deferred values are the `PohGameIds` `-1` sentinels and empty `varbitDefault` maps — these are *injected dependencies* with a dedicated resolving task (12), fully unit-tested via constructor-provided ids, and safe-by-fallback (empty map → Unknown bucket) until filled. The "populate all destinations" step (Task 2 Step 6) names the exact source, format, and a completeness test — concrete data entry, not hand-waving.

**Type consistency:** `TeleportRecognizer.onMenuOptionClicked(MenuOptionClicked, GameStateView) → Optional<Destination>` used identically in Tasks 6–11. `Destination` accessors (`getId/getDisplayName/getTransport/getSubGroup/getCostBasis`), `SavingsValuator.gpPerUse/gpTotal`, `TeleportSavingsStore.record/count/snapshot/totalCount/load/persist`, and `PanelModel.build/Section/SubGroup/Row` signatures match across all consumers. `Destination.unknownFor(Transport)` defined in Task 2, used in Tasks 7/10. `MountedGloryRecognizer.armFromObjectClick` defined in Task 9, called in Task 11 glue.

Verify RuneLite API surface names (`MenuOptionClicked.getMenuOption/getMenuTarget/getId`, `Client.getVarbitValue/getMapRegions/getTickCount`, `ItemManager.getItemPrice`, `QuantityFormatter`, `NavigationButton`, `PluginPanel`) against the local `runelite-api-1.12.29.1` jar the first time each appears — these are idiomatic but version-sensitive per the Global Constraints.
