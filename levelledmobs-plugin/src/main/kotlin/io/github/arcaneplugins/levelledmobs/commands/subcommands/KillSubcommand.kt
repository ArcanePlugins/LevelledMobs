package io.github.arcaneplugins.levelledmobs.commands.subcommands

import java.util.Locale
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesBase
import io.github.arcaneplugins.levelledmobs.misc.RequestedLevel
import io.github.arcaneplugins.levelledmobs.util.Utils
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.entity.ZombieVillager
import org.bukkit.metadata.FixedMetadataValue

/**
 * Allows you to kill LevelledMobs with various options including all levelled mobs, specific worlds
 * or levelled mobs in your proximity
 *
 * @author stumper66
 * @since 2.0
 */
class KillSubcommand : MessagesBase(), Subcommand {
    override fun parseSubcommand(
        sender: CommandSender,
        label: String,
        args: Array<String>
    ) {
        commandSender = sender
        messageLabel = label
        val main = LevelledMobs.instance

        if (!sender.hasPermission("levelledmobs.command.kill")) {
            main.configUtils.sendNoPermissionMsg(sender)
            return
        }

        if (args.size <= 1) {
            showMessage("command.levelledmobs.kill.usage")
            return
        }

        var checkArgs = args.size
        var useNoDrops = false
        val rl = getLevelFromCommand(sender, args)
        if (rl != null) {
            if (rl.hadInvalidArguments) {
                return
            }
            checkArgs -= 2
        }

        for (arg in args) {
            if ("/nodrops".equals(arg, ignoreCase = true)) {
                useNoDrops = true
                break
            }
        }

        when (args[1].lowercase(Locale.getDefault())) {
            "all" -> {
                if (!sender.hasPermission("levelledmobs.command.kill.all")) {
                    main.configUtils.sendNoPermissionMsg(sender)
                    return
                }
                if (checkArgs == 2) {
                    if (sender is Player) {
                        parseKillAll(mutableListOf(sender.world), useNoDrops, rl)
                    } else {
                        showMessage("command.levelledmobs.kill.all.usage-console")
                    }
                } else if (checkArgs == 3 || checkArgs == 4) {
                    if (args[2] == "*") {
                        parseKillAll(Bukkit.getWorlds(), useNoDrops, rl)
                        return
                    }

                    if ("/nodrops".equals(args[2], ignoreCase = true)) {
                        parseKillAll(Bukkit.getWorlds(), true, rl)
                    } else {
                        val world = Bukkit.getWorld(args[2])
                        if (world == null) {
                            showMessage(
                                "command.levelledmobs.kill.all.invalid-world", "%world%",
                                args[2]
                            )
                            return
                        }
                        parseKillAll(mutableListOf(world), useNoDrops, rl)
                    }
                } else {
                    showMessage("command.levelledmobs.kill.all.usage")
                }
            }

            "near" -> {
                if (!sender.hasPermission("levelledmobs.command.kill.near")) {
                    main.configUtils.sendNoPermissionMsg(sender)
                    return
                }
                if (checkArgs == 3 || checkArgs == 4) {
                    if (sender !is BlockCommandSender && sender !is Player) {
                        showMessage("common.players-only")
                        return
                    }

                    var radius: Int
                    try {
                        radius = args[2].toInt()
                    } catch (exception: NumberFormatException) {
                        showMessage(
                            "command.levelledmobs.kill.near.invalid-radius", "%radius%",
                            args[2]
                        )
                        return
                    }

                    val maxRadius = 1000
                    if (radius > maxRadius) {
                        radius = maxRadius
                        showMessage(
                            "command.levelledmobs.kill.near.invalid-radius-max",
                            "%maxRadius%", maxRadius.toString()
                        )
                    }

                    val minRadius = 1
                    if (radius < minRadius) {
                        radius = minRadius
                        showMessage(
                            "command.levelledmobs.kill.near.invalid-radius-min",
                            "%minRadius%", minRadius.toString()
                        )
                    }

                    var killed = 0
                    var skipped = 0
                    val mobsToKill: Collection<Entity>
                    if (sender is BlockCommandSender) {
                        val block = sender.block
                        mobsToKill = block.world
                            .getNearbyEntities(block.location, radius.toDouble(), radius.toDouble(), radius.toDouble())
                    } else {
                        mobsToKill = (sender as Player).getNearbyEntities(
                            radius.toDouble(),
                            radius.toDouble(),
                            radius.toDouble()
                        )
                    }

                    for (entity in mobsToKill) {
                        if (entity !is LivingEntity) continue

                        if (!main.levelInterface.isLevelled(entity)) continue

                        if (skipKillingEntity(entity, rl)) {
                            skipped++
                            continue
                        }

                        entity.setMetadata(
                            "noCommands",
                            FixedMetadataValue(main, 1)
                        )

                        if (useNoDrops) {
                            entity.remove()
                        } else {
                            entity.health = 0.0
                        }
                        killed++
                    }

                    showMessage(
                        "command.levelledmobs.kill.near.success",
                        arrayOf("%killed%", "%skipped%", "%radius%"),
                        arrayOf(
                            killed.toString(), skipped.toString(),
                            radius.toString()
                        )
                    )
                } else {
                    showMessage("command.levelledmobs.kill.near.usage")
                }
            }

            else -> showMessage("command.levelledmobs.kill.usage")
        }
    }

    private fun getLevelFromCommand(
        sender: CommandSender,
        args: Array<String>
    ): RequestedLevel? {
        var rangeSpecifiedFlag = -1

        for (i in args.indices) {
            if ("/levels".equals(args[i], ignoreCase = true)) {
                rangeSpecifiedFlag = i + 1
            }
        }

        if (rangeSpecifiedFlag <= 0) {
            return null
        }

        val rl = RequestedLevel()
        if (args.size <= rangeSpecifiedFlag) {
            sender.sendMessage("No value was specified for /levels")
            rl.hadInvalidArguments = true
            return rl
        }

        val value = args[rangeSpecifiedFlag]
        if (!rl.setLevelFromString(value)) {
            sender.sendMessage("Invalid number or range specified for /levels")
            rl.hadInvalidArguments = true
        }

        return rl
    }

    override fun parseTabCompletions(
        sender: CommandSender,
        args: Array<String>
    ): MutableList<String> {
        if (!sender.hasPermission("levelledmobs.command.kill")) {
            return mutableListOf()
        }

        var containsNoDrops = false
        var containsLevels = false

        for (arg in args) {
            if ("/nodrops".equals(arg, ignoreCase = true)) {
                containsNoDrops = true
            } else if ("/levels".equals(arg, ignoreCase = true)) {
                containsLevels = true
            }
        }

        if (args.size == 2) {
            return mutableListOf("all", "near")
        }

        if (args[1].equals("all", ignoreCase = true) && (args.size == 3 || args.size == 4)) {
            if (sender.hasPermission("levelledmobs.command.kill.all")) {
                val worlds = mutableListOf<String>()

                if (!containsNoDrops) {
                    worlds.add("/nodrops")
                }
                if (!containsLevels) {
                    worlds.add("/levels")
                }
                if (args.size == 3) {
                    for (world in Bukkit.getWorlds()) {
                        worlds.add("*")
                        if (LevelledMobs.instance.rulesManager.getRuleIsWorldAllowedInAnyRule(world)) {
                            worlds.add(world.name)
                        }
                    }
                }

                return worlds
            }
        }
        if (args[1].equals("near", ignoreCase = true) && sender.hasPermission("levelledmobs.command.kill.near")) {
            if (args.size == 4 && "/levels".equals(args[3], ignoreCase = true)) {
                return mutableListOf("/levels")
            } else if (args.size == 3) {
                return Utils.oneToNine
            }
        }

        val result = mutableListOf<String>()
        if (!containsNoDrops) {
            result.add("/nodrops")
        }
        if (!containsLevels) {
            result.add("/levels")
        }

        return result
    }

    private fun parseKillAll(
        worlds: MutableList<World>,
        useNoDrops: Boolean,
        rl: RequestedLevel?
    ) {
        var killed = 0
        var skipped = 0

        for (world in worlds) {
            for (entity in world.entities) {
                if (entity !is LivingEntity) {
                    continue
                }
                if (!LevelledMobs.instance.levelInterface.isLevelled(entity)) {
                    continue
                }

                if (skipKillingEntity(entity, rl)) {
                    skipped++
                    continue
                }

                entity.setMetadata("noCommands", FixedMetadataValue(LevelledMobs.instance, 1))

                if (useNoDrops) {
                    entity.remove()
                } else {
                    entity.health = 0.0
                }

                killed++
            }
        }

        showMessage(
            "command.levelledmobs.kill.all.success",
            arrayOf("%killed%", "%skipped%", "%worlds%"),
            arrayOf(
                killed.toString(), skipped.toString(),
                worlds.size.toString()
            )
        )
    }

    private fun skipKillingEntity(
        livingEntity: LivingEntity,
        rl: RequestedLevel?
    ): Boolean {
        val main = LevelledMobs.instance
        @Suppress("DEPRECATION")
        if (livingEntity.customName != null && main.helperSettings.getBoolean(
                "kill-skip-conditions.nametagged"
            )
        ) {
            return true
        }

        if (rl != null) {
            val mobLevel: Int = main.levelInterface.getLevelOfMob(livingEntity)
            if (mobLevel < rl.levelMin || mobLevel > rl.levelMax) {
                return true
            }
        }

        // Tamed
        if (livingEntity is Tameable && livingEntity.isTamed
            && main.helperSettings.getBoolean( "kill-skip-conditions.tamed")
        ) {
            return true
        }

        // Leashed
        if (livingEntity.isLeashed && main.helperSettings.getBoolean(
                "kill-skip-conditions.leashed"
            )
        ) {
            return true
        }

        // Converting zombie villager
        return livingEntity.type == EntityType.ZOMBIE_VILLAGER &&
                (livingEntity as ZombieVillager).isConverting &&
                main.helperSettings.getBoolean(
                    "kill-skip-conditions.convertingZombieVillager"
                )
    }
}