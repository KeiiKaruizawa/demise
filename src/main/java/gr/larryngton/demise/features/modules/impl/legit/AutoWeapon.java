package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.utils.player.ItemUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;

@ModuleInfo(name = "AutoWeapon", description = "Automatically switches to the best weapon in your hotbar when attacking.")
public class AutoWeapon extends Module {
    private final BoolValue switchBack = new BoolValue("Switch Back", true, this);

    public AutoWeapon() {
        switchBack.setDescription("Switches back after sending an attack packet. Funny on some servers' murder mystery.");
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        int slot = -1;
        int oldSlot = mc.thePlayer.inventory.currentItem;

        double maxDamage = -1.0;

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemSword) {
                double damage = stack.getAttributeModifiers().get("generic.attackDamage").stream().findFirst().map(AttributeModifier::getAmount).orElse(0.0)
                        + 1.25 * ItemUtils.getEnchantment(stack, Enchantment.sharpness);
                if (damage > maxDamage) {
                    maxDamage = damage;
                    slot = i;
                }
            }
        }

        if (slot == -1 || slot == mc.thePlayer.inventory.currentItem)
            return;

        mc.thePlayer.inventory.currentItem = slot;
        mc.playerController.updateController();

        e.setCancelled(true);
        sendPacketNoEvent(new C02PacketUseEntity(e.getTargetEntity(), C02PacketUseEntity.Action.ATTACK));

        if (switchBack.get()) {
            mc.thePlayer.inventory.currentItem = oldSlot;
        }
    }
}