package io.github.arcaneplugins.levelledmobs.result

/**
 * Used for PlayerLevelling options
 *
 * @author stumper66
 * @since 3.3.0
 */
class PlayerLevelSourceResult {
    var numericResult = 0
    var randomVarianceResult: Int? = null
    var stringResult: String? = null
        private set
    var isNumericResult = false
        private set
    var homeNameUsed: String? = null

    constructor(numericResult: Int){
        this.numericResult = numericResult
        this.isNumericResult = true
    }

    constructor(stringResult: String?){
        this.stringResult = stringResult ?: ""
    }
}