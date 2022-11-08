package io.github.arcaneplugins.levelledmobs.bukkit.listener;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.ItemDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDropResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import redempt.crunch.Crunch;

public class EntityDeathListener extends ListenerWrapper {

    public EntityDeathListener() {
        super(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void handle(final @Nonnull EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        if(EntityDataUtil.isLevelled(entity, true)) {
            handleItemDrops(event);
            handleExpDrops(event);
        }

        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            new Context().withEntity(entity), "on-entity-death"
        );
    }

    private void handleItemDrops(final @Nonnull EntityDeathEvent event) {
        final List<ItemStack> vanillaDrops = new LinkedList<>();
        final List<ItemStack> nonVanillaDrops = new LinkedList<>();
        final List<ItemStack> customDrops = new LinkedList<>();

        sortItemDrops(event, vanillaDrops, nonVanillaDrops, customDrops);
        generateCustomItemDrops(event, vanillaDrops, nonVanillaDrops, customDrops);
        multiplyItemDrops(event, vanillaDrops, nonVanillaDrops, customDrops);

        event.getDrops().clear();
        event.getDrops().addAll(vanillaDrops);
        event.getDrops().addAll(nonVanillaDrops);
        event.getDrops().addAll(customDrops);
    }

    private void sortItemDrops(
        final @Nonnull EntityDeathEvent event,
        final @Nonnull List<ItemStack> vanillaDrops,
        final @Nonnull List<ItemStack> nonVanillaDrops,
        final @Nonnull List<ItemStack> customDrops
    ) {
        // Note that LM Equipment which is dropped from the mob is considered non-vanilla
        // in the item sorting, NOT as an itemstack to be added to the customDrops list.
        // The customDrops list contains a list of NEW itemstacks to be dropped by the mob
        // upon their death as defined in the custom drops file.

        for(final ItemStack drop : event.getDrops()) {
            if(isNonVanillaItemDrop(event.getEntity(), drop)) {
                nonVanillaDrops.add(drop);
            } else {
                vanillaDrops.add(drop);
            }
        }
    }

    private boolean isNonVanillaItemDrop(
        final @Nonnull LivingEntity entity,
        final @Nonnull ItemStack itemStack
    ) {
        // Return true if the item was created by LM ('Custom').
        if(ItemDataUtil.isItemCustom(itemStack).toFalsyBoolean())
            return true;

        // Return true if item is in the entity's inventory
        return entity instanceof InventoryHolder ih && ih.getInventory().contains(itemStack);
    }

    private void generateCustomItemDrops(
        final @Nonnull EntityDeathEvent event,
        final @Nonnull List<ItemStack> vanillaDrops,
        final @Nonnull List<ItemStack> nonVanillaDrops,
        final @Nonnull List<ItemStack> customDrops
    ) {
        //TODO FIXME IMPORTANT !!!!!!!!!!!!!!!!
        // Remove instantiation of CDR here, it's just to stop IDE from complaining before
        //  CustomDropHandler#createDropResult(EntityDeathEvent) is implemented.
        final CustomDropResult result =
            new CustomDropResult(Collections.emptyList(), false, false);

        customDrops.addAll(result.getDrops());

        if(result.overridesVanillaDrops())
            vanillaDrops.clear();

        if(result.overridesNonVanillaDrops())
            nonVanillaDrops.clear();
    }

    private void multiplyItemDrops(
        final @Nonnull EntityDeathEvent event,
        final @Nonnull List<ItemStack> vanillaDrops,
        final @Nonnull List<ItemStack> nonVanillaDrops,
        final @Nonnull List<ItemStack> customDrops
    ) {
        final LivingEntity entity = event.getEntity();

        final double evaluatedMultiplier = Crunch.evaluateExpression(
            LogicHandler.replacePapiAndContextPlaceholders(
                EntityDataUtil.getItemDropMultiplierFormula(entity, true),
                new Context().withEntity(entity)
            )
        );

        final Function<ItemStack, ItemStack[]> stackMultiplier = (stack) -> {
            final int newAmount = (int) (stack.getAmount() * evaluatedMultiplier);

            final int additionalStacks = (int) Math.floor(newAmount * 1.0d / stack.getMaxStackSize());
            final int lastStack = newAmount - (stack.getMaxStackSize() * additionalStacks);

            final ItemStack[] result = new ItemStack[additionalStacks + 1];

            for(int i = 1; i < additionalStacks; i++) {
                final ItemStack is = stack.clone();
                is.setAmount(stack.getMaxStackSize());
                result[i - 1] = is;
            }

            stack.setAmount(lastStack);
            result[additionalStacks] = stack;

            return result;
        };

        /*
        handle vanilla drops
         */
        final LinkedList<ItemStack> newVanillaDrops = new LinkedList<>();
        for(final ItemStack vanillaDrop : vanillaDrops) {
            newVanillaDrops.addAll(Arrays.asList(stackMultiplier.apply(vanillaDrop)));
        }
        vanillaDrops.clear();
        vanillaDrops.addAll(newVanillaDrops);

        /*
        handle custom drops
         */
        final LinkedList<ItemStack> newCustomDrops = new LinkedList<>();
        for(final ItemStack customDrop : customDrops) {
            //TODO if custom drop is not multipliable, then `continue;`
            newCustomDrops.addAll(Arrays.asList(stackMultiplier.apply(customDrop)));
        }
        customDrops.clear();
        customDrops.addAll(newCustomDrops);


        /*
        handle non-vanilla drops
         */
        final boolean multiplyNonVanillaDrops = LevelledMobs.getInstance()
            .getConfigHandler()
            .getSettingsCfg()
            .getRoot().node("advanced", "multiply-non-vanilla-drops")
            .getBoolean(false);

        if(multiplyNonVanillaDrops) {
            final LinkedList<ItemStack> newNonVanillaDrops = new LinkedList<>();
            for(final ItemStack nonVanillaDrop : nonVanillaDrops) {
                newNonVanillaDrops.addAll(Arrays.asList(stackMultiplier.apply(nonVanillaDrop)));
            }
            nonVanillaDrops.clear();
            nonVanillaDrops.addAll(newNonVanillaDrops);
        }
    }

    private void handleExpDrops(final @Nonnull EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        final String multFormula = EntityDataUtil
            .getExpDropMultiplierFormula(entity, true);

        if(multFormula == null || multFormula.isBlank()) return;

        final double eval = Crunch.evaluateExpression(
            LogicHandler.replacePapiAndContextPlaceholders(
                multFormula,
                new Context().withEntity(entity)
            )
        );

        event.setDroppedExp(event.getDroppedExp() * (int) eval);
    }

}
