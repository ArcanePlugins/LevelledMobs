package io.github.arcaneplugins.levelledmobs.commands.subcommands


/**
 * Gives the user a specialized spawner that only spawns mobs within certain level criteria
 *
 * @author stumper66
 * @since 3.0.0
 */
object SpawnerSubcommand : SpawnerBaseClass() {

    private enum class OperationEnum {
        CREATE,
        COPY,
        INFO
    }
}