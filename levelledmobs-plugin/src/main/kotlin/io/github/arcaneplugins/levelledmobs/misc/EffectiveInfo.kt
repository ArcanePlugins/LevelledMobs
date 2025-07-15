package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper

/**
 * If a class used in rules implements this interface then it will be
 * called when executing the 'rules show-effective' command rather
 * than a simple #toString() call
 *
 * @author stumper66
 * @since 4.4.0
 */
interface EffectiveInfo {
    fun getEffectiveInfo(lmEntity: LivingEntityWrapper): String
}