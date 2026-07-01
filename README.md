![ChamoItemSkins](assets/ChamoItemSKins%20ad.png)

ChamoItemSkins is a high-performance, feature-rich Minecraft cosmetic skin plugin built for **Spigot (Forks too!) 1.21.11** and **Folia**. It allows players to unlock and apply custom models to their items using a sleek GUI system. Primarily made for model support, providing a seamless cosmetic experience for large-scale networks.

##  Features

- **BetterModel Integration**: Native support for custom models with high performance.
- **Folia & Paper Support**: Fully compatible with multi-threaded server environments.
- **Dynamic Skin System**: Define skins in `skins.yml` with custom display items, names, and lore.
- **Physical Unlockables**: Give players "Skin Notes" that they can right-click to permanently unlock a cosmetic.
- **Multiple GUI Menus**:
  - **Main Menu**: Browse skin categories by item type.
  - **Skin Selection**: Equippable skins for owned items.
  - **Admin GUI**: Manage grants, revoke skins, and reload configurations in-game.
  - **Live Skin Editor**: Create and modify skins directly from a GUI with real-time `skins.yml` updates.
- **Database Support**: Choose between **SQLite** (local) or **MySQL** (multi-server sync).
- **PlaceholderAPI Support**: Custom placeholders for active skins, ownership status, and totals.
- **Developer API**: Clean, event-driven API for third-party integrations.

##  Requirements

- **Java 21** or higher.
- **Paper/Folia/Spigot 1.21.11+**.
- **BetterModel 3.1.0+** plugin installed.
- (Optional) **PlaceholderAPI** for menu/chat integration.

##  Installation

1. Download the latest `ChamoItemSkins.jar`.
2. Place it in your server's `plugins/` folder.
3. Ensure **BetterModel** is also present in the `plugins/` folder.
4. Restart your server to generate the default configuration files.
5. Configure your database settings in `config.yml` (default is SQLite).

##  Configuration
**Keep in mind that our wiki has a more detailed guide, so we recommend checking that instead :D**

The plugin uses several YAML files for deep customization:

- **`config.yml`**: Database settings, global messages, and default Note item settings.
- **`skins.yml`**: Define all available skins, their model IDs, and associated item types.
- **`gui.yml`**: Layout and icons for the main `/skins` menu.
- **`admin-gui.yml`**: Configuration for the administrative dashboard.

### Example Skin Definition (`skins.yml`)
```yaml
skins:
  infernal_blade:
    id: "infernal_blade"
    name: "<gradient:red:dark_red>Infernal Blade"
    model-id: "chamoitemskins:infernal_blade"
    item-type: DIAMOND_SWORD
    enabled: true
    display-item:
      id: DIAMOND_SWORD
      name: "<gradient:red:dark_red>Infernal Blade"
      lore: ["<gray>A blade forged in hellfire."]
      glow: true
```

##  Commands & Permissions

### Player Commands
| Command  | Description                        | Permission                     |
|:---------|:-----------------------------------|:-------------------------------|
| `/skins` | Opens the main skin selection GUI. | `chamoitemskins.use` (default) |

### Admin Commands
| Command                              | Description                                | Permission             |
|:-------------------------------------|:-------------------------------------------|:-----------------------|
| `/skinsadmin gui`                    | Opens the administrative management GUI.   | `chamoitemskins.admin` |
| `/skinsadmin give <player> <skinId>` | Gives a physical Skin Note to a player.    | `chamoitemskins.admin` |
| `/skinsadmin reload`                 | Reloads all configuration files and skins. | `chamoitemskins.admin` |

##  Placeholders

Use these with **PlaceholderAPI**:

- `%chamoitemskins_active_<MATERIAL>%`: Returns the name of the active skin for the specified material.
- `%chamoitemskins_owns_<skinId>%`: Returns `true` or `false` if the player owns the skin.
- `%chamoitemskins_total_owned%`: Total number of skins owned by the player.
- `%chamoitemskins_total_skins%`: Total number of enabled skins available.

##  Developer API

Developers can hook into ChamoItemSkins using the `:api` submodule.

### Repository
The repository for the API can be accessed here:<br>
Gradle (build.gradle.(kts))
``` kotlin
maven {
    name = "chamosmpRepoReleases"
    url = uri("https://maven.chamosmp.net/releases")
}
```
``` kotlin
compileOnly("net.chamosmp.chamoitemskins:api:version")
```
Maven (pom.xml)
``` xml
<repository>
  <id>chamosmp-repo-snapshots</id>
  <name>ChamoSMP Maven Repository</name>
  <url>https://maven.chamosmp.net/snapshots</url>
</repository>
```
``` xml
<dependency>
  <groupId>net.chamosmp.chamoitemskins</groupId>
  <artifactId>api</artifactId>
  <version>version</version>
</dependency>
```

### Events
- `SkinEquipEvent`: Fired when a player equips a skin.
- `SkinUnequipEvent`: Fired when a player unequips a skin.
- `SkinGrantEvent`: Fired when a player unlocks a skin via a Note or Admin command.
- `SkinRevokeEvent`: Fired when a skin is removed from a player.

### Accessing the API
```java
ChamoItemSkinsApi api = Bukkit.getServicesManager().load(ChamoItemSkinsApi.class);
if (api != null) {
    SkinService skinService = api.getSkinService();
            // Your logic here
}
```

---
*Brought to you by SQD Studios*. _Keep in mind this is vibecoded to see how powerful (even though it isn't a valid excuse) it is. I've personally checked every file to
maintain the quality. There aren't a lot of things (except unperfomant code) that could go wrong right? If I have time, I may 
recode this as some people actually need it_
