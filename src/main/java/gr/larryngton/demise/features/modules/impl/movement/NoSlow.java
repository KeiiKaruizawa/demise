package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.RightClickEvent;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.SlowDownEvent;
import gr.larryngton.demise.events.impl.player.SneakSlowDownEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.misc.ChatUtils;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

@ModuleInfo(name = "NoSlow", description = "Modifies item-use slowdown.")
public class NoSlow extends Module {
    private final BoolValue sword = new BoolValue("Sword", false, this);
    private final ModeValue swordMode = new ModeValue("Sword mode", new String[]{"Vanilla", "Intave", "Intave old", "NCP", "Tick", "Polar", "Hypixel"}, "Vanilla", this, sword::get);
    private final SliderValue swordSpeed = new SliderValue("Sword speed", 1, 0.2f, 1, 0.01f, this, () -> sword.get() && (swordMode.is("Vanilla") || swordMode.is("Tick")));
    private final BoolValue swordSprint = new BoolValue("Sword sprint", true, this, () -> sword.get() && (swordMode.is("Vanilla") || swordMode.is("Tick")));

    private final BoolValue consumable = new BoolValue("Consumable", false, this);
    private final ModeValue consumableMode = new ModeValue("Consumable mode", new String[]{"Vanilla", "Intave", "Intave old", "NCP", "Tick", "Packet", "Polar", "Hypixel"}, "Vanilla", this, consumable::get);
    private final SliderValue consumableSpeed = new SliderValue("Consumable speed", 1, 0.2f, 1, 0.01f, this, () -> consumable.get() && (consumableMode.is("Vanilla") || consumableMode.is("Tick")));
    private final BoolValue consumableSprint = new BoolValue("Consumable sprint", true, this, () -> consumable.get() && (consumableMode.is("Vanilla") || consumableMode.is("Tick")));

    private final BoolValue bow = new BoolValue("Bow", false, this);
    private final ModeValue bowMode = new ModeValue("Bow mode", new String[]{"Vanilla", "Tick", "Polar", "Hypixel"}, "Vanilla", this, bow::get);
    private final SliderValue bowSpeed = new SliderValue("Bow speed", 1, 0.2f, 1, 0.01f, this, () -> bow.get() && (bowMode.is("Vanilla") || bowMode.is("Tick")));
    private final BoolValue bowSprint = new BoolValue("Bow sprint", true, this, () -> bow.get() && (bowMode.is("Vanilla") || bowMode.is("Tick")));

    private final BoolValue sneak = new BoolValue("Sneak", false, this);
    private final ModeValue sneakMode = new ModeValue("Sneak mode", new String[]{"Vanilla"}, "Vanilla", this, sneak::get);
    private final SliderValue sneakSpeed = new SliderValue("Sneak speed", 1, 0.3f, 1, 0.01f, this, () -> sneak.get() && sneakMode.is("Vanilla"));

    private CurrentSlowDown currentSlowDown = CurrentSlowDown.NONE;
    private String mode;
    private float speed;
    private boolean sprint;
    private boolean wasUsingItem;
    private boolean ncpShouldWork = true;
    private int watchdogOffGroundTicks;
    private boolean watchdogDisable;
    private boolean useStartedInAir;
    private boolean prevUsingItem;

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
            refreshSlowDownState();

            if (!mc.thePlayer.isUsingItem()) {
                return;
            }
        }

        switch (mode) {
            case "Intave":
                if (e.isPost()) break;
                switch (currentSlowDown) {
                    case CONSUMABLE -> {
                        if (!wasUsingItem) {
                            sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP));
                        }
                    }
                    case SWORD -> sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                }
                break;
            case "Intave old":
                if (e.isPost()) break;
                sendPacketNoEvent(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
                sendPacketNoEvent(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                break;
            case "NCP":
                switch (currentSlowDown) {
                    case CONSUMABLE -> {
                        if (ncpShouldWork && mc.thePlayer.ticksExisted % 3 == 0) {
                            sendPacketNoEvent(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 1, null, 0, 0, 0));
                        }
                    }
                    case SWORD -> {
                        if (e.isPre()) {
                            sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        }

                        if (e.isPost()) {
                            sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getCurrentEquippedItem()));
                        }
                    }
                }
                break;
            case "Packet":
                if (mc.thePlayer.getItemInUseDuration() > 2) {
                    mc.thePlayer.clearItemInUse();
                }
                break;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return;
        if (!hypixelModeActive()) return;

        if (mc.theWorld != null
                && mc.theWorld.getBlockState(mc.thePlayer.getPosition().down()).getBlock() != Blocks.air
                && !mc.thePlayer.isUsingItem()) {
            watchdogDisable = false;
        }

        if (Math.abs(mc.thePlayer.posY - Math.round(mc.thePlayer.posY)) > 0.03 && mc.thePlayer.onGround) {
            watchdogDisable = true;
        }

        boolean nonSwordUse = mc.thePlayer.isUsingItem() && !(mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword);
        if (nonSwordUse) {
            if (mc.thePlayer.onGround) {
                watchdogOffGroundTicks = 0;
            } else {
                watchdogOffGroundTicks++;
            }
        }

        boolean usingNow = mc.thePlayer.isUsingItem();
        if (usingNow && !prevUsingItem) {
            useStartedInAir = !mc.thePlayer.onGround;

            if (!useStartedInAir) {
                ItemStack held = mc.thePlayer.getHeldItem();
                Item heldItem = held == null ? null : held.getItem();

                boolean hypixelConsumable = heldItem instanceof ItemFood || heldItem instanceof ItemPotion;
                boolean hypixelBow = heldItem instanceof ItemBow;

                if ((hypixelConsumable && consumable.get() && consumableMode.is("Hypixel"))
                        || (hypixelBow && bow.get() && bowMode.is("Hypixel"))) {
                    ChatUtils.sendMessageClient("You must start using the item in air for the No Slow to work!");
                }
            }
        }
        prevUsingItem = usingNow;

        if (isSwordHypixelActive()) {
            int slot = mc.thePlayer.inventory.currentItem;
            sendPacket(new C09PacketHeldItemChange(slot % 7 + (int) (Math.random() * 2) + 1));
            sendPacket(new C09PacketHeldItemChange(slot));
        }
    }

    @EventTarget
    public void onRightClick(RightClickEvent e) {
        if (mc.thePlayer == null) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return;

        Item item = held.getItem();

        boolean qualifies = (sword.get() && swordMode.is("Hypixel") && item instanceof ItemSword && mc.thePlayer.isUsingItem())
                || (consumable.get() && consumableMode.is("Hypixel") && item instanceof ItemPotion)
                || (consumable.get() && consumableMode.is("Hypixel") && item instanceof ItemFood)
                || (bow.get() && bowMode.is("Hypixel") && item instanceof ItemBow);
        if (!qualifies) return;

        if (watchdogOffGroundTicks < 2 && watchdogOffGroundTicks != 0 && !watchdogDisable) {
            ChatUtils.sendMessageClient("You must start using the item in air for the No Slow to work!");
            e.setCancelled(true);
        }
    }

    private boolean hypixelModeActive() {
        return (sword.get() && swordMode.is("Hypixel"))
                || (consumable.get() && consumableMode.is("Hypixel"))
                || (bow.get() && bowMode.is("Hypixel"));
    }

    private boolean isSwordHypixelActive() {
        return sword.get() && swordMode.is("Hypixel") && mc.thePlayer.isUsingItem()
                && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    private void refreshSlowDownState() {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return;

        if (!mc.thePlayer.isUsingItem()) {
            wasUsingItem = false;
            currentSlowDown = CurrentSlowDown.NONE;
        } else {
            ItemStack stack = mc.thePlayer.getCurrentEquippedItem();
            Item item = stack == null ? null : stack.getItem();

            if (item == null) {
                currentSlowDown = CurrentSlowDown.NONE;
            } else if (sword.get() && item instanceof ItemSword) {
                currentSlowDown = CurrentSlowDown.SWORD;
            } else if (consumable.get() && (item instanceof ItemFood || item instanceof ItemPotion)) {
                currentSlowDown = CurrentSlowDown.CONSUMABLE;
            } else if (bow.get() && item instanceof ItemBow) {
                currentSlowDown = CurrentSlowDown.BOW;
            } else {
                currentSlowDown = CurrentSlowDown.NONE;
            }
        }

        switch (currentSlowDown) {
            case SWORD -> {
                mode = swordMode.get();
                speed = swordSpeed.get();
                sprint = swordSprint.get();
            }
            case CONSUMABLE -> {
                mode = consumableMode.get();
                speed = consumableSpeed.get();
                sprint = consumableSprint.get();
            }
            case BOW -> {
                mode = bowMode.get();
                speed = bowSpeed.get();
                sprint = bowSprint.get();
            }
            case NONE -> {
                mode = "null";
                speed = 0.2F;
                sprint = false;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (currentSlowDown == CurrentSlowDown.CONSUMABLE && mode.equals("NCP")) {
            ncpShouldWork = !(e.getPacket() instanceof C07PacketPlayerDigging);
        }
    }

    @EventTarget
    public void onSlowDown(SlowDownEvent e) {
        refreshSlowDownState();

        switch (mode) {
            case "Tick":
                if (mc.thePlayer.onGroundTicks % 2 != 0) {
                    e.setForward(1);
                    e.setStrafe(1);
                    e.setSprinting(true);
                }
                break;
            case "Polar":
                if (mc.thePlayer.onGroundTicks % 2 != 0) {
                    e.setForward(0.28F);
                    e.setStrafe(0.28F);
                    e.setSprinting(true);
                }
                break;
            case "Hypixel":
                if (currentSlowDown == CurrentSlowDown.SWORD) {
                    int slot = mc.thePlayer.inventory.currentItem;
                    sendPacket(new C09PacketHeldItemChange(slot % 7 + (int) (Math.random() * 2) + 1));
                    sendPacket(new C09PacketHeldItemChange(slot));
                    e.setForward(1);
                    e.setStrafe(1);
                    e.setSprinting(true);
                } else if (currentSlowDown != CurrentSlowDown.NONE && !watchdogDisable && useStartedInAir) {
                    e.setForward(1);
                    e.setStrafe(1);
                    e.setSprinting(true);
                }
                break;
            default:
                e.setForward(speed);
                e.setStrafe(speed);
                e.setSprinting(sprint);
                break;
        }
    }

    @EventTarget
    public void onSneakSlowDown(SneakSlowDownEvent e) {
        if (sneak.get() && sneakMode.is("Vanilla")) {
            e.setForward(sneakSpeed.get());
            e.setStrafe(sneakSpeed.get());
        }
    }

    private enum CurrentSlowDown {
        SWORD,
        CONSUMABLE,
        BOW,
        NONE
    }
}