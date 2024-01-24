package io.github.arcaneplugins.levelledmobs.result

import org.bukkit.Location

/**
 * Used to hold info that is used for various
 * custom placeholders
 *
 * @author stumper66
 * @since 3.6.0
 */
class PlayerHomeCheckResult {
    var location: Location? = null
        private  set
    var resultMessage: String? = null
        private set
    var homeNameUsed: String? = null
        private set

    constructor(resultMessage: String?){
        this.resultMessage = resultMessage
    }

    constructor(resultMessage: String?, location: Location?){
        this.resultMessage = resultMessage
        this.location = location
    }

    constructor(resultMessage: String?, location: Location?, homeNameUsed: String?){
        this.resultMessage = resultMessage
        this.location = location
        this.homeNameUsed = homeNameUsed
    }
}