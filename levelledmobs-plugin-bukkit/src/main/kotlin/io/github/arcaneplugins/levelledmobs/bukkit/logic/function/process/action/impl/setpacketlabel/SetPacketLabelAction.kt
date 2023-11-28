package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setpacketlabel

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.getLabelHandlerFormulaMap
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setLabelHandlerFormulaMap
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import io.github.arcaneplugins.levelledmobs.bukkit.logic.label.LabelRegistry.setPrimaryLabelHandler
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.TimeUtils.parseTimeToTicks
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import java.util.*

class SetPacketLabelAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    val formula: String
    val visibilityMethods: EnumSet<VisibilityMethod> =
        EnumSet.noneOf(VisibilityMethod::class.java)
    val visibilityDuration: Long
    val primary: Boolean
    companion object val LABEL_ID = "Packet"

    init {
        formula = actionNode.node("formula").getString("")

        val visibilityMethodsStr: List<String> = try {
            actionNode.node("visibility-methods")
                .getList(String::class.java, emptyList())
        } catch (ex: SerializationException) {
            throw RuntimeException(ex)
        }
        for (visibilityMethodStr in visibilityMethodsStr) {
            visibilityMethods.add(
                VisibilityMethod.valueOf(visibilityMethodStr.uppercase())
            )
        }

        visibilityDuration = parseTimeToTicks(
            actionNode.node("visibility-duration").getString("5s")
        )

        primary = actionNode.node("primary").getBoolean(false)
    }

    override fun run(context: Context) {
        if (context.livingEntity == null) return
        val lent = context.livingEntity!!
        val labelHandlerFormulaMap = getLabelHandlerFormulaMap(lent, true).toMutableMap()

        labelHandlerFormulaMap[LABEL_ID] = formula

        setLabelHandlerFormulaMap(lent, labelHandlerFormulaMap, true)

        if (primary) setPrimaryLabelHandler(lent, LABEL_ID, true)

        debug(DebugCategory.PACKET_LABELS) { "SetPacketLabelAction#run; sending empty update packet" }
        //SetPacketLabelAction.PacketLabelHandler.INSTANCE.update(lent, context)
    }
}