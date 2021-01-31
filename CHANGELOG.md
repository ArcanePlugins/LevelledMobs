# Changelog

***

## v2.1.0
**WARNING** This update includes file changes! Please see the Notes section below. If you do not update your files then the plugin will malfunction!

### Notes:
* File changes:
  * `settings.yml` updated **[RESET REQUIRED]**
  * `customdrops.yml` added

### Changelog:
**Custom Drops Configuration!** (stumper66)
* stumper66 worked long and hard to implement the suggestion originally from @Ravenlant back in February last year that had been frequently requested since.
* It is a *very* configurable system. Take a look for yourself!
* Thanks to those on the Discord server who provided suggestions to stumper whilst he worked on this system - especially Hugo5551! Another thank-you to the testers, @Oathkeeper and **squeebis**!

**Mob Additions Values Scaling!** (lokka30, stumper66)
* You can now safely change the min and max levels in `fine-tuning` without having to change the `additions` values in `settings.yml` as they will now scale to the min and max levels you set. :)
* **Movement speed, health, attack damage, etc have significantly changed as there is a new formula that uses these values. Please, do not use your old movement speed values, etc.**

**Config Migration!** (stumper66)
* We have received a dozen users join the server wondering why LM wasn't working on their server - 90% of the case this is because the user is running an outdated settings file.
* stumper66 whipped up a brilliant system which automatically updates your config files (and backs up the previous files) just to make sure that the plugin runs smooth if you forgot to update a config file.
* **Note:** It is still recommended you reset your files when they update regardless since otherwise you will miss out on new/improved config comments which are meant to guide you on how to configure the plugin.

**New compatibility!**
* Previously, LevelledMobs only made compatibility Citizens. Now as requested by many, LevelledMobs is now compatible with MythicMobs, EliteMobs, InfernalMobs and Shopkeepers too!
* Thanks to @MagmaGuy for linking me towards the relevant metadata values to make LM compatible with EM.
* Added compatibility with the 4 mentioned plugins (lokka30)
* Added toggles in settings for these 4 compatibilities (stumper66)

**Bug Fixes**
* Fixed name tags ignoring a mob's existing 'isCustomNameVisible' boolean (lokka30)
* Fixed `show-label-for-default-levelled-mobs` setting causing mobs not to be levelled properly (lokka30)
* Forced armor stands to not be levellable (thanks squeebis for reporting this) (lokka30)

**Minor Improvements**
* Added 'Is settings.yml outdated?' notice to the 'Mode is unset at path ...' warning.

**Removed Features**
* `passive-mobs-change-movement-speed` was removed as it was a setting that was not even implemented in the first place. It may be added back in the future. (lokka30)

***

## v2.0.0:
### Notes:
* **Before installing this update, you must read the 'Important' section below. The plugin WILL malfunction if you refuse to follow the information provided there.**
* I wish to start off with a huge thank you to **stumper66** for their massive list of contributions this update. Stumper also gave me the spark to recontinue work on the resource. This update wouldn't be here without him!
* I've recontinued work on the plugin. Most of the code has been updated from the previous version. This update took so many hours to make, so I really hope you all like it. 7 other people assisted stumper and I to get this update through.
* (psst: leave a like / positive review if you enjoy this update!)

### Important:
Before installing this update on your server, please understand the following:
  * **Nametags now require ProtocolLib.** If you do not install ProtocolLib, then you will not see levelled mobs' nametags.
  * **PhantomLib is no longer used by LevelledMobs.** Uninstall it if you wish (unless you have other plugins that depend on it installed)
  * **Every single permission has changed, and a bunch have been added in too.** I recommend you remove any current permissions from LM you have set to your groups and apply the new ones. See the documentation.
  * If you are using any **WorldGuard flags from this plugin**, you should **remove them from your regions prior to installing this new version**. ALL previous LevelledMobs flags have been renamed, and I've also added in a new flag which was a popular request. :)
  * **Reset your config!** You might want to back it up in case you want to copy old settings across, but the settings file has changed enough that I would recommend starting from scratch.

### Impossible without big help from:
* @stumper66, whose contributions are too many to list here ;)
* @CoolBoy, @Esophose and @7smile7 for creating & fixing the complicated packet nametag method
* @Oathkeeper for their great feature suggestions and testing
* Hugo5551 for making a handful of great code suggestions
* squeebis for helping us out with testing
* JacksaYT for reporting multiple bugs, some of high importance
  
### Changelog:
**Summon Subcommand** (stumper66)
* The summon command is finally complete thanks to efforts by stumper66!
* Summon the specified level, amount of mobs and at your location, another player's or an x/y/z location.
* Added setting `'`summon-command-spawn-distance-from-player` with default of 5.

**Kill Subcommands** (stumper66)
* The `KillAll` subcommand has been revamped and split into two subcommands; `kill all [world]` and `kill near [radius]`. (stumper66)
* Now kills mobs instead of instantly removing them. This means drops, exp, etc. (stumper66)
* `<world>` argument accepts a wildcard (`*` character) to specify 'all worlds loaded on the server' to have levelled mobs purged from.
* Now checks for new entity NBT 'isLevelled' to kill the mob. (lokka30)
  * This means that mobs that were previously considered levellable will also be killed.
  * This means that mobs that are not levelled but now considered levellable will not not be killed.

**WorldGuard Flag Changes and Fixes**
* Added a new flag 'LM_AllowLevelledMobs': deny this flag to tell LevelledMobs not to level a mob that spawns in a region. (lokka30)
* Renamed the old flags. The renamed flags are:
  * 'LM_UseCustomLevels': this determines if the custom min and custom max level limit flags are in effect. (lokka30)
  * 'LM_CustomMinLevel' and 'LM_CustomMaxLevel' put a boundary on how high and/or low a mob's level will be if they spawn in the region. (lokka30)
* WorldGuard flags were broken, we are unsure for how long this was the case. Luckily it was a simple fix. (stumper66)
* WorldGuard level restrictions now properly respect the max level if both max and min levels are set to the same value. (stumper66)

**New Powerful Permissions** (lokka30)
* With all the new features finding their way into LevelledMobs, it was becoming necessary to restructure the permissions.
* Almost all permissions have changed, and a bunch have been added.
* Full wildcard support (`*` character, e.g. `levelledmobs.command.*`) to make applying permissions far quicker.
* The command now requires a permission to run too (given by default) in case you wanted to restrict access to it.

**Packet Nametags** (lokka30, CoolBoy, Esophose, 7smile7, stumper66)
* This feature requires ProtocolLib to be installed, else the LevelledMobs nametag system won't be enabled.
* No longer does LevelledMobs mess with nametags! It instead modifies the packets sent to the client.
* In other words, LM sends a 'fake nametag' to the client, but there isn't actually a nametag on the mob.
* This has fixed the following problems:
  * You can now apply nametags to levelled mobs without losing the levelled nametag
  * Anti-lag plugins usually ignore nametagged mobs - this means they will now kill levelled mobs too, since there isn't actually a nametag on the entity.
  * Essentials' killall command now works properly! :)
* This was possible thanks to help from @CoolBoy, @Esophose and @7smile7 in [this](https://www.spigotmc.org/threads/changing-an-entitys-name-using-protocollib.482855/) thread. A big thank you to them for their assistance.
* Thanks to @stumper66 for assisting me with band-aiding issues caused by this new system.

**Full Tab Auto-Completion Implemented** (stumper66)
* The commands now feature full auto tab-completion to help speed things up.

**Bye bye, PhantomLib** (lokka30)
* I have made the switch to my new development library, MicroLib.
* **Server owners do not need to install MicroLib** as it is embedded inside of the LevelledMobs plugin file. It is also not a separate plugin, rather part of this plugin.
* Server owners are recommended to **remove PhantomLib** if it is no longer used by any of their installed plugins as it will remain unsupported.

**Y-Coordinate Levelling** (stumper66)
* A brilliant suggestion by @Oathkeeper, this levels entities depending on their y-coordinate. It functions very similarly to 'distance from spawn levelling'.
* This can make mining in caves that much harder! :)

**Creeper Blast Radius Scaling** (stumper66)
* Creeper blast radius is now scaled with their corresponding level.
* The amount is configurable in settings. Default is 5 for max blast radius (vanilla creeper radius is 3)

**Configurable Chat Messages** (lokka30)
* Added a `messages.yml` configuration file, in which server owners can now translate all chat messages from the plugin i.e. those sent from the command.
* Generic colour code support (no rgb yet).
* Multi-line support.
* `%prefix%` variable available on all messages.

**Default Level Nametags** (stumper66)
* New setting to apply nametags on default leveled mobs.
* If set to false and a level 1 mob is spawned, no nametag will be applied to the mob.
* This gives a more vanilla feel.

**New Additions System** (lokka30)
* Replacing the old "multipliers" system that didn't actually function as a multiplier, introducing the Additions system!
* For each level a mob has, the 'addition values' will be added to their attributes, e.g. movement speed.
* It is highly recommended that you don't touch anything in the fine-tuning section since the numbers are pretty sensitive.
* The default addition values have been properly tested, unlike the previous multipliers which were beyond hardcore ;)

**New Attributes System** (lokka30)
* LM now stores a pregenerated attributes file in its data folder. This contains all of the default attributes for every living entity in 1.16.
* This file will be updated with each new Minecraft version.
* This will improve compatibility with plugins such as SafariNets immensely.
* A secret, secure command has been implemented (only accessible by console with a password) which generates a new copy of the file. This is only meant to be used by developers. It is used to update the file with new Minecraft versions.

**Entity Transformation Fixes** (lokka30 + stumper66)
* All entities that transform into other entities (e.g. Zombies->Drowned and Slimes) now carry over their parent's level.

**Other Bugfixes**
* Magma Cubes, Slimes, Ghasts, Phantoms, Ender Dragons, Shulkers and Hoglins are now levelled as expected (stumper66)
* Armor Stands no longer have items/xp drops from death multiplied (lokka30)
* XP drop management now checks if the mob is levelled rather than is levellable, fixes possible NPE (lokka30)
* Many unwritten bugs fixed by stumper66 and lokka30 during the pre-release testing stage

**Removed Features**
* Removed an old feature 'flight speed multiplier' which we found out Minecraft only applies to parrots. Completely unnecessary and misleading. (lokka30)
* Removed 'default attack damage increase' (lokka30)
* Removed 'remove nametag on death' which is no longer necessary with the introduction of packet nametags. (lokka30)
* Removed 'update nametag on health update' as LM does this automatically (stumper66)

**Other Code Changes**
* Cleaned up the every single class in this project significantly. A bunch of methods and variables have been moved to more fitting classes too. (lokka30)
* Removed the LightningStorage system - it has been replaced with a far simpler system. This will not only increase performance, but also decrease clutter in the code and decrease the file size of the plugin. (lokka30)
* Moved all subcommands to their own classes implementing a Subcommand interface - cleans up the LevelledMobsCommand class to a huge degree (lokka30)
* Added `LevelManager#isLevellable(EntityType)` so a mob can be checked if it is levellable or not before it spawns in. (used in the summon command) (lokka30)
* Added the ListMode enum which will make checking lists in the config easier and cleaner (lokka30)
* Added all code contributors to the 'info' subcommand. (lokka30)
* Use HashSet for forced entity types instead (Hugo5551)
* Determine level after the entity is deemed levellable (Hugo5551)
* Removed sound from debug feature - in case older versions become supported, this sound will need to go anyways (lokka30)
* Rearranged the Manager classes from the utils package to the main package (lokka30)
* Use `(ignoreCancelled = true, priority = EventPriority.MONITOR)` with most event handlers (Hugo5551)
* Improvements to the debug listener (stumper66)
* Improved compatibility checker (lokka30)