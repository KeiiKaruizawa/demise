package gr.larryngton.demise.features.modules.impl.player;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

@ModuleInfo(name = "FastPlace", description = "Allows you to place blocks faster.")
public class FastPlace extends Module {
    private final SliderValue speed = new SliderValue("Right click delay", 1, 0, 5, this);
    private final BoolValue onlyOnXZPlane = new BoolValue("Only on xz plane", false, this);
    private final BoolValue ignoreTickCycle = new BoolValue("Ignore tick cycle", false, this);
    private final SliderValue frameDelay = new SliderValue("Frame delay", 0, 0, 50, 1, this, ignoreTickCycle::get);
    private final BoolValue swing = new BoolValue("Swing", true, this, ignoreTickCycle::get);

    public FastPlace() {
        ignoreTickCycle.setDescription("Instead of placing blocks on ticks, places on frames instead.");
    }

    private final TimerUtil timer = new TimerUtil();

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (ignoreTickCycle.get() && !getModule(Scaffold.class).isEnabled()) {
            MovingObjectPosition ray = mc.objectMouseOver;

            if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                if (mc.gameSettings.keyBindUseItem.isKeyDown() && timer.hasTimeElapsed(frameDelay.get()) && mc.objectMouseOver.sideHit != EnumFacing.UP && mc.objectMouseOver.sideHit != EnumFacing.DOWN) {
                    if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), ray.getBlockPos(), ray.sideHit, ray.hitVec)) {
                        if (swing.get()) {
                            mc.thePlayer.swingItem();
                            mc.getItemRenderer().resetEquippedProgress();
                        } else {
                            mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                        }
                    }

                    timer.reset();
                }
            }
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        setTag(String.valueOf(ignoreTickCycle.get() ? frameDelay.get() : speed.get()));

        if (mc.thePlayer.getHeldItem() == null) return;
        if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) {
            if (speed.getInt() == 5 || (onlyOnXZPlane.get() && (mc.objectMouseOver.sideHit == EnumFacing.UP || mc.objectMouseOver.sideHit == EnumFacing.DOWN))) {
                mc.rightClickDelayTimer = 276;
            } else {
                mc.rightClickDelayTimer = Math.min(mc.rightClickDelayTimer, speed.getInt());
            }
        }
    }
}
