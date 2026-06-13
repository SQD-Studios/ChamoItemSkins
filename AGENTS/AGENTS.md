# AGENTS.md — ChamoItemSkins

You are a senior Minecraft plugin developer with deep expertise in **Java 25**, **Paper 1.21+**, **Folia**, **Gradle Kotlin DSL multiproject builds**, **BetterModel**, **HikariCP**, **Adventure (MiniMessage)**, **PlaceholderAPI**, and **StrokkCommands (Brigadier)**. You write clean, production-ready Java used by large public networks.

---

## Project Identity

| Field         | Value                                            |
|---------------|--------------------------------------------------|
| **Name**      | ChamoItemSkins                                   |
| **Package**   | `net.chamosmp.chamoitemskins`                    |
| **Group**     | `net.chamosmp.chamoitemskins`                    |
| **Version**   | `1.0.0`                                          |
| **MC Target** | Paper 1.21.11 / Folia                            |
| **Java**      | 25 (`JavaLanguageVersion.of(25)`)                |
| **Build**     | Gradle Kotlin DSL — every file is `*.gradle.kts` |

---

## Repository Layout

```
ChamoItemSkins/
├── settings.gradle.kts
├── build.gradle.kts                         (root — shared repos, Java toolchain)
├── api/
│   ├── build.gradle.kts
│   └── src/main/java/net/chamosmp/chamoitemskins/api/
│       ├── ChamoItemSkinsApi.java           (interface + static get())
│       ├── model/
│       │   ├── Skin.java                    (record)
│       │   └── SkinGrant.java               (record)
│       ├── event/
│       │   ├── SkinEquipEvent.java          (cancellable)
│       │   ├── SkinUnequipEvent.java        (cancellable)
│       │   ├── SkinGrantEvent.java          (cancellable)
│       │   └── SkinRevokeEvent.java         (cancellable)
│       └── service/
│           ├── SkinService.java
│           └── GrantService.java
└── plugin/
    ├── build.gradle.kts
    └── src/main/
        ├── java/net/chamosmp/chamoitemskins/
        │   ├── ChamoItemSkinsPlugin.java    (JavaPlugin main, implements ChamoItemSkinsApi)
        │   ├── scheduler/
        │   │   └── SchedulerUtil.java       (Folia/Paper abstraction)
        │   ├── command/
        │   │   ├── SkinsCommand.java        (@Command("skins"))
        │   │   └── AdminCommand.java        (@Command("skinsadmin"))
        │   ├── database/
        │   │   ├── DatabaseManager.java     (sealed interface)
        │   │   ├── MySQLDatabase.java
        │   │   └── SQLiteDatabase.java
        │   ├── gui/
        │   │   ├── MainSkinsGui.java
        │   │   ├── SkinSelectionGui.java
        │   │   ├── AdminGui.java
        │   │   ├── SkinEditorGui.java
        │   │   └── config/
        │   │       ├── SlotType.java        (sealed interface)
        │   │       └── GuiSlotDef.java      (record)
        │   ├── listener/
        │   │   ├── NoteListener.java
        │   │   └── GuiListener.java
        │   ├── manager/
        │   │   ├── SkinManager.java         (implements SkinService)
        │   │   ├── GrantManager.java        (implements GrantService)
        │   │   └── CacheManager.java
        │   ├── bettermodel/
        │   │   └── BetterModelService.java
        │   ├── placeholder/
        │   │   └── ChamoItemSkinsExpansion.java
        │   └── util/
        │       ├── MessageUtil.java
        │       ├── NoteUtil.java
        │       ├── ConfigUtil.java
        │       └── YamlUtil.java            (live skins.yml writer, atomic write)
        └── resources/
            ├── plugin.yml
            ├── config.yml
            ├── skins.yml
            ├── gui.yml
            ├── admin-gui.yml
            └── skin-editor-gui.yml
```

---

## Tech Stack

### Language & Build
- **Java 25** — use records, sealed interfaces, pattern-matching switch expressions, text blocks, virtual threads, `var`. No Kotlin source files anywhere.
- **Gradle Kotlin DSL only.** Every build file is `*.gradle.kts`. Zero Groovy, zero `build.gradle` / `settings.gradle`.
- Root `build.gradle.kts` sets shared repos in `allprojects {}` and Java 25 toolchain in `subprojects {}`.
- Shadow plugin: `com.gradleup.shadow` version `9.4.2`. The plugin submodule produces a fat jar; HikariCP is relocated.

### Minecraft
- **Paper API:** `io.papermc.paper:paper-api:26.1.2.build.+` (compileOnly in plugin)
- **Folia:** full support. **Never call `BukkitScheduler`.** See Scheduling section below.
- **BetterModel:** `io.github.toxicity188:bettermodel-api:3.1.0` — all calls isolated in `BetterModelService`.
- **StrokkCommands:** `net.strokkur.commands:annotations-paper:2.1.1` (compileOnly) + annotation processor `processor-paper:2.1.1`. Commands registered via `LifecycleEvents.COMMANDS`.
- **Adventure + MiniMessage** everywhere. Zero legacy `ChatColor`.
- **PlaceholderAPI:** `me.clip:placeholderapi:2.11.6` (compileOnly, soft-depend). Register expansion only if PAPI is present at runtime.

### Database
- **HikariCP** `6.2.1` (implementation, relocated to `net.chamosmp.chamoitemskins.libs.hikari`).
- All DB I/O on virtual threads via `CompletableFuture.runAsync(task, Executors.newVirtualThreadPerTaskExecutor())`.
- MySQL uses `HikariDataSource` with pool size 10; SQLite uses pool size 1.

### Repositories (in `allprojects {}`)
```kotlin
mavenCentral()
maven("https://repo.papermc.io/repository/maven-public/")
maven { name = "eldonexus"; url = uri("https://eldonexus.de/repository/maven-public/") }
maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
```

---

## Scheduling Rules — Enforce Everywhere

**Never use `BukkitScheduler` (`Bukkit.getScheduler()`).** Use `SchedulerUtil` for every scheduled or thread-dispatched operation:

| Method                                         | Folia path                                                           | Paper path                                                  |
|------------------------------------------------|----------------------------------------------------------------------|-------------------------------------------------------------|
| `runAsync(plugin, task)`                       | `Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run())`         | `Bukkit.getScheduler().runTaskAsynchronously(plugin, task)` |
| `runSync(plugin, task)`                        | `Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run())`     | `Bukkit.getScheduler().runTask(plugin, task)`               |
| `runForEntity(plugin, entity, task, fallback)` | `entity.getScheduler().run(plugin, t -> task.run(), fallback)`       | `Bukkit.getScheduler().runTask(plugin, task)`               |
| `runAtLocation(plugin, location, task)`        | `Bukkit.getRegionScheduler().run(plugin, location, t -> task.run())` | `Bukkit.getScheduler().runTask(plugin, task)`               |
| `runDelayed(plugin, task, delayTicks)`         | `Bukkit.getGlobalRegionScheduler().runDelayed(...)`                  | `Bukkit.getScheduler().runTaskLater(...)`                   |

Folia detection at runtime:
```java
boolean isFolia = Bukkit.getServer().getClass().getSimpleName().contains("Folia");
```

All DB `CompletableFuture` `.thenAccept()` callbacks that touch Bukkit API must call `SchedulerUtil.runSync` or `runForEntity` to return to the correct thread. GUI open/close always uses `runForEntity`.

---

## Java 25 Guidelines

- **Records** for all immutable data: `Skin`, `SkinGrant`, `GuiSlotDef`, `CacheEntry`.
- **Sealed interfaces** for discriminated types: `SlotType` (permitted: `Decorative`, `SkinSlot`, `FilterSlot`, `BackSlot`, `ActionSlot`), `DatabaseManager` (permitted: `MySQLDatabase`, `SQLiteDatabase`).
- **Pattern-matching switch** over every `SlotType` / `DatabaseManager` branch — no `instanceof` chains.
- `SequencedCollection`, `Stream.toList()`, modern collectors.
- **Virtual threads** for all blocking DB I/O.
- **Text blocks** for multi-line SQL.
- **Switch expressions** over switch statements.

---

## Core Data Models (`:api`)

### `Skin` (record)
```java
public record Skin(
    String id,           // unique identifier (UUID string or human-readable slug)
    String name,         // MiniMessage display name
    String modelId,      // BetterModel model ID, e.g. "chamoitemskins:infernal_blade"
    Material itemType,   // Bukkit Material this skin applies to
    boolean enabled,
    Material noteMaterial,        // nullable — overrides config default
    DisplayItem displayItem       // GUI item config
) {
    public record DisplayItem(Material material, String name, List<String> lore, boolean glow) {}
}
```

### `SkinGrant` (record)
```java
public record SkinGrant(UUID grantId, UUID playerUuid, String skinId, Instant grantedAt, String source) {}
```

---

## Database Schema

```sql
CREATE TABLE IF NOT EXISTS player_skin_grants (
    grant_id    VARCHAR(36) PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    skin_id     VARCHAR(64) NOT NULL,
    granted_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source      VARCHAR(32) NOT NULL
);
-- MySQL adds: INDEX (player_uuid)
-- SQLite adds: CREATE INDEX IF NOT EXISTS idx_player_uuid ON player_skin_grants(player_uuid)

CREATE TABLE IF NOT EXISTS player_active_skins (
    player_uuid VARCHAR(36) NOT NULL,
    item_type   VARCHAR(64) NOT NULL,
    skin_id     VARCHAR(64) NOT NULL,
    PRIMARY KEY (player_uuid, item_type)
);
```

MySQL upsert uses `ON DUPLICATE KEY UPDATE skin_id = ?`. SQLite uses `INSERT OR REPLACE`.

---

## Config Loading Rules (`ConfigUtil`)

On `onEnable`, call `ConfigUtil.loadOrAdapt(plugin, filename)` for every config file **before** using its values.

`loadOrAdapt` logic:
1. If the file exists in `plugins/ChamoItemSkins/`, load it and **deep-merge** missing keys from the embedded jar resource without overwriting existing user values.
2. If the file does not exist, copy the embedded default from the jar's `resources/` and save it.

**Never silently overwrite user values.**

---

## `YamlUtil` — Live Skin Editor Writes

- `YamlUtil.saveSkin(plugin, skin)` — async write on virtual thread, atomic (write to `skins.yml.tmp`, delete `skins.yml`, rename).
- `YamlUtil.deleteSkin(plugin, id)` — same pattern, sets `skins.<id>` to `null`.
- Both are synchronized on `YamlUtil.class` to prevent concurrent writes.
- After writing, call `SkinManager.reload()` via `SchedulerUtil.runSync`.

---

## GUI System

### `SlotType` (sealed interface)
```java
public sealed interface SlotType {
    record Decorative()                 implements SlotType {}
    record SkinSlot(int index)          implements SlotType {}
    record FilterSlot()                 implements SlotType {}
    record BackSlot()                   implements SlotType {}
    record ActionSlot(String action)    implements SlotType {}
}
```

### `GuiSlotDef` (record)
```java
public record GuiSlotDef(SlotType type, int slot, Material material, String name, List<String> lore, boolean glow) {}
```

### `GuiListener`
All custom GUIs implement `GuiListener.ChamoGui extends InventoryHolder`. The listener intercepts `InventoryClickEvent`, cancels it, and calls `gui.handleClick(event)`. GUI open/close always dispatched via `SchedulerUtil.runForEntity`.

### Main GUIs

| Class              | Opens via                           | Permission             |
|--------------------|-------------------------------------|------------------------|
| `MainSkinsGui`     | `/skins`                            | `chamoitemskins.use`   |
| `SkinSelectionGui` | Click on a category in MainSkinsGui | `chamoitemskins.use`   |
| `AdminGui`         | `/skinsadmin gui`                   | `chamoitemskins.admin` |
| `SkinEditorGui`    | EDITOR action in AdminGui           | `chamoitemskins.admin` |

**`SkinSelectionGui` behaviour:**
- `SkinSlot` — if player owns skin: show `displayItem`, click toggles equip/unequip via `GrantManager`. If unowned: show `BARRIER`, click sends `skin-not-owned`. If no skin at index: show filler (AIR).
- `FilterSlot` — toggles Owned/All filter in-place.
- `BackSlot` — returns to `MainSkinsGui` (runs `/skins`).
- Equip fires `SkinEquipEvent` (cancellable), unequip fires `SkinUnequipEvent` (cancellable).

**`AdminGui` action strings** (from `admin-gui.yml`):
- `RELOAD` — reloads plugin sync, closes inventory.
- `EDITOR` — opens `SkinEditorGui`.
- `GRANT`, `REVOKE`, `GIVE` — require player/skin input (anvil/sign input flow or command fallback).
- `BROWSE_GRANTS` — paginated list of a player's grants.

**`SkinEditorGui` screens:**
1. **List screen** — paginated, one icon per skin. Buttons: New Skin, Prev, Next, Back.
2. **Detail screen** — toggle enabled, edit name/model-id/item-type/note material/display, delete.
3. **New Skin flow** — step-by-step anvil input: name → model-id → item-type picker → review → confirm & save.

---

## Note Item

- Material: from `config.yml note.default-material`, overridable per skin via `note-material`.
- Skin ID stored in `PersistentDataContainer` under key `chamoitemskins:skin_id`.
- Name/lore from `config.yml note.*`, MiniMessage with `{skin_name}` resolved at give-time.
- Consumed on `PlayerInteractEvent` RIGHT_CLICK_AIR or RIGHT_CLICK_BLOCK.
- Flow: check already-owned → fire `SkinGrantEvent` (cancellable) → if not cancelled: consume item, `GrantManager.grantSkin(uuid, skinId, "NOTE")`, send `grant-received`.

---

## Commands

Commands use **StrokkCommands** annotations. Registered in `ChamoItemSkinsPlugin.onEnable` via `LifecycleEvents.COMMANDS`.

### `/skins`
Permission: `chamoitemskins.use` (default: true). Opens `MainSkinsGui`.

### `/skinsadmin`
Permission: `chamoitemskins.admin` (default: op).

| Subcommand                                | Description                                         |
|-------------------------------------------|-----------------------------------------------------|
| `gui`                                     | Opens AdminGui                                      |
| `reload`                                  | Reloads all configs and skins                       |
| `give <player> <skinId>`                  | Gives physical Note item to player                  |
| `access <grant/revoke> <player> <skinId>` | Grants or revokes the acess of a skin from a player |

---

## PlaceholderAPI Expansion

Identifier: `chamoitemskins`. `persist()` returns `true`. Registered only if PAPI is present.

| Placeholder                          | Returns                                     |
|--------------------------------------|---------------------------------------------|
| `%chamoitemskins_active_<MATERIAL>%` | Active skin name for that material, or `""` |
| `%chamoitemskins_owns_<skinId>%`     | `"true"` / `"false"`                        |
| `%chamoitemskins_total_owned%`       | Total grants count for player               |
| `%chamoitemskins_total_skins%`       | Total enabled skin count                    |

Return `null` for unrecognised placeholders.

---

## Developer API (`:api` submodule)

`:api` has **no compile dependency** on BetterModel, HikariCP, or StrokkCommands. It compiles only against Paper API.

```java
// Access the API
ChamoItemSkinsApi api = Bukkit.getServicesManager().load(ChamoItemSkinsApi.class);
SkinService skinService = api.getSkinService();
GrantService grantService = api.getGrantService();
```

`ChamoItemSkinsPlugin` implements `ChamoItemSkinsApi` and registers itself via `ServicesManager` at `ServicePriority.Normal`.

All public types in `:api` must have Javadoc.

---

## Coding Conventions

- **File headers:** every file begins with a path comment:
    - Java: `// --- path/to/FileName.java ---`
    - YAML: `# --- path/to/filename.yml ---`
    - Gradle: `// --- path/to/file.gradle.kts ---`
- Inline `//` comments for non-obvious logic. No prose outside Javadoc.
- `MessageUtil` must run MiniMessage deserialization AND `PlaceholderAPI.setPlaceholders()` (if PAPI present) before sending to player.
- `BetterModelService` is the **only** place that may call BetterModel API.
- `:api` must compile cleanly without BetterModel, HikariCP, or StrokkCommands on the classpath.
- All `@NotNull` / `@Nullable` annotations from `org.jetbrains.annotations` on all public API methods.
- `MessageUtil.sendMessage(sender, template)` and `MessageUtil.sendMessage(sender, template, Map<String,String> placeholders)` both exist; the latter resolves `{key}` tokens before MiniMessage parsing.

---

## What NOT to Do

- ❌ Never call `Bukkit.getScheduler()` — use `SchedulerUtil`.
- ❌ Never use `ChatColor` or legacy colour codes — use MiniMessage.
- ❌ Never create Groovy Gradle files (`build.gradle`, `settings.gradle`).
- ❌ Never put BetterModel, HikariCP, or StrokkCommands imports in `:api`.
- ❌ Never write to `skins.yml` without going through `YamlUtil.saveSkin` / `YamlUtil.deleteSkin` (atomic write + sync reload).
- ❌ Never run Bukkit API calls from a virtual thread — always bounce back via `SchedulerUtil`.
- ❌ Never overwrite existing user config values in `ConfigUtil.loadOrAdapt`.
- ❌ Never use `plugin.onDisable()` / `plugin.onEnable()` for reload from within the plugin itself without understanding lifecycle implications — prefer a dedicated `reload()` method.

---

## Build Submodule Dependency Summary

| Dependency     | `:api`        | `:plugin`                                 |
|----------------|---------------|-------------------------------------------|
| Paper API      | `compileOnly` | `compileOnly`                             |
| BetterModel    | —             | `implementation`                          |
| StrokkCommands | —             | `compileOnly` + annotationProcessor       |
| PlaceholderAPI | —             | `compileOnly`                             |
| HikariCP       | —             | `implementation` (relocated in shadowJar) |
| `:api`         | —             | `implementation(project(":api"))`         |

HikariCP relocated: `com.zaxxer.hikari` → `net.chamosmp.chamoitemskins.libs.hikari`

--- 
## Refactoring the AGENTS.md
Instead of changing this file directly create a new one in the
./AGENTS/AGENT_PREF

---
## Getting Stuck
If you believe you became stuck, ask me about it AND NOT THINK IT YOURSELF, YOU'RE GOING TO BE WRONG