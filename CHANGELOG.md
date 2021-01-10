# Changelog

## v1.35.0-PRE:

### Notes:
* I wish to start off with a big thank you to stumper66 for their huge list of contributions this update. Stumper also gave me the spark to recontinue work on the resource. Give him your thanks too if you enjoy this update ;)
* I've recontinued work on the plugin. Almost all of the code has been altered from the previous version.
* **IMPORTANT:** Before installing this update on your server, please understand the following:
  * PhantomLib is no longer used by LevelledMobs! Uninstall it if you wish (unless you have other plugins that depend on it installed)
  * Almost all permissions have changed. I recommend you remove any current permissions from LM you have set and apply the new ones. See the documentation.
  * If you are using any WorldGuard flags from LM, you should remove them from your regions prior to installing this new version. ALL previous LevelledMobs flags have been renamed, and I've also added in a new flag. :)
  

### Changelog:

**Summon Command** (stumper66)
* The summon command is now completed!
* Summon the specified level, amount of mobs and at your location, another player's or an x/y/z location.
* Added setting 'summon-command-spawn-distance-from-player' with default of 5.

**Kill Subcommands**
* The `KillAll` subcommand has been revamped and split into two subcommands; `kill all [world]` and `kill near [radius]`. (stumper66)
* Now kills mobs instead of instantly removing them. This means drops, exp, etc. (stumper66)
* `<world>` argument accepts a wildcard (`*` character) to specify 'all worlds loaded on the server' to have levelled mobs purged from.
* Now checks for new entity metadata 'levelled' to kill the mob. (lokka30)
 * This means that mobs that were previously considered levellable will also be killed.
 * This means that mobs that are not levelled but now considered levellable will not not be killed.

**WorldGuard Flag Changes and Fixes** (lokka30 & stumper66)
* Added a new flag 'LM_AllowLevelledMobs': deny this flag to tell LevelledMobs not to level a mob that spawns in a region.
* Renamed the old flags. The renamed flags are:
  * 'LM_UseCustomLevels': this determines if the custom min and custom max level limit flags are in effect.
  * 'LM_CustomMinLevel' and 'LM_CustomMaxLevel' put a boundary on how high and/or low a mob's level will be if they spawn in the region.
* WorldGuard level restrictions now properly respect the max level if both max and min levels are set to the same value.

**New Advanced Permissions** (lokka30)
* With all the new features finding their way into LevelledMobs, it was becoming necessary to restructure the permissions.
* Almost all permissions have changed, and a bunch have been added.
* Full wildcard support (`*` character, e.g. `levelledmobs.command.*`) to make applying permissions far quicker.
* The command now requires a permission to run too (given by default) in case you wanted to restrict access to it.

**Full Tab Auto-Completion Implemented** (stumper66)
* The commands now feature full auto tab-completion to help speed things up.

**Bye bye, PhantomLib** (lokka30)
* I have made the switch to my new development library, MicroLib.
* Server owners do not need to install it as MicroLib is shaded inside of the LevelledMobs plugin file.
* Server owners are recommended to remove PhantomLib if it is no longer used by any of their installed plugins.

**Creeper Blast Radius Scaling** (stumper66)
* Creeper blast radius is now scaled with their corresponding level.
* The amount is configured in settings.
* Default is 5 for max blast radius (vanilla creeper radius is 3)

**Configurable Chat Messages** (lokka30)
* Added a `messages.yml` configuration file, in which server owners can now translate all chat messages from the plugin i.e. those sent from the command.

**Slime Splitting** (stumper66)
* New setting (enabled by default) for when slimes / magmacubes split, they will retain the level of their parents.
* If disabled, then each child is randomly leveled (same behavior as previous versions)

**Default Level Nametags** (stumper66)
* New setting to apply nametags on default leveled mobs.
* If set to false and a level 1 mob is spawned, no nametag will be applied to the mob.

**Better Default Settings** (lokka30)
* The previous default settings were *way* too difficult. I was scared of my own creation when I deployed it on a private server with friends. I've now finally toned down the multipliers so that it can provide a harder challenge but not something beyond hardcore.

**Other Bugfixes**
* Magma Cubes, Slimes, Ghasts and Hoglins are now levelled as expected (stumper66)
* Shulkers are now levelled as expected (lokka30)

**Other Code Changes** (lokka30)
* Cleaned up the main class significantly. A bunch of methods and variables have been moved to more fitting classes.
* Removed the LightningStorage system - it has been replaced with a far simpler system. This will not only increase performance, but also decrease clutter in the code and decrease the file size of the plugin.
* Moved all subcommands to their own classes implementing a Subcommand interface - cleans up the LevelledMobsCommand class to a huge degree
* Added `LevelManager.isLevellable(EntityType)` so a mob can be checked if it is levellable or not before it spawns in. (used in the summon command)
* Added the ListMode enum which will make checking lists in the config easier and cleaner
* Removed 'isLevelled' NamespacedKey
* Added all code contributors to the 'info' subcommand.
* Removed sound from debug feature - in case older versions become supported, this sound will need to go anyways