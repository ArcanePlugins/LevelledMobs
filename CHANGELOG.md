# Changelog
⚠ Not all versions have been recorded in this changelog. Please check the Updates tab on our SpigotMC page for a full
list of changelogs.

***

## v3.0.4 b444

* @stumper66 fixed exception when using lower-mob-level-bias-factor
* @stumper66 added back in lower-mob-level-bias-factor
* @stumper66 fixed custom commands not respecting min and max level restrictions
* @stumper66 adjusted the attribute multiplier slightly
* @stumper66 fixed low level skeletons doing no or little damage with arrows
* @stumper66 fixed enchanted books now work properly with custom drops
* @lokka30 updated MicroLib shaded dep
* @lokka30 cleaned up MC1.17Compat class

***

## v3.0.3 b439

* @stumper66 made minLevel and maxLevel casing possible in customdrops.yml
* @stumper66 fixed shields not blocking damage in certain scenarios
* @lokka30 updated MicroLib & bStats inbuilt deps.
* @lokka30 fixed old groupid being used in inbuilt pom.xml

***

## v3.0.0 b425

* New modular rules system. Make your own customized rule set as broad or as specific as you want. How it works:
  * Modify the default rules to apply general rules
  * Create custom rules by creating conditions that must be met to apply the rule. Once met then apply any number of
    constraints, attributes, modifiers and more
  * Create and pick rule presets to avoid repetitiveness and to ease readability.
* New Levelled Spawners. Use a command to create a spawner that creates LevelledMobs only in a specific level or range
  of levels.
* New mob commands. Run commands when a mob is killed. Can have specific conditions and placeholders are provided
* New customdrops features: drop mob heads, custom mob head textures, overall chance, player caused only option
* New option sunlight-intensity: have mobs burn in the daylight faster
* Customized tiered coloring scheme for nametags
* New levelling scheme: blended levelling. Combines y levelling and spawn distance levelling
* Custom names. Apply a list of names to be randomly used on a particular mob. Apply a name template to all entities or specific entities that utilizes placeholders.
* Biome conditions. Make rules that apply to specific biomes
* Allowed worldguard regions. Make rules apply to only certain worldguard regions
* Spawn distance levelling can now be manually set to a specific coordinate rather than the world spawn location
* Y-levels. Make a rule apply only above or below a specific y level coordinate.
* Plugin conditions. Have rules only apply to mobs from specific 3rd party plugins.
* Attribute multipliers use a different formula which should have more predictable results
* Custom mob groups. Create a defined list of mobs then make rules apply or excluded from them
* Baby mob rule separation. You can define separate rules / behaviors for baby mob variants
* Attributes are applied differently so that the base values are not overwritten. This means that mobs with different variants of health, etc are preserved such as with horses and other tamable mobs.
* Added PAPI support for %level% and %displayname%

## v2.5.5 b407

### Notes:

* Summary: **Fix for errors relating to fine-tuning section**
* Testing depth: **Low**
* Configuration changes: **None**

### Changelog:

* Fixed and improved the Async Nametag Auto Update Task
  * [stumper66](https://www.spigotmc.org/members/stumper66.1118435/) Fix for errors when setting min and max level to the same value under the fine-tuning section
  * [stumper66](https://www.spigotmc.org/members/stumper66.1118435/) Removed hard-coded max health limit of 2024 and now uses the spigot config value instead

***

## v2.5.1 b389

## v2.5.4 b401

### Notice:

The LevelledMobs team has been working on **LevelledMobs 3**! This update will significantly extend the capability and
customisability of the plugin, through adding a new major configuration system called the **Rules System**. With such a
large new feature, we need help to test it - if you're willing to help,
please [join our Discord](https://discord.io/arcaneplugins) and let us know. Thanks :)

### Notes:

* Summary: **Improvement and fix for the Async Nametag Auto Update Task**
* Testing depth: **Low**
* Configuration changes: **None**

### Changelog:

* Fixed and improved the Async Nametag Auto Update Task
  * [lokka30](https://www.spigotmc.org/members/lokka30.828699/) fixed a bug that caused errors when players teleported
    to different worlds whilst the task analysed locations. Thank you to [MrMineToons](https://github.com/MrMineToons)
    for reporting this!
  * [lokka30](https://www.spigotmc.org/members/lokka30.828699/) rearranged some code in the task to make it cleaner to
    see and work on.
* [lokka30](https://www.spigotmc.org/members/lokka30.828699/) (IntelliJ, really) made general code cleanup concerning
  unnecessary `toString()` calls for enum values

***

## v2.5.1 b389

### Announcement:

* The LevelledMobs team has been working on **LevelledMobs 3**! This update will significantly extend the capability and
  customisability of the plugin, through adding a new major configuration system called the **Rules System**. With such
  a large new feature, we need help to test it - if you're willing to help,
  please [join our Discord](https://discord.io/arcaneplugins) and let us know. Thanks :)

### Notes:

* This update has been tested to a moderate extent - thank you, @Oathkeeper for your generous testing!
* No configuration changes this update.

### Changelog:

* Fixes missing `=` symbol in the code, that previously made max level mobs impossible (@Asttear)
* Removed Validate usage (@stumper66, @lokka30)
* Fixes EntityTameListener not levelling mobs properly due to a value of `-1` (@stumper66)
* Fixes console error concerning death nametag (@Asttear (reporter), @lokka30)

***

## v2.5.0 b372

### Notes:

#### It's not actually a troll update.

At first, we intended to release this update with only improvements to the existing code - we wouldn't really have a
changelog that would make anyone happy, so we nicknamed it the 'troll' update whilst in development.

Instead, we have not only improved the code by quite a significant margin, but fix a bunch of bugs, and added a few new
features.

In the coming updates 2.6 and 2.7, we plan on adding some *significant* stuff to this plugin. This update was
essentially a stepping stone for them - the code for LM was too messy and becoming a bit of a headache to work with, so
here's 2.5, which aims to fix most of the issue. :)

#### Developers!

* The LevelInterface is a new (and *far* easier) way for developers of other plugins to directly interact with
  LevelledMobs and the decisions it makes.
* Our hope is that this will unlock new possibilities with LevelledMobs, such as 'addons' and other integrations. We
  plan to expand on this class over time.
* Three new events have been added to LevelledMobs - check the changelog below if you're interested.

### Changelog:

#### Added LevelInterface

* Improves a lot of messy code, relocated from other classes, primarily LevelManager.java.

#### New Events

* Another treat for other developers: LM now has three new events!
  * MobPreLevelEvent
    * Called when a mob is being considered for levelling.
  * SummonedMobPreLevelEvent
    * Called when a summoned mob is being considered for levelling (through `/lm summon`).
  * MobPostLevelEvent
    * Called when a mob has been levelled. Unlike the two events above, this one is not cancellable, since the event is
      called *after* LM has tampered with the entity.

#### Custom Drops Improvements

by @stumper66

* Added new attributes: equipped, priority, maxdropgroup
* Added item flags to custom drops.
* Fixed null reference error when using summon command and you get a baby mob
* Code cleanup: removed MigrateBehavior enum, cleaned up customdrops attribute processing
* Fixed a bunch of other bugs.

#### Update Checker Improvements

by @lokka30

* Added customisable multiline messages for update checker.
* Update checker now informs players with permission (OP by default) when they join, if an LM update is available. Can
  be toggled in messages.yml
* Allowed toggle of sending update checker messages to console in messages.yml
* Added permission 'levelledmobs.receive-update-notifications', self explanatory.

#### Code Cleanup

A *lot* of code cleanup was done for this release. Helps us and other developers out :)

* Replaced isSpawnerKey with spawnReasonKey. This also affected the 'SpawnReason.REINFORCEMENTS' level determination.
* Renamed current events to contain 'PRE', more fitting as they are called *before* the mobs are actually levelled. Will
  add a 'POST' event soon.. hopefully.
* Renamed instance variable in ChunkLoadListener.java
* CreatureSpawnListener upgrades:
  * Renamed to EntitySpawnListener.java, more fitting with the new changes.
  * Cleaned up the event handlers
  * Cleaned and relocated the messy 'processMobSpawn' method, its code is now split in both LevelInterface.java and
    LevelManager.java, mainly LevelManager.
* MythicMobsListener now attaches a 'noLevelKey' persistent data NamespacedKey to a mob, telling LM not to level it.
  This will stick with the mob over restarts
* Removed excess debug settings, simply using 'debug entity damage' and 'debug misc' instead.
* Other general code cleanup.

#### Other Changes

* @stumper66 added the `/nodrops` option to the `kill all` subcommand
* @lokka30 made it so Guardians now have their attack damage modified
* @lokka30 patched the concurrent modification exception (Reported by @oathkeeper)
* @lokka30 made improvements to the entity nametag validity issue
* @lokka30 fixed how 'un-levelling' a mob didn't revert its attributes
* @lokka30 added 'DebugType', a universal enum for debug logging
* @lokka30 added nullability annotations to a bunch of methods
* @lokka30 made it so skipped death message handling if initial death message is null
* @stumper66 fixed missing specified defaults in LevelManager.java#524 (Thanks to GetSirius55 for reporting)
* @stumper66 nametag update task now checks for unlevelled mobs and levels them if applicable
* @stumper66 updated customdrops.yml and settings.yml with new comments and more
* @stumper66 added new debug enum ENTITY_TRANSFORM_FAIL
* @stumper66 shortened debug messages
* @stumper66 corrected spacing issues in customdrops.yml and settings.yml
* @stumper66 changed nametag updater task to only apply levels to formallly unlevelled baby mobs
* @stumper66 added debug type: ENTITY_MISC. So far only used for the above condition

***

## v2.4.2 b348

### Changelog:

* I hope this fixes the update checker. (lokka30)

***

## v2.4.1 b347

### Notes:

- All changes by **stumper66** this update - with thorough testing by **Oathkeeper**. Thank you both!

### Changelog:

- fixed customdrops not dropping in certain scenarios
- fixed dup bug with donkeys with chests
- fixed customdrops that use groupIds not randomizing the list properly
- summon command checks for 2 block height room before summoning
- defaults not working in certain scenarios
- more customdrop related bugs squashed

***

## v2.4.0 b332

### Notes:

* A huge thank-you to Stumper and Oath for developing and testing this update!

### Changelog:

- Huge amount of code improvements. Significant things to note is the package has changed from '
  io.github.lokka30.levelledmobs' to 'me.lokka30.levelledmobs' to suit my other new plugins. (lokka30)
- Renamed default inbuilt data files (lokka30)
- Removed isLevelledKey, instead use LevelInterface#isLevelled(LivingEntity) (lokka30)
- Improved Utils#isBabyMob and the methods which use it. Should allow for more 'BABY' variants of entities to be
  configured. (lokka30)
- if raw meat has been specified in customdrops and the mob is killed by fire, the drop becomes cooked meat (stumper66)
- overhauled custom drops debug messages (stumper66)
- added a defaults section to customdrops.yml. Eliminates the need to specify repeated attributes constantly (stumper66)
- customdrops.yml - Added Default Config Comments (Oathkeeper)
- groupIds in customdrops.yml are completely strings now (was internally an int in one place) (stumper66)
- older versions of customdrops.yml will now be migrated. No more resets (stumper66)
- fixed customdrops issues, settings migration issues from old versions (stumper66)
- moved chunkload listener to it's own class and will unload itself if disabled (stumper66)
- customdrops.yml: when utilizing a drop-table and individual entities, the drop-table would get used but the individual
  entity would be ignored (stumper66)
- custom drops improvements. When override is used on an entity in customdrops, it will no longer remove saddles and any
  chest contents from a chested animal. Also fixed a bug where override was not applying to mobs that were levelled (
  stumper66)
- when a levelled animal mob is killed, any chested items, armor, etc will not be multiplied anymore, only vanilla
  drops (stumper66)

***

## v2.3.1 b318

### Notes:

* This update makes some critical fixes to a few bugs left in v2.3.0. Please update as soon as possible.
* This update was not thoroughly tested. Production servers are recommended to quickly test this version beforehand.

### Changelog:

* @stumper66 fixed MythicMobs compatibility
* @stumper66 fixed call stack with null reference.
* @stumper66 fixed y-level and spawn-distance-levelling.

***

## v2.3.0 b315

### READ! Very Important:

#### CustomDrops users!

* If you do not use custom drops, ignore this!
  * Your old customdrops.yml file has been **reset**, and all of the contents of that file have been moved to **
    customdrops.yml.old**. Custom drops have received a bunch of new features and fixes, especially a plethora of
    helpful comments to assist you in configuring the system. **You must manually transfer your old custom drops if you
    want to keep them.** In addition, **the customdrops setting has been disabled in settings.yml, you must re-enable
    this if you want customdrops to work.**
  * Please, pay close attention when migrating these.
  * If you have any issues with this update, please join the Discord and our support team will respond to you as soon as
    they can.

### Notes:

* Updated `settings.yml` - the auto update checker will automatically update it for you, unless you do it manually
* Updated `customdrops.yml` - see 'IMPORTANT' section above.
* Overhauled the config comments for settings.yml, messages.yml and customdrops.yml! This should significantly help
  users in configuring the files.

### Changelog:

#### Config Comments Overhaul!

* @Oathkeeper led a big change to the config files in making the config comments purely amazing! These should assist
  people in configuring the files far more than previously. @stumper66 and @lokka30 assisted in this.
* @Oathkeeper also fixed a bunch of spelling and grammar errors in settings.yml.

#### 1.16+ RGB Hex Colors Support!

* @lokka30 updated MicroLib to support RGB hex color codes, LevelledMobs now uses this system.
  * Servers running 1.16 or newer may use hex color codes anywhere you can use standard color codes too. :)
  * RGB hex colors are a 1.16 feature from Minecraft, we can't add it to older versions.
  * If your server is older than 1.16, don't worry, this feature won't disturb compatibility with your server in any
    capacity.

#### Other new features!

* @stumper66 added the optional `/override` argument to `/lm summon`, which overrides the min & max level limits.
* @lokka30 added compatibility with the DangerousCaves plugin (untested). Thanks to @gaugt980131gg2 for suggesting this.

#### Bug Fixes!

* @Shevchik supplied the code to fix the null name tags issue. Thanks!
* @stumper66 fixed a bunch of issues relating to the config migrator system.
* @lokka30 suppressed `IllegalArgumentException` for `LevelManager#updateNametag`. Thanks to Phthiscicus for reporting
  the error.
* @lokka30 fixed the nametag async task for not accounting for players quitting the server.
* @lokka30 fixed custom drops no being reloaded properly with the `/lm reload` command.
* @lokka30 fixed nametags not being updated force-updated when players teleport within the same world.
* @lokka30 suppressed `ConcurrentModificationException` for `LevelManager#updateNametag`. Thanks to the handful of users
  that reported the error.
* @lokka30 fixed a `NullPointerException` when WorldGuardManager was unable to provide a region set.

#### Code Improvements!

* @stumper66 improved the direction code in the `/lm summon` command.
* @stumper66 improved the enchantment code for the Custom Drops system.
* @stumper66 made `drops.yml` and `attributes.yml` no longer copy inside the config directory, it now runs hidden inside
  the plugin itself!
* @lokka30 added author and contributors to each class.
* @lokka30 made external compatibilities now handled by **ExternalCompatibilityManager**. The relevant part of the
  settings file has also been cleaned up.
* @lokka30 and @stumper66 worked on the EntityTameListener code. Taming mobs now considers the 'don't tame levelled
  mobs' setting.
* @lokka30 made armor stands, item frames, paintings and dropped items force-blocked in the code so that they should be
  impossible to level.
* @lokka30 removed the 'nametagContainsHealthPlaceholders' code, since it is too difficult to update and it is not
  necessary to keep anyways.
* @lokka30 replaced the rick-roll at the bottom of settings.yml - you brilliant people don't deserve that ;)
* @stumper66 and @lokka30 made a bunch of other code improvements - there are too many small changes to list.

***

## v2.2.0

### Notes:

* This update includes file changes, although LevelledMobs' file migrator can take care of this for you automatically -
  just start up the new version with your old files and it'll update your configs!

### Changelog:

#### Nametag Enhancements
* New `%tiered%` placeholder which changes color depending on the level of the mob (green -> red), suggested by
  @ItsGamingSoni (stumper66)
* Disable-able nametags! Set `creature-nametag` to `disabled` or disable an individual entity's nametag
  in `entity-name-override` too. (stumper66)

#### Custom Drops Enhancements
* Added customisable item meta and attributes, suggested by @Noiverre (stumper66)
* Added different level increase rates for different mob types, suggested by @Oathkeeper (stumper66)
* Added unsafe enchantment support (e.g. sharpness 300), suggested by @Noiverre (lokka30)

#### Misc Improvements
* Now allowing translation of baby zombie's nametag in specific, suggested by @bvssy (lokka30)
* Added config option `assert-entity-validity-with-nametag-packets`, this by default stops nametags being updated on
  dead mobs. Stops plugins such as ViaBackwards from complaining, but therefore no longer shows '0 HP' on mobs that have
  just been killed. Thanks to @MelaniumAS for reporting the ViaBackwards issue. (lokka30)
* Nametag update task timer is now configurable (lokka30)

#### Bug Fixes
* Fixed 1.14 and 1.15 incompatibility due to entity groups in code (stumper66)
* Fixed `Utils#getSupportedServerVersions` not including `1.14` (lokka30)
* Fixed chunk load re-level issue (stumper66)

***

## v2.1.0

**WARNING** This update includes file changes! Please see the Notes section below. If you do not update your files then
the plugin will malfunction!

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
