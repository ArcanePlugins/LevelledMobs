package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.enums.InternalSpawnReason
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.persistence.PersistentDataType

class LMSpawnReason {
    private var _internalSpawnReason: InternalSpawnReason? = null
    private var _minecraftSpawnReason: SpawnReason? = null
    private var hasCheckedPDCKey = false

    init {
        _minecraftSpawnReason = SpawnReason.DEFAULT
    }

    companion object{
        fun validateSpawnReason(name: String): Boolean{
            try {
                SpawnReason.valueOf(name)
                return true
            }
            catch (_: Exception){ }

            try {
                InternalSpawnReason.valueOf(name)
                return true
            }
            catch (_: Exception){ }

            return false
        }
    }

    fun getMinecraftSpawnReason(lmEntity: LivingEntityWrapper): SpawnReason?{
        if (!hasCheckedPDCKey) checkPDCKey(lmEntity)
        return _minecraftSpawnReason
    }

    fun setMinecraftSpawnReason(
        lmEntity: LivingEntityWrapper,
        spawnReason: SpawnReason?,
        doForce: Boolean = false
    ){
        _minecraftSpawnReason = spawnReason
        _internalSpawnReason = null
        setPDCKey(lmEntity, doForce)
    }

    fun getInternalSpawnReason(lmEntity: LivingEntityWrapper): InternalSpawnReason?{
        if (!hasCheckedPDCKey) checkPDCKey(lmEntity)
        return _internalSpawnReason
    }

    fun setInternalSpawnReason(
        lmEntity: LivingEntityWrapper?,
        spawnReason: InternalSpawnReason?,
        doForce: Boolean = false
    ){
        _internalSpawnReason = spawnReason
        _minecraftSpawnReason = null
        if (lmEntity != null) setPDCKey(lmEntity, doForce)
    }

    val hasSpawnReason: Boolean
        get() = _internalSpawnReason != null || _minecraftSpawnReason != null

    fun clear(){
        _internalSpawnReason = null
        _minecraftSpawnReason = null
    }

    fun setLMSpawnReason(lmEntity: LivingEntityWrapper){
        val lmSpawnReason = lmEntity.spawnReason

        val minecraftReason = lmSpawnReason.getMinecraftSpawnReason(lmEntity)
        if (minecraftReason != null){
            setMinecraftSpawnReason(lmEntity, minecraftReason)
            return
        }

        val internalReason = lmSpawnReason.getInternalSpawnReason(lmEntity)
        if (internalReason != null){
            setInternalSpawnReason(lmEntity, internalReason)
            return
        }

        clear()
    }

    fun checkPDCKey(lmEntity: LivingEntityWrapper){
        hasCheckedPDCKey = true
        var temp: String? = null
        for (i in 0..1) {
            try {
                if (lmEntity.pdc.has(NamespacedKeys.spawnReasonKey, PersistentDataType.STRING))
                    temp = lmEntity.pdc[NamespacedKeys.spawnReasonKey, PersistentDataType.STRING]
                break
            }
            catch (ignored: ConcurrentModificationException) {
                _minecraftSpawnReason = SpawnReason.DEFAULT
                _internalSpawnReason = null
            }
        }

        if (!temp.isNullOrEmpty()){
            var spawnReason: SpawnReason? = null
            var internalSpawnReason: InternalSpawnReason? = null

            try { spawnReason = SpawnReason.valueOf(temp) }
            catch (_: Exception){ }

            if (spawnReason != null)
                _minecraftSpawnReason = spawnReason
            else{
                try { internalSpawnReason = InternalSpawnReason.valueOf(temp) }
                catch (_: Exception){ }

                if (internalSpawnReason != null) _internalSpawnReason = internalSpawnReason
            }
        }
    }

    private fun setPDCKey(lmEntity: LivingEntityWrapper, doForce: Boolean = false){
        hasCheckedPDCKey = true
        val keyValue = if (_internalSpawnReason != null) _internalSpawnReason.toString()
        else _minecraftSpawnReason.toString()
        if (doForce || !lmEntity.pdc.has(NamespacedKeys.spawnReasonKey, PersistentDataType.STRING)) {
            lmEntity.pdc.set(
                NamespacedKeys.spawnReasonKey, PersistentDataType.STRING,
                keyValue
            )
        }
    }

    override fun toString(): String {
        return if (hasSpawnReason){
            if (_internalSpawnReason != null) _internalSpawnReason!!.toString()
            else _minecraftSpawnReason!!.toString()
        } else
            "UNKNOWN"
    }
}