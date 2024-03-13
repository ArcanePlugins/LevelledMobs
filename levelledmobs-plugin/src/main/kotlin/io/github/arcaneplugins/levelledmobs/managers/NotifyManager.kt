package io.github.arcaneplugins.levelledmobs.managers

import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MessageUtils
import java.time.Duration
import java.time.Instant
import org.bukkit.Bukkit

object NotifyManager {
    private var lastNotification: Instant? = null
    private const val MIN_NOTIFY_SECONDS = 30L
    var pendingMessage: String? = null
        private set
    private var opGotNotified = false
    var opHasMessage: Boolean = false
        get() = pendingMessage != null && !opGotNotified
        private set

    fun notifyOfError(message: String){
        if (!canNotify()) return

        Log.sev(message)

        this.lastNotification = Instant.now()
        this.pendingMessage = null
        this.opGotNotified = false
        for (player in Bukkit.getOnlinePlayers()){
            if (!player.isOp) continue

            this.opGotNotified = true
            player.sendMessage(MessageUtils.colorizeStandardCodes("Severe: &c$message&r"))
        }

        if (!opGotNotified) pendingMessage = message
    }

    private fun canNotify(): Boolean{
        if (lastNotification == null) return true

        val duration = Duration.between(lastNotification, Instant.now()).toSeconds()
        return (duration > MIN_NOTIFY_SECONDS)
    }

    fun clearLastError(){
        this.lastNotification = null
        this.pendingMessage = null
    }
}