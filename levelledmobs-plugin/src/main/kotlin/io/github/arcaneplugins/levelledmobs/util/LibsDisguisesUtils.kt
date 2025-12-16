package io.github.arcaneplugins.levelledmobs.util

import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.entity.Entity

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

        // now using reflection due to lib's maven repo being very unreliable
        val clazzDisguise = Class.forName("me.libraryaddict.disguise.disguisetypes.Disguise")
        val clazzDisguiseAPI = Class.forName("me.libraryaddict.disguise.DisguiseAPI")
        val methodGetDisguise = clazzDisguiseAPI.getMethod("getDisguise", Entity::class.java)
        val methodIsDisguiseInUse = clazzDisguise.getMethod("isDisguiseInUse")

        val disguise: Any? // me.libraryaddict.disguise.disguisetypes.Disguise?

        if (lmEntity.libsDisguiseCache != null)
            disguise = lmEntity.libsDisguiseCache
        else {
            //disguise = me.libraryaddict.disguise.DisguiseAPI.getDisguise(lmEntity.livingEntity)
            disguise = methodGetDisguise.invoke(null, lmEntity.livingEntity)
            lmEntity.libsDisguiseCache = disguise
        }

        return if (disguise != null)
            methodIsDisguiseInUse.invoke(disguise) as Boolean
        else
            false
    }

    fun updateLibsDisguiseNametag(
        lmEntity: LivingEntityWrapper,
        nametag: String?
    ) {
        if (!isMobUsingLibsDisguises(lmEntity)) return

        //val disguise = lmEntity.libsDisguiseCache as me.libraryaddict.disguise.disguisetypes.Disguise
        // me.libraryaddict.disguise.disguisetypes.Disguise
        val disguise = lmEntity.libsDisguiseCache

        val clazzDisguise = Class.forName("me.libraryaddict.disguise.disguisetypes.Disguise")
        val clazzFlagWatcher = Class.forName("me.libraryaddict.disguise.disguisetypes.FlagWatcher")
        val methodGetWather = clazzDisguise.getMethod("getWatcher")
        val methodSetCustomName = clazzFlagWatcher.getMethod("setCustomName", String::class.java)

        val wrapper = SchedulerWrapper(lmEntity.livingEntity) {
            // public FlagWatcher getWatcher()
            // public void setCustomName(String name)
            // disguise.watcher.customName = nametag
            val watcher = methodGetWather.invoke(disguise)
            methodSetCustomName.invoke(watcher, nametag)

            lmEntity.free()
        }

        lmEntity.inUseCount.getAndIncrement()
        wrapper.run()
    }
}