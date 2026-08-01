package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.JumpEvent;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.player.WebSlowDownEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.SimulatedPlayer;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import net.minecraft.util.BlockPos;
import org.lwjglx.input.Keyboard;

@ModuleInfo(name = "NoWeb", description = "Modifies web slowdown.")
public class NoWeb extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Legit flat", "Motion", "Polar"}, "Vanilla", this);
    private final SliderValue hurtTime = new SliderValue("Hurt time", 1, 1, 10, 1, this, () -> mode.is("Polar"));
    private final SliderValue xzMotion = new SliderValue("XZ motion", 0.2f, 0, 1, 0.01f, this, () -> mode.is("Motion"));
    private final SliderValue xzTicks = new SliderValue("XZ ticks", 1, 1, 5, 1, this, () -> mode.is("Motion"));
    private final SliderValue yMotion = new SliderValue("Y motion", 1.5f, 0, 2, 0.01f, this, () -> mode.is("Motion"));
    private final BoolValue safeJump = new BoolValue("Safe jump", true, this, () -> mode.is("Vanilla"));
    private final BoolValue extraDown = new BoolValue("Extra down", false, this, () -> mode.is("Legit flat"));

    public NoWeb() {
        safeJump.setDescription("Modifies jump values to not get flagged.");
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());
    }

    @EventTarget
    public void onWebSlowDown(WebSlowDownEvent e) {
        switch (mode.get()) {
            case "Vanilla":
                e.setCancelled(true);

                if (mc.thePlayer.offGroundTicks > 10 && safeJump.get()) {
                    mc.thePlayer.motionX *= 0.5f;
                    mc.thePlayer.motionZ *= 0.5f;
                }
                break;
            case "Legit flat":
                if (extraDown.get()) {
                    e.setMotionY(-1.5);
                }
                break;
            case "Motion":
                if (MoveUtils.isMoving() && mc.thePlayer.ticksExisted % xzTicks.getInt() == 0) {
                    e.setMotionX(-Math.sin(MoveUtils.getDirection()) * xzMotion.get());
                    e.setMotionZ(Math.cos(MoveUtils.getDirection()) * xzMotion.get());
                }

                if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                    e.setMotionY(yMotion.get());
                }

                if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                    e.setMotionY(-yMotion.get());
                }
                break;
            case "Polar":
                //GOD BYPASS
                if (mc.thePlayer.hurtTime >= hurtTime.getInt()) {
                    e.setCancelled(Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
                } else {
                    if (!mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.05, mc.thePlayer.posZ)).getBlock().getMaterial().isOpaque()) {
                        if (!mc.thePlayer.isSneaking()) {
                            e.setMotionY(0.080399998158216F);
                        } else {
                            e.setMotionY(-10);
                        }

                        SimulatedPlayer simulatedSelf = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);
                        simulatedSelf.rotationYaw = RotationHandler.currentRotation != null ? RotationHandler.currentRotation[0] : mc.thePlayer.rotationYaw;
                        simulatedSelf.tick();
                        if (simulatedSelf.isInWeb && MoveUtils.isMoving()) {
                            //todo: test 0.21 once every 2 ticks

                            e.setMotionX(-Math.sin(MoveUtils.getDirection()) * 0.1F);
                            e.setMotionZ(Math.cos(MoveUtils.getDirection()) * 0.1F);
                        }
                    }
                }
                break;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (mode.is("Legit flat") && mc.thePlayer.isInWeb && MoveUtils.isMoving()) {
            mc.thePlayer.jumpTicks = 0;
            e.setJumping(true);
        }
    }

    @EventTarget
    public void onJump(JumpEvent e) {
        if (mc.thePlayer.isInWeb) {
            switch (mode.get()) {
                case "Vanilla":
                    if (safeJump.get()) {
                        e.setJumpoff(0.05f);
                    }
                    break;
                case "Legit flat":
                    e.setMotionY(0);
                    break;
            }
        }
    }
}
