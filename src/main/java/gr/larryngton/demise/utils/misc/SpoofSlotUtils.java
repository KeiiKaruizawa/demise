package gr.larryngton.demise.utils.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.utils.InstanceAccess;
import lombok.Getter;
import net.minecraft.item.ItemStack;

public class SpoofSlotUtils implements InstanceAccess {
    private static int spoofedSlot;

    @Getter
    private static boolean spoofing;

    public static void startSpoofing(int slot) {
        spoofing = true;
        spoofedSlot = slot;
    }

    public static void stopSpoofing() {
        spoofing = false;
    }

    public static int getSpoofedSlot() {
        return spoofing ? spoofedSlot : mc.thePlayer.inventory.currentItem;
    }

    public static ItemStack getSpoofedStack() {
        return spoofing ? mc.thePlayer.inventory.getStackInSlot(spoofedSlot) : mc.thePlayer.inventory.getCurrentItem();
    }

    @EventTarget
    public void onWorld(WorldChangeEvent event) {
        stopSpoofing();
    }
}