package net.minecraft.util;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.SneakSlowDownEvent;
import gr.larryngton.demise.features.modules.impl.misc.InputFix;
import net.minecraft.client.settings.GameSettings;

public class MovementInputFromOptions extends MovementInput {
    private final GameSettings gameSettings;

    public MovementInputFromOptions(GameSettings gameSettingsIn) {
        this.gameSettings = gameSettingsIn;
    }

    public void updatePlayerMoveState() {
        if (Demise.INSTANCE.getModuleManager().getModule(InputFix.class).isEnabled()) {
            InputFix.PreMovementInputEvent preEvent = new InputFix.PreMovementInputEvent();
            Demise.INSTANCE.getEventManager().call(preEvent);

            this.moveForward = preEvent.getForward();
            this.moveStrafe = preEvent.getStrafe();
            this.jump = preEvent.isJumping();
            this.sneak = preEvent.isSneaking();
        } else {
            this.moveForward = 0.0F;
            this.moveStrafe = 0.0F;

            if (this.gameSettings.keyBindForward.isKeyDown()) {
                ++this.moveForward;
            }

            if (this.gameSettings.keyBindBack.isKeyDown()) {
                --this.moveForward;
            }

            if (this.gameSettings.keyBindLeft.isKeyDown()) {
                ++this.moveStrafe;
            }

            if (this.gameSettings.keyBindRight.isKeyDown()) {
                --this.moveStrafe;
            }

            this.jump = this.gameSettings.keyBindJump.isKeyDown();
            this.sneak = this.gameSettings.keyBindSneak.isKeyDown();
        }

        MoveInputEvent event = new MoveInputEvent(moveForward, moveStrafe, jump, sneak);
        Demise.INSTANCE.getEventManager().call(event);

        if (!event.isCancelled()) {
            this.moveForward = event.getForward();
            this.moveStrafe = event.getStrafe();

            this.jump = event.isJumping();
            this.sneak = event.isSneaking();
        } else {
            this.moveForward = this.moveStrafe = 0F;
            this.jump = this.sneak = false;
        }

        if (this.sneak) {
            SneakSlowDownEvent sneakSlowDownEvent = new SneakSlowDownEvent(0.3, 0.3);
            Demise.INSTANCE.getEventManager().call(sneakSlowDownEvent);

            this.moveStrafe = (float) ((double) this.moveStrafe * sneakSlowDownEvent.getStrafe());
            this.moveForward = (float) ((double) this.moveForward * sneakSlowDownEvent.getForward());
        }
    }
}
