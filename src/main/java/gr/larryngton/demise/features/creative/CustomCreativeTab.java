package gr.larryngton.demise.features.creative;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.player.ItemUtils;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class CustomCreativeTab extends CreativeTabs implements InstanceAccess {
    public CustomCreativeTab() {
        super(12, "demise");
    }

    @Override
    public void displayAllReleventItems(List<ItemStack> items) {
        ItemStack godSword = ItemUtils.getItemStack("diamond_sword 1 0 {ench:[{id:19,lvl:32767},{id:20,lvl:32767},{id:18,lvl:32767},{id:16,lvl:32767},{id:17,lvl:32767}],Unbreakable:1}");
        godSword.setStackDisplayName("\247r\2478\247lGod sword");
        items.add(godSword);

        ItemStack godBow = ItemUtils.getItemStack("bow 1 0 {ench:[{id:48,lvl:32767},{id:49,lvl:32767},{id:50,lvl:32767},{id:51,lvl:32767},{id:19,lvl:32767}],Unbreakable:1}");
        godBow.setStackDisplayName("\247r\2478\247lGod bow");
        items.add(godBow);

        ItemStack godHelmet = ItemUtils.getItemStack("diamond_helmet 1 0 {ench:[{id:0,lvl:32767},{id:6,lvl:32767},{id:3,lvl:32767},{id:1,lvl:32767},{id:7,lvl:32767},{id:4,lvl:32767}],Unbreakable:1}");
        godHelmet.setStackDisplayName("\247r\2478\247lGod helmet");
        items.add(godHelmet);

        ItemStack godChestplate = ItemUtils.getItemStack("diamond_chestplate 1 0  {ench:[{id:0,lvl:32767},{id:3,lvl:32767},{id:1,lvl:32767},{id:7,lvl:32767}],Unbreakable:1}");
        godChestplate.setStackDisplayName("\247r\2478\247lGod chestplate");
        items.add(godChestplate);

        ItemStack godLeggings = ItemUtils.getItemStack("diamond_leggings 1 0  {ench:[{id:0,lvl:32767},{id:3,lvl:32767},{id:1,lvl:32767},{id:7,lvl:32767}],Unbreakable:1}");
        godLeggings.setStackDisplayName("\247r\2478\247lGod leggings");
        items.add(godLeggings);

        ItemStack godBoots = ItemUtils.getItemStack("diamond_boots 1 0  {ench:[{id:0,lvl:32767},{id:8,lvl:32767},{id:3,lvl:32767},{id:1,lvl:32767},{id:7,lvl:32767}],Unbreakable:1}");
        godBoots.setStackDisplayName("\247r\2478\247lGod boots");
        items.add(godBoots);

        ItemStack opSign = ItemUtils.getItemStack("sign 1 0 {BlockEntityTag:{Text1:\"{\\\"text\\\":\\\"right click me\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"run_command\\\",\\\"value\\\":\\\"/op " + mc.thePlayer.getNameClear() + "\\\"}}\"}}");
        opSign.setStackDisplayName("\247rOP sign");
        items.add(opSign);

        ItemStack deathPotion = ItemUtils.getItemStack("potion 1 16385 {CustomPotionEffects:[{Id:6,Amplifier:125,Duration:1000000}]}");
        deathPotion.setStackDisplayName("\247rSplash potion of instant death");
        items.add(deathPotion);

        ItemStack itemStack = ItemUtils.getItemStack("anvil");
        itemStack.setItemDamage(2);
        itemStack.setRepairCost(32767);
        for (Enchantment enchantment : Enchantment.enchantmentsList) {
            if (enchantment != null) {
                itemStack.addEnchantment(enchantment, 1);
            }
        }
        itemStack.setStackDisplayName("Funny anvil");
        items.add(itemStack);

        items.add(ItemUtils.getItemStack("barrier"));

        items.add(ItemUtils.getItemStack("command_block"));
    }

    @Override
    public String getTranslatedTabLabel() {
        return Demise.INSTANCE.getClientName();
    }

    @Override
    public Item getTabIconItem() {
        return Item.getItemFromBlock(Blocks.bedrock);
    }
}