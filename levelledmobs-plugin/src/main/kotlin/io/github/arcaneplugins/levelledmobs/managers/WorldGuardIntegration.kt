package io.github.arcaneplugins.levelledmobs.managers

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import org.bukkit.Bukkit

/**
 * Manages communication to WorldGuar for the purposes of obtaining region and flag information
 *
 * @author Eyrian, lokka30, stumper66
 * @since 2.4.0
 */
object WorldGuardIntegration {
    fun getWorldGuardRegionsForLocation(
        lmInterface: LivingEntityInterface
    ): MutableList<String> {
        val wgRegions = mutableListOf<String>()

        if (lmInterface.world == null) {
            return wgRegions
        }

        val regions: Set<ProtectedRegion> = getRegionSet(lmInterface)
            ?: return wgRegions

        for (region in regions) {
            wgRegions.add(region.id)
        }

        return wgRegions
    }

    fun getWorldGuardRegionOwnersForLocation(
        lmInterface: LivingEntityInterface): MutableList<String> {
        val wgOwners = mutableListOf<String>()

        if (lmInterface.world == null) {
            return wgOwners
        }

        val regions: Set<ProtectedRegion> = getRegionSet(lmInterface)
            ?: return wgOwners

        for (region in regions)  {
            if (region.owners.players != null) {
                for (id in region.owners.uniqueIds)  {
                    val player = Bukkit.getPlayer(id!!)
                    val playerName = player?.name ?: "unknown"
                    if (!wgOwners.contains(playerName)) {
                        wgOwners.add(playerName)
                    }
                }
            }
        }

        return wgOwners
    }

    private fun getRegionSet(
        lmInterface: LivingEntityInterface
    ): Set<ProtectedRegion>? {
        val world = BukkitAdapter.adapt(lmInterface.world)

        val position = BlockVector3.at(
            lmInterface.location!!.blockX,
            lmInterface.location!!.blockY,
            lmInterface.location!!.blockZ
        )

        val container = WorldGuard.getInstance().platform
            .regionContainer
        val regions = container[world] ?: return null

        return regions.getApplicableRegions(position).regions
    }
}