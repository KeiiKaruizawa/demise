package gr.larryngton.demise.features.modules.impl.player;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.player.AngleEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.ItemUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import net.minecraft.block.BlockChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "Stealer", description = "Steals items from chests.")
public class Stealer extends Module {
    private final SliderValue startDelay = new SliderValue("Start Delay", 1, 1, 10, 1, this);
    private final SliderValue closeDelay = new SliderValue("Close Delay", 1, 1, 10, 1, this);
    private final DoubleSliderValue delay = new DoubleSliderValue("Delay", 1, 1, 0, 10, 1, this);
    private final ModeValue indexMode = new ModeValue("Index mode", new String[]{"Linear", "Random", "Nearest"}, "Linear", this);
    private final SliderValue missClickChance = new SliderValue("Miss-click chance", 0, 0, 99, 1, this);
    private final BoolValue allowInstant = new BoolValue("Allow instant", false, this);
    private final BoolValue capBlocks = new BoolValue("Cap blocks", false, this);
    private final SliderValue maxBlocks = new SliderValue("Max blocks", 128, 64, 512, 32, this, capBlocks::get);
    private final BoolValue menuCheck = new BoolValue("Menu Check", true, this);
    private final BoolValue aura = new BoolValue("Aura", false, this);
    private final BoolValue correction = new BoolValue("Correction", false, this, aura::get);
    private final BoolValue checkVisibility = new BoolValue("Check visibility", false, this, aura::get);

    public Stealer() {
        menuCheck.setDescription("Checks for if you are in a valid container before stealing items.");
    }

    private final TimerUtil stealTimer = new TimerUtil();
    private final TimerUtil startDelayTimer = new TimerUtil();
    private final TimerUtil closeDelayTimer = new TimerUtil();
    private long currentStealWait;
    public static float[] rotation;
    public int slot;
    private final ArrayList<BlockPos> lootedChests = new ArrayList<>();
    private BlockPos chestPos;

    private final String[] list = new String[]{"mode", "delivery", "menu", "selector", "game", "gui", "server", "inventory", "play", "teleporter",
            "shop", "melee", "armor", "block", "castle", "mini", "warp", "teleport", "user", "team", "tool", "sure", "trade", "cancel", "accept",
            "soul", "book", "recipe", "profile", "tele", "port", "map", "kit", "select", "lobby", "vault", "lock", "anticheat", "travel", "settings",
            "user", "preference", "compass", "cake", "wars", "buy", "upgrade", "ranged", "potions", "utility"};

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (chestPos != null && aura.get()) {
            RotationHandler.setBasicRotation(RotationUtils.getRotations(chestPos), correction.get(), 180, 180);
        }
    }

    @EventTarget
    private void onUpdate(UpdateEvent e) {
        if (aura.get()) {
            getChestPos();

            if (mc.currentScreen == null) {
                if (!checkVisibility.get()) {
                    MovingObjectPosition ray = PlayerUtils.blockRayTrace(chestPos);

                    if (ray != null) {
                        if (ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                            Vec3 hitVec = ray.hitVec;
                            BlockPos hitPos = ray.getBlockPos();

                            float f = (float) (hitVec.xCoord - (double) hitPos.getX());
                            float f1 = (float) (hitVec.yCoord - (double) hitPos.getY());
                            float f2 = (float) (hitVec.zCoord - (double) hitPos.getZ());

                            sendPacket(new C08PacketPlayerBlockPlacement(hitPos, ray.sideHit.getIndex(), mc.thePlayer.inventory.getCurrentItem(), f, f1, f2));
                        }
                    }
                } else if (mc.objectMouseOver.getBlockPos().equalsBlockPos(chestPos)) {
                    mc.rightClickMouse();
                }
            }
        }

        if (mc.thePlayer.openContainer instanceof ContainerChest container) {
            if (menuCheck.get()) {
                String name = container.getLowerChestInventory().getDisplayName().getUnformattedText().toLowerCase();
                for (String str : list) {
                    if (name.contains(str)) return;
                }
            }

            boolean chestEmpty = false;

            if (startDelayTimer.hasTimeElapsed(startDelay.get() * 50L)) {
                List<Integer> validSlots = new ArrayList<>();
                int blocksInInventory = 0;

                if (capBlocks.get()) {
                    for (int slot = ItemUtils.EXCLUDE_ARMOR_BEGIN; slot < ItemUtils.END; slot++) {
                        final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();

                        if (stack != null && ItemUtils.isValidStack(stack) && stack.getItem() instanceof ItemBlock) {
                            blocksInInventory += stack.stackSize;
                        }
                    }
                }

                for (int i = 0; i < container.getLowerChestInventory().getSizeInventory(); ++i) {
                    ItemStack stack = container.getLowerChestInventory().getStackInSlot(i);

                    if (stack != null && ItemUtils.isValidStack(stack)) {
                        if (capBlocks.get() && stack.getItem() instanceof ItemBlock && blocksInInventory >= maxBlocks.getInt()) {
                            continue;
                        }

                        validSlots.add(i);
                    }
                }

                if (!validSlots.isEmpty() && stealTimer.hasTimeElapsed(currentStealWait * 50L)) {
                    int i = validSlots.get(0);

                    switch (indexMode.get()) {
                        case "Random":
                            i = validSlots.get(rand.nextInt(validSlots.size()));
                            break;
                        case "Nearest":
                            validSlots.sort(Comparator.comparingInt(slot -> {
                                int rowA = slot / 9;
                                int colA = slot % 9;

                                int rowB = this.slot / 9;
                                int colB = this.slot % 9;

                                return (colA - colB) * (colA - colB) + (rowA - rowB) * (rowA - rowB);
                            }));

                            i = validSlots.get(rand.nextInt(validSlots.size()));
                            break;
                    }

                    if (currentStealWait != 0 || !allowInstant.get()) {
                        steal(container, i);
                    } else {
                        // doing steal(container, i1) didn't work, so im just using this
                        for (int i1 = 0; i1 < container.getLowerChestInventory().getSizeInventory(); ++i1) {
                            ItemStack stack = container.getLowerChestInventory().getStackInSlot(i1);
                            if (stack != null && ItemUtils.isValidStack(stack) && currentStealWait == 0) {
                                slot = i1;
                                mc.playerController.windowClick(container.windowId, i1, 0, 1, mc.thePlayer);
                                currentStealWait = (int) delay.random();
                                stealTimer.reset();
                            }
                        }
                    }
                }

                if (validSlots.isEmpty()) {
                    chestEmpty = true;
                }
            }

            if (ItemUtils.isInventoryFull() || chestEmpty) {
                if (closeDelayTimer.hasTimeElapsed(closeDelay.get() * 50L)) {
                    if (aura.get()) {
                        lootedChests.add(chestPos);
                    }
                    mc.thePlayer.closeScreen();
                }
            } else {
                closeDelayTimer.reset();
            }
        } else {
            currentStealWait = (int) delay.random();
            startDelayTimer.reset();
        }
    }

    private void steal(ContainerChest container, int i) {
        if (rand.nextInt(100) <= missClickChance.get()) {
            int row = i / 9;
            int col = i % 9;

            int offsetRow = Math.max(0, Math.min((container.getLowerChestInventory().getSizeInventory() / 9) - 1, row + rand.nextInt(3) - 1));
            int offsetCol = Math.max(0, Math.min(8, col + rand.nextInt(3) - 1));

            int nearbySlot = offsetRow * 9 + offsetCol;

            if (nearbySlot != i && nearbySlot < container.getLowerChestInventory().getSizeInventory()) {
                i = nearbySlot;
            }
        }

        slot = i;
        mc.playerController.windowClick(container.windowId, i, 0, 1, mc.thePlayer);
        currentStealWait = (int) delay.random();
        stealTimer.reset();
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        lootedChests.clear();
    }

    @Override
    public void onDisable() {
        lootedChests.clear();
    }

    private void getChestPos() {
        chestPos = null;
        double nearestPos = Double.MAX_VALUE;
        for (double x = mc.thePlayer.posX - 4.5; x <= mc.thePlayer.posX + 4.5; x++) {
            for (double y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - 4.5; y <= mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + 4.5; y++) {
                for (double z = mc.thePlayer.posZ - 4.5; z <= mc.thePlayer.posZ + 4.5; z++) {
                    BlockPos pos = new BlockPos((int) x, (int) y, (int) z);

                    if (lootedChests.contains(pos)) continue;

                    if (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockChest && pos.distanceTo(mc.thePlayer.getPositionVector()) < nearestPos) {
                        chestPos = pos;
                        nearestPos = pos.distanceTo(mc.thePlayer.getPositionVector());
                    }
                }
            }
        }
    }
}