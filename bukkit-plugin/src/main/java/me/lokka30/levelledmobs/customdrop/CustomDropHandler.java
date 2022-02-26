/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrop;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;

public class CustomDropHandler {

    // A list of what materials each entitytype drops by default, e.g. CREEPER has gunpowder
    // Use drop tables to find this info out :)
    //TODO implement
    private final HashMap<EntityType, Material[]> defaultDropsMap = new HashMap<>();

    /*
    TODO: code for determining what drops will be dropped
    a good mix of English, Java and kind-of-Java ;)

    when levelled mob dies:
      HashMap<String groupId, Set<CustomDrop> dropsInGroup> groupDropsMap = new HashMap<>();
      HashSet<CustomDrop> nonGroupedDrops = new HashSet<>();
      LinkedList<CustomDrop> dropsToMake = new ArrayList<>();

      for each group id:
        dropsInGroup.put(groupId, new Set<>());

      for each custom drop applicable to the entity:

        # sorts all of the drops by their group id
        if custom drop has a group id:
          add custom drop to the set in dropsInGroup with its own group id.
        else:
          add custom drop to the nonGroupedDrops set

        for each groupId in the dropsInGroup map:
          int maxDropsForGroup = get value from configuration;
          int dropsDoneForGroup = 0;

          WeightedRandomContainer<CustomDrop> container = new WeightedRandomContainer<>();

          for each custom drop in the set paired with the group id:
            add entry to the container with configured priority. if no priority configured then assume priority=1.

          for(int i = Math.min(maxDropsForGroup, dropsInTheCurrentGroup.size()); i >= 0; i--):
            CustomDrop chosenDrop = container.getRandom();

            container = new WeightedRandomContainer<>();
            groupDropsMap.get(groupId).remove(chosenDrop);
            groupDropsMap.get(groupId).forEach(drop -> container.add(drop, drop.priority()));

            if chance of chosenDrop is successful:
              dropsToMake.add(chosenDrop);

        for each drop in nonGroupedDrops:
          if chance of chosenDrop is successful:
            dropsToMake.add(chosenDrop);

        for each drop in dropsToMake:
          drop the drop 😎
     */

}
