package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.JumpEvent;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.MoveUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import org.apache.commons.lang3.Range;

@ModuleInfo(name = "LongJump", description = "Jump, but long.")
public class LongJump extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Boat", "Miniblox", "Matrix", "NCP damage", "Polar"}, "Vanilla", this);
    private final SliderValue jumpOffAmount = new SliderValue("JumpOff amount", 0.2f, 0.01f, 2, 0.01f, this, () -> mode.is("Vanilla"));
    private final SliderValue verticalMotion = new SliderValue("Vertical motion", 0.25f, 0, 2, 0.01f, this, () -> mode.is("Boat"));
    private final SliderValue horizontalMotion = new SliderValue("Horizontal motion", 0.25f, 0, 2, 0.01f, this, () -> mode.is("Boat"));

    private boolean jumped;
    private int currentTimer;
    private int pauseTimes;
    private int activeTicks;
    private boolean flagged;
    private boolean wasInBoat;
    private int ticks;
    private int jumps;

    @EventTarget
    public void onJump(JumpEvent e) {
        if (mode.is("Vanilla")) {
            e.setJumpoff(jumpOffAmount.get());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());

        switch (mode.get()) {
            case "Miniblox":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                activeTicks++;

                if (activeTicks <= 10) {
                    MoveUtils.stop();
                } else {
                    if (!jumped) {
                        if (mc.thePlayer.onGround) {
                            MoveUtils.stop();
                            mc.thePlayer.jump();
                        }

                        jumped = true;
                    } else {
                        int maxTimer = 0;
                        float yaw = (float) Math.toRadians(mc.thePlayer.rotationYaw);

                        switch (pauseTimes) {
                            case 0:
                                mc.thePlayer.motionX = 1.9 * -Math.sin(yaw);
                                mc.thePlayer.motionZ = 1.9 * Math.cos(yaw);
                                maxTimer = 10;
                                break;
                            case 1:
                                mc.thePlayer.motionX = 1.285 * -Math.sin(yaw);
                                mc.thePlayer.motionZ = 1.285 * Math.cos(yaw);
                                maxTimer = 15;
                                break;
                            case 2:
                                mc.thePlayer.motionX = 1.1625 * -Math.sin(yaw);
                                mc.thePlayer.motionZ = 1.1625 * Math.cos(yaw);
                                maxTimer = 5;
                                break;
                        }

                        mc.thePlayer.motionY = 0.29;
                        currentTimer++;

                        if (Range.between(4, maxTimer).contains(currentTimer)) {
                            MoveUtils.stop();
                        } else if (currentTimer > maxTimer) {
                            pauseTimes++;
                            currentTimer = 0;
                            jumped = false;
                        }
                    }

                    if (pauseTimes >= 3) {
                        MoveUtils.stop();
                        setEnabled(false);
                    }
                }
                break;
            case "Matrix":
                if (mc.thePlayer.onGround) {
                    if (MoveUtils.isMoving()) {
                        mc.thePlayer.jump();
                    }
                } else if (MoveUtils.isMoving()) {
                    mc.thePlayer.motionY = 0.42;
                    MoveUtils.strafe(1.97);
                }

                if (flagged) {
                    setEnabled(false);
                }
                break;
            case "Boat":
                if (mc.thePlayer.ridingEntity != null) {
                    wasInBoat = true;
                } else if (wasInBoat) {
                    mc.thePlayer.motionY = verticalMotion.get();
                    MoveUtils.strafe(horizontalMotion.get());
                    wasInBoat = false;
                }
                break;
            case "Polar":
                float yaw = (float) Math.toRadians(mc.thePlayer.rotationYaw);

                if (mc.thePlayer.ridingEntity != null) {
                    wasInBoat = true;
                    mc.timer.timerSpeed = 0.5f;
                } else if (wasInBoat) {
                    mc.timer.timerSpeed = 0.75f;
                    mc.thePlayer.motionX = 1.5 * -Math.sin(yaw);
                    mc.thePlayer.motionZ = 1.5 * Math.cos(yaw);
                    wasInBoat = false;
                }

                if (mc.thePlayer.ridingEntity == null && mc.thePlayer.onGround && mc.timer.timerSpeed == 0.75f) {
                    mc.timer.timerSpeed = 1;
                }
                break;
            case "NCP damage":
                switch (ticks) {
                    case 0:
                        if (mc.thePlayer.onGround) {
                            if (jumps == 3) {
                                ticks++;
                            } else {
                                mc.thePlayer.jump();
                                jumps++;
                            }
                        }
                        break;
                    case 1:
                        mc.timer.timerSpeed = 0.5f;

                        if (mc.thePlayer.hurtTime != 0) {
                            mc.thePlayer.motionY = 0.33;
                            MoveUtils.strafe(0.25);
                            ticks = 2;
                        }
                        break;
                    case 3:
                        MoveUtils.strafe(1.25);
                        break;
                    case 4:
                        mc.timer.timerSpeed = 1;
                        break;
                }

                if (ticks >= 2) {
                    ticks++;
                }
                break;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (mode.is("NCP damage") && jumps >= 3 && ticks < 4) {
            e.setForward(1);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        switch (mode.get()) {
            case "Matrix":
                if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                    flagged = true;
                }
                break;
            case "NCP damage":
                if (e.getPacket() instanceof C03PacketPlayer c03 && jumps < 3) {
                    c03.setOnGround(false);
                }

                if (e.getPacket() instanceof S12PacketEntityVelocity) {
                    e.setCancelled(true);
                }
                break;
        }
    }

    @Override
    public void onDisable() {
        if (mode.is("Miniblox")) {
            MoveUtils.stop();
        }
        jumped = false;
        currentTimer = 0;
        pauseTimes = 0;
        activeTicks = 0;
        flagged = false;
        wasInBoat = false;
        jumps = 0;
        ticks = 0;
        mc.timer.timerSpeed = 1;
    }
}