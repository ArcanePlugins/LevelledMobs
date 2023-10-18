package io.github.arcaneplugins.levelledmobs.bukkit.config.translations

import org.apache.commons.lang3.Validate
import java.lang.IllegalArgumentException

enum class InbuiltLang {
    /**
     * German
     *
     * @since 4.0.0
     */
    DE_DE,
    /**
     * English (Australia)
     * Joke translation, not intended for actual use.
     *
     * @since 4.0.0
     */
    EN_AU,
    /**
     * English (Great Britain)
     *
     * @since 4.0.0
     */
    EN_GB,
    /**
     * English (United States of America)
     *
     * @since 4.0.0
     */
    EN_US,
    /**
     * Spanish
     *
     * @since 4.0.0
     */
    ES_ES,
    /**
     * French
     *
     * @since 4.0.0
     */
    FR_FR;

    /**
     * @return language code, formatted in the IEEE-standard manner (e.g. 'en_GB', 'fr_FR').
     */
    override fun toString(): String {
        val uppercase = super.toString()
        val split = uppercase.split("_")

        Validate.isTrue(split.size == 2,
            "Invalid language code '$uppercase'!"
        )

        return split[0].lowercase() + "_" + split[1].uppercase()
    }

    companion object{
        fun of(languageCode: String) : InbuiltLang?{
            return try{
                InbuiltLang.valueOf(languageCode.uppercase())
            } catch (e: IllegalArgumentException){
                null
            }
        }

        fun getDefault(): InbuiltLang{
            return EN_US
        }
    }
}