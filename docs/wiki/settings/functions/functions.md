<details>
<summary>Page Description</summary>
This page describes how LevelledMobs' "Functions System" works, and how you can use it to make LevelledMobs operate the exact way you want it to with fine control.
</details>

# 🎛️ F️unctions System

LM4's staple feature is its Functions System, which allows  users to customise the logic that runs behind the scenes  down to whichever degree they wish. Fine logic adjustments can be accomplished easily using the Functions System.

The default [Settings](../settings.md) file uses the Functions System inside the `functions` area, with two goals:

1. Levels mobs! Applies levels, buffs, and labels to newly spawned entities, but only if they are considered 'levellable'.

2. Update labels of levelled mobs on certain events, such as whenever their health changes.

You could remove both of the functions in the [Settings](../settings.md) file, and find that LevelledMobs no longer does anything except have a presence by using its commands and whenever your server starts up or shuts down. It's an integral part of how the plugin operates.

## Components

### Functions

The Functions System is comprised of any amount of Functions defined by the user in the [Settings](../settings.md) file (unless modified by a third-party plugin).

A Function can be thought of as a 'procedure': it can be repeatedly called by the name (identifier) you give to the Function.

Each Function contains its own collection of [Processes](#Processes), each of which take part in doing something when the Function is called, such as 'adjust the movement speed of a mob if they are a Zombie'.

Each Function may contain a collection of [Triggers](triggers.md).

Functions can be called by other functions, and they can also be called by their respective Triggers.

<details>
<summary>Still confused?</summary>

Perhaps, this terrible real-life example could help you connect the dots:

Pretend of a function named 'make-toast'...

- We'll set two conditions:
    1. There is at least 1 loaf of sliced bread available
    2. The toaster is available
    - [Learn more about Conditions](conditions.md)

- If these conditions are met, we want these two actions to run:
    1. Put a slice of bread in the toaster
    2. Activate the toaster
    - [Learn more about Actions](conditions.md)

- We'll make this function run with the following trigger:
    - "On Person Wake-Up"
    - [Learn more about Triggers](triggers.md)

Now, whenever a person wakes up, the 'make-toast' function will be called. This function will make toast (by running both of the actions) if both of the conditions are met.

> We'll ignore the existence of [Processes](#Processes) for the sake of this example.

</details>

### Processes

Each [Function](#Functions) contains a collection of Processes, which can be thought of 'child procedures' which are called sequentially (in order of declaration) by the 'parent procedure', that being the Process's parent Function.

Each Process contains a collection of [Conditions](conditions.md), and a collection of [Actions](actions.md). You are encouraged to view those pages to learn what they do.

Processes allow you to make one function able to have multiple paths of logic. For example, a 'Level Entity' function can have three Processes defined:

1. "Check Not Levellable": establishes conditions required for any mob to be levelled

2. "Wither Levelling": provides special logic for levelling Withers, which use 'Wither Levelling' instead of 'Standard Levelling'.

3. "Standard Levelling": provides levelling functionality for all other entities.

This hypothetical Function would have a structure like this:

> - Function: `Level Entity`
>   - Process: `Check Not Levellable`: *Doesn't do any levelling if the entity is not hostile.*
>     - Condition: `Entity Type`: *Entity type must be a passive or neutral mob.*
>     - Action: `Exit Function`: *Don't proceed with running any other Processes in this Function.*
>   - Process: `Wither Levelling`: *Specific level behaviour for Withers.*
>     - Condition: `Entity Type`: *Entity type must be a Wither.*
>     - Action: `Set Level`: *Set the level of the Wither.*
>     - Action: `Exit Function`: *Don't proceed with running any other Processes in this Function.*
>   - Process: `Standard Levelling`: *Level behaviour for all other mobs.*
>     - Action: `Set Level`: *Set the level of the entity.*

### Advanced Details

- When a Function is ran, it has a [Context](context.md). This stuff happens behind the scenes, but you are using Context all of the time in your [Actions](actions.md) and [Conditions](conditions.md).

***

# See Also

- [🔁 Actions](actions.md)
- [✅ Conditions](conditions.md)
- [❓Context](context.md)
- [🛠️ Settings](../settings.md)

***

# Footnotes

- You might consider LM's Functions System as a basic 'scripting language' which you write in a YAML file. But don't let that scare you away: the Functions System is super simple to use, and you can customise your LevelledMobs experience however you like using it.
- If you *really* wanted to, you could use the Functions System to make LevelledMobs do things wildly different than the mob levelling it was designed for. It's verbose nature makes it difficult to write long trees of logic, but nothing's stopping you from trying it if you wanted to. ;)