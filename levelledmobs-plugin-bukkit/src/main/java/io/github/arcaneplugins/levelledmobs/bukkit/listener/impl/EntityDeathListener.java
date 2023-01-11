package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.DROPS;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.ItemDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDropHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.cdevent.CustomDropsEventType;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.CommandCustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.StandardCustomDropType;
import io.github.arcaneplugins.levelledmobs.bukkit.util.EquipmentUtils;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
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
            Log.debug(DROPS, () -> "Entity is levelled, handling death drops");
            handleItemDrops(event, context);
            handleExpDrops(event);
            Log.debug(DROPS, () -> "Finished handling death drops");
        }

        LogicHandler.runFunctionsWithTriggers(
            context, "on-entity-death"
        );
    }

    private void handleItemDrops(
        final @Nonnull EntityDeathEvent event,
        final @Nonnull Context context
    ) {
        final LivingEntity entity = event.getEntity();

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

        Log.debug(DROPS, debugDropLister);

        final List<ItemStack> vanillaDrops = new LinkedList<>();
        final List<ItemStack> nonVanillaDrops = new LinkedList<>();
        final List<CustomDrop> customDrops = new LinkedList<>();
        final List<ItemStack> customDropsToDrop = new LinkedList<>();

        sortItemDrops(event, vanillaDrops, nonVanillaDrops);
        generateCustomDrops(event, vanillaDrops, nonVanillaDrops, customDrops);
        multiplyItemDrops(event, context,
            vanillaDrops, nonVanillaDrops,
            customDrops, customDropsToDrop
        );

        Log.debug(DROPS, () -> """
            Custom Drops to Drop (pre-filtering): %s."""
            .formatted(
                customDropsToDrop.size()
            )
        );

        customDropsToDrop.removeIf(itemStack ->
            itemStack != null && EquipmentUtils.findSimilarItemStackInEntity(
                entity,
                itemStack,
                predicateStack -> predicateStack != null &&
                    ItemDataUtil.isItemCustom(predicateStack).toFalsyBoolean()
            ) != null);

        Log.debug(DROPS, () -> """
            Vanilla Drops: %s; Non-Vanilla Drops: %s; Custom Drops: %s; Custom Drops to Drop: %s."""
            .formatted(
                vanillaDrops.size(), nonVanillaDrops.size(),
                customDrops.size(), customDropsToDrop.size()
            )
        );

        event.getDrops().clear();
        event.getDrops().addAll(vanillaDrops);
        event.getDrops().addAll(nonVanillaDrops);
        event.getDrops().addAll(customDropsToDrop);

        Log.debug(DROPS, debugDropLister);
    }

    private void sortItemDrops(
        final @Nonnull EntityDeathEvent event,
        final @Nonnull List<ItemStack> vanillaDrops,
        final @Nonnull List<ItemStack> nonVanillaDrops
    ) {
        Log.debug(DROPS, () -> "[sort] Sorting item drops");
        // Note that LM Equipment which is dropped from the mob is considered non-vanilla
        // in the item sorting, NOT as an itemstack to be added to the customDrops list.
        // The customDrops list contains a list of NEW itemstacks to be dropped by the mob
        // upon their death as defined in the custom drops file.

        for(final ItemStack drop : event.getDrops()) {
            if(isNonVanillaItemDrop(event.getEntity(), drop)) {
                Log.debug(DROPS, () -> "[sort] Sorted non-vanilla drop '%s'."
                    .formatted(drop.getType()));
                nonVanillaDrops.add(drop);
            } else {
                Log.debug(DROPS, () -> "[sort] Sorted vanilla drop '%s'."
                    .formatted(drop.getType()));
                vanillaDrops.add(drop);
            }
        }
    }

    private boolean isNonVanillaItemDrop(
        final @Nonnull LivingEntity entity,
        final @Nonnull ItemStack itemStack
    ) {
        Log.debug(DROPS, () -> "[isNonVanilla] Checking if '%s' is non-vanilla"
            .formatted(itemStack.getType()));

        // Return true if the item was created by LM ('Custom').
        if(ItemDataUtil.isItemCustom(itemStack).toFalsyBoolean())
            return true;

        // Return true if item is in the entity's inventory
        return entity instanceof InventoryHolder ih && ih.getInventory().contains(itemStack);
    }

    private void generateCustomDrops(
        final @Nonnull EntityDeathEvent event,
        final @Nonnull List<ItemStack> vanillaDrops,
        final @Nonnull List<ItemStack> nonVanillaDrops,
        final @Nonnull List<CustomDrop> customDrops
    ) {
        final LivingEntity entity = event.getEntity();

        Log.debug(DROPS, () -> "[GenCustDrp] Generating custom item drops. "
            + "Collection size: %s".formatted(customDrops.size()));

        final Context context = new Context()
            .withEntity(entity)
            .withEvent(event);

        Log.debug(DROPS, () -> "attempting to retrieve player context");
        Player player = null;
        if(entity.getLastDamageCause() != null) {
            Log.debug(DROPS, () -> "last damage cause found");
            if(entity.getLastDamageCause() instanceof final EntityDamageByEntityEvent lastDamage) {
                Log.debug(DROPS, () -> "last damage cause is instanceof ...ByEntity");
                if(lastDamage.getDamager() instanceof final Player found) {
                    player = found;
                    context.withPlayer(player);
                    Log.debug(DROPS, () -> "player context found");
                }
            }
        }

        customDrops.addAll(
            CustomDropHandler.getDefinedCustomDropsForEntity(
                entity,
                context
            )
        );

        Log.debug(DROPS, () -> "[GenCustDrp] Generated %s custom drops."
            .formatted(customDrops.size()));

        Log.debug(DROPS, () -> "[GenCustDrp] Filtering custom drops by chance");
        customDrops.removeIf(drop -> drop.getChance() == 0f ||
            drop.getChance() < ThreadLocalRandom.current().nextFloat(0, 100));
        Log.debug(DROPS, () -> "[GenCustDrp] Drops chance-filtered, resulting in %s custom drops."
            .formatted(customDrops.size()));

        Log.debug(DROPS, () -> "[GenCustDrp] Filtering custom drops by death cause");
        customDrops.removeIf(drop -> {
            Log.debug(DROPS, () -> "START Checking drop of type " + drop.getType());
            final Collection<String> deathCauses = drop.getDeathCauses();
            Log.debug(DROPS, () -> "Allowed death causes: " + deathCauses + "(OK)");
            if (deathCauses.isEmpty())
                return false;
            Log.debug(DROPS, () -> "Death causes is not empty; continuing (OK)");

            final EntityDamageEvent ed = entity.getLastDamageCause();
            if (ed == null)
                return false;
            Log.debug(DROPS, () -> "Last damage cause is defined (OK)");

            if(ed instanceof final EntityDamageByEntityEvent ede &&
                ede.getDamager().getType() == EntityType.PLAYER
            ) {
                Log.debug(DROPS, () ->
                    "removing drop due to player death cause: " + !deathCauses.contains("PLAYER")
                );
                return !deathCauses.contains("PLAYER");
            }

            Log.debug(DROPS, () -> "checking death cause " + ed.getCause().name());
            return !deathCauses.contains(ed.getCause().name());
        });
        Log.debug(DROPS, () -> "[GenCustDrp] Drops cause-filtered, resulting in %s custom drops."
            .formatted(customDrops.size()));

        /*
        We want to increase the chance of custom item drops if the player is using a looting item.
         */
        if(player != null) {
            final int lootingLevel = getPlayerItemLootingEnchLevel(player);

            Log.debug(DROPS, () -> "[GenCustDrp] Player looting level: %s"
                .formatted(lootingLevel));

            if(lootingLevel > 0) {
                for(final CustomDrop drop : customDrops) {
                    if(!drop.getType().equals(StandardCustomDropType.ITEM.name())) return;

                    drop.withChance(Math.max(0, Math.min(100.0f, drop.getChance() + (1.0f * lootingLevel))));
                }
            }
        }

        if(customDrops.stream().anyMatch(CustomDrop::shouldOverrideVanillaDrops))
            vanillaDrops.clear();

        if(customDrops.stream().anyMatch(CustomDrop::shouldOverrideNonVanillaDrops))
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
        Log.debug(DROPS, () -> "[Mult] Multiplying item drops");

        final LivingEntity entity = event.getEntity();

        final double evaluatedMultiplier = LogicHandler.evaluateExpression(
            LogicHandler.replacePapiAndContextPlaceholders(
                EntityDataUtil.getItemDropMultiplierFormula(entity, true),
                new Context().withEntity(entity)
            )
        );

        Log.debug(DROPS, () -> "[Mult] Evaluated multiplier: " + evaluatedMultiplier);

        final Function<ItemStack, ItemStack[]> stackMultiplier = (stack) -> {
            final int newAmount = (int) (stack.getAmount() * evaluatedMultiplier);
            Log.debug(DROPS, () -> "[Mult] NewAmount=" + newAmount);

            final int additionalStacks = (int) Math.floor(newAmount * 1.0d / stack.getMaxStackSize());
            final int lastStack = newAmount - (stack.getMaxStackSize() * additionalStacks);

            Log.debug(DROPS, () -> "[Mult] AdditionalStacks=" + additionalStacks);
            Log.debug(DROPS, () -> "[Mult] LastStack=" + lastStack);

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
        Log.debug(DROPS, () -> "[Mult] Multiplying vanilla drops");
        final LinkedList<ItemStack> newVanillaDrops = new LinkedList<>();
        for(final ItemStack vanillaDrop : vanillaDrops) {
            newVanillaDrops.addAll(Arrays.asList(stackMultiplier.apply(vanillaDrop)));
        }
        vanillaDrops.clear();
        vanillaDrops.addAll(newVanillaDrops);

        /*
        handle custom drops
         */
        Log.debug(DROPS, () -> "[Mult] Multiplying custom drops");
        for(final CustomDrop customDrop : customDrops) {
            if(customDrop instanceof final ItemCustomDrop icd) {
                final ItemStack is = icd.toItemStack();

                if(icd.requiresNoMultiplier()) {
                    customDropsToDrop.add(is);
                } else {
                    customDropsToDrop.addAll(Arrays.asList(stackMultiplier.apply(is)));
                }
            } else if(customDrop instanceof final CommandCustomDrop ccd) {
                Log.debug(DROPS, () -> "[Mult] Command drop detected, running if applicable");
                if(ccd.getCommandRunEvents().contains("ON_DEATH")) {
                    ccd.execute(CustomDropsEventType.ON_DEATH, context);
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

        Log.debug(DROPS, () -> "[Mult] Multiply non-vanilla drops?=" + multiplyNonVanillaDrops);

        if(multiplyNonVanillaDrops) {
            Log.debug(DROPS, () -> "[Mult] Multiplying non-vanilla drops");
            final LinkedList<ItemStack> newNonVanillaDrops = new LinkedList<>();
            for(final ItemStack nonVanillaDrop : nonVanillaDrops) {
                newNonVanillaDrops.addAll(Arrays.asList(stackMultiplier.apply(nonVanillaDrop)));
            }
            nonVanillaDrops.clear();
            nonVanillaDrops.addAll(newNonVanillaDrops);
        }
    }

    private void handleExpDrops(final @Nonnull EntityDeathEvent event) {
        Log.debug(DROPS, () -> "[Exp] Handling exp; starting with " + event.getDroppedExp());
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

        Log.debug(DROPS, () -> "[Exp] Multiplying exp drops by " + eval);

        event.setDroppedExp((int) (event.getDroppedExp() * eval));

        Log.debug(DROPS, () -> "[Exp] finishing with " + event.getDroppedExp());
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
