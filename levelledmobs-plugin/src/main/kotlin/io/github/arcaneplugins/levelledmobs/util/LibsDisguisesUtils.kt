package io.github.arcaneplugins.levelledmobs.util

import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.Disguise
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper

/**
 * Various methods for detecting or updating mobs using
 * Lib's Disguises
 *
 * @author stumper66
 * @since 3.14.0
 */
object LibsDisguisesUtils {
    private var hasLibsDisguises: Boolean = false

    init {
        hasLibsDisguises = ExternalCompatibilityManager.hasLibsDisguisesInstalled
    }

    fun isMobUsingLibsDisguises(
        lmEntity: LivingEntityWrapper
    ): Boolean {
        if (!hasLibsDisguises) return false

        val disguise: Disguise?
        if (lmEntity.libsDisguiseCache != null) {
            disguise = lmEntity.libsDisguiseCache as Disguise?
        } else {
            disguise = DisguiseAPI.getDisguise(lmEntity.livingEntity)
            lmEntity.libsDisguiseCache = disguise
        }

        return disguise != null && disguise.isDisguiseInUse
    }

    fun updateLibsDisguiseNametag(
        lmEntity: LivingEntityWrapper,
        nametag: String?
    ) {
        if (!isMobUsingLibsDisguises(lmEntity)) return

        val disguise = lmEntity.libsDisguiseCache as Disguise
        val wrapper = SchedulerWrapper(lmEntity.livingEntity) {
            disguise.watcher.customName = nametag
            lmEntity.free()
        }

        lmEntity.inUseCount.getAndIncrement()
        wrapper.run()
    }
}