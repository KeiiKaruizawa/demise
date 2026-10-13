package gr.larryngton.demise.features.modules.impl.player;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.ItemUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "Manager", description = "Manages your inventory.")
public class Manager extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Open inv", "Silent", "Legit silent"}, "Open inv", this);
    private final BoolValue notWhileMoving = new BoolValue("Not while moving", false, this, () -> !mode.is("Open inv"));
    private final BoolValue spam = new BoolValue("Spam", false, this, () -> !mode.is("Open inv"));
    private final SliderValue reopenDelay = new SliderValue("Re-open delay", 0, 0, 20, 1, this, () -> spam.get() && spam.canDisplay());

    private final SliderValue startDelay = new SliderValue("Start delay", 1, 1, 10, 1, this);

    private final BoolValue autoArmor = new BoolValue("Auto armor", true, this);
    private final DoubleSliderValue armorDelay = new DoubleSliderValue("Armor delay", 1, 1, 0, 10, 1, this, autoArmor::get);

    private final BoolValue sortItems = new BoolValue("Sort items", true, this);
    private final DoubleSliderValue sortDelay = new DoubleSliderValue("Sort delay", 1, 1, 0, 10, 1, this, sortItems::get);
    private final SliderValue swordSlot = new SliderValue("Sword slot", 1, 0, 9, 1, this, sortItems::get);
    private final SliderValue bowSlot = new SliderValue("Bow slot", 3, 0, 9, 1, this, sortItems::get);
    private final SliderValue gappleSlot = new SliderValue("Gapple slot", 2, 0, 9, 1, this, sortItems::get);
    private final SliderValue pickaxeSlot = new SliderValue("Pickaxe slot", 4, 0, 9, 1, this, sortItems::get);
    private final SliderValue axeSlot = new SliderValue("Axe slot", 5, 0, 9, 1, this, sortItems::get);
    private final SliderValue shovelSlot = new SliderValue("Shovel slot", 6, 0, 9, 1, this, sortItems::get);
    private final SliderValue blockSlot = new SliderValue("Block slot", 9, 0, 9, 1, this, sortItems::get);

    private final BoolValue dropItems = new BoolValue("Drop items", true, this);
    private final DoubleSliderValue dropDelay = new DoubleSliderValue("Drop delay", 1, 1, 0, 10, 1, this, dropItems::get);
    private final BoolValue ignoreCompass = new BoolValue("Ignore compass", false, this, dropItems::get);

    private final int[] bestArmorPieces = new int[4];
    private final List<Integer> trash = new ArrayList<>();
    private final int[] bestToolSlots = new int[3];
    private final List<Integer> gappleStackSlots = new ArrayList<>();
    private final List<Integer> targetBlockSlot = new ArrayList<>();
    private int bestSwordSlot;
    private int bestBowSlot;
    public int slot;
    private final TimerUtil armorTimer = new TimerUtil();
    private int armorWait;
    private final TimerUtil sortTimer = new TimerUtil();
    private int sortWait;
    private final TimerUtil dropTimer = new TimerUtil();
    private int dropWait;
    private final TimerUtil startDelayTimer = new TimerUtil();
    private final TimerUtil finishTimer = new TimerUtil();
    private boolean inventoryOpen;
    private final TimerUtil reopenTimer = new TimerUtil();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        boolean open = mc.currentScreen instanceof GuiInventory || !mode.is("Open inv");

        if (!open) {
            startDelayTimer.reset();
        } else {
            clear();

            for (int slot = ItemUtils.INCLUDE_ARMOR_BEGIN; slot < ItemUtils.END; slot++) {
                ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();

                if (stack != null) {
                    processInventoryItem(slot, stack);
                }
            }

            boolean armorReady = armorTimer.hasTimeElapsed(armorWait * 50L);
            boolean sortReady = sortTimer.hasTimeElapsed(sortWait * 50L);
            boolean dropReady = dropTimer.hasTimeElapsed(dropWait * 50L);

            if (!mode.is("Open inv") && (mc.currentScreen != null || (notWhileMoving.get() && MoveUtils.isMoving()) || getModule(Scaffold.class).isEnabled())) {
                closeInventory();
                return;
            }

            if ((equipArmor(true) || dropItem(trash, true) || sortItems(true)) && !mode.is("Open inv")) {
                if (!spam.get() || (armorReady && sortReady && dropReady)) {
                    openInventory();
                }
            }

            if (startDelayTimer.hasTimeElapsed(startDelay.get() * 50L) && (inventoryOpen || mode.is("Open inv"))) {
                boolean tickWasValid = false;

                if (armorReady && equipArmor(false)) {
                    resetTimings();
                    tickWasValid = true;
                } else if (dropReady && dropItem(trash, false)) {
                    resetTimings();
                    tickWasValid = true;
                } else if (sortReady && sortItems(false)) {
                    resetTimings();
                    tickWasValid = true;
                } else if (armorReady && sortReady && dropReady) {
                    if (!mode.is("Open inv")) {
                        closeInventory();
                    }
                }

                if (spam.get() && !mode.is("Open inv") && !tickWasValid) {
                    closeInventory();
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (inventoryOpen && mode.is("Legit silent")) {
            e.setCancelled(true);
        }
    }

    public boolean isInventoryOpen() {
        return inventoryOpen;
    }

    private void openInventory() {
        if (!inventoryOpen && (reopenTimer.hasTimeElapsed(reopenDelay.getInt() * 50L) || !spam.get())) {
            inventoryOpen = true;
            mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            startDelayTimer.reset();
        }
    }

    private void closeInventory() {
        if (inventoryOpen) {
            inventoryOpen = false;
            mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(0));
            reopenTimer.reset();
        }
    }

    private boolean equipArmor(boolean test) {
        if (autoArmor.get()) {
            for (int i = 0; i < bestArmorPieces.length; i++) {
                int piece = bestArmorPieces[i];

                if (piece != -1) {
                    int armorPieceSlot = i + 5;
                    ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(armorPieceSlot).getStack();
                    if (stack != null)
                        continue;

                    if (!test) {
                        windowClick(piece, 0, 1);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dropItem(List<Integer> listOfSlots, boolean test) {
        if (dropItems.get()) {
            if (!listOfSlots.isEmpty()) {
                if (!test) {
                    int slot = listOfSlots.remove(0);
                    windowClick(slot, 1, 4);
                }
                return true;
            }
        }
        return false;
    }

    private boolean sortItems(boolean test) {
        if (sortItems.get()) {
            if (bestSwordSlot != -1) {
                int target = 36 + swordSlot.getInt() - 1;
                if (bestSwordSlot != target) {
                    if (!test) {
                        putItemInSlot(target, bestSwordSlot);
                        bestSwordSlot = target;
                    }
                    return true;
                }
            }

            if (bestBowSlot != -1) {
                int target = 36 + bowSlot.getInt() - 1;
                if (bestBowSlot != target) {
                    if (!test) {
                        putItemInSlot(target, bestBowSlot);
                        bestBowSlot = target;
                    }
                    return true;
                }
            }

            if (!gappleStackSlots.isEmpty()) {
                gappleStackSlots.sort(Comparator.comparingInt(slot -> mc.thePlayer.inventoryContainer.getSlot(slot).getStack().stackSize));
                int bestGappleSlot = gappleStackSlots.get(0);

                int target = 36 + gappleSlot.getInt() - 1;
                if (bestGappleSlot != target) {
                    if (!test) {
                        putItemInSlot(target, bestGappleSlot);
                        gappleStackSlots.set(0, target);
                    }
                    return true;
                }
            }

            if (!targetBlockSlot.isEmpty()) {
                targetBlockSlot.sort(Comparator.comparingInt(slot -> -mc.thePlayer.inventoryContainer.getSlot(slot).getStack().stackSize));
                int blockSlotIndex = targetBlockSlot.get(0);

                int target = 36 + blockSlot.getInt() - 1;
                if (blockSlotIndex != target) {
                    if (!test) {
                        putItemInSlot(target, blockSlotIndex);
                        targetBlockSlot.set(0, target);
                    }
                    return true;
                }
            }

            int[] toolTargets = {
                    36 + pickaxeSlot.getInt() - 1,
                    36 + axeSlot.getInt() - 1,
                    36 + shovelSlot.getInt() - 1
            };

            for (int toolSlot : bestToolSlots) {
                if (toolSlot != -1) {
                    int type = ItemUtils.getToolType(mc.thePlayer.inventoryContainer.getSlot(toolSlot).getStack());
                    if (type != -1 && toolSlot != toolTargets[type]) {
                        if (!test) {
                            putToolsInSlot(type, toolTargets);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void processInventoryItem(int slot, ItemStack stack) {
        if (stack == null || (stack.getItem() == Items.compass && ignoreCompass.get())) return;

        if ((!trash.contains(slot) && !ItemUtils.isValidStack(stack)) || forceDropStack(stack)) {
            trash.add(slot);
            return;
        }

        if (processCombatItems(slot, stack)) return;
        if (processToolsAndArmor(slot, stack)) return;
        if (processUtilityItems(slot, stack)) return;
    }

    private boolean forceDropStack(ItemStack stack) {
        return stack.getItem() instanceof ItemSword && swordSlot.getInt() == 0 ||
                stack.getItem() instanceof ItemBow && bowSlot.getInt() == 0 ||
                stack.getItem() instanceof ItemAppleGold && gappleSlot.getInt() == 0 ||
                stack.getItem() instanceof ItemPickaxe && pickaxeSlot.getInt() == 0 ||
                stack.getItem() instanceof ItemAxe && axeSlot.getInt() == 0 ||
                stack.getItem() instanceof ItemSpade && shovelSlot.getInt() == 0 ||
                stack.getItem() instanceof ItemBlock && blockSlot.getInt() == 0;
    }

    private boolean processCombatItems(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemSword && ItemUtils.isBestSword(stack)) {
            bestSwordSlot = slot;
            return true;
        }
        if (stack.getItem() instanceof ItemBow && ItemUtils.isBestBow(stack)) {
            bestBowSlot = slot;
            return true;
        }
        if (stack.getItem() instanceof ItemAppleGold) {
            gappleStackSlots.add(slot);
            return true;
        }
        return false;
    }

    private boolean processToolsAndArmor(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemTool && ItemUtils.isBestTool(stack)) {
            updateBestTool(slot, stack);
            return true;
        }
        if (stack.getItem() instanceof ItemArmor armor && ItemUtils.isBestArmor(stack)) {
            updateBestArmor(slot, armor);
            return true;
        }
        return false;
    }

    private boolean processUtilityItems(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemBlock && slot == ItemUtils.findBestBlockStack()) {
            targetBlockSlot.add(slot);
            return true;
        }
        return false;
    }

    private void updateBestTool(int slot, ItemStack stack) {
        int toolType = ItemUtils.getToolType(stack);
        if (toolType != -1 && slot != bestToolSlots[toolType]) {
            bestToolSlots[toolType] = slot;
        }
    }

    private void updateBestArmor(int slot, ItemArmor armor) {
        int currentBestSlot = bestArmorPieces[armor.armorType];
        if (currentBestSlot == -1 || slot != currentBestSlot) {
            bestArmorPieces[armor.armorType] = slot;
        }
    }

    private void resetTimings() {
        armorTimer.reset();
        dropTimer.reset();
        sortTimer.reset();

        armorWait = (int) armorDelay.random();
        dropWait = (int) dropDelay.random();
        sortWait = (int) sortDelay.random();
        finishTimer.reset();
    }

    public void windowClick(int slotId, int mouseButtonClicked, int mode) {
        slot = slotId;
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slotId, mouseButtonClicked, mode, mc.thePlayer);
    }

    private void putItemInSlot(int slot, int slotIn) {
        windowClick(slotIn, slot - 36, 2);
    }

    private void putToolsInSlot(int tool, int[] toolSlots) {
        int toolSlot = toolSlots[tool];

        windowClick(bestToolSlots[tool], toolSlot - 36, 2);
        bestToolSlots[tool] = toolSlot;
    }

    @Override
    public void onDisable() {
        clear();
    }

    private void clear() {
        trash.clear();
        bestBowSlot = -1;
        bestSwordSlot = -1;
        gappleStackSlots.clear();
        targetBlockSlot.clear();
        Arrays.fill(bestArmorPieces, -1);
        Arrays.fill(bestToolSlots, -1);
    }

    public boolean isFinished() {
        return finishTimer.hasTimeElapsed((Math.max(Math.max(armorDelay.getEnd(), sortDelay.getEnd()), dropDelay.getEnd()) * 50L) + 250);
    }
}