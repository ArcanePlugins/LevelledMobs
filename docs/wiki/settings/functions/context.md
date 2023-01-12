<details>
<summary>Page Description</summary>
This page describes what Context is in LevelledMobs' "Functions System".
</details>

# ❓Context in the Functions System

When using the [Functions System](functions.md), you most likely haven't thought of,

> How does [Action](actions.md)/[Condition](conditions.md) X have access to the data from [Trigger](triggers.md) Y?

The Context System is an element of LevelledMobs' [Functions System](functions.md) which does not directly appear to you, the server administrator, but runs in the background so that Functions can be given data, such as an entity, world, location, and various other things, which are directly related to whatever has called a particular Function.

For example, when using the 'On Entity Spawn' trigger in a function, it provides an Entity context (and various others, such as location, world, etc.) which allows you to use many actions and conditions associated with the entity, such as the 'Entity Type' condition. Functions can also be ran without any Context. Context is just a container which is stuffed with whatever information is known about the situation in which the Function is ran; it's very flexible.

One of the powerful features within the Context System is that you can use several different [Placeholders](#Placeholders) in various places, most notably in chat messages and formulas, allowing you to achieve awesome things without relying on LevelledMobs directly implementing features.

> **Note:** It is important that you verify that the [Actions](actions.md) and [Conditions](conditions.md) you define in your [Processes](functions.md#Processes) have all of the Context they need, such as a reference to an Entity, whenever its parent Function is called.

## Placeholders

The following Context Placeholders are available.

> **Note:** In almost all places where Context Placeholders can be used, you can also use any placeholders from [PlaceholderAPI](https://spigotmc.org/resources/placeholderapi.6245/), if that plugin is installed alongside LevelledMobs.
> Remember to download any e-cloud modules (such as `Player`) for the PAPI placeholders you want to use. ;)
> We recommend using LevelledMobs Context Placeholders instead of PAPI Placeholders wherever possible, as they have a lower performance overhead.

- This list has not been written yet. :(