package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.movement.Speed;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.MoveUtils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

@ModuleInfo(name = "Criticals", description = "Allows you to get more critical hits.")
public class Criticals extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Jump", "NoGround", "Visual", "NCP", "Timer", "Offset", "Float"}, "Jump", this);
    private final BoolValue staticOffset = new BoolValue("Static offset", false, this, () -> mode.is("NCP"));
    private final BoolValue stopXZOnHit = new BoolValue("Stop XZ on hit", true, this, () -> mode.is("NCP"));
    private final SliderValue tickDelay = new SliderValue("Tick delay", 9, 0, 20, 1, this);

    public Criticals() {
        staticOffset.setDescription("Use a fixed motion value instead of an array of values to tp.");
        stopXZOnHit.setDescription("Stop xz motion on attack.");
        tickDelay.setDescription("Ticks before dealing a critical hit again.");
    }

    private boolean attacked;
    private final TimerUtil timer = new TimerUtil();
    private final double[] offsets = {0.06, 0.0, 0.03, 0.0};
    private final TimerUtil attackTimer = new TimerUtil();
    private boolean jumped;
    private boolean waitForNextGround;

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1f;
        attacked = false;
        jumped = false;
        waitForNextGround = false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (mc.thePlayer.isInWater()) {
            return;
        }

        if (timer.hasTimeElapsed(tickDelay.get() * 50)) {
            attacked = true;

            switch (mode.get()) {
                case "Jump":
                    if (MoveUtils.isMoving() && mc.thePlayer.onGround
                            && !getModule(Speed.class).isEnabled()
                            && !mc.thePlayer.isJumping) {
                        mc.thePlayer.jump();
                    }
                    break;
                case "Visual":
                    mc.thePlayer.onCriticalHit(e.getTargetEntity());
                    break;
                case "Timer":
                    if (MoveUtils.isMoving()) {
                        if (mc.thePlayer.onGround) {
                            if (!getModule(Speed.class).isEnabled() && !mc.thePlayer.isJumping) {
                                mc.thePlayer.jump();
                            }
                        }

                        if (mc.thePlayer.motionY >= 0) {
                            mc.timer.timerSpeed = 2.75f;
                        } else {
                            mc.timer.timerSpeed = 0.6f;
                        }
                    }
                    break;
                case "NCP":
                    if (mc.thePlayer.isCollidedVertically) {
                        if (getModule(Speed.class).isEnabled()) {
                            return;
                        }

                        if (mc.thePlayer.onGround) {
                            if (stopXZOnHit.get()) {
                                mc.thePlayer.moveEntity(0, staticOffset.get() ? 0.06251 : offsets[mc.thePlayer.ticksExisted % 5], 0);
                            } else {
                                mc.thePlayer.posY += staticOffset.get() ? 0.06251 : offsets[mc.thePlayer.ticksExisted % 5];
                            }
                        }
                    }
                    break;
                case "Offset":
                    if (!mc.thePlayer.onGround) {
                        sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY - 0.0000001, mc.thePlayer.posZ, false));
                    }
                    break;
                case "Float":
                    attackTimer.reset();
                    break;
            }

            timer.reset();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        switch (mode.get()) {
            case "NCP":
                if (e.getPacket() instanceof C03PacketPlayer c03PacketPlayer) {
                    c03PacketPlayer.setMoving(false);
                    c03PacketPlayer.setOnGround(false);
                }
                break;
            case "Float":
                if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                    attackTimer.setTime(attackTimer.getTime() + 676767);
                    waitForNextGround = true;
                }
                break;
        }
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (e.isPre()) {
            switch (mode.get()) {
                case "NoGround":
                    if (attacked) {
                        e.setOnGround(false);
                    }
                    attacked = false;
                    break;
                case "Float":
                    if (!attackTimer.hasTimeElapsed(250) && (e.isOnGround() || !waitForNextGround)) {
                        if (!jumped) {
                            if (e.isOnGround()) {
                                mc.thePlayer.motionY = 0.2;
                            }
                            jumped = true;
                        } else {
                            mc.thePlayer.motionY = 0;
                            MoveUtils.strafe(0.221);
                            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 1e-4, mc.thePlayer.posZ);
                        }

                        waitForNextGround = false;
                    } else {
                        jumped = false;
                        waitForNextGround = true;
                    }
                    break;
            }
        }
    }
}
