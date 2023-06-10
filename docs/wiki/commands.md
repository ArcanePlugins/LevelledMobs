<details>
<summary>Page Description</summary>
This page lists and describes all of LevelledMobs' commands.
</details>

# 👉 LevelledMobs Commands

## `/lm`

- **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
    - Customisable messages not yet implemented on this base command.

- **Description:**
    - Manage the LevelledMobs plugin on your server in various ways, from reloading its configuration to creating a custom levelled mob spawn egg.

- **Required Permissions:**
    - `levelledmobs.command.levelledmobs`

- **Aliases:**
    - `/lvlmobs`
    - `/levelledmobs`
    - `/leveledmobs`


## `/lm about`

- **Description:**
    - View information about the installed version of the plugin.

- **Required Permissions:**
    - `levelledmobs.command.levelledmobs.about`
    - Requires additional permission(s) from parent command, `/lm`.

## `/lm backup`

- **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
    - Command functionality not yet implemented.

- **Description:**
    - Copies LevelledMobs' configuration files into a compressed zip file, placed in a `backups` folder (at `plugins/LevelledMobs/backups`).
    - If the `backups` folder does not exist, it will be created automatically.

- **Required Permissions:**
    - `levelledmobs.command.levelledmobs.backup`
    - Requires additional permission(s) from parent command, `/lm`.

## `/lm confirm`

- **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
    - Customisable messages not yet implemented.

- **Description:**
    - Allows users to confirm actions from potentially dangerous LM commands, such as some routines in `/lm routine`.

- **Required Permissions:**
    - `levelledmobs.command.levelledmobs.confirm`
    - Requires additional permission(s) from parent command, `/lm`.

## `/lm egg`

- **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
    - This command has not yet been re-designed for LM4 implementation.
    - Command functionality not yet implemented.
    - 'Example' section for this command's documentation is not yet provided.

- **Description:**
    - Create and customise spawn eggs, which summon levelled mobs of your chosen specifications, from the level to the entity type and much more.

- **Required Permissions:**
    - `levelledmobs.command.levelledmobs.egg`
    - Requires additional permission(s) from parent command, `/lm`.

## `/lm help [chapterPath]`

- **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
    - Most help chapters have not been written.
    - Customisable messages not yet implemented.

- **Description:**
    - Sends the user a list of available LevelledMobs commands and also a list of URLs providing documentation and support. It's like a mini in-game Wiki!
    - Running `/lm help` without any arguments will display the home page. You are encouraged to open the chat box and click on the links on the chat interface to navigate through LM's help system.
    - For more information on what the optional `chapterPath` argument does, check the *Details for Nerds* section below.

<ul><li><details>
<summary><b>Details for Nerds:</b></summary>

- If you are a crazy power user ... here is how to use the `chapterPath` argument:
    - You can also manually type the chapter path of the help chapter you want to view by specifying it in the `chapterPath` argument (`/lm help [chapterPath]`).
    - For example, running `/lm help /lm about` will display the `about` chapter (which has the path `home` -> `/lm` -> `about`).
        - You do not specify `home` in `chapterPath`; LM does this already for you.
    - It also supports adding the page number at the end of the chapter path, such as `/lm help /lm 2` to view page 2 of the `/lm` chapter.
- A further explanation on how the chapters work:
    - You can think of help chapters in a tree structure, all which originate from the `home` chapter, which you can think of as the *root* of the chapter tree.
    - Each chapter may have sub-chapters, which you can think of as *branches* on the tree.
        - The `home` chapter has a few sub-chapters, such as `/lm` and `support`.
        - The `/lm` chapter has many sub-chapters, one for each of `/lm`'s sub-commands. 

</details></li></ul>


- **Required Permissions:**
    - `levelledmobs.command.levelledmobs.help`
    - Requires additional permission(s) from parent command, `/lm`.

## `/lm kill <entities> <world> [fineTuningParams]`

- **⚠️ Notice:**
    - **Note:** You are suggested to learn [how Minecraft's `/kill` command works](https://minecraft.fandom.com/wiki/Commands/kill) before using LM's `/lm kill` command.
    - **Note:** If your main world is named `world`, type `overworld` instead for the `<world>` argument. See the *Details for Nerds* section if you are intrigued by that.

- **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
    - Customisable messages not yet implemented.
    - 'Example' section for this command's documentation is not yet provided.

- **Description:**
    - Kills levelled mobs on the server.
    - This command is essentially the same as [Minecraft's `/kill` command](https://minecraft.fandom.com/wiki/Commands/kill), however, it only kills levelled mobs, and has various default safety measures.
    - This command's safety measures help you avoid killing entities which matter to your players, such as those which are tamed or nametagged. You can override these protections using the `fineTuningParams` argument, which is explained below.
    - Firstly, specify an entity selector for the `entities` argument, such as:
        - `@e` (all entities)
        - `@e[distance=..10]` (entities within 10 blocks of you)
        - `@e[type=Zombie]` (all zombies)
    - Secondly, specify the name of the world you want to kill these mobs in:
        - Again, use `overworld` instead of `world`.

<ul><li><details>
<summary><b>Details for Nerds:</b></summary>

- Why do I need to use `overworld` instead of `world` for the `<world>` argument?
    - It's how Minecraft handles the world names internally. This is one of the very few quirks of [Brigadier](https://github.com/Mojang/brigadier) - Mojang's official command library - which most other Bukkit plugins don't use.
    - For example, to kill all zombies in a world `world`, you would run `/lm kill @e[type=zombie] overworld`.
    - *We are unsure what happens if you have changed the default world name from `world` to something else. Minecraft could still ask you to type `overworld` instead of it, or maybe it won't.*

</details></li></ul>
    

- **Required Permissions:**
    - `levelledmobs.command.levelledmobs.kill`
    - Requires additional permission(s) from parent command, `/lm`.

## `/lm reload`

- **Description:**
    - Reloads LevelledMobs config files.

- **Required Permissions:**
    - `levelledmobs.command.levelledmobs.reload`
    - Requires additional permission(s) from parent command, `/lm`.

## `/lm routine`

- __**⚠️ Notice:**__
    - This potentially dangerous command is only suitable for use by advanced users.

- **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
    - Some routines are yet to be fully implemented. 

- **Description:**
    - Run the specified routine. This is a potentially dangerous command (depending on which routine is used). You are advised to not use this without the instruction of a LevelledMobs maintainer.
    - Routines are basic utility commands, grouped under `/lm routine` as they are rarely used.

<ul><li><details>
<summary><b>Available Routines:</b></summary>

- `compatibility`
    - **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
        - Translatable messages are yet to be implemented.

    - **Full Command:** `/lm routine compatibility`

    - **Description:**
        - Run compatibility diagnostic tool.
        - The output of this command is useful for LM maintainers when diagnosing issues.

    - **Required Permissions:**
        - `levelledmobs.command.levelledmobs.routine.compatibility`
        - Requires additional permission(s) from parent command, `/lm routine`.

    - **Potentially Dangerous:** No; as it only displays information about the server.

- `halt`
    - **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
        - This routine is yet to be implemented.

    - **Full Command:** `/lm routine halt`

    - **Description:**
        - Toggle whether LM will automatically trigger functions.
        - This will effectively disable the Functions System.
        - Halts do not persist over restarts.

    - **Required Permissions:**
        - `levelledmobs.command.levelledmobs.routine.halt`
        - Requires additional permission(s) from parent command, `/lm routine`.

    - **Potentially Dangerous:** Yes; as it effectively disables all Functions from running as intended.

- `reload-commands`
    - **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
        - Translatable messages are yet to be implemented.

    - **Full Command:** `/lm routine reload-commands`

    - **Description:**
        - Reload all of LevelledMobs' commands.

    - **Required Permissions:**
        - `levelledmobs.command.levelledmobs.routine.reload-commands`
        - Requires additional permission(s) from parent command, `/lm routine`.

    - **Potentially Dangerous:** Yes; as it is considered to cause undefined behaviour by CommandAPI.

- `reset-configs`
    - **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
        - This routine is yet to be implemented.

    - **Full Command:** `/lm routine reset-configs`

    - **Description:**
        - Backups, __resets__, and reloads LevelledMobs configuration files.
    - **Required Permissions:**
        - `levelledmobs.command.levelledmobs.routine.reset-configs`
        - Requires additional permission(s) from parent command, `/lm routine`.
    - **Potentially Dangerous:** Yes; as it will make the default configuration immediately apply, which means that your customised changes are no longer in effect.

- `test`
    - **Full Command:** `/lm routine test`

    - **Description:**
        - Contains some random code of whatever a LevelledMobs maintainer was last testing.
        - __This could do some crazy unusual stuff on your server, don't use it.__
    - **Required Permissions:**
        - `levelledmobs.command.levelledmobs.routine.test`
        - Requires additional permission(s) from parent command, `/lm routine`.
    - **Potentially Dangerous:** Yes; as it will run some random stuff on your server which was intended only to be used by LM maintainers.

- `unlevel-all [world]`
    - **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
        - Translatable messages are yet to be implemented.

    - **Full Command:** `/lm routine unlevel-all [world]`

    - **Description:**
        - Unlevels ALL mobs (without any protections!) on your server, or only in a specified world.
    - **Required Permissions:**
        - `levelledmobs.command.levelledmobs.routine.unlevel-all`
        - Requires additional permission(s) from parent command, `/lm routine`.
    - **Potentially Dangerous:** Yes; as it will unlevel the mobs, without any way to directly reverse this action.

</details></li></ul>

- **Required Permissions:**
    - `levelledmobs.command.levelledmobs.routine`
    - Requires additional permission(s) from parent command, `/lm`.

## `/lm spawner`

- **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
    - This command has not yet been re-designed for LM4 implementation.
    - Command functionality not yet implemented.
    - 'Example' section for this command's documentation is not yet provided.

- **Description:**
    - Create and customise spawners which spawn levelled mobs, from the allowed level range to the period of spawning cycles and more.

- **Required Permissions:**
    - `levelledmobs.command.levelledmobs.spawner`
    - Requires additional permission(s) from parent command, `/lm`.

## `/lm summon`

- **Description:**
    - Summons a levelled mob of chosen specifications, similar to Minecraft's `/summon` command.
    - See the `/lm summon entity` section of this page to learn how to summon generic entities by their type (e.g. `zombie`).
    - Alternatively, see the `/lm summon custom` section of this page to learn how to summon custom LM entities by their ID (e.g. `alpha-zombie`).

- **Required Permissions:**
    - `levelledmobs.command.levelledmobs.summon`
    - Requires additional permission(s) from parent command, `/lm`.

### `/lm summon entity ...`

- **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
    - Customisable messages not yet implemented for entity type summoning.

- **Full Command:** `/lm summon entity <entityType> <amountRange> <levelRange> <minLevel> <maxLevel> <location>`

- **Description:**
    - Summons a levelled mob by the chosen `entityType` and other specifications.
    - Enter an Entity Type in the `<entityType>` argument, such as `ZOMBIE` or `WITHER_SKELETON`. This is the type of mob that will be summoned.
    - Enter however many mobs you want to be summoned, such as `1`, in the `<amountRange>` argument.
        - You can also specify a random amount range, such as `1..5` to spawn in 1 to 5 mobs.
    - Similar to the amount range you filled out before, specify a level (or range of levels) that the mob will have, in the `<levelRange>` argument.
    - Enter the minimum level of the mob in the `<minLevel>` argument, vice-versa with `<maxLevel>`.
        - You may be confused why you need to enter a min/max level when you already have told LM what level you want the mob to have. This is because LM requires the min and max level fields to be set on all levelled mobs so that it knows how 'strong' the mob is relative to other mobs - known as the level ratio. If you have a mob which has `minLevel=1, maxLevel=100, level=50` and another mob which has `minLevel=1, maxLevel=10, level=5`, they will have similar level ratios (as `levelRatio = (level - minLevel) / (maxLevel - minLevel)`).
    - Specify the coordinates of wherever you want the mobs to spawn for the `<location>` argument. You can write `~ ~ ~` to make it spawn in at your current coordinates. This is identical to how [Minecraft's summon command](https://minecraft.fandom.com/wiki/Commands/summon) does locations, so read up on that if you are confused.

<ul><li><b>Example:</b>

| ...                 | `<entityType>` | `<amountRange>` | `<levelRange>` | `<minLevel>` | `<maxLevel> ` | `<location>` |
|---------------------|----------------|-----------------|----------------|--------------|---------------|--------------|
| `/lm summon custom` | `Zombie`       | `1`             | `53`           | `1`          | `100`         | `7 20 100`   | 

This example spawns in a Zombie...

- with an amount of 1 (so only one zombie spawns);
- with a level of 53;
- with a min level of 1;
- with a max level of 100;
- at the location `7 20 100` in the world of whoever/whatever is executing the command.

</li></ul>


### `/lm summon custom ...`

- **⚠️ Notice for Testers:** *(last updated 20th Jan 2023)*
    - Command functionality not yet implemented for custom entity summoning.

- **Full Command:** `/lm summon custom <customEntityId> <amountRange> <levelRange> <minLevel> <maxLevel> <location>`

- **Description:**
    - Summons a levelled mob by the chosen `entityType` and other specifications.
    - Enter a Custom Entity ID in the `<customEntityId>` argument. You can use the ID of any custom entity you create in the LM configuration.
    - Enter however many mobs you want to be summoned, such as `1`, in the `<amountRange>` argument.
        - You can also specify a random amount range, such as `1..5` to spawn in 1 to 5 mobs.
    - Similar to the amount range you filled out before, specify a level (or range of levels) that the mob will have, in the `<levelRange>` argument.
    - Enter the minimum level of the mob in the `<minLevel>` argument, vice-versa with `<maxLevel>`.
        - You may be confused why you need to enter a min/max level when you already have told LM what level you want the mob to have. This is because LM requires the min and max level fields to be set on all levelled mobs so that it knows how 'strong' the mob is relative to other mobs - known as the level ratio. If you have a mob which has `minLevel=1, maxLevel=100, level=50` and another mob which has `minLevel=1, maxLevel=10, level=5`, they will have similar level ratios (as `levelRatio = (level - minLevel) / (maxLevel - minLevel)`).
    - Specify the coordinates of wherever you want the mobs to spawn for the `<location>` argument. You can write `~ ~ ~` to make it spawn in at your current coordinates. This is identical to how [Minecraft's summon command](https://minecraft.fandom.com/wiki/Commands/summon) does locations, so read up on that if you are confused.

<ul><li><b>Example:</b>

| ...                 | `<customEntityId>` | `<amountRange>` | `<levelRange>` | `<minLevel>` | `<maxLevel> ` | `<location>` |
|---------------------|--------------------|-----------------|----------------|--------------|---------------|--------------|
| `/lm summon custom` | `Crawler`          | `2..3`          | `20..100`      | `1`          | `100`         | `~ ~3 ~`     | 

This example spawns in a custom entity (`Crawler`)...

- in any amount from 2 to 3 (random);
- with any level from 20 to 100 (random);
- with a min level of 1;
- with a max level of 100;
- with a location that is 3 blocks above whoever/whatever is executing the command.

</li></ul>
