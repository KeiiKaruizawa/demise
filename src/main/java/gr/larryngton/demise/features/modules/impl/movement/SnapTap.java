package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import org.lwjglx.input.Keyboard;

@ModuleInfo(name = "SnapTap", description = "Prioritize last key pressed.")
public class SnapTap extends Module {

    private boolean lastForward = false;
    private boolean lastStrafe = false;
    private boolean wasForward = false;
    private boolean wasBack = false;
    private boolean wasLeft = false;
    private boolean wasRight = false;

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (mc.currentScreen != null) { // Inv Manager moment, this code is shitty so might break some shit but we fucking ball
            lastForward = false;
            lastStrafe = false;
            wasForward = false;
            wasBack = false;
            wasLeft = false;
            wasRight = false;
            return;
        }

        boolean backward = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        boolean forward = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        boolean right = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
        boolean left = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());

        if (forward && !wasForward) lastForward = true;
        if (backward && !wasBack) lastForward = false;

        if (right && !wasRight) lastStrafe = true;
        if (left && !wasLeft) lastStrafe = false;

        wasForward = forward;
        wasBack = backward;
        wasLeft = left;
        wasRight = right;

        if (forward && backward) {
            e.setForward(lastForward ? 1.0F : -1.0F);
        }

        if (left && right) {
            e.setStrafe(lastStrafe ? -1.0F : 1.0F);
        }
    }

    @Override
    public void onDisable() {
        lastForward = false;
        lastStrafe = false;
        wasForward = false;
        wasBack = false;
        wasLeft = false;
        wasRight = false;
    }

    // This is the most shittest code I ever wrote someone please recode this shit code
}

