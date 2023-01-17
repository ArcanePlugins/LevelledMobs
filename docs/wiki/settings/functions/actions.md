<details>
<summary>Page Description</summary>
This page lists and describes the various Actions which are available in LevelledMobs' "Functions System".
</details>

# 🔁 Actions in the Functions System

Actions are components which 'do things' in the [Functions System](functions.md).

It can be thought of something that can be *ran* to achieve a result on the server.  For example, the 'Broadcast Message to Server' Action gives you the ability to specify a message to broadcast to all online players in their chat box.

Actions are defined within [Processes](functions.md#Processes). Whenever the parent Process is ran, assuming the [Conditions](conditions.md) within the same Process are met, all of the Actions in the Process are ran.  

## Actions List

#### Add Drop Tables

Adds a list of drop tables to an entity.

##### Example:

```yaml
- action: 'add-drop-tables'
  ids: ['cool-drop-table', 'another-drop-table']
```

> Adds drop tables 'cool-drop-table' and 'another-drop-table' to the entity.

#### Add NBT Tag

Merges a list of NBT tags to an entity.

Requires [NBT API](https://www.spigotmc.org/resources/nbt-api.7939/).

##### Example

```yaml
- action: 'add-nbt-tag'
  tags: ['...', '...', '...']
  # Alternatively...
  tag: '...'
```

> **Note:** This example is not complete, we would like to add some real NBT tags in there later on.

#### Broadcast Message to Nearby Players

Broadcasts a given message to all players who are nearby the location context when this action is ran.

By default, the range is 32 blocks.

You can set a permission required for each player to receive the message.

##### Example

```yaml
- action: 'broadcast-message-to-nearby-players'
  message:
    - '%prefix-info% First line of the broadcast message.'
    - '%prefix-info% Another line.'
    - '%prefix-info% Entity: Lvl.%entity-level% %entity-name%'
  
  # Optional settings:
  required-permission: 'some.permission.here'
  range: 32.0
```

> Broadcasts the multi-line message to all online players who are within 32 blocks of the location context of which the action is ran in. Each player requires the permission above to receive the message.

#### Broadcast Message to Server

Broadcasts a given message to all online players on the server.

You can set a permission required for each player to receive the message.

##### Example

```yaml
- action: 'broadcast-message-to-server'
  message:
    - '%prefix-info% First line of the broadcast message.'
    - '%prefix-info% Another line.'
  
  # Optional settings:
  required-permission: 'some.permission.here'
```

> Broadcasts the multi-line message to all online players on the server who have the required permission above.

#### Broadcast Message to World

Broadcasts a given message to all online players in the world context when this action is ran.

You can set a permission required for each player to receive the message.

##### Example

```yaml
- action: 'broadcast-message-to-world'
  message:
    - '%prefix-info% First line of the broadcast message.'
    - '%prefix-info% Another line.'
    - '%prefix-info% Your world: %player-world%'
    - '%prefix-info% Entity: Lvl.%entity-level% %entity-name%'
  
  # Optional settings:
  required-permission: 'some.permission.here'
```

> Broadcasts the multi-line message to all players who are online in the world of the action's run context. Each player requires the given permission to receive the message.

#### Exit All

Exits the current Function and all of the other Functions which have called it.

If you call a Function with another Function, and you want the inner Function to immediately stop both of them from running, then you use an `exit-all`.

##### Example

```yaml
- action: 'exit-all
```

For instance, if we call Function 'Z' from Function 'Y' which was originally called by Function 'X', and if we do an `exit-all` inside of 'Function Z', then LevelledMobs will immediately terminate functions 'Z', 'Y', and 'X'.

In contrast, if you used an `exit-function` instead of an `exit-all` action, then LevelledMobs will only terminate 'Function Z', and Functions 'Y' and 'X' would continue running.

#### Exit Function

Terminates the current Function, so that it no longer runs any more Processes. It also terminates the current Process running within the Function.

The difference between 'Exit Function' and 'Exit Process' is that 'Exit Function' terminates the logic flow at the Function level, which therefore prevents any other Processes running within that Function (only in that particular instance of the Function being called). 'Exit Process' will stop the current Process from running, though it will just tell LevelledMobs to run whatever the next Process is.

##### Example

```yaml
- action: 'exit-function'
```

#### Exit Process

Terminates the current Process which is running, and LevelledMobs advances to whatever the next Process is in the particular Function which the Process exists within.

It's like a `return` in programming; it stops the Process in its tracks and returns back to wherever it came from.

This is the lowest-level 'Exit' Action available, see [Exit Function](#Exit-Function) and [Exit All](#Exit-All) for ones which have more power. ⚡️

##### Example

```yaml
- action: 'exit-process'
```

#### Remove Drop Tables

If the entity has any of the drop tables listed, then they are removed.

##### Example

```yaml
- action: 'remove-drop-tables'
  ids: ['DropTableA', 'DropTableB', 'DropTableC']
```

#### Run Function

Runs a Function, within another Function.

##### Example

```yaml
- action: 'run-function'
  otherFuncId: 'Entity-Leveller'
```

> Calls a Function with the ID 'Entity Leveller'. Once the 'Entity Leveller' Function has finished, the current Function will resume whatever else it has to do. 

#### Set Buffs

##### Buff Types

> **Note:** Learn more about Minecraft's inbuilt attributes [here](https://minecraft.fandom.com/wiki/Attribute).

| Buff Type                     | Description                                                     |
|-------------------------------|-----------------------------------------------------------------|
| `ARMOR_TOUGHNESS`             | Represents Minecraft's attribute.                               |
| `ATTACK_DAMAGE`               | Represents Minecraft's attribute.                               |
| `ATTACK_KNOCKBACK`            | Represents Minecraft's attribute.                               |
| `CREEPER_BLAST_DAMAGE`        | Adjusts the blast damage of Creepers.                           |
| `EXP_DROP`                    | Adjusts the EXP drop multiplier on death of the entity.         |
| `FLYING_SPEED`                | Represents Minecraft's attribute.                               |
| `FOLLOW_RANGE`                | Represents Minecraft's attribute.                               |
| `HORSE_JUMP_STRENGTH`         | Represents Minecraft's attribute.                               |
| `ITEM_DROP`                   | Adjusts the item drop multiplier on death of the entity.        |
| `KNOCKBACK_RESISTANCE`        | Represents Minecraft's attribute.                               |
| `MAX_HEALTH`                  | Represents Minecraft's attribute.                               |
| `MOVEMENT_SPEED`              | Represents Minecraft's attribute.                               |
| `RANGED_ATTACK_DAMAGE`        | Adjusts the attack damage of projectiles from ranged mobs.      |
| `SHIELD_BREAKER`              | This does not currently do anything due to a Bukkit limitation. |
| `ZOMBIE_SPAWN_REINFORCEMENTS` | Represents Minecraft's attribute.                               |

##### Example

```yaml
- action: 'set-buffs'
  buffs:
    - buff: 'health-boost'
      
      affected-entities:
        in-list: ['ZOMBIE', 'WITCH', 'WITHER']
        # Alternatively...
        not-in-list: ['CREEPER', 'ENDERMAN']
        
      types:
        in-list: ['MAX_HEALTH']
        # Alternatively... (we don't recommend using not-in-list here!!)
        not-in-list: ['ARMOR_TOUGHNESS']
        
      multiplier-formula: '1 + (%entity-level-ratio% * 0.75)'
    
      # Optional
      adjust-current-health: true
```

> Applies a health boost to the entity.
> 
> Buff: Just a name which shortly describes the purpose of the particular buff instance.
> 
> Affected Entities: A list of entity types which this buff should or shouldn't apply to.
> 
> Types: A list of buff types which this buff applies. Please avoid using `not-in-list` here. See [Buff Types](#Buff-Types).
> 
> Multiplier Formula: The value here means it can range from 1x (at min level) and 1.75x (at max level).
> 
> Adjust Current Health: An optional value, but we recommend keeping it `true` (default). When the max health is adjusted, this option makes it also adjust their current health to be the same proportion as it was before. Otherwise, you will increase the max health of mobs, and they will remain weak because their current health has not changed.

> **Note:** You can specify whatever name you want for each Buff, such as `health-boost`. It only serves as a reminder to you of what the purpose of that particular buff is.

#### Set Death Label Action

Adjusts the name of the entity used whenever they kill a player.

##### Example

```yaml
- action: 'set-death-label'
  formula: '[Lvl.%entity-level%](color=aqua) %entity-name%'
```

> Adds a level indication to the entity's name. So everyone can troll you about being camped by a
> pack of high-level skeletons at your base door. Or having a huge crater from a high-level creeper.

#### Set Drop Tables

Sets a list of drop tables to an entity. Overrides all existing custom drop tables on that entity;
see [Add Drop Tables](#Add-Drop-Tables) and [Remove Drop Tables](#Remove-Drop-Tables) to have
more control over that.

##### Example:

```yaml
- action: 'set-drop-tables'
  ids: ['cool-drop-table', 'another-drop-table']
```

> Sets drop tables 'cool-drop-table' and 'another-drop-table' to the entity.

#### Set Level

> **Note:** [Click here for a list of Levelling Strategies](levelling-strategies.md).

Documentation N/A

This is by far the largest Action available in LM4, and I am dreading the thought of documenting
this. 🥲

If you need to read this then please yell at lokka30 to work on it.

#### Set Packet Label

Applies a label to a mob, but it is done all client-side using Packets, so the actual nametag of the mob is not modified.

##### Visibility Methods

> **Note:** We recommend using `TARGETED` and `ATTACKED`.

| Visibility Method | Description                                                                  |
|-------------------|------------------------------------------------------------------------------|
| `TARGETED`        | The player is being targeted by the entity.                                  |
| `ATTACKED`        | The player is being attacked by the entity.                                  |
| `TRACKED`         | The player is being tracked by the entity.                                   |
| `MELEE`           | You are within melee range of the entity and you are directly looking at it. |
| `ALWAYS`          | Always display the label, even through obstructions.                         |

##### Example

```yaml
- action: 'set-packet-label'
  formula: '&bLvl.%entity-level%&f %entity-name%'
  visibility-methods: ['TARGETED', 'ATTACKED']
  visibility-duration: '5s'
```

> **Formula:** A piece of text which is parsed by LevelledMobs to 'colorize' and replace [Placeholders](context.md#Placeholders) in.
> 
> **Visibility Methods:** A list of [Visibility Methods](#Visibility-Methods) to use.
> 
> **Visibility Duration:** How long the label should display before expiring. (default: 5 seconds)

#### Set Permanent Label

> ⚠️ **Warning:** We **highly advise** that you do not use this type of label, and we suggest you use the [Set Packet Label Action](#Set-Packet-Label) instead. This one changes the server-side nametag of the entity to the label you set, rather than client-side (the latter is *much* better). **Using this Action makes permanent, irreversible changes to mobs.** It only exists for very niche use cases.

<details>
<summary>I understand I should use the Set Packet Label Action instead. But I am still curious.</summary>

Updates the actual nametag of the entity to display LevelledMobs' label on it.

It's a bad idea.

##### Example

```yaml
- action: 'set-permanent-label'
  formula: '&bLvl.%entity-level%&f %entity-type-formatted%'
```

</details>

#### Update Labels

Updates the labels of all entities involved in the context of the Action being ran. 

It is useful to run this Action whenever an entity's health changes, or a player teleports somewhere, so that they are viewing the latest information about the entity on its label.

##### Example

```yaml
- action: 'update-labels'
```

