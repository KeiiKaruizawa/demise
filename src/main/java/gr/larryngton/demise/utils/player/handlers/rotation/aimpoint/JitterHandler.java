package gr.larryngton.demise.utils.player.handlers.rotation.aimpoint;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.MathUtils;
import net.minecraft.network.play.client.C0APacketAnimation;

import java.util.ArrayList;

public class JitterHandler implements InstanceAccess {
    private static final ArrayList<Long> clicks = new ArrayList<>();
    private static int lastTicksExisted;

    private static float tremorYaw = 0;
    private static float tremorPitch = 0;

    private static float targetYaw = 0;
    private static float targetPitch = 0;

    private static float clickImpulseYaw;
    private static float clickImpulsePitch;

    private static float jitterYaw;
    private static float jitterPitch;

    public static float[] calculateJitter(float strength, boolean interpolate) {
        long time = System.currentTimeMillis();
        clicks.removeIf(aLong -> aLong + 1000L < time);

        if (clicks.isEmpty()) {
            return new float[]{jitterYaw, jitterPitch};
        }

        boolean lastTickClicked = false;

        for (Long clickTime : clicks) {
            // buffer of 1 frame update on 60 fps
            if (clickTime + (long) 16.67 > time) {
                lastTickClicked = true;
                break;
            }
        }

        if (lastTickClicked) {
            clickImpulseYaw += MathUtils.randomizeFloat(-strength, strength);
            clickImpulsePitch += MathUtils.randomizeFloat(-strength, strength);
        }

        float cpsFactor = Math.min(clicks.size() / 12f, 1.5f);

        float yawJitter = tremorYaw * 0.6f * cpsFactor + clickImpulseYaw;
        float pitchJitter = tremorPitch * 0.6f * cpsFactor + clickImpulsePitch;

        if (interpolate) {
            jitterYaw = MathUtils.interpolate(jitterYaw, yawJitter, 0.25F);
            jitterPitch = MathUtils.interpolate(jitterPitch, pitchJitter, 0.25F);
        } else {
            jitterYaw = yawJitter;
            jitterPitch = pitchJitter;
        }

        if (mc.thePlayer.ticksExisted != lastTicksExisted) {
            if (Math.random() < 0.08) {
                targetYaw = MathUtils.randomizeFloat(-0.15f, 0.15f);
                targetPitch = MathUtils.randomizeFloat(-0.15f, 0.15f);
            }

            tremorYaw += (targetYaw - tremorYaw) * 0.12f;
            tremorPitch += (targetPitch - tremorPitch) * 0.12f;

            clickImpulseYaw *= 0.75f;
            clickImpulsePitch *= 0.75f;
            lastTicksExisted = mc.thePlayer.ticksExisted;
        }

        return new float[]{jitterYaw, jitterPitch};
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.OUTGOING && e.getPacket() instanceof C0APacketAnimation) {
            clicks.add(System.currentTimeMillis());
        }
    }
}
