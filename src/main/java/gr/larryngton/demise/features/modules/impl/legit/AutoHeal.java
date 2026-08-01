package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.ItemUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.Range;

// this code is a fucking mess
@ModuleInfo(name = "AutoHeal", description = "Automatically heals you.")
public class AutoHeal extends Module {
    private final BoolValue head = new BoolValue("Head", true, this);
    private final SliderValue headHealth = new SliderValue("Head health", 15, 0, 20, 1, this, head::get);

    private final BoolValue soup = new BoolValue("Soup", true, this);
    private final SliderValue soupHealth = new SliderValue("Soup health", 15, 0, 20, 1, this, soup::get);
    private final DoubleSliderValue consumeDelay = new DoubleSliderValue("Consume delay", 1, 1, 0, 10, 1, this, soup::get);
    private final DoubleSliderValue dropDelay = new DoubleSliderValue("Drop delay", 1, 1, 0, 10, 1, this, soup::get);
    private final DoubleSliderValue switchBackDelay = new DoubleSliderValue("Switch back delay", 1, 1, 0, 10, 1, this, soup::get);
    private final BoolValue refill = new BoolValue("Refill", true, this, soup::get);
    private final SliderValue openDelay = new SliderValue("Open delay", 1, 0, 10, 1, this, () -> soup.get() && refill.get());
    private final SliderValue refillDelay = new SliderValue("Refill delay", 1, 0, 10, 1, this, () -> soup.get() && refill.get());

    private final TimerUtil refillTimer = new TimerUtil();
    private final TimerUtil openTimer = new TimerUtil();
    private boolean resetOnSwap;
    private final TimerUtil consumeTimer = new TimerUtil();
    private int lastSoupSlot;
    private int nextConsumeTime;
    private boolean consumed;
    private final TimerUtil switchBackTimer = new TimerUtil();
    private int nextSwitchBackDelay;
    private final TimerUtil dropTimer = new TimerUtil();
    private int nextDropDelay;
    private boolean dropped;
    private boolean switchedBack;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (head.get() && mc.currentScreen == null) {
            if (Range.between(1, 9).contains(getHeadSlot()) && mc.thePlayer.getHealth() < headHealth.get() && MoveUtils.getSpeedEffect() == 0) {
                int lastHeadSlot = mc.thePlayer.inventory.currentItem;

                mc.thePlayer.inventory.currentItem = getHeadSlot();
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                mc.thePlayer.inventory.currentItem = lastHeadSlot;
            }
        }

        if (soup.get()) {
            if (mc.currentScreen == null) {
                if (mc.thePlayer.getHealth() < soupHealth.get() && !resetOnSwap && getSoupSlot() != -1) {
                    lastSoupSlot = mc.thePlayer.inventory.currentItem;
                    mc.thePlayer.inventory.currentItem = getSoupSlot();
                    nextConsumeTime = (int) consumeDelay.random();
                    consumeTimer.reset();
                    consumed = false;
                    resetOnSwap = true;
                }

                if (resetOnSwap) {
                    if (!consumed) {
                        if (consumeTimer.hasTimeElapsed(nextConsumeTime * 50L)) {
                            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                            switchBackTimer.reset();
                            nextDropDelay = (int) dropDelay.random();
                            dropped = false;
                            consumed = true;
                        }
                    } else {
                        if (!dropped) {
                            if (dropTimer.hasTimeElapsed(nextDropDelay * 50L)) {
                                mc.thePlayer.dropOneItem(false);
                                nextSwitchBackDelay = (int) switchBackDelay.random();
                                switchBackTimer.reset();
                                switchedBack = false;
                                dropped = true;
                            }
                        } else {
                            if (switchBackTimer.hasTimeElapsed(nextSwitchBackDelay * 50L) && !switchedBack) {
                                mc.thePlayer.inventory.currentItem = lastSoupSlot;
                                switchedBack = true;
                                resetOnSwap = false;
                                dropped = false;
                                consumed = false;
                            }
                        }
                    }
                }

                refillTimer.reset();
                openTimer.reset();
            } else if (mc.currentScreen instanceof GuiInventory) {
                if (refill.get() && refillTimer.hasTimeElapsed(refillDelay.get() * 50L) && openTimer.hasTimeElapsed(openDelay.get() * 50L)) {
                    for (int slot = ItemUtils.EXCLUDE_ARMOR_BEGIN; slot < ItemUtils.ONLY_HOT_BAR_BEGIN; slot++) {
                        final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();

                        if (stack != null) {
                            if (stack.getItem() instanceof ItemSoup) {
                                for (int hotbarSlot = ItemUtils.ONLY_HOT_BAR_BEGIN; hotbarSlot < 45; hotbarSlot++) {
                                    final ItemStack hotbarStack = mc.thePlayer.inventoryContainer.getSlot(hotbarSlot).getStack();

                                    if (hotbarStack == null) {
                                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, 0, 1, mc.thePlayer);
                                        if (refillDelay.get() > 0) {
                                            refillTimer.reset();
                                            return;
                                        }
                                    }
                                }
                            } else if (stack.getItem() == Items.bowl) {
                                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, 1, 4, mc.thePlayer);
                            }
                        }
                    }
                }
            }
        }
    }

    private int getHeadSlot() {
        int slot = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemSkull) {
                if (heldItem != null && heldItem.getItem() instanceof ItemSkull) {
                    continue;
                }

                slot = i;
            }
        }
        return slot;
    }

    private int getSoupSlot() {
        int slot = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemSoup) {
                if (heldItem != null && heldItem.getItem() instanceof ItemSoup) {
                    continue;
                }

                slot = i;
            }
        }
        return slot;
    }
}