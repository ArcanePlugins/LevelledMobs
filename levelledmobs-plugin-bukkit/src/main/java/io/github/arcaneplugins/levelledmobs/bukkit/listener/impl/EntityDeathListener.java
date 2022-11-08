package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.DEATH_DROPS;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.ItemDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDropResult;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
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
            Log.debug(DEATH_DROPS, () -> "Entity is levelled, handling death drops");
            handleItemDrops(event);
            handleExpDrops(event);
            Log.debug(DEATH_DROPS, () -> "Finished handling death drops");
        }

        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            new Context().withEntity(entity), "on-entity-death"
        );
    }

    private void handleItemDrops(final @Nonnull EntityDeathEvent event) {
        final Supplier<String> debugDropLister = () -> {
            final StringBuilder sb = new StringBuilder("*** Total Drop Evaluation (%s): "
                .formatted(event.getDrops().size()));

            for(final ItemStack is : event.getDrops()) {
                if(is == null) {
                    sb.append("[!!null!!]; ");
                } else {
                    sb.append("[%s x%s]; ".formatted(is.getType(), is.getAmount()));
                }
            }

            return sb.toString();
        };

        Log.debug(DEATH_DROPS, debugDropLister);

        final List<ItemStack> vanillaDrops = new LinkedList<>();
        final List<ItemStack> nonVanillaDrops = new LinkedList<>();
        final List<ItemStack> customDrops = new LinkedList<>();

        sortItemDrops(event, vanillaDrops, nonVanillaDrops);
        generateCustomItemDrops(event, vanillaDrops, nonVanillaDrops, customDrops);
        multiplyItemDrops(event, vanillaDrops, nonVanillaDrops, customDrops);

        Log.debug(DEATH_DROPS, () -> "v-drops: %s, nv-drops: %s, c-drops: %s".formatted(
            vanillaDrops.size(), nonVanillaDrops.size(), customDrops.size()));

        event.getDrops().clear();
        event.getDrops().addAll(vanillaDrops);
        event.getDrops().addAll(nonVanillaDrops);
        event.getDrops().addAll(customDrops);

        Log.debug(DEATH_DROPS, debugDropLister);
    }

    private void sortItemDrops(
        final @Nonnull EntityDeathEvent event,
        final @Nonnull List<ItemStack> vanillaDrops,
        final @Nonnull List<ItemStack> nonVanillaDrops
    ) {
        Log.debug(DEATH_DROPS, () -> "[sort] Sorting item drops");
        // Note that LM Equipment which is dropped from the mob is considered non-vanilla
        // in the item sorting, NOT as an itemstack to be added to the customDrops list.
        // The customDrops list contains a list of NEW itemstacks to be dropped by the mob
        // upon their death as defined in the custom drops file.

        for(final ItemStack drop : event.getDrops()) {
            if(isNonVanillaItemDrop(event.getEntity(), drop)) {
                Log.debug(DEATH_DROPS, () -> "[sort] Sorted non-vanilla drop '%s'."
                    .formatted(drop.getType()));
                nonVanillaDrops.add(drop);
            } else {
                Log.debug(DEATH_DROPS, () -> "[sort] Sorted vanilla drop '%s'."
                    .formatted(drop.getType()));
                vanillaDrops.add(drop);
            }
        }
    }

    private boolean isNonVanillaItemDrop(
        final @Nonnull LivingEntity entity,
        final @Nonnull ItemStack itemStack
    ) {
        Log.debug(DEATH_DROPS, () -> "[isNonVanilla] Checking if '%s' is non-vanilla"
            .formatted(itemStack.getType()));

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
        Log.debug(DEATH_DROPS, () -> "[GenCustDrp] Generating custom item drops");

        //TODO FIXME IMPORTANT !!!!!!!!!!!!!!!!
        // Remove instantiation of CDR here, it's just to stop IDE from complaining before
        //  CustomDropHandler#createDropResult(EntityDeathEvent) is implemented.
        final CustomDropResult result =
            new CustomDropResult(Collections.emptyList(), false, false);

        Log.debug(DEATH_DROPS, () -> "[GenCustDrp] Count: %s; Over-Van: %s; Over-NonVan: %s"
            .formatted(
                result.getDrops().size(),
                result.overridesVanillaDrops(),
                result.overridesNonVanillaDrops())
        );

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
        Log.debug(DEATH_DROPS, () -> "[Mult] Multiplying item drops");

        final LivingEntity entity = event.getEntity();

        final double evaluatedMultiplier = Crunch.evaluateExpression(
            LogicHandler.replacePapiAndContextPlaceholders(
                EntityDataUtil.getItemDropMultiplierFormula(entity, true),
                new Context().withEntity(entity)
            )
        );

        Log.debug(DEATH_DROPS, () -> "[Mult] Evaluated multiplier: " + evaluatedMultiplier);

        final Function<ItemStack, ItemStack[]> stackMultiplier = (stack) -> {
            final int newAmount = (int) (stack.getAmount() * evaluatedMultiplier);
            Log.debug(DEATH_DROPS, () -> "[Mult] NewAmount=" + newAmount);

            final int additionalStacks = (int) Math.floor(newAmount * 1.0d / stack.getMaxStackSize());
            final int lastStack = newAmount - (stack.getMaxStackSize() * additionalStacks);

            Log.debug(DEATH_DROPS, () -> "[Mult] AdditionalStacks=" + additionalStacks);
            Log.debug(DEATH_DROPS, () -> "[Mult] LastStack=" + lastStack);

            final ItemStack[] result = new ItemStack[additionalStacks + 1];

            for(int i = 1; i <= additionalStacks; i++) {
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
        Log.debug(DEATH_DROPS, () -> "[Mult] Multiplying vanilla drops");
        final LinkedList<ItemStack> newVanillaDrops = new LinkedList<>();
        for(final ItemStack vanillaDrop : vanillaDrops) {
            newVanillaDrops.addAll(Arrays.asList(stackMultiplier.apply(vanillaDrop)));
        }
        vanillaDrops.clear();
        vanillaDrops.addAll(newVanillaDrops);

        /*
        handle custom drops
         */
        Log.debug(DEATH_DROPS, () -> "[Mult] Multiplying custom drops");
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

        Log.debug(DEATH_DROPS, () -> "[Mult] Multiply non-vanilla drops?=" + multiplyNonVanillaDrops);

        if(multiplyNonVanillaDrops) {
            Log.debug(DEATH_DROPS, () -> "[Mult] Multiplying non-vanilla drops");
            final LinkedList<ItemStack> newNonVanillaDrops = new LinkedList<>();
            for(final ItemStack nonVanillaDrop : nonVanillaDrops) {
                newNonVanillaDrops.addAll(Arrays.asList(stackMultiplier.apply(nonVanillaDrop)));
            }
            nonVanillaDrops.clear();
            nonVanillaDrops.addAll(newNonVanillaDrops);
        }
    }

    private void handleExpDrops(final @Nonnull EntityDeathEvent event) {
        Log.debug(DEATH_DROPS, () -> "[Exp] Handling exp; starting with " + event.getDroppedExp());
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

        Log.debug(DEATH_DROPS, () -> "[Exp] Multiplying exp drops by " + eval);

        event.setDroppedExp((int) (event.getDroppedExp() * eval));

        Log.debug(DEATH_DROPS, () -> "[Exp] finishing with " + event.getDroppedExp());
    }

}
