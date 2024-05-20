package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.result.NametagResult
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.UUID
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * Holds data that goes into various queues for processing
 *
 * @author stumper66
 * @since 3.0.0
 */
class QueueItem {
    var entityId: UUID
        private set
    var lmEntity: LivingEntityWrapper
        private set
    var event: Event? = null
        private set
    var players: MutableList<Player>? = null
    var nametag: NametagResult? = null

    constructor(
        lmEntity: LivingEntityWrapper,
        event: Event?
    ){
        this.lmEntity = lmEntity
        this.event = event
        this.entityId = lmEntity.livingEntity.uniqueId
    }

    constructor(
        lmEntity: LivingEntityWrapper,
        nametag: NametagResult,
        players: MutableList<Player>
    ){
        this.lmEntity = lmEntity
        this.nametag = nametag
        this.players = players
        this.entityId = lmEntity.livingEntity.uniqueId
    }
}