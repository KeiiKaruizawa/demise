package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MoveEvent;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

@ModuleInfo(name = "LegitScaffold", description = "Automatically sneaks at the edge of blocks.")
public class LegitScaffold extends Module {
    private final SliderValue sneakDist = new SliderValue("Sneak dist", 0.15F, 0.12F, 0.5F, 0.01F, this);
    private final DoubleSliderValue unSneakDelay = new DoubleSliderValue("Un-sneak delay", 1, 3, 0, 10, 1, this);
    private final BoolValue sneakOnly = new BoolValue("Sneak only", false, this);

    private PlayerUtils.PredictProcess selfPrediction;
    private final TimerUtil unSneakTimer = new TimerUtil();
    private int nextUnSneakDelay;
    private double onGroundY;

    @EventTarget
    public void onDisable() {
        selfPrediction = null;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.onGround) {
            onGroundY = Math.floor(mc.thePlayer.posY);
        }
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        selfPrediction = PlayerUtils.simulatePlayer(1, true);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (mc.thePlayer.movementInput.moveForward != -1 || mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
            return;
        }

        if (sneakOnly.get() && !e.isSneaking()) {
            return;
        }

        e.setSneaking(false);

        Vec3 pp = selfPrediction.position;
        AxisAlignedBB predictedBB = new AxisAlignedBB(pp.xCoord - 0.5, pp.yCoord - 0.5, pp.zCoord - 0.5, pp.xCoord + 0.5, pp.yCoord + 0.5, pp.zCoord + 0.5);
        predictedBB = predictedBB.offset(new Vec3(0, onGroundY - mc.thePlayer.posY, 0));

        if (!MoveUtils.getOnGroundPosBased(predictedBB, sneakDist.get())) {
            e.setSneaking(true);
            nextUnSneakDelay = (int) unSneakDelay.random();
            unSneakTimer.reset();
        }

        if (!unSneakTimer.hasTimeElapsed(nextUnSneakDelay * 50L)) {
            e.setSneaking(true);
        }
    }
}
