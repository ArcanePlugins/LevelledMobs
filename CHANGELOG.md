# Changelog

## v1.35.0-PRE:

### Notes:
* **Before installing this update, read the Important section below.**
* I wish to start off with a huge thank you to **stumper66** for their massive list of contributions this update. Stumper also gave me the spark to recontinue work on the resource. This update wouldn't be here without him!
* I've recontinued work on the plugin. Most of the code has been updated from the previous version. This update took many hours to make, so I hope it was worth it! :)
* (psst: leave a like / positive review if you enjoy this update!)

### Important:
Before installing this update on your server, please understand the following:
  * **PhantomLib is no longer used by LevelledMobs!** Uninstall it if you wish (unless you have other plugins that depend on it installed)
  * **Almost all permissions have changed.** I recommend you remove any current permissions from LM you have set and apply the new ones. See the documentation.
  * If you are using any **WorldGuard flags from this plugin**, you should **remove them from your regions prior to installing this new version**. ALL previous LevelledMobs flags have been renamed, and I've also added in a new flag which was a popular request. :)
  * **Reset your config!** You might want to back it up in case you want to copy old settings across, but the settings file has changed enough that I would recommend starting from scratch.
  
### Changelog:
**Summon Subcommand** (stumper66)
* The summon command is now completed!
* Summon the specified level, amount of mobs and at your location, another player's or an x/y/z location.
* Added setting 'summon-command-spawn-distance-from-player' with default of 5.

**Kill Subcommands**
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
* WorldGuard level restrictions now properly respect the max level if both max and min levels are set to the same value. (stumper66)

**New Powerful Permissions** (lokka30)
* With all the new features finding their way into LevelledMobs, it was becoming necessary to restructure the permissions.
* Almost all permissions have changed, and a bunch have been added.
* Full wildcard support (`*` character, e.g. `levelledmobs.command.*`) to make applying permissions far quicker.
* The command now requires a permission to run too (given by default) in case you wanted to restrict access to it.

**Full Tab Auto-Completion Implemented** (stumper66)
* The commands now feature full auto tab-completion to help speed things up.

**Bye bye, PhantomLib** (lokka30)
* I have made the switch to my new development library, MicroLib.
* Server owners do not need to install MicroLib as it is embedded inside of the LevelledMobs plugin file. It is also not a separate plugin, rather part of this plugin.
* Server owners are recommended to **remove PhantomLib** if it is no longer used by any of their installed plugins as it will remain unsupported.

**Creeper Blast Radius Scaling** (stumper66)
* Creeper blast radius is now scaled with their corresponding level.
* The amount is configurable in settings. Default is 5 for max blast radius (vanilla creeper radius is 3)

**Configurable Chat Messages** (lokka30)
* Added a `messages.yml` configuration file, in which server owners can now translate all chat messages from the plugin i.e. those sent from the command.
* Generic colour code support (no rgb yet).
* Multi-line support.
* `%prefix%` variable available on all messages.

**Slime Splitting** (stumper66)
* New setting (enabled by default) for when slimes / magmacubes split, they will retain the level of their parents.
* If disabled, then each child is randomly leveled (same behavior as previous versions)

**Default Level Nametags** (stumper66)
* New setting to apply nametags on default leveled mobs.
* If set to false and a level 1 mob is spawned, no nametag will be applied to the mob.

**New Multipliers System** (lokka30)
* The old "multipliers" weren't actually multipliers.
* I've re-coded that system. Not only is it easier to change multipliers (since you're not dealing with Minecraft's mysterious scales), it is also no longer misleading :)

**Better Default Settings** (lokka30)
* Until this update, the default settings have been beyond hardcore, and I am honestly surprised servers are coping with them.
* Following the new multipliers system being added in, I have tested new values to ensure they function well.

**New Attributes System** (lokka30)
* LM now stores a pregenerated attributes file in its data folder. This contains all of the default attributes for every living entity in 1.16.
* This file will be updated with each new Minecraft version.
* This will improve compatibility with plugins such as SafariNets immensely.
* A secret, secure command has been implemented (only accessible by console with a password) which generates a new copy of the file. This is only meant to be used by developers. It is used to update the file with new Minecraft versions.

**Other Bugfixes**
* Magma Cubes, Slimes, Ghasts and Hoglins are now levelled as expected (stumper66)
* Shulkers are now levelled as expected (lokka30)

**Removed Features** (lokka30)
* Removed an old feature 'flight speed multiplier' which we found out Minecraft only applies to parrots. Completely unnecessary and misleading.
* Removed 'default attack damage increase'

**Other Code Changes** (lokka30)
* Cleaned up the every single class in this project significantly. A bunch of methods and variables have been moved to more fitting classes too.
* Removed the LightningStorage system - it has been replaced with a far simpler system. This will not only increase performance, but also decrease clutter in the code and decrease the file size of the plugin.
* Moved all subcommands to their own classes implementing a Subcommand interface - cleans up the LevelledMobsCommand class to a huge degree
* Added `LevelManager#isLevellable(EntityType)` so a mob can be checked if it is levellable or not before it spawns in. (used in the summon command)
* Added the ListMode enum which will make checking lists in the config easier and cleaner
* Added all code contributors to the 'info' subcommand.
* Removed sound from debug feature - in case older versions become supported, this sound will need to go anyways