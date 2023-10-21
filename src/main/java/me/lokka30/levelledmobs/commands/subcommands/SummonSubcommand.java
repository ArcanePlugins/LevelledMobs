/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import me.lokka30.levelledmobs.managers.LevelManager;
import me.lokka30.levelledmobs.misc.AdditionalLevelInformation;
import me.lokka30.levelledmobs.misc.LevellableState;
import me.lokka30.levelledmobs.misc.LivingEntityPlaceholder;
import me.lokka30.levelledmobs.misc.MinAndMaxHolder;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.RequestedLevel;
import me.lokka30.levelledmobs.util.PaperUtils;
import me.lokka30.levelledmobs.util.SpigotUtils;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Summons a levelled mob with a specific level and criteria
 *
 * @author stumper66
 * @author lokka30
 * @since v2.0.0
 */
public class SummonSubcommand extends MessagesBase implements Subcommand {

    public SummonSubcommand(final LevelledMobs main) {
        super(main);
    }

    @Override
    public void parseSubcommand(
        @NotNull final LevelledMobs main,
        @NotNull final CommandSender sender,
        @NotNull final String label,
        @NotNull final String... args
    ) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.summon")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        boolean useOverride = false;
        final List<String> useArgs = new LinkedList<>();
        int startOfNbt = -1;
        int endOfNbt = -1;

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if ("/override".equalsIgnoreCase(arg)) {
                useOverride = true;
            } else {
                useArgs.add(arg);
            }

            if (startOfNbt == -1 && arg.startsWith("{")) {
                startOfNbt = i;
                if (arg.endsWith("}")) {
                    endOfNbt = i;
                }
            } else if (startOfNbt >= 0 && endOfNbt == -1 && arg.endsWith("}")) {
                endOfNbt = i;
            }
        }

        String nbtData = null;
        if (startOfNbt >= 0 && endOfNbt >= 0 && endOfNbt >= startOfNbt) {
            nbtData = useArgs.subList(startOfNbt, endOfNbt + 1).toString();
            nbtData = nbtData.substring(1, nbtData.length() - 1);
            useArgs.subList(startOfNbt, endOfNbt + 1).clear();
        }

        final String[] useArgs2 = new String[useArgs.size()];
        useArgs.toArray(useArgs2);

        parseSubcommand2(useArgs2, useOverride, nbtData);
    }

    private void parseSubcommand2(final String @NotNull [] args, final boolean override,
        final String nbtData) {
        if (args.length < 4) {
            showMessage("command.levelledmobs.summon.usage");
            return;
        }

        final int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (final NumberFormatException ex) {
            showMessage("command.levelledmobs.summon.invalid-amount", "%amount%", args[1]);
            // messages = Utils.replaceAllInList(messages, "%amount%", args[1]); // This is after colorize so that args[1] is not colorized.
            return;
        }

        final EntityType entityType;
        try {
            entityType = EntityType.valueOf(args[2].toUpperCase());
        } catch (final IllegalArgumentException ex) {
            showMessage("command.levelledmobs.summon.invalid-entity-type", "%entityType%", args[2]);
            // messages = Utils.replaceAllInList(messages, "%entityType%", args[2]); // This is after colorize so that args[2] is not colorized.
            return;
        }

        final RequestedLevel requestedLevel = new RequestedLevel();
        if (!requestedLevel.setLevelFromString(args[3])) {
            showMessage("command.levelledmobs.summon.invalid-level", "%level%", args[3]);
            //messages = Utils.replaceAllInList(messages, "%level%", args[3]); // This is after colorize so that args[3] is not colorized.
            return;
        }

        SummonType summonType = SummonType.HERE;
        if (args.length > 4) {
            switch (args[4].toLowerCase()) {
                case "here" -> {
                }
                case "atplayer" -> summonType = SummonType.AT_PLAYER;
                case "atlocation" -> summonType = SummonType.AT_LOCATION;
                default -> {
                    showMessage("command.levelledmobs.summon.invalid-level", "%summonType%",
                            args[4]);
                    return;
                }
            }
        }

        if (summonType == SummonType.HERE) {
            if (!(commandSender instanceof Player)
                && !(commandSender instanceof BlockCommandSender)) {
                showMessage("command.levelledmobs.summon.invalid-summon-type-console");
                return;
            }

            if (args.length == 4 || args.length == 5) {
                Player player = null;
                if (commandSender instanceof Player) {
                    player = (Player) commandSender;
                }
                final Location location = (player != null) ?
                    ((Player) commandSender).getLocation()
                    : ((BlockCommandSender) commandSender).getBlock().getLocation();

                if (location.getWorld() == null) {
                    final String playerName = main.getVerInfo().getIsRunningPaper() ?
                        PaperUtils.getPlayerDisplayName(player)
                        : SpigotUtils.getPlayerDisplayName(player);
                    showMessage("common.player-offline", "%player%", playerName);
                    return;
                }

                final LivingEntityPlaceholder lmPlaceHolder = LivingEntityPlaceholder.getInstance(
                    entityType, location, main);

                final SummonMobOptions options = new SummonMobOptions(lmPlaceHolder, commandSender);
                options.amount = amount;
                options.requestedLevel = requestedLevel;
                options.summonType = summonType;
                options.player = player;
                options.override = override;
                options.nbtData = nbtData;

                summonMobs(options);
                lmPlaceHolder.free();
            } else {
                showMessage("command.levelledmobs.summon.here.usage");
            }
        } else if (summonType == SummonType.AT_PLAYER) {
            if (args.length == 6) {

                boolean offline = false;
                Location location = null;
                World world = null;

                final Player target = Bukkit.getPlayer(args[5]);
                if (target == null) {
                    offline = true;
                } else if (commandSender instanceof final Player player) {
                    // Vanished player compatibility.
                    if (!player.canSee(target) && !player.isOp()) {
                        offline = true;
                    }
                    location = (target.getLocation());
                    world = location.getWorld();
                } else {
                    location = target.getLocation();
                    world = target.getWorld();
                }

                if (offline || world == null) {
                    showMessage("common.player-offline", "%player%", args[5]);
                    // messages = Utils.replaceAllInList(messages, "%player%", args[5]); // This is after colorize so that args[5] is not colorized.
                    return;
                }

                final LivingEntityPlaceholder lmPlaceHolder = LivingEntityPlaceholder.getInstance(
                    entityType, location, main);
                final SummonMobOptions options = new SummonMobOptions(lmPlaceHolder, commandSender);
                options.amount = amount;
                options.requestedLevel = requestedLevel;
                options.summonType = summonType;
                options.player = target;
                options.override = override;
                options.nbtData = nbtData;

                summonMobs(options);
                lmPlaceHolder.free();
            } else {
                showMessage("command.levelledmobs.summon.atPlayer.usage");
            }
        } else { // At Location
            if (args.length == 8 || args.length == 9) {
                final String worldName;

                if (args.length == 8) {
                    if (commandSender instanceof Player) {
                        worldName = ((Player) commandSender).getWorld().getName();
                    } else if (commandSender instanceof BlockCommandSender) {
                        worldName = ((BlockCommandSender) commandSender).getBlock().getWorld()
                            .getName();
                    } else {
                        showMessage("command.levelledmobs.summon.atLocation.usage-console");
                        return;
                    }
                } else { //args.length==9
                    final World world = Bukkit.getWorld(args[8]);

                    if (world == null) {
                        showMessage("command.levelledmobs.summon.atLocation.usage-console",
                            "%world%", args[8]);
                        // messages = Utils.replaceAllInList(messages, "%world%", args[8]); //This is after colorize so that args[8] is not colorized.
                        return;
                    } else {
                        worldName = world.getName();
                    }
                }

                final Location location = getRelativeLocation(commandSender, args[5], args[6],
                    args[7], worldName);

                if (location == null || location.getWorld() == null) {
                    showMessage("command.levelledmobs.summon.atLocation.invalid-location");
                } else {
                    final LivingEntityPlaceholder lmPlaceHolder = LivingEntityPlaceholder.getInstance(
                        entityType, location, main);
                    final SummonMobOptions options = new SummonMobOptions(lmPlaceHolder,
                        commandSender);
                    options.amount = amount;
                    options.requestedLevel = requestedLevel;
                    options.summonType = summonType;
                    options.override = override;
                    options.nbtData = nbtData;
                    summonMobs(options);
                    lmPlaceHolder.free();
                }
            } else {
                showMessage("command.levelledmobs.summon.atLocation.usage");
            }
        }
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main,
        final @NotNull CommandSender sender, @NotNull final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.summon")) {
            return null;
        }

        // len:    1      2        3        4       5          6            7   8     9     10
        // arg:    0      1        2        3       4          5            6   7     8     9
        // lvlmobs summon <amount> <entity> <level> here       /override
        // lvlmobs summon <amount> <entity> <level> atPlayer   <playername> /override
        // lvlmobs summon <amount> <entity> <level> atLocation <x>          <y> <z> [world] /override

        // <amount>
        if (args.length == 2) {
            return Utils.oneToNine;
        }

        // <entity>
        if (args.length == 3) {
            final List<String> entityNames = new LinkedList<>();
            for (final EntityType entityType : EntityType.values()) {
                entityNames.add(entityType.toString().toLowerCase());
            }
            return entityNames;
        }

        // <level>
        if (args.length == 4) {
            return Utils.oneToNine;
        }

        // here, atPlayer, atLocation
        if (args.length == 5) {
            return List.of("here", "atPlayer", "atLocation", "/override");
        }

        boolean skipOverride = false;
        for (int i = 5; i < args.length; i++) {
            final String arg = args[i];
            if (arg.startsWith("{") && !arg.endsWith("}")) {
                skipOverride = true;
            }
            if (skipOverride && arg.endsWith("}")) {
                skipOverride = false;
            }
        }
        if (args[args.length - 1].endsWith("}")) {
            skipOverride = true;
        }

        // no suggestions for 'here' since it is the last argument for itself
        // these are for atPlayer and atLocation
        if (args.length > 5) {
            switch (args[4].toLowerCase()) {
                case "atplayer" -> {
                    if (args.length == 6) {
                        final List<String> suggestions = new LinkedList<>();
                        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            if (sender instanceof final Player player) {
                                if (player.canSee(onlinePlayer) || player.isOp()) {
                                    suggestions.add(onlinePlayer.getName());
                                }
                            } else {
                                suggestions.add(onlinePlayer.getName());
                            }
                        }
                        return suggestions;
                    } else if (args.length == 7) {
                        if (!skipOverride) {
                            return List.of("/override");
                        } else {
                            return List.of();
                        }
                    }
                }
                case "atlocation" -> {
                    if (args.length < 9) { // args 6, 7 and 8 = x, y and z
                        return List.of("~");
                    } else if (args.length == 9) {
                        final List<String> worlds = new LinkedList<>();
                        Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
                        return worlds;
                    } else if (args.length == 10) {
                        if (!skipOverride) {
                            return List.of("/override");
                        } else {
                            return List.of();
                        }
                    }
                }
                case "here" -> {
                    if (!skipOverride) {
                        return List.of("/override");
                    } else {
                        return List.of();
                    }
                }
                default -> {
                    return List.of();
                }
            }
        }

        return List.of();
    }

    enum SummonType {
        HERE,
        AT_PLAYER,
        AT_LOCATION
    }

    private void summonMobs(@NotNull final SummonMobOptions options) {

        final CommandSender sender = options.sender;
        final LevelledMobs main = options.lmPlaceholder.getMainInstance();
        final Player target = options.player;
        Location location = options.lmPlaceholder.getLocation();

        if (main.levelManager.FORCED_BLOCKED_ENTITY_TYPES.contains(
            options.lmPlaceholder.getEntityType())) {
            List<String> messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.not-levellable");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%entity%",
                options.lmPlaceholder.getTypeName());
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
            return;
        }

        if (!sender.isOp() && !options.override
            && main.levelInterface.getLevellableState(options.lmPlaceholder)
            != LevellableState.ALLOWED) {
            List<String> messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.not-levellable");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%entity%",
                options.lmPlaceholder.getTypeName());
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
            return;
        }

        if (options.amount < 1) {
            List<String> messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.amount-limited.min");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }

        final int maxAmount = main.helperSettings.getInt(main.settingsCfg,
            "customize-summon-command-limit", 100);
        if (options.amount > maxAmount) {
            options.amount = maxAmount;

            List<String> messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.amount-limited.max");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%maxAmount%", String.valueOf(maxAmount));
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }

        final MinAndMaxHolder levels = main.levelManager.getMinAndMaxLevels(options.lmPlaceholder);

        if (options.requestedLevel.getLevelMin() < levels.min && !sender.hasPermission(
            "levelledmobs.command.summon.bypass-level-limit") && !options.override) {
            options.requestedLevel.setMinAllowedLevel(levels.min);

            List<String> messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.level-limited.min");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%minLevel%", String.valueOf(levels.min));
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }

        if (options.requestedLevel.getLevelMax() > levels.max && !sender.hasPermission(
            "levelledmobs.command.summon.bypass-level-limit") && !options.override) {
            options.requestedLevel.setMaxAllowedLevel(levels.max);

            List<String> messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.level-limited.max");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%maxLevel%", String.valueOf(levels.max));
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }

        if (options.summonType == SummonType.HERE) {
            location = addVarianceToLocation(location);
        }

        if (options.summonType == SummonType.HERE || options.summonType == SummonType.AT_PLAYER) {
            final int distFromPlayer = main.settingsCfg.getInt(
                "summon-command-spawn-distance-from-player", 5);
            if (distFromPlayer > 0 && target != null) {
                int useDistFromPlayer = distFromPlayer;
                final Location origLocation = location;
                // try up to 50 times to find a open spot to spawn the mob.  Keep getting closer to the player if needed
                for (int i = 0; i < 50; i++) {
                    useDistFromPlayer -= i;
                    if (useDistFromPlayer <= 0) {
                        location = location.add(0, 1, 0);
                        break;
                    }

                    location = getLocationNearPlayer(target, origLocation, useDistFromPlayer);
                    final Location location_YMinus1 = location.add(0.0, -1.0, 0.0);
                    if (location.getBlock().isPassable() && location_YMinus1.getBlock()
                        .isPassable()) {
                        location = location.add(0, 1, 0);
                        break; // found an open spot
                    }
                }
            } else if (target == null && sender instanceof BlockCommandSender) {
                // increase the y by one so they don't spawn inside the command block
                location = new Location(location.getWorld(), location.getBlockX(),
                    location.getBlockY() + 2, location.getBlockZ());
            }
        }

        for (int i = 0; i < options.amount; i++) {
            assert location.getWorld() != null;

            final int useLevel = options.requestedLevel.hasLevelRange ?
                ThreadLocalRandom.current().nextInt(options.requestedLevel.levelRangeMin,
                    options.requestedLevel.levelRangeMax + 1) :
                options.requestedLevel.level;

            final Entity entity = location.getWorld()
                .spawnEntity(location, options.lmPlaceholder.getEntityType());

            if (entity instanceof LivingEntity) {
                final LivingEntityWrapper lmEntity = LivingEntityWrapper
                    .getInstance((LivingEntity) entity, main);

                lmEntity.setSummonedLevel(useLevel);
                lmEntity.isNewlySpawned = true;
                synchronized (LevelManager.summonedOrSpawnEggs_Lock) {
                    main.levelManager.summonedOrSpawnEggs.put(lmEntity.getLivingEntity(), null);
                }
                if (!Utils.isNullOrEmpty(options.nbtData)) {
                    lmEntity.nbtData = List.of(options.nbtData);
                }
                lmEntity.summonedSender = sender;
                main.levelInterface.applyLevelToMob(lmEntity, useLevel, true, options.override,
                    new HashSet<>(List.of(AdditionalLevelInformation.NOT_APPLICABLE)));
                synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                    lmEntity.getPDC()
                        .set(main.namespacedKeys.wasSummoned, PersistentDataType.INTEGER, 1);
                }
                lmEntity.free();
            }
        }

        final boolean printResults = main.helperSettings.getBoolean(main.settingsCfg, "print-lm-summon-results", true);

        switch (options.summonType) {
            case HERE -> {
                if (printResults) {
                    showMessage("command.levelledmobs.summon.here.success",
                            new String[]{"%amount%", "%level%", "%entity%"},
                            new String[]{String.valueOf(options.amount), options.requestedLevel.toString(),
                                    options.lmPlaceholder.getTypeName()}
                    );
                }
            }
            case AT_LOCATION -> {
                if (printResults){
                showMessage("command.levelledmobs.summon.atLocation.success",
                    new String[]{"%amount%", "%level%", "%entity%", "%x%", "%y%", "%z%", "%world%"},
                    new String[]{
                        String.valueOf(options.amount),
                        options.requestedLevel.toString(),
                        options.lmPlaceholder.getTypeName(),
                        Integer.toString(location.getBlockX()),
                        Integer.toString(location.getBlockY()),
                        Integer.toString(location.getBlockZ()),
                        location.getWorld() == null ? "(null)" : location.getWorld().getName()
                    });
                }
            }
            case AT_PLAYER -> {
                if (printResults) {
                    final String playerName = main.getVerInfo().getIsRunningPaper() ?
                            PaperUtils.getPlayerDisplayName(target)
                            : SpigotUtils.getPlayerDisplayName(target);
                    showMessage("command.levelledmobs.summon.atPlayer.success",
                            new String[]{"%amount%", "%level%", "%entity%", "%targetUsername%",
                                    "%targetDisplayname%"},
                            new String[]{String.valueOf(options.amount), options.requestedLevel.toString(),
                                    options.lmPlaceholder.getTypeName(),
                                    target == null ? "(null)" : target.getName(),
                                    target == null ? "(null)" : playerName}
                    );
                }
            }
            default -> throw new IllegalStateException(
                    "Unexpected SummonType value of " + options.summonType + "!");
        }
    }

    @Contract("_, _, _ -> new")
    @NotNull private Location getLocationNearPlayer(final @NotNull Player player,
        final @NotNull Location location, final int useDistFromPlayer) {
        int newX = location.getBlockX();
        int newZ = location.getBlockZ();

        double rotation = (player.getLocation().getYaw() - 180) % 360;
        if (rotation < 0) {
            rotation += 360.0;
        }

        if (0 <= rotation && rotation < 22.5) // N
        {
            newZ -= useDistFromPlayer;
        } else if (22.5 <= rotation && rotation < 67.5) { // NE
            newX += useDistFromPlayer;
            newZ -= useDistFromPlayer;
        } else if (67.5 <= rotation && rotation < 112.5) // E
        {
            newX += useDistFromPlayer;
        } else if (112.5 <= rotation && rotation < 157.5) { // SE
            newX += useDistFromPlayer;
            newZ += useDistFromPlayer;
        } else if (157.5 <= rotation && rotation < 202.5) // S
        {
            newZ += useDistFromPlayer;
        } else if (202.5 <= rotation && rotation < 247.5) { // SW
            newX -= useDistFromPlayer;
            newZ += useDistFromPlayer;
        } else if (247.5 <= rotation && rotation < 292.5) // W
        {
            newX -= useDistFromPlayer;
        } else if (292.5 <= rotation && rotation < 337.5) { // NW
            newX -= useDistFromPlayer;
            newZ -= useDistFromPlayer;
        } else // N
        {
            newZ -= useDistFromPlayer;
        }

        return new Location(location.getWorld(), newX, location.getBlockY(), newZ);
    }

    @Nullable private Location getRelativeLocation(final CommandSender sender, final String xStr,
        final String yStr, final String zStr, final String worldName) {
        double x = 0, y = 0, z = 0;
        boolean xRelative = false, yRelative = false, zRelative = false;

        if (sender instanceof Player
            || sender instanceof BlockCommandSender) { //Player or Command blocks
            if (xStr.charAt(0) == '~') {
                if (sender instanceof Player) {
                    x = ((Player) sender).getLocation().getX();
                } else {
                    x = ((BlockCommandSender) sender).getBlock().getX();
                }

                if (xStr.length() > 1) {
                    final double addition;
                    try {
                        addition = Double.parseDouble(xStr.substring(1));
                    } catch (final NumberFormatException ex) {
                        return null;
                    }
                    x += addition;
                }

                xRelative = true;
            }
            if (yStr.charAt(0) == '~') {
                if (sender instanceof Player) {
                    y = ((Player) sender).getLocation().getY();
                } else {
                    y = ((BlockCommandSender) sender).getBlock().getY();
                }

                if (yStr.length() > 1) {
                    final double addition;
                    try {
                        addition = Double.parseDouble(yStr.substring(1));
                    } catch (final NumberFormatException ex) {
                        return null;
                    }

                    y += addition;
                }

                yRelative = true;
            }
            if (zStr.charAt(0) == '~') {
                if (sender instanceof Player) {
                    z = ((Player) sender).getLocation().getZ();
                } else {
                    z = ((BlockCommandSender) sender).getBlock().getZ();
                }

                if (zStr.length() > 1) {
                    final double addition;
                    try {
                        addition = Double.parseDouble(zStr.substring(1));
                    } catch (final NumberFormatException ex) {
                        return null;
                    }
                    z += addition;
                }

                zRelative = true;
            }
        }

        if (!xRelative) {
            try {
                x = Double.parseDouble(xStr);
            } catch (final NumberFormatException ex) {
                return null;
            }
        }
        if (!yRelative) {
            try {
                y = Double.parseDouble(yStr);
            } catch (final NumberFormatException ex) {
                return null;
            }
        }
        if (!zRelative) {
            try {
                z = Double.parseDouble(zStr);
            } catch (final NumberFormatException ex) {
                return null;
            }
        }

        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(world, x, y, z);
    }

    private Location addVarianceToLocation(final Location oldLocation) {
        final double min = 0.5;
        final double max = 2.5;

        //Creates 3x new Random()s for a different seed each time
        final Random random1 = new Random();
        final Random random2 = new Random();

        for (int i = 0; i < 20; i++) {
            final double x = oldLocation.getX() + min + (max - min) * random1.nextDouble();
            final double z = oldLocation.getZ() + min + (max - min) * random2.nextDouble();

            final Location newLocation = new Location(oldLocation.getWorld(), x, oldLocation.getY(),
                z);
            if (newLocation.getBlock().isPassable() && newLocation.add(0, 1, 0).getBlock()
                .isPassable()) {
                return newLocation;
            }
        }

        return oldLocation;
    }
}
