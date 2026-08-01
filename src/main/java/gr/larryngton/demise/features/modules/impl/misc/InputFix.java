package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.Event;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.utils.math.TimerUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import java.util.ArrayList;

//todo
@ModuleInfo(name = "InputFix", description = "Applies the input fix from newer Minecraft versions and allows for sub-tick input listening.", legit = true)
public class InputFix extends Module {
    private GuiScreen prevGuiScreen;
    private final KeyBinding[] keyBindings = new KeyBinding[]{
            mc.gameSettings.keyBindPlayerList,
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindUseItem,
            mc.gameSettings.keyBindSprint,
            mc.gameSettings.keyBindRight,
            mc.gameSettings.keyBindSneak,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindJump
    };

    private ArrayList<Float> moveForwardList = new ArrayList<>();
    private ArrayList<Float> moveStrafeList = new ArrayList<>();
    private ArrayList<Boolean> jumpList = new ArrayList<>();
    private ArrayList<Boolean> sneakList = new ArrayList<>();
    private int updateCount;

    private boolean firstTick;

    private final TimerUtil tickTimer = new TimerUtil();

    @EventTarget
    public void onGame(GameEvent e) {
        if (mc.currentScreen == null) {
            float moveForward = 0.0F;
            float moveStrafe = 0.0F;

            if (GameSettings.isKeyDown(mc.gameSettings.keyBindForward)) {
                ++moveForward;
            }

            if (GameSettings.isKeyDown(mc.gameSettings.keyBindBack)) {
                --moveForward;
            }

            if (GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
                ++moveStrafe;
            }

            if (GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
                --moveStrafe;
            }

            boolean jump = GameSettings.isKeyDown(mc.gameSettings.keyBindJump);
            boolean sneak = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak);

            updateCount++;

            moveForwardList.add(moveForward);
            moveStrafeList.add(moveStrafe);
            jumpList.add(jump);
            sneakList.add(sneak);

            firstTick = true;
        } else if (firstTick) {
//            moveStrafe = moveForward = 0.0F;
//            jump = sneak = false;
//            firstTick = false;
        }
    }

    @EventTarget
    public void onPreMovementInput(PreMovementInputEvent e) {
        if (updateCount == 0) return;

        float moveForward = 0;
        float moveStrafe = 0;
        float jumpF = 0;
        float sneakF = 0;

        boolean jump = false;
        boolean sneak = false;

        for (int i = 0; i < updateCount; i++) {
            moveForward += moveForwardList.get(i);
            moveStrafe += moveStrafeList.get(i);
            jumpF += jumpList.get(i) ? 1 : 0;
            sneakF += sneakList.get(i) ? 1 : 0;
        }

        moveForward /= updateCount;
        moveStrafe /= updateCount;
        jumpF /= updateCount;
        sneakF /= updateCount;

        e.setForward(moveForward);
        e.setStrafe(moveStrafe);
        e.setJumping(jump);
        e.setSneaking(sneak);

        updateCount = 0;
    }

    @EventTarget
    private void onUpdate(UpdateEvent e) {
        if (mc.currentScreen == null && prevGuiScreen != null) {
            if (tickTimer.hasTimeElapsed(50)) {
                for (KeyBinding keyBinding : this.keyBindings) {
                    KeyBinding.setKeyBindState(keyBinding.getKeyCode(), GameSettings.isKeyDown(keyBinding));
                }
            }
        } else {
            tickTimer.reset();
        }

        prevGuiScreen = mc.currentScreen;
    }

    @Getter
    @Setter
    public static class PreMovementInputEvent implements Event {
        private float forward;
        private float strafe;
        private boolean jumping;
        private boolean sneaking;
    }
}