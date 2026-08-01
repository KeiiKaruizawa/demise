package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.JumpEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.Value;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.gui.notification.NotificationType;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.sound.SoundUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

@ModuleInfo(name = "MotionModifier", description = "Testing module for speeds. Good luck trying to configure this.")
public class MotionModifier extends Module {
    private final BoolValue motionMulti = new BoolValue("Motion multi", false, this);

    private final BoolValue air = new BoolValue("On air", true, this, motionMulti::get);
    private final SliderValue airXZ1 = new SliderValue("XZ multi (air tick 1)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ2 = new SliderValue("XZ multi (air tick 2)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ3 = new SliderValue("XZ multi (air tick 3)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ4 = new SliderValue("XZ multi (air tick 4)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ5 = new SliderValue("XZ multi (air tick 5)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ6 = new SliderValue("XZ multi (air tick 6)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ7 = new SliderValue("XZ multi (air tick 7)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ8 = new SliderValue("XZ multi (air tick 8)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ9 = new SliderValue("XZ multi (air tick 9)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ10 = new SliderValue("XZ multi (air tick 10)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ11 = new SliderValue("XZ multi (air tick 11)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ12 = new SliderValue("XZ multi (air tick 12)", 1, 0, 2, 0.001f, this, () -> air.get() && motionMulti.get());
    private final BoolValue airAuto = new BoolValue("Automatically config air boost", false, this, () -> air.get() && motionMulti.get());

    private final BoolValue ground = new BoolValue("On ground", false, this, motionMulti::get);
    private final SliderValue groundXZ1 = new SliderValue("XZ multi (ground tick 1)", 1, 0, 2, 0.001f, this, () -> ground.get() && motionMulti.get());
    private final SliderValue groundXZ2 = new SliderValue("XZ multi (ground tick 2)", 1, 0, 2, 0.001f, this, () -> ground.get() && motionMulti.get());
    private final SliderValue groundXZ3 = new SliderValue("XZ multi (ground tick 3)", 1, 0, 2, 0.001f, this, () -> ground.get() && motionMulti.get());
    private final SliderValue groundXZ4 = new SliderValue("XZ multi (ground tick 4)", 1, 0, 2, 0.001f, this, () -> ground.get() && motionMulti.get());
    private final SliderValue groundXZ5 = new SliderValue("XZ multi (ground tick 5)", 1, 0, 2, 0.001f, this, () -> ground.get() && motionMulti.get());

    private final BoolValue pulldownOnAir = new BoolValue("Pulldown on air", false, this);
    private final SliderValue pulldown1 = new SliderValue("Pulldown (tick 1)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown2 = new SliderValue("Pulldown (tick 2)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown3 = new SliderValue("Pulldown (tick 3)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown4 = new SliderValue("Pulldown (tick 4)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown5 = new SliderValue("Pulldown (tick 5)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown6 = new SliderValue("Pulldown (tick 6)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown7 = new SliderValue("Pulldown (tick 7)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown8 = new SliderValue("Pulldown (tick 8)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown9 = new SliderValue("Pulldown (tick 9)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown10 = new SliderValue("Pulldown (tick 10)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown11 = new SliderValue("Pulldown (tick 11)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final SliderValue pulldown12 = new SliderValue("Pulldown (tick 12)", 0, 0, 1, 0.001f, this, pulldownOnAir::get);
    private final BoolValue pullDownAuto = new BoolValue("Automatically config pulldown", false, this, pulldownOnAir::get);

    private final BoolValue strafe = new BoolValue("Strafe", false, this);
    private final SliderValue strafe1 = new SliderValue("Strafe (tick 1)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe2 = new SliderValue("Strafe (tick 2)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe3 = new SliderValue("Strafe (tick 3)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe4 = new SliderValue("Strafe (tick 4)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe5 = new SliderValue("Strafe (tick 5)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe6 = new SliderValue("Strafe (tick 6)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe7 = new SliderValue("Strafe (tick 7)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe8 = new SliderValue("Strafe (tick 8)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe9 = new SliderValue("Strafe (tick 9)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe10 = new SliderValue("Strafe (tick 10)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe11 = new SliderValue("Strafe (tick 11)", 0, -0.05f, 1, 0.001f, this, strafe::get);
    private final SliderValue strafe12 = new SliderValue("Strafe (tick 12)", 0, -0.05f, 1, 0.001f, this, strafe::get);

    private final BoolValue autoJump = new BoolValue("Auto jump", true, this);
    private final SliderValue jumpHeight = new SliderValue("Jump height", 0.42f, 0, 1, 0.001f, this, autoJump::get);
    private final SliderValue jumpOff = new SliderValue("Jumpoff", 0.2f, 0, 1, 0.001f, this, autoJump::get);

    private final SliderValue timer = new SliderValue("Timer", 1, 0.1f, 2, 0.001f, this);

    private final BoolValue reset = new BoolValue("Reset", false, this);
    private final BoolValue printAirTicks = new BoolValue("Print air ticks", false, this);

    private int checkingTicks;
    public int index;
    private boolean flagged;
    private int checkingTicksP;
    public int indexP;
    private boolean flaggedP;
    private final TimerUtil flagTimer = new TimerUtil();
    private int currentRepeat;
    private float autoConfigStep;

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1;
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            if (flagTimer.hasTimeElapsed(500)) {
                if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                    flagged = flaggedP = true;
                    flagTimer.reset();
                }

                if (e.getPacket() instanceof S02PacketChat s02PacketChat) {
                    // for silent flagging anticheats
                    if (s02PacketChat.getChatComponent().getUnformattedText().contains("failed")) {
                        flagged = flaggedP = true;
                        flagTimer.reset();
                    }
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (reset.get()) {
            for (Value val : getValues()) {
                if (val instanceof SliderValue sliderValue) {
                    if (sliderValue.getName().startsWith("XZ multi (air") || sliderValue.getName().startsWith("XZ multi (ground") || sliderValue.getName().equals("Timer")) {
                        sliderValue.setValue(1);
                    } else if (sliderValue.getName().startsWith("Pulldown (") || sliderValue.getName().startsWith("Strafe (")) {
                        sliderValue.setValue(0);
                    } else if (sliderValue.getName().equals("Jump height")) {
                        sliderValue.setValue(0.42f);
                    } else if (sliderValue.getName().equals("Jumpoff")) {
                        sliderValue.setValue(0.2f);
                    }
                }
            }

            flagged = false;
            checkingTicks = 0;
            index = 0;
            flaggedP = false;
            checkingTicksP = 0;
            indexP = 0;
            autoConfigStep = 0.1f;

            reset.set(false);
        }

        mc.timer.timerSpeed = timer.get();

        if ((motionMulti.get() && air.get() && airAuto.get()) || (pulldownOnAir.get() && pullDownAuto.get())) {
            autoConfigStep = switch (currentRepeat) {
                case 0 -> 0.1f;
                case 1 -> 0.05f;
                case 2 -> 0.025f;
                case 3 -> 0.01f;
                case 4 -> 0.005f;
                case 5 -> 0.0025f;
                case 6 -> 0.001f;
                default -> 0;
            };

            // secret method
            //todo: proper pathfinding between 2 spots
//            if (mc.thePlayer.isCollidedHorizontally) {
//                mc.thePlayer.rotationYaw += 180;
//            }
        }

        if (motionMulti.get()) {
            if (air.get()) {
                if (airAuto.get()) {
                    if (MoveUtils.isMoving()) {
                        switch (index) {
                            case 0 -> index = 1;
                            case 1 -> checkValue(airXZ1);
                            case 2 -> checkValue(airXZ2);
                            case 3 -> checkValue(airXZ3);
                            case 4 -> checkValue(airXZ4);
                            case 5 -> checkValue(airXZ5);
                            case 6 -> checkValue(airXZ6);
                            case 7 -> checkValue(airXZ7);
                            case 8 -> checkValue(airXZ8);
                            case 9 -> checkValue(airXZ9);
                            case 10 -> checkValue(airXZ10);
                            case 11 -> checkValue(airXZ11);
                            case 12 -> checkValue(airXZ12);
                        }
                    }
                } else {
                    flagged = false;
                    checkingTicks = 0;
                    index = 0;
                }

                switch (mc.thePlayer.offGroundTicks) {
                    case 1 -> multiXZ(airXZ1.get());
                    case 2 -> multiXZ(airXZ2.get());
                    case 3 -> multiXZ(airXZ3.get());
                    case 4 -> multiXZ(airXZ4.get());
                    case 5 -> multiXZ(airXZ5.get());
                    case 6 -> multiXZ(airXZ6.get());
                    case 7 -> multiXZ(airXZ7.get());
                    case 8 -> multiXZ(airXZ8.get());
                    case 9 -> multiXZ(airXZ9.get());
                    case 10 -> multiXZ(airXZ10.get());
                    case 11 -> multiXZ(airXZ11.get());
                    case 12 -> multiXZ(airXZ12.get());
                }
            } else {
                flagged = false;
                checkingTicks = 0;
                index = 0;
            }

            if (ground.get()) {
                switch (mc.thePlayer.onGroundTicks) {
                    case 1 -> multiXZ(groundXZ1.get());
                    case 2 -> multiXZ(groundXZ2.get());
                    case 3 -> multiXZ(groundXZ3.get());
                    case 4 -> multiXZ(groundXZ4.get());
                    case 5 -> multiXZ(groundXZ5.get());
                }
            }
        } else {
            flagged = false;
            checkingTicks = 0;
            index = 0;
        }

        if (pulldownOnAir.get()) {
            if (pullDownAuto.get()) {
                switch (indexP) {
                    case 0 -> indexP = 1;
                    case 1 -> checkValuePullDown(pulldown1);
                    case 2 -> checkValuePullDown(pulldown2);
                    case 3 -> checkValuePullDown(pulldown3);
                    case 4 -> checkValuePullDown(pulldown4);
                    case 5 -> checkValuePullDown(pulldown5);
                    case 6 -> checkValuePullDown(pulldown6);
                    case 7 -> checkValuePullDown(pulldown7);
                    case 8 -> checkValuePullDown(pulldown8);
                    case 9 -> checkValuePullDown(pulldown9);
                    case 10 -> checkValuePullDown(pulldown10);
                    case 11 -> checkValuePullDown(pulldown11);
                    case 12 -> checkValuePullDown(pulldown12);
                }
            } else {
                flaggedP = false;
                checkingTicksP = 0;
                indexP = 0;
            }

            switch (mc.thePlayer.offGroundTicks) {
                case 1 -> mc.thePlayer.motionY -= pulldown1.get();
                case 2 -> mc.thePlayer.motionY -= pulldown2.get();
                case 3 -> mc.thePlayer.motionY -= pulldown3.get();
                case 4 -> mc.thePlayer.motionY -= pulldown4.get();
                case 5 -> mc.thePlayer.motionY -= pulldown5.get();
                case 6 -> mc.thePlayer.motionY -= pulldown6.get();
                case 7 -> mc.thePlayer.motionY -= pulldown7.get();
                case 8 -> mc.thePlayer.motionY -= pulldown8.get();
                case 9 -> mc.thePlayer.motionY -= pulldown9.get();
                case 10 -> mc.thePlayer.motionY -= pulldown10.get();
                case 11 -> mc.thePlayer.motionY -= pulldown11.get();
                case 12 -> mc.thePlayer.motionY -= pulldown12.get();
            }
        } else {
            flaggedP = false;
            checkingTicksP = 0;
            indexP = 0;
        }

        if (strafe.get()) {
            switch (mc.thePlayer.offGroundTicks) {
                case 1 -> realStrafe(strafe1.get());
                case 2 -> realStrafe(strafe2.get());
                case 3 -> realStrafe(strafe3.get());
                case 4 -> realStrafe(strafe4.get());
                case 5 -> realStrafe(strafe5.get());
                case 6 -> realStrafe(strafe6.get());
                case 7 -> realStrafe(strafe7.get());
                case 8 -> realStrafe(strafe8.get());
                case 9 -> realStrafe(strafe9.get());
                case 10 -> realStrafe(strafe10.get());
                case 11 -> realStrafe(strafe11.get());
                case 12 -> realStrafe(strafe12.get());
            }
        }

        if (autoJump.get() && MoveUtils.isMoving()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), mc.thePlayer.onGround);
        }

        if (printAirTicks.get()) {
            ChatUtils.sendMessageClient(mc.thePlayer.offGroundTicks);
        }
    }

    private void checkValue(SliderValue value) {
        checkingTicks++;

        if (checkingTicks > 200) {
            value.setValue(value.get() + autoConfigStep);
            checkingTicks = 0;
            SoundUtil.notifSound(NotificationType.INFO);
        }

        if (flagged) {
            value.setValue(value.get() - autoConfigStep);
            index++;
            checkingTicks = 0;
            flagged = false;
        }

        ChatUtils.sendMessageClient("Index: " + index + " - Ticks: " + checkingTicks + " | Multiplier: " + value.get() + " | Step: " + autoConfigStep);

        if (index == 12) {
            SoundUtil.notifSound(NotificationType.SUCCESS);

            currentRepeat++;
            index = 0;
            if (currentRepeat > 5) {
                ChatUtils.sendMessageClient("Done configuring air boost.");
                autoConfigStep = 0.1f;
                currentRepeat = 0;
                airAuto.set(false);
            }
        }
    }

    private void checkValuePullDown(SliderValue value) {
        checkingTicksP++;

        if (checkingTicksP > 200) {
            value.setValue(value.get() + autoConfigStep);
            checkingTicksP = 0;
            SoundUtil.notifSound(NotificationType.INFO);
        }

        if (flaggedP) {
            value.setValue(value.get() - autoConfigStep);
            indexP++;
            checkingTicksP = 0;
            flaggedP = false;
        }

        ChatUtils.sendMessageClient("Index: " + indexP + " - Ticks: " + checkingTicksP + " | Motion: " + value.get());

        if (indexP == 12) {
            SoundUtil.notifSound(NotificationType.SUCCESS);

            currentRepeat++;
            index = 0;
            if (currentRepeat > 5) {
                ChatUtils.sendMessageClient("Done configuring pulldown.");
                autoConfigStep = 0.1f;
                currentRepeat = 0;
                pullDownAuto.set(false);
            }
        }
    }

    @EventTarget
    public void onJump(JumpEvent e) {
        if (autoJump.get()) {
            e.setMotionY(jumpHeight.get());
            e.setJumpoff(jumpOff.get());
        }
    }

    private void multiXZ(float amount) {
        mc.thePlayer.motionX *= amount;
        mc.thePlayer.motionZ *= amount;
    }

    private void realStrafe(float amount) {
        if (amount > 0) {
            MoveUtils.strafe(amount);
        } else if (amount == 0) {
            MoveUtils.strafe();
        }
    }
}