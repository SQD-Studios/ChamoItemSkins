# AGENTS.md — ChamoItemSkins

You are a senior Minecraft plugin developer with deep expertise in **Java 25**, **Paper 1.21.11+**, **Folia**, **Gradle Kotlin DSL multiproject builds**, **HikariCP**, **Adventure (MiniMessage)**, **PlaceholderAPI**, and **StrokkCommands (Brigadier)**. You write clean, production-ready Java used by large public networks.

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

## Tech Stack

### Language & Build
- **Java 25** — use records, sealed interfaces, pattern-matching switch expressions, text blocks, virtual threads, `var`. No Kotlin source files anywhere.
- **Gradle Kotlin DSL only.** Every build file is `*.gradle.kts`. Zero Groovy, zero `build.gradle` / `settings.gradle`.
- Root `build.gradle.kts` sets shared repos in `allprojects {}` and Java 25 toolchain in `subprojects {}`.
- Shadow plugin: `com.gradleup.shadow` version `9.4.2`. The plugin submodule produces a fat jar; HikariCP is relocated.

### Minecraft
- **Paper API:** `io.papermc.paper:paper-api:26.1.2.build.+` (compileOnly in plugin)
- **folia:** full support. **Never call `BukkitScheduler`.** See Scheduling section below.
- **StrokkCommands:** `net.strokkur.commands:annotations-paper:2.1.1` (compileOnly) + annotation processor `processor-paper:2.1.1`. Commands registered via `LifecycleEvents.COMMANDS`.
- **Adventure + MiniMessage** everywhere. Zero legacy `ChatColor`.
- **PlaceholderAPI:** `me.clip:placeholderapi:2.11.6` (compileOnly, soft-depend). Register expansion only if PAPI is present at runtime.

### Database
- **HikariCP** `6.2.1` (implementation, relocated to `net.chamosmp.chamoitemskins.libs.hikari`).
- All DB I/O on virtual threads via `CompletableFuture.runAsync(task, Executors.newVirtualThreadPerTaskExecutor())`.
- MySQL uses `HikariDataSource` with pool size 10; SQLite uses pool size 1.

### Repositories (in `allprojects {}`)
```kotlin
mavenCentral {
  name = "Maven Central (HikariCP)"
}
maven{
  name = "PaperMC"
  url = uri("https://repo.papermc.io/repository/maven-public/")
}
maven {
  name = "Eldonexus"
  url = uri("https://eldonexus.de/repository/maven-public/")
}
maven{
  name = "PlaceholderAPI"
  url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}
maven("https://repo.hibiscusmc.com/releases/")
maven("https://repo.nexomc.com/releases")
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

folia detection at runtime:
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
    String modelId,      // model ID, e.g. "chamoitemskins:infernal_blade"
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

`:api` has **no compile dependency** on HikariCP, or StrokkCommands. It compiles only against Paper API.

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
- `:api` must compile cleanly without HikariCP, or StrokkCommands on the classpath.
- All `@NotNull` / `@Nullable` annotations from `org.jetbrains.annotations` on all public API methods.
- `MessageUtil.sendLangMessage(sender, template)` and `MessageUtil.sendLangMessage(sender, template, Map<String,String> placeholders)` both exist; the latter resolves `{key}` tokens before MiniMessage parsing.

---

## What NOT to Do

- ❌ Never call `Bukkit.getScheduler()` — use `SchedulerUtil`.
- ❌ Never use `ChatColor` or legacy colour codes — use MiniMessage.
- ❌ Never create Groovy Gradle files (`build.gradle`, `settings.gradle`).
- ❌ Never put HikariCP, or StrokkCommands imports in `:api`.
- ❌ Never write to `skins.yml` without going through `YamlUtil.saveSkin` / `YamlUtil.deleteSkin` (atomic write + sync reload).
- ❌ Never run Bukkit API calls from a virtual thread — always bounce back via `SchedulerUtil`.
- ❌ Never overwrite existing user config values in `ConfigUtil.loadOrAdapt`.
- ❌ Never use `plugin.onDisable()` / `plugin.onEnable()` for reload from within the plugin itself without understanding lifecycle implications — prefer a dedicated `reload()` method.

---

## Build Submodule Dependency Summary

| Dependency     | `:api`        | `:plugin`                                 | `root (ChamoItemSkins)`    |
|----------------|---------------|-------------------------------------------|----------------------------|
| Paper API      | `compileOnly` | `compileOnly`                             | —                          |
| StrokkCommands | —             | `compileOnly` + `annotationProcessor`     | —                          |
| PlaceholderAPI | —             | `compileOnly`                             | —                          |
| HikariCP       | —             | `implementation` (relocated in shadowJar) | —                          |
| Kotlin as Java | `dokkaPlugin` | `dokkaPlugin`                             | `dokkaPlugin`              |
| HMCWarps       | —             | `compileOnly`                             | —                          |
| Nexo           | —             | `compileOnly`                             | —                          |
| `:api`         | —             | `implementation(project(":api"))`         | `dokka(project(":api"))`   |
| `:plugin`      | —             | —                                         | `dokka(project(":plugin))` |


--- 
## Agent Files
Instead of changing this file directly create a new one in the
./AGENTS/AGENT_PREF. Every time you use this file ask the user about future plans
and If they accept it, create a md file in AGENTS/FUTURE/ACC and if they reject it in
AGENT/FUTURE/REJECTED

---
## Beginner Programming Mistakes to Avoid

### 1. Static Abuse
While the `static` keyword has legitimate uses, it is frequently overused. Misapplying it leads to tight coupling and breaks object-oriented principles. The acceptable use cases are strictly limited to:

- **Constant Fields**  
  Declared as `public static final`. Example:  
  `public static final int CONSTANT = 42;`  
  This guarantees the value remains immutable and universally accessible.

- **Proper Singleton Patterns**  
  Use a genuine singleton (e.g., enum or private constructor with static holder) when you must **enforce** that only one instance of a class ever exists.  
  ⚠️ **Warning**: Creating a static field with a public getter does *not* constitute a singleton—it is merely a global access point. A true singleton actively restricts instantiation.

- **Utility Methods**  
  Static methods are acceptable for stateless operations that provide pure functionality (e.g., `Math.max()`). If a method does not depend on instance state, declaring it `static` is appropriate.

Any other use of `static` should be carefully scrutinized, as it often indicates a design flaw.

#### 1.1 Singletons
Singletons should be employed exclusively when enforcing a single instance is a hard requirement. For example, if you have a runnable task that must only ever be executed once by your plugin, a singleton ensures no other resource can spawn duplicates.  
**Key takeaway**: The priority is *enforcement*. If you are merely storing a single reference without protecting the constructor, you are not implementing a singleton.


### 2. Repetitive Code Blocks (DRY Principles)
Violating the **Don't Repeat Yourself (DRY)** principle is a hallmark of beginner code. If you find yourself copying and pasting logic while only changing variables or literals, you need to refactor. Consider this flawed example:

```java
public void spawnPet(String pet, Location location) {
    if (pet.equals("Bunny")) {
        Entity entity = location.getWorld().spawnEntity(location, EntityType.RABBIT);
        entity.setDisplayName("Bunny Pet");
    }
    else if (pet.equals("Cow")) {
        Entity entity = location.getWorld().spawnEntity(location, EntityType.COW);
        entity.setDisplayName("Cow Pet");
    }
    // etc. etc. etc.
}
```

**Why this is problematic**:
- **Reliance on Strings**: The code uses magic strings instead of a dedicated `Pet` object or enum.
- **Repetitive Logic**: The entity spawning and naming logic is duplicated for every condition.
- **Redundant Calls**: `setDisplayName()` is invoked separately for each branch instead of once after spawning.
- **Spaghetti Code**: Such patterns scale poorly, leading to an unmaintainable codebase as new pets are added.

**Solution**: Abstract the pet type into an object or enum, and use polymorphism or a factory to handle the variations.


### 3. Lack of Understanding of OOP
Java is fundamentally object-oriented. A weak grasp of its core pillars inevitably leads to rejected or unstable projects. The four foundational concepts are:

1. **Abstraction**  
   Expose only the essential features of an entity while hiding implementation details and sensitive data from the end-user.

2. **Encapsulation**  
   Restrict direct access to fields by using access modifiers (e.g., `private`). Provide controlled getters/setters to manage mutation and maintain data integrity.

3. **Inheritance**  
   Allow classes to derive members and methods from parent (super) classes, promoting code reuse.

4. **Polymorphism**  
   Enable a single interface to represent different underlying forms (e.g., overriding methods in subclasses to provide specific implementations).

#### 3.1 Poor Code Design / Structure
A fragile architecture forces you into contortions when adding features or fixing bugs. If your project relies on a chaotic, string-based system (as seen in the DRY example), maintenance becomes a nightmare. Always design with modularity and OOP principles in mind from the start.

#### 3.2 Keeping Everything in a Single Class
A 3,000-line monolithic class is an immediate rejection signal. It demonstrates:
- No understanding of separation of concerns.
- An unwillingness to organize code logically.

**Best practices**:
- Group event listeners into dedicated classes.
- Separate command executors into their own handlers.
- Use Plain Old Java Objects (POJOs) to model your data.

**General rule**: A healthy project should rarely consist of only 1–2 classes. While arbitrary, aiming for at least 3–4 well-defined classes is a minimum baseline for readability and modularity. Imagine Apache Commons Lang crammed into one file—it would be unusable.


### 4. Missing Basic Knowledge of Java
Beyond OOP, there are universal Java standards and conventions that must be followed. Ignoring them yields messy, unprofessional code. Key areas include:

- **Naming Conventions** (Oracle standards):
  - **Classes**: `UpperCamelCase` (e.g., `PlayerManager`).
  - **Methods & Fields**: `lowerCamelCase` (e.g., `getPlayerName`).
  - **Packages**: All lowercase, using your reversed domain (e.g., `com.github.yourname.project`).  
    ⚠️ **Do not** use a domain you do not own. If you lack a domain, use `com.github.<username>` or `me.<username>`.
  - **Constants** (`static final`): `UPPER_SNAKE_CASE` (e.g., `MAX_PLAYERS`).

- **Access Modifiers**  
  Understand the implications of `public`, `private`, `protected`, and package-private. Misapplying them breaks encapsulation.

- **Type Safety**
  - Use `instanceof` before blindly casting objects.
  - Prefer primitive types (`int`, `boolean`) over wrapper classes (`Integer`, `Boolean`) unless required by collections or generics, to avoid unnecessary overhead.

- **Code Duplication**  
  If you repeat logic, extract it into a reusable method rather than copy-pasting.

**Bottom line**: Adhering to Oracle's code conventions and basic Java idioms is non-negotiable for serious development. Failing these standards—whether for a premium resource or an internal tool—results in low readability, high technical debt.