"""
================================================================================
CustomDropLogic: a demo script for new LM4 CustomDrops logic, written in Python.
================================================================================

PURPOSE:
  • This script was created to demonstrate how custom drops are processed in LM4. It walks through
    important stages of group sorting, priority sorting, shuffling, and group-specific drop limits.
  • Notably, this script does not factor in any conditions such as chances or entity types, and it
    does not consider whether an individual CustomDrop represents an item drop or worn equipment.

USAGE:
  • Simply run this script and it will print out the steps when an entity's custom drops are being
    processed. You can modify the data which is used in this script by modifying the `drops` list
    near the bottom of the script. With each step, this script will print out the full list of
    custom drops it is currently considering, so you can compare how it has changed since the last
    step in processing the drops.

AUTHORS:
  • lokka30 (2022): original creator

COPYRIGHT:
  • Please reference the copyright notice on LevelledMobs' README.md file. This file follows the
    same license as the rest of LevelledMobs' code, that being, the GNU AGPL v3 license.

SEE ALSO:
  • Relevant v4.0 Development Issue:
     • About: Provides additional context for the purpose of this script.
     • URL: <https://github.com/ArcanePlugins/LevelledMobs/issues/424>
"""

import random

# Define max drops per group
GROUPS_MAX_DROPS = {
    "1": 1,
    "2": 1,
    "undefined": 2
}

class CustomDrop:
    name: str
    priority: int
    shuffle: bool
    group_id: str

    def __init__(self, name: str, priority: int, shuffle: bool, group_id: str) -> None:
        self.name = name
        self.priority = priority
        self.shuffle = shuffle
        self.group_id = group_id

    def get_priority(self):
        return self.priority

def process_custom_drops(drops: list[CustomDrop]) -> list[CustomDrop]:
    print('Processing custom drops')

    # Initialise empty dictionary which is used to group CustomDrops by their group_id
    grouped_drops_dict = dict()

    """
    Debug Print-out
    """
    def print_debug_grouped_drops(grouped_drops_dict: dict):
        print("*** START Grouped Drops Debug Print-out ***")
        for group_id in grouped_drops_dict:
            print("* • Group '" + group_id + "' contains:")
            for drop in grouped_drops_dict[group_id]:
                print(f"*    • {drop.name} \tpriority={drop.priority} \tshuffle={drop.shuffle}.")
        print("*** DONE Grouped Drops Debug Print-out ***")

    """
    Group drops by their group_id
    """
    print('Sorting drops by group_id')
    for drop in drops:
        if drop.group_id not in grouped_drops_dict:
            grouped_drops_dict[drop.group_id] = list()

        grouped_drops_dict[drop.group_id].append(drop)

    print_debug_grouped_drops(grouped_drops_dict)

    """
    Sort drops by their priority
    """
    print('Sorting drops by priority')
    def get_drop_priority(drop: CustomDrop) -> int:
        return drop.priority

    for group_id in grouped_drops_dict:
        grouped_drops_dict[group_id].sort(reverse = True, key = get_drop_priority)

    print_debug_grouped_drops(grouped_drops_dict)

    """
    Shuffling
    """
    print('Shuffling drops')

    for group_id in grouped_drops_dict:
        grouped_drops_list: list = grouped_drops_dict[group_id]

        shuffled_drops = []
        shuffled_indexes = dict() # indexes are associated with a drop priority.

        for index, drop in enumerate(grouped_drops_list):
            if not drop.shuffle:
                continue

            shuffled_drops.append(drop)

            priority_str: str = str(drop.priority)
            if priority_str not in shuffled_indexes:
                shuffled_indexes[priority_str] = []
            shuffled_indexes[priority_str].append(index)

        for priority_str in shuffled_indexes:
            random.shuffle(shuffled_indexes[priority_str])

        for drop in shuffled_drops:
            grouped_drops_list[shuffled_indexes[str(drop.priority)].pop()] = drop

    print_debug_grouped_drops(grouped_drops_dict)

    """
    Max Drops per Group
    """
    print('Sorting max drops per group')
    for group_id in grouped_drops_dict:
        grouped_drops_list: list = grouped_drops_dict[group_id]
        max_drop_group: int = GROUPS_MAX_DROPS[group_id]

        while len(grouped_drops_list) > max_drop_group:
            grouped_drops_list.remove(grouped_drops_list[-1])

    print_debug_grouped_drops(grouped_drops_dict)

    new_drops = []
    for drop_list in grouped_drops_dict.values():
        for drop in drop_list:
            new_drops.append(drop)

    return new_drops

"""
Main Code
"""
drops: list[CustomDrop] = [
    CustomDrop(name = 'Iron Sword',   priority=1, shuffle=True,  group_id='1'),
    CustomDrop(name = 'Iron Pickaxe', priority=1, shuffle=True,  group_id='1'),
    CustomDrop(name = 'Cyan Wool',    priority=2, shuffle=True,  group_id='undefined'),
    CustomDrop(name = 'Orange Wool',  priority=1, shuffle=False, group_id='undefined'),
    CustomDrop(name = 'Iron Hoe',     priority=2, shuffle=True,  group_id='1'),
    CustomDrop(name = 'Magenta Wool', priority=1, shuffle=True,  group_id='undefined'),
    CustomDrop(name = 'Iron Shovel',  priority=2, shuffle=True,  group_id='1'),
    CustomDrop(name = 'Lime Wool',    priority=1, shuffle=False, group_id='undefined'),
    CustomDrop(name = 'Iron Axe',     priority=2, shuffle=False, group_id='1'),
    CustomDrop(name = 'Pink Wool',    priority=2, shuffle=False, group_id='undefined'),
    CustomDrop(name = 'Gold Ingot',   priority=1, shuffle=True,  group_id='2'),
    CustomDrop(name = 'Red Wool',     priority=1, shuffle=False, group_id='undefined'),
    CustomDrop(name = 'White Wool',   priority=1, shuffle=True,  group_id='undefined'),
    CustomDrop(name = 'Purple Wool',  priority=1, shuffle=True,  group_id='undefined'),
    CustomDrop(name = 'Iron Ingot',   priority=1, shuffle=False, group_id='2'),
    CustomDrop(name = 'Yellow Wool',  priority=1, shuffle=False, group_id='undefined'),
    CustomDrop(name = 'Copper Ingot', priority=2, shuffle=False, group_id='2')
]

new_drops = process_custom_drops(drops)
print(f"Completed program with {len(new_drops)} drops.")
