package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.enums.ModalListParsingTypes
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList

/**
 * Holds various info used for parsing rules.yml
 *
 * @author stumper66
 * @since 3.7.5
 */
class ModalListParsingInfo(
    val type: ModalListParsingTypes
) {
    var configurationKey: String? = null
    var itemName: String? = null
    var supportsGroups: Boolean = false
    var cachedModalList: CachedModalList<*>? = null
    var groupMapping: MutableMap<String, MutableSet<String>>? = null
}