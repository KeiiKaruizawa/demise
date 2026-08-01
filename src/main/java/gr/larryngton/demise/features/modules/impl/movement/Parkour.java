package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import org.lwjglx.input.Keyboard;

@ModuleInfo(name = "Parkour")
public class Parkour extends Module {
    private final BoolValue notWhileShifting = new BoolValue("Not while shifting", true, this);

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if ((notWhileShifting.get() && Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) || !mc.thePlayer.onGround)
            return;

        if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(mc.thePlayer.motionX / 3.0D, -1.0D, mc.thePlayer.motionZ / 3.0D)).isEmpty()) {
            e.setJumping(true);
        }
    }
}