package gr.larryngton.demise.utils.player;

import com.google.common.collect.Multimap;
import gr.larryngton.demise.utils.InstanceAccess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockStone;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public class ItemUtils implements InstanceAccess {
    public static final int INCLUDE_ARMOR_BEGIN = 5;
    public static final int EXCLUDE_ARMOR_BEGIN = 9;
    public static final int ONLY_HOT_BAR_BEGIN = 36;
    public static final int END = 45;

    public static final List<Block> blacklistedBlocks = Arrays.asList(
            Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.lava, Blocks.wooden_slab, Blocks.chest,
            Blocks.flowing_lava, Blocks.enchanting_table, Blocks.carpet, Blocks.glass_pane, Blocks.skull,
            Blocks.stained_glass_pane, Blocks.iron_bars, Blocks.snow_layer, Blocks.ice, Blocks.packed_ice,
            Blocks.trapped_chest, Blocks.torch, Blocks.anvil, Blocks.noteblock, Blocks.jukebox, Blocks.tnt,
            Blocks.wooden_pressure_plate, Blocks.stone_pressure_plate, Blocks.light_weighted_pressure_plate,
            Blocks.heavy_weighted_pressure_plate, Blocks.stone_button, Blocks.wooden_button, Blocks.lever,
            Blocks.tallgrass, Blocks.tripwire, Blocks.tripwire_hook, Blocks.rail, Blocks.waterlily, Blocks.red_flower,
            Blocks.red_mushroom, Blocks.brown_mushroom, Blocks.vine, Blocks.trapdoor, Blocks.yellow_flower,
            Blocks.ladder, Blocks.furnace, Blocks.sand, Blocks.cactus, Blocks.dispenser, Blocks.noteblock,
            Blocks.dropper, Blocks.crafting_table, Blocks.pumpkin, Blocks.sapling, Blocks.cobblestone_wall,
            Blocks.oak_fence, Blocks.activator_rail, Blocks.detector_rail, Blocks.golden_rail, Blocks.redstone_torch,
            Blocks.acacia_stairs, Blocks.birch_stairs, Blocks.brick_stairs, Blocks.dark_oak_stairs,
            Blocks.jungle_stairs, Blocks.nether_brick_stairs, Blocks.oak_stairs, Blocks.quartz_stairs,
            Blocks.red_sandstone_stairs, Blocks.sandstone_stairs, Blocks.spruce_stairs, Blocks.stone_brick_stairs,
            Blocks.stone_stairs, Blocks.double_wooden_slab, Blocks.stone_slab, Blocks.double_stone_slab,
            Blocks.stone_slab2, Blocks.double_stone_slab2, Blocks.web, Blocks.gravel, Blocks.daylight_detector_inverted,
            Blocks.daylight_detector, Blocks.soul_sand, Blocks.piston, Blocks.piston_extension, Blocks.piston_head,
            Blocks.sticky_piston, Blocks.iron_trapdoor, Blocks.ender_chest, Blocks.end_portal, Blocks.end_portal_frame,
            Blocks.standing_banner, Blocks.wall_banner, Blocks.deadbush, Blocks.slime_block, Blocks.acacia_fence_gate,
            Blocks.birch_fence_gate, Blocks.dark_oak_fence_gate, Blocks.jungle_fence_gate, Blocks.spruce_fence_gate,
            Blocks.oak_fence_gate
    );

    public static boolean isValidStack(ItemStack stack) {
        return stack.getItem() instanceof ItemBlock && isGoodBlockStack(stack) ||
                stack.getItem() instanceof ItemPotion && isBuffPotion(stack) ||
                stack.getItem() instanceof ItemArmor && isBestArmor(stack) ||
                stack.getItem() instanceof ItemSword && isBestSword(stack) ||
                stack.getItem() instanceof ItemFood && isGoodFood(stack) ||
                stack.getItem() instanceof ItemTool && isBestTool(stack) ||
                stack.getItem() instanceof ItemBow && isBestBow(stack) ||
                stack.getItem() instanceof ItemFishingRod ||
                stack.getItem() instanceof ItemEnderPearl ||
                stack.getItem() instanceof ItemSnowball ||
                stack.getItem() == Items.water_bucket ||
                stack.getItem() instanceof ItemBoat ||
                stack.getItem() instanceof ItemEgg ||
                stack.getItem() == Items.arrow ||
                stack.getItem() == Items.gold_nugget;
    }

    public static double getBowDamage(ItemStack stack) {
        double damage = 0.0;
        if (stack.getItem() instanceof ItemBow && stack.isItemEnchanted()) {
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
        }
        return damage;
    }

    public static boolean isBuffPotion(ItemStack stack) {
        ItemPotion potion = (ItemPotion) stack.getItem();
        List<PotionEffect> effects = potion.getEffects(stack);
        List<Integer> BAD_EFFECTS_IDS = Arrays.asList(Potion.poison.id, Potion.weakness.id, Potion.wither.id, Potion.blindness.id, Potion.digSlowdown.id, Potion.harm.id);
        if (effects.isEmpty()) {
            return false;
        }
        for (PotionEffect effect : effects) {
            if (BAD_EFFECTS_IDS.contains(effect.getPotionID())) {
                return false;
            }
        }
        return true;
    }

    public static boolean isGoodFood(ItemStack stack) {
        ItemFood food = (ItemFood) stack.getItem();
        return food instanceof ItemAppleGold || (food.getHealAmount(stack) >= 4 && food.getSaturationModifier(stack) >= 0.3f);
    }

    private static boolean isBestItem(ItemStack target, Predicate<ItemStack> filter, ToDoubleFunction<ItemStack> scorer, int begin) {
        ItemStack best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = begin; i < ItemUtils.END; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if (stack != null && filter.test(stack)) {
                double score = scorer.applyAsDouble(stack);

                if (score > bestScore) {
                    bestScore = score;
                    best = stack;
                }
            }
        }

        return best == target || scorer.applyAsDouble(target) > bestScore;
    }

    public static boolean isBestBow(ItemStack stack) {
        return isBestItem(
                stack,
                s -> s.getItem() instanceof ItemBow,
                ItemUtils::getBowDamage,
                EXCLUDE_ARMOR_BEGIN
        );
    }

    public static boolean isBestSword(ItemStack stack) {
        return isBestItem(
                stack,
                s -> s.getItem() instanceof ItemSword,
                ItemUtils::getItemDamage,
                EXCLUDE_ARMOR_BEGIN
        );
    }

    public static boolean isBestTool(ItemStack stack) {
        int type = getToolType(stack);

        return isBestItem(
                stack,
                s -> s.getItem() instanceof ItemTool && getToolType(s) == type,
                ItemUtils::getToolEfficiency,
                EXCLUDE_ARMOR_BEGIN
        );
    }

    public static boolean isBestArmor(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemArmor armor))
            return false;

        return isBestItem(
                stack,
                s -> s.getItem() instanceof ItemArmor a && a.armorType == armor.armorType,
                ItemUtils::getDamageReduction,
                INCLUDE_ARMOR_BEGIN
        );
    }

    public static int getToolType(ItemStack stack) {
        ItemTool tool = (ItemTool) stack.getItem();
        if (tool instanceof ItemPickaxe) {
            return 0;
        }
        if (tool instanceof ItemAxe) {
            return 1;
        }
        if (tool instanceof ItemSpade) {
            return 2;
        }
        return -1;
    }

    public static float getToolEfficiency(ItemStack itemStack) {
        return getToolEfficiency(itemStack, null);
    }

    public static double getItemDamage(ItemStack stack) {
        double baseDamage = 0.0;

        Multimap<String, AttributeModifier> attributeModifierMap = stack.getAttributeModifiers();
        for (String attributeName : attributeModifierMap.keySet()) {
            if (attributeName.equals("generic.attackDamage")) {
                Iterator<AttributeModifier> attributeModifiers = attributeModifierMap.get(attributeName).iterator();
                if (attributeModifiers.hasNext()) {
                    baseDamage += attributeModifiers.next().getAmount();
                }
                break;
            }
        }

        baseDamage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25;
        double totalDamage = baseDamage * 20;

        int fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);

        if (fireAspect > 0) {
            totalDamage += fireAspect == 1 ? 80 : 160;
        }

        return totalDamage;
    }

    public static float getToolEfficiency(ItemStack itemStack, Block block) {
        float speed;

        if (block == null) {
            speed = switch (getToolType(itemStack)) {
                case 0 -> itemStack.getStrVsBlock(new BlockStone());
                case 1 -> itemStack.getStrVsBlock(new BlockLog() {});
                case 2 -> itemStack.getStrVsBlock(new BlockDirt());
                default -> 0;
            };
        } else {
            speed = itemStack.getStrVsBlock(block);
        }

        float efficiency = 4;

        int lvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);

        if (lvl > 0)
            efficiency += lvl * lvl + 1;

        return speed + efficiency;
    }

    public static double getDamageReduction(ItemStack stack) {
        double reduction = 0.0;

        ItemArmor armor = (ItemArmor) stack.getItem();
        reduction += armor.damageReduceAmount;
        if (stack.isItemEnchanted()) {
            reduction += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 0.25D;
            reduction += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) * 0.1D;
            reduction += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack) * 0.15D;
        }
        return reduction;
    }

    public static boolean isGoodBlockStack(ItemStack stack) {
        return stack.stackSize >= 1 && !blacklistedBlocks.contains(Block.getBlockFromItem(stack.getItem()));
    }

    public static boolean isInventoryFull() {
        for (int i = 9; i < 45; i++) {
            if (!mc.thePlayer.inventoryContainer.getSlot(i).getHasStack())
                return false;
        }
        return true;
    }

    public static int findBestBlockStack() {
        int bestSlot = -1;
        int blockCount = -1;
        for (int i = 44; i >= 9; --i) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemBlock && ItemUtils.isGoodBlockStack(stack) && stack.stackSize > blockCount) {
                bestSlot = i;
                blockCount = stack.stackSize;
            }
        }
        return bestSlot;
    }

    public static int getEnchantment(ItemStack itemStack, Enchantment enchantment) {
        if (itemStack == null || itemStack.getEnchantmentTagList() == null || itemStack.getEnchantmentTagList().hasNoTags())
            return 0;

        for (int i = 0; i < itemStack.getEnchantmentTagList().tagCount(); i++) {
            NBTTagCompound tagCompound = itemStack.getEnchantmentTagList().getCompoundTagAt(i);

            if ((tagCompound.hasKey("ench") && tagCompound.getShort("ench") == enchantment.effectId) || (tagCompound.hasKey("id") && tagCompound.getShort("id") == enchantment.effectId))
                return tagCompound.getShort("lvl");
        }

        return 0;
    }

    public static ItemStack getItemStack(String command) {
        try {
            command = command.replace('&', '\u00a7');
            String[] args;
            int i = 1;
            int j = 0;
            args = command.split(" ");
            ResourceLocation resourcelocation = new ResourceLocation(args[0]);
            Item item = Item.itemRegistry.getObject(resourcelocation);

            if (args.length >= 2 && args[1].matches("\\d+")) {
                i = Integer.parseInt(args[1]);
            }

            if (args.length >= 3 && args[2].matches("\\d+")) {
                j = Integer.parseInt(args[2]);
            }

            ItemStack itemstack = new ItemStack(item, i, j);
            if (args.length >= 4) {
                StringBuilder NBT = new StringBuilder();

                int nbtCount = 3;
                while (nbtCount < args.length) {
                    NBT.append(" ").append(args[nbtCount]);
                    nbtCount++;
                }

                itemstack.setTagCompound(JsonToNBT.getTagFromJson(NBT.toString()));
            }
            return itemstack;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ItemStack(Blocks.barrier);
        }
    }
}