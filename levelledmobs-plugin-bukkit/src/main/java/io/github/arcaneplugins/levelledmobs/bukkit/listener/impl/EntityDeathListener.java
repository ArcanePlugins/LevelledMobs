package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.DEATH_DROPS;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.ItemDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDropHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.EntityDeathCustomDropResult;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.CommandCustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class EntityDeathListener extends ListenerWrapper {

    public EntityDeathListener() {
        super(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void handle(final @Nonnull EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        final Context context = new Context().withEntity(entity);

        if(EntityDataUtil.isLevelled(entity, true)) {
            Log.debug(DEATH_DROPS, () -> "Entity is levelled, handling death drops");
            handleItemDrops(event, context);
            handleExpDrops(event);
            Log.debug(DEATH_DROPS, () -> "Finished handling death drops");
        }

        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            context, "on-entity-death"
        );
    }

    private void handleItemDrops(
        final @Nonnull EntityDeathEvent event,
        final @Nonnull Context context
    ) {
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
        final List<CustomDrop> customDrops = new LinkedList<>();
        final List<ItemStack> customDropsToDrop = new LinkedList<>();

        sortItemDrops(event, vanillaDrops, nonVanillaDrops);
        generateCustomItemDrops(event, vanillaDrops, nonVanillaDrops, customDrops);
        multiplyItemDrops(event, context,
            vanillaDrops, nonVanillaDrops,
            customDrops, customDropsToDrop
        );

        Log.debug(DEATH_DROPS, () -> "v-drops: %s, nv-drops: %s, cd: %s, cd-td: %s".formatted(
            vanillaDrops.size(), nonVanillaDrops.size(),
            customDrops.size(), customDropsToDrop.size())
        );

        event.getDrops().clear();
        event.getDrops().addAll(vanillaDrops);
        event.getDrops().addAll(nonVanillaDrops);
        event.getDrops().addAll(customDropsToDrop);

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
        final @Nonnull List<CustomDrop> customDrops
    ) {
        Log.debug(DEATH_DROPS, () -> "[GenCustDrp] Generating custom item drops");

        final EntityDeathCustomDropResult result = CustomDropHandler.handleEntityDeath(event);

        Log.debug(DEATH_DROPS, () -> "[GenCustDrp] Count: %s; Over-Van: %s; Over-NonVan: %s"
            .formatted(
                result.getDrops().size(),
                result.overridesVanillaDrops(),
                result.overridesNonVanillaDrops())
        );

        customDrops.addAll(result.getDrops());

        final EntityDamageEvent lastDamage = event.getEntity().getLastDamageCause();
        if(lastDamage instanceof final EntityDamageByEntityEvent lastDamageByEntity) {
            if(lastDamageByEntity.getDamager() instanceof final Player player) {
                final int lootingLevel = getPlayerItemLootingEnchLevel(player);
                for(final CustomDrop drop : customDrops) {
                    drop.withChance(Math.max(1f, drop.getChance() + (0.01f * lootingLevel)));
                }
            }
        }

        if(result.overridesVanillaDrops())
            vanillaDrops.clear();

        if(result.overridesNonVanillaDrops())
            nonVanillaDrops.clear();
    }

    /*
    Multiplies applicable items

    It also runs applicable Command custom drops
     */
    private void multiplyItemDrops(
        final @Nonnull EntityDeathEvent event,
        final @Nonnull Context context,
        final @Nonnull List<ItemStack> vanillaDrops,
        final @Nonnull List<ItemStack> nonVanillaDrops,
        final @Nonnull List<CustomDrop> customDrops,
        final @Nonnull List<ItemStack> customDropsToDrop
    ) {
        Log.debug(DEATH_DROPS, () -> "[Mult] Multiplying item drops");

        final LivingEntity entity = event.getEntity();

        final double evaluatedMultiplier = LogicHandler.evaluateExpression(
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
        for(final CustomDrop customDrop : customDrops) {
            if(customDrop instanceof final ItemCustomDrop icd) {
                final ItemStack is = icd.toItemStack();
                if(icd.hasNoMultiplier()) {
                    customDropsToDrop.add(is);
                } else {
                    customDropsToDrop.addAll(Arrays.asList(stackMultiplier.apply(is)));
                }
            } else if(customDrop instanceof final CommandCustomDrop ccd) {
                Log.debug(DEATH_DROPS, () -> "[Mult] Command drop detected, running if applicable");
                if(ccd.getCommandRunEvents().contains("ON_DEATH")) {
                    ccd.execute(context);
                }
            }
        }


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

        final double eval = LogicHandler.evaluateExpression(
            LogicHandler.replacePapiAndContextPlaceholders(
                multFormula,
                new Context().withEntity(entity)
            )
        );

        Log.debug(DEATH_DROPS, () -> "[Exp] Multiplying exp drops by " + eval);

        event.setDroppedExp((int) (event.getDroppedExp() * eval));

        Log.debug(DEATH_DROPS, () -> "[Exp] finishing with " + event.getDroppedExp());
    }

    private static @Nonnull ItemStack getPlayerPrimaryHeldItem(
        final @Nonnull Player player
    ) {
        final PlayerInventory inv = player.getInventory();

        return inv.getItemInMainHand().getType() == Material.AIR ?
            inv.getItemInOffHand() : inv.getItemInMainHand();
    }

    private static int getPlayerItemLootingEnchLevel(
        final @Nonnull Player player
    ) {
        return getPlayerPrimaryHeldItem(player).getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
    }

}
