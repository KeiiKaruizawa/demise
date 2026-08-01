package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.SlowDownEvent;
import gr.larryngton.demise.events.impl.player.SneakSlowDownEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

@ModuleInfo(name = "NoSlow", description = "Modifies item-use slowdown.")
public class NoSlow extends Module {
    private final BoolValue sword = new BoolValue("Sword", false, this);
    private final ModeValue swordMode = new ModeValue("Sword mode", new String[]{"Vanilla", "Intave", "Intave old", "NCP", "Tick", "Polar"}, "Vanilla", this, sword::get);
    private final SliderValue swordSpeed = new SliderValue("Sword speed", 1, 0.2f, 1, 0.01f, this, () -> sword.get() && (swordMode.is("Vanilla") || swordMode.is("Tick")));
    private final BoolValue swordSprint = new BoolValue("Sword sprint", true, this, () -> sword.get() && (swordMode.is("Vanilla") || swordMode.is("Tick")));

    private final BoolValue consumable = new BoolValue("Consumable", false, this);
    private final ModeValue consumableMode = new ModeValue("Consumable mode", new String[]{"Vanilla", "Intave", "Intave old", "NCP", "Tick", "Packet", "Polar"}, "Vanilla", this, consumable::get);
    private final SliderValue consumableSpeed = new SliderValue("Consumable speed", 1, 0.2f, 1, 0.01f, this, () -> consumable.get() && (consumableMode.is("Vanilla") || consumableMode.is("Tick")));
    private final BoolValue consumableSprint = new BoolValue("Consumable sprint", true, this, () -> consumable.get() && (consumableMode.is("Vanilla") || consumableMode.is("Tick")));

    private final BoolValue bow = new BoolValue("Bow", false, this);
    private final ModeValue bowMode = new ModeValue("Bow mode", new String[]{"Vanilla", "Tick", "Polar"}, "Vanilla", this, bow::get);
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

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
            if (!mc.thePlayer.isUsingItem()) {
                wasUsingItem = false;
                currentSlowDown = CurrentSlowDown.NONE;
                return;
            }

            Item item = mc.thePlayer.getCurrentEquippedItem().getItem();

            if (sword.get() && item instanceof ItemSword) {
                currentSlowDown = CurrentSlowDown.SWORD;
            }

            if (consumable.get() && (item instanceof ItemFood || item instanceof ItemPotion)) {
                currentSlowDown = CurrentSlowDown.CONSUMABLE;
            }

            if (bow.get() && item instanceof ItemBow) {
                currentSlowDown = CurrentSlowDown.BOW;
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
    public void onPacket(PacketEvent e) {
        if (currentSlowDown == CurrentSlowDown.CONSUMABLE && mode.equals("NCP")) {
            ncpShouldWork = !(e.getPacket() instanceof C07PacketPlayerDigging);
        }
    }

    @EventTarget
    public void onSlowDown(SlowDownEvent e) {
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