You are a senior Minecraft plugin developer with deep expertise in Java 21+,
Paper 1.21.11, Folia, Gradle Kotlin DSL multiproject builds, BetterModel,
HikariCP, Adventure (MiniMessage), PlaceholderAPI, and StrokkCommands
(Brigadier). You write clean, production-ready Java used by large public
networks like Lifesteal Network (lifesteal.net).

---

PROJECT NAME: ChamoItemSkins
PACKAGE ROOT:  net.chamosmp.chamoitemskins

---

CONTEXT:
Build a fully featured ItemSkin plugin called "ChamoItemSkins" natively
targeting Paper 1.21.11, with full Folia support. Players unlock cosmetic
skins for their items by consuming physical "Note" items. Skins are rendered
via BetterModel. Grants are stored in MySQL or SQLite. The plugin ships as a
Gradle Kotlin DSL multiproject with a clean :api submodule and supports
multi-server deployments. All administration (grant, revoke, give notes,
reload) is accessible via a fully configurable in-game Admin GUI as well as
commands. A live Skin Editor GUI lets admins create and modify skins and write
changes directly back to skins.yml without restarting.

On startup the plugin scans its working directory
(plugins/ChamoItemSkins/) for pre-generated config files (config.yml,
skins.yml, gui.yml, admin-gui.yml, skin-editor-gui.yml). If found, it parses
and adapts to them; if not, it writes clean defaults. All config keys are
optional with sane defaults so partially filled files are tolerated.

---

TECH STACK:
- Language:    Java 21+ — use records, sealed interfaces, pattern matching
  switch expressions, text blocks, virtual threads, var.
- Build:       Gradle Kotlin DSL ONLY. Every build file is *.gradle.kts.
  Zero Groovy. Zero build.gradle or settings.gradle files.
- MC API:      Paper 1.21.11 (io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT
  from https://repo.papermc.io/repository/maven-public/)
- Folia:       Full support. No BukkitScheduler usage anywhere. Use:
  • Bukkit.getAsyncScheduler()      — DB / virtual-thread I/O
  • Bukkit.getGlobalRegionScheduler()— plugin lifecycle tasks
  • entity.getScheduler()           — per-entity tasks
  • Bukkit.getRegionScheduler()     — location-bound tasks
  Detect Folia at runtime:
  boolean isFolia = Bukkit.getServer().getClass()
  .getSimpleName().contains("Folia");
  Provide a SchedulerUtil that wraps both paths (Folia vs Paper)
  transparently for the rest of the codebase.
- Model API:   BetterModel (latest stable). Isolated in BetterModelService.
- Commands:    StrokkCommands (Brigadier).
- Messaging:   Adventure API + MiniMessage everywhere. Zero legacy ChatColor.
- Database:    HikariCP; MySQL or SQLite (config.yml). Async via virtual
  threads + CompletableFuture.
- PAPI:        Internal PlaceholderExpansion (persist()=true). Soft-depend.
  Registered in onEnable only if PAPI is present.
- No other external libraries.

---

CRITICAL BUILD RULES:
- settings.gradle.kts:
  rootProject.name = "ChamoItemSkins"
  include(":api", ":plugin")
- Root build.gradle.kts: shared repos (papermc, hibiscusmc for BetterModel,
  strokkcommands, placeholderapi, maven central) in allprojects {}; shared
  Java 21 toolchain in subprojects {}.
- api/build.gradle.kts: compileOnly Paper API only. No BetterModel,
  HikariCP, StrokkCommands — keeps :api independently shadeable.
- plugin/build.gradle.kts: compileOnly Paper, BetterModel, StrokkCommands,
  PlaceholderAPI; implementation(project(":api")); implementation HikariCP
  (relocated via shadowJar to com.example.chamoitemskins.libs.hikari).
- Both submodules: java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
- plugin shadowJar: fat jar, relocate HikariCP. You can use plugin.yml's libraries (libraries:(next line) - "package") function to make Spigot download dependencies from mavenCentral

---

JAVA 21 GUIDELINES:
- Records for all immutable data (Skin, SkinGrant, GuiSlotDef, AdminAction).
- Sealed interfaces for discriminated types (SlotType permitted: Decorative,
  SkinSlot, FilterSlot, BackSlot, ActionSlot).
- Pattern matching switch over every SlotType branch.
- SequencedCollection, Stream.toList(), modern collectors.
- Virtual threads for all blocking DB I/O.
- Text blocks for multi-line SQL.
- switch expressions over switch statements.

---

FOLIA SCHEDULER RULES (enforce everywhere):
- NEVER call BukkitScheduler (Bukkit.getScheduler()).
- Use SchedulerUtil for every scheduled or thread-dispatched operation:
  SchedulerUtil.runAsync(plugin, task)
  → Folia: Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run())
  → Paper: Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
  SchedulerUtil.runSync(plugin, task)
  → Folia: Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run())
  → Paper: Bukkit.getScheduler().runTask(plugin, task)
  SchedulerUtil.runForEntity(plugin, entity, task, fallback)
  → Folia: entity.getScheduler().run(plugin, t -> task.run(), fallback)
  → Paper: Bukkit.getScheduler().runTask(plugin, task)
  SchedulerUtil.runAtLocation(plugin, location, task)
  → Folia: Bukkit.getRegionScheduler().run(plugin, location, t -> task.run())
  → Paper: Bukkit.getScheduler().runTask(plugin, task)
- All DB CompletableFutures execute on virtual threads; their .thenAccept()
  callbacks that touch Bukkit API call SchedulerUtil.runSync or runForEntity
  to return to the correct thread.
- GUI open/close operations that touch inventory always run on the entity's
  scheduler (Folia) or main thread (Paper) via SchedulerUtil.runForEntity.

---

STARTUP CONFIG ADAPTATION:
- On onEnable, before loading defaults, call ConfigUtil.loadOrAdapt() for
  each config file (config.yml, skins.yml, gui.yml, admin-gui.yml,
  skin-editor-gui.yml).
- loadOrAdapt() logic:
    1. If the file exists in plugins/ChamoItemSkins/, load it as-is and
       merge any missing keys from the embedded default (resource) using a
       deep-merge strategy that never overwrites existing user values.
    2. If the file does not exist, copy the embedded default from the jar's
       resources/ and save it.
- This ensures pre-generated files are fully respected and never silently
  overwritten.

---

CORE FEATURES:

1. SKIN CONFIGURATION (skins.yml):
    - id: unique String (auto-generated UUID on first creation)
    - name: MiniMessage
    - model-id: BetterModel model ID
    - item-type: Bukkit Material name
    - enabled: boolean
    - note-material: optional override
    - display-item: (id, name, lore list, glow boolean)

2. NOTE ITEM:
    - Material from config or per-skin override.
    - Name/lore from config.yml (MiniMessage, {skin_name} resolved at give-time).
    - Skin ID in PersistentDataContainer key "chamoitemskins:skin_id".
    - Trigger: PlayerInteractEvent RIGHT_CLICK_AIR or RIGHT_CLICK_BLOCK.
    - SkinGrantEvent fired (cancellable). If not cancelled: consume item,
      persist grant, send messages.grant-received.

3. DATABASE:
   Tables (CREATE TABLE IF NOT EXISTS on startup):

   player_skin_grants (
   grant_id    VARCHAR(36) PRIMARY KEY,
   player_uuid VARCHAR(36) NOT NULL,
   skin_id     VARCHAR(64) NOT NULL,
   granted_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
   source      VARCHAR(32) NOT NULL
   )

   player_active_skins (
   player_uuid VARCHAR(36) NOT NULL,
   item_type   VARCHAR(64) NOT NULL,
   skin_id     VARCHAR(64) NOT NULL,
   PRIMARY KEY (player_uuid, item_type)
   )

    - DatabaseManager: sealed interface; MySQLDatabase and SQLiteDatabase
      are the two permitted implementations.
    - All queries on virtual threads, results piped back via
      SchedulerUtil.runForEntity / runSync.

4. MULTI-SERVER: MySQL shared store. Per-player in-memory cache (CacheManager)
   with configurable TTL. Cache invalidated on grant/revoke.

5. /skins MAIN GUI:
    - Permission: chamoitemskins.use (default: true)
    - One category icon per item-type with ≥1 enabled skin.
    - Config in gui.yml: title, size, slot definitions.
    - Click → opens SkinSelectionGui for that item-type.

6. SKIN SELECTION GUI:
   Slot type system (sealed SlotType):
    - 0 Decorative   → static item, no action.
    - 1 SkinSlot     → owned: show display-item, click equips/unequips;
      unowned: show locked-item, click sends not-owned msg;
      empty index: show filler-item.
    - 2 FilterSlot   → toggles Owned/All in-place. Two configurable states.
    - 3 BackSlot     → returns to MainSkinsGui.

   On equip:   fire SkinEquipEvent (cancellable), apply BetterModelService,
   write player_active_skins async, send messages.skin-equipped.
   On unequip: fire SkinUnequipEvent (cancellable), remove model,
   delete row, send messages.skin-unequipped.

7. ADMIN GUI (admin-gui.yml, permission: chamoitemskins.admin):
   Opened via /skins admin gui OR the admin button inside /skins (if player
   has chamoitemskins.admin). Slot type 4 = ActionSlot.
   Configurable action buttons (each slot defines an action string):
    - "GRANT"  → opens an anvil/sign input GUI asking for player name + skin
      ID, then calls GrantManager.grantSkin() async.
    - "REVOKE" → same input flow, calls GrantManager.revokeSkin() async.
    - "GIVE"   → input: player name + skin ID, gives physical Note item.
    - "RELOAD" → calls plugin reload; sends messages.reload-success.
    - "EDITOR" → opens SkinEditorGui.
    - "BROWSE_GRANTS" → opens a paginated GUI listing all grants for a player
      (player name input first).
      All action buttons: material, name, lore fully configurable in
      admin-gui.yml. Confirmation step configurable per action
      (confirm: true/false in admin-gui.yml).

8. SKIN EDITOR GUI (skin-editor-gui.yml, permission: chamoitemskins.admin):
   A live editor that reads from and writes back to skins.yml.
   Layout configurable in skin-editor-gui.yml.
   Screens / flows:
   a) SKIN LIST screen: paginated list of all skins (one icon per skin).
   Each skin icon: material = skin's display-item material; name = skin
   name; lore shows id, item-type, enabled status.
   Buttons: [New Skin] [← Prev] [Next →] [Back to Admin]
   b) SKIN DETAIL screen (click a skin): shows all properties of one skin.
   Slots:
    - Toggle Enabled  → flips skin.enabled, writes skins.yml immediately.
    - Edit Name       → anvil input; saves new MiniMessage name to yml.
    - Edit Model ID   → anvil input; saves new model-id to yml.
    - Edit Item Type  → cycles through a configurable list of Materials;
      saves to yml.
    - Edit Note Mat.  → cycles Materials; saves to yml.
    - Edit Display    → opens sub-GUI for display-item (material, name,
      lore lines, glow toggle).
    - Delete Skin     → confirmation prompt; removes from yml and memory.
    - [Back to List]
      c) NEW SKIN screen: guided step-by-step:
      Step 1: Enter name via anvil input.
      Step 2: Enter model-id via anvil input.
      Step 3: Pick item-type (clickable material icons from a predefined
      configurable list in skin-editor-gui.yml).
      Step 4: Review summary → [Confirm & Save] [Cancel].
      On confirm: generates UUID id, writes new entry to skins.yml, reloads
      SkinManager in memory, sends admin success message.
    - YamlUtil.saveSkin(Skin) and YamlUtil.deleteSkin(String id) perform
      thread-safe (virtual thread) writes to skins.yml and immediately call
      SkinManager.reload() on the sync thread via SchedulerUtil.runSync.

9. PLACEHOLDERAPI EXPANSION (identifier: chamoitemskins):
    - %chamoitemskins_active_<MATERIAL>%  → active skin name or ""
    - %chamoitemskins_owns_<skinId>%      → "true" / "false"
    - %chamoitemskins_total_owned%        → grant count
    - %chamoitemskins_total_skins%        → total enabled skin count
      Follow internal expansion pattern exactly (persist()=true, dependency
      injection, return null for unknown params).

10. DEVELOPER API (:api):
    - ChamoItemSkinsApi interface with static get() via ServicesManager.
    - Exposes SkinService, GrantService, all four events.
    - All public types fully Javadoc'd.
    - :api has no compile dependency on BetterModel, HikariCP, or
      StrokkCommands.

---

CONFIGURATION FILES (embedded defaults in resources/):

config.yml:
database:
type: sqlite                        # or "mysql"
host: localhost
port: 3306
database: chamoitemskins
username: root
password: ""
sqlite-file: plugins/ChamoItemSkins/data.db
cache:
ttl-seconds: 300
note:
default-material: PAPER
display-name: "<gold><bold>Skin Note"
lore:
- "<gray>Right-click to unlock <white>{skin_name}<gray>!"
messages:
skin-equipped:        "<green>✔ Equipped <white>{skin_name}<green>."
skin-unequipped:      "<yellow>Skin <white>{skin_name}<yellow> removed."
skin-not-owned:       "<red>✘ You don't own <white>{skin_name}<red>."
grant-received:       "<green>✔ You unlocked <white>{skin_name}<green>!"
skin-revoked-notify:  "<red>Your skin <white>{skin_name}<red> was revoked."
admin-grant-success:  "<green>Granted <white>{skin_name}<green> to <white>{player}<green>."
admin-revoke-success: "<red>Revoked <white>{skin_name}<red> from <white>{player}<red>."
reload-success:       "<green>ChamoItemSkins reloaded."
editor-saved:         "<green>Skin <white>{skin_name}<green> saved."
editor-deleted:       "<red>Skin <white>{skin_id}<red> deleted."

skins.yml:
skins:
- id: "infernal_blade"
name: "<gradient:red:dark_red>Infernal Blade"
model-id: "chamoitemskins:infernal_blade"
item-type: DIAMOND_SWORD
enabled: true
note-material: PAPER
display-item:
id: DIAMOND_SWORD
name: "<gradient:red:dark_red>Infernal Blade"
lore: ["<gray>A blade forged in hellfire."]
glow: true

gui.yml:         (main /skins menu — see slot layout spec in FEATURE 5)
admin-gui.yml:   (admin panel — see FEATURE 7)
skin-editor-gui.yml: (editor screens — see FEATURE 8)
# skin-editor-gui.yml must also define:
#   allowed-item-types: list of Material names shown in the item-type picker

---

CONSTRAINTS:
- Java 25 only. No Kotlin source files anywhere.
- ALL Gradle build files use *.gradle.kts. Zero Groovy. Zero exceptions.
- Adventure + MiniMessage for every player-facing string. No legacy color.
- MessageUtil must run MiniMessage deserialization AND PlaceholderAPI
  setPlaceholders() (if PAPI present) before sending to player.
- StrokkCommands for all commands. No legacy CommandExecutor / onCommand().
- NEVER use BukkitScheduler. All scheduling through SchedulerUtil.
- All DB operations on virtual threads + CompletableFuture. Main thread
  callbacks via SchedulerUtil.
- GUI inventory open/close always dispatched via SchedulerUtil.runForEntity.
- BetterModel calls isolated entirely in BetterModelService.java.
- :api compiles without BetterModel, HikariCP, StrokkCommands.
- ConfigUtil.loadOrAdapt() deep-merges existing user files; never silently
  overwrites user values.
- YamlUtil writes are atomic (write to temp file, rename) where possible.
- All public API types in :api have Javadoc.
- Inline // comments for non-obvious logic; no prose outside Javadoc.

---

OUTPUT ORDER:
1.  .gitignore
2.  settings.gradle.kts
3.  build.gradle.kts                                      (root)
4.  api/build.gradle.kts
5.  api/.../model/Skin.java
6.  api/.../model/SkinGrant.java
7.  api/.../event/SkinEquipEvent.java
8.  api/.../event/SkinUnequipEvent.java
9.  api/.../event/SkinGrantEvent.java
10. api/.../event/SkinRevokeEvent.java
11. api/.../service/SkinService.java
12. api/.../service/GrantService.java
13. api/.../ChamoItemSkinsApi.java
___14. plugin/build.gradle.kts___
15. plugin/.../scheduler/SchedulerUtil.java
16. plugin/.../util/ConfigUtil.java
17. plugin/.../util/MessageUtil.java
18. plugin/.../util/NoteUtil.java
19. plugin/.../util/YamlUtil.java
20. plugin/.../gui/config/SlotType.java
21. plugin/.../gui/config/GuiSlotDef.java
22. plugin/.../database/DatabaseManager.java
23. plugin/.../database/MySQLDatabase.java
24. plugin/.../database/SQLiteDatabase.java
25. plugin/.../manager/CacheManager.java
26. plugin/.../manager/SkinManager.java
27. plugin/.../manager/GrantManager.java
28. plugin/.../bettermodel/BetterModelService.java
29. plugin/.../placeholder/ChamoItemSkinsExpansion.java
30. plugin/.../listener/NoteListener.java
31. plugin/.../listener/GuiListener.java
32. plugin/.../gui/MainSkinsGui.java
33. plugin/.../gui/SkinSelectionGui.java
34. plugin/.../gui/AdminGui.java
35. plugin/.../gui/SkinEditorGui.java
36. plugin/.../command/SkinsCommand.java
37. plugin/.../command/AdminCommand.java
38. plugin/.../ChamoItemSkinsPlugin.java
39. plugin/src/main/resources/plugin.yml
40. plugin/src/main/resources/config.yml
41. plugin/src/main/resources/skins.yml
42. plugin/src/main/resources/gui.yml
43. plugin/src/main/resources/admin-gui.yml
44. plugin/src/main/resources/skin-editor-gui.yml

Now generate all 44 files in full, in the order listed above.