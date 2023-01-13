<details>
<summary>Page Description</summary>
This page lists and describes the various Conditions which are available in LevelledMobs' "Functions System".
</details>

# ✅ Conditions in the Functions System

Conditions are components which 'check things' in the [Functions System](functions.md).

You can use Conditions in [Processes](functions.md#Processes) to make its respective list of [Actions](actions.md) only be ran, only if the defined list of Conditions are met.

For instance, you can use the 'Entity Type' condition to make a Process only run its Actions if the entity in its context is a Creeper. This example was explored in the [Processes](functions.md#Processes) description in this Wiki, where we used entity types to use particular logic for Withers, and different logic for all other entities.

## Conditions List

#### Chance

Allows you to use a random percentage chance that the Condition will be satisfied.

##### Example

```yaml
- condition: 'chance'
  value: 75.5
```

> Sets a 75.5% chance that the condition will be satisfied.

#### Entity Biome

Checks what biome an entity is. The condition is met if the biome is/isn't in a list/[Group](../../groups/groups.md) of biomes.

##### Example

N/A, please yell at lokka30.

#### Entity Custom Name Contains

> *This Condition will be altered quite a lot soon, so please don't use it. I won't document it so I don't waste time. ;)*

#### Entity Level

Documentation N/A

##### Example

Documentation N/A

#### Entity Owner

Documentation N/A

##### Example

Documentation N/A

#### Entity Type

Documentation N/A

##### Example

Documentation N/A

#### Entity World

Documentation N/A

##### Example

Documentation N/A

#### Player World

Documentation N/A

##### Example

Documentation N/A
