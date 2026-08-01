package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.gui.notification.NotificationType;
import gr.larryngton.demise.utils.player.MoveUtils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.client.*;

@ModuleInfo(name = "FreeCam", description = "Allows you to move freely.")
public class FreeCam extends Module {
    private final BoolValue fly = new BoolValue("Fly", false, this);
    private final BoolValue noClip = new BoolValue("No clip", false, this, fly::get);

    private boolean waitingForStop;
    private double x, y, z;
    private float[] rotation;
    private EntityOtherPlayerMP serverPlayer;

    @Override
    public void onEnable() {
        waitingForStop = !mc.thePlayer.onGround || MoveUtils.getSpeed() != 0;

        if (waitingForStop) {
            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "FreeCam", "Waiting for stop.", new ScaledResolution(mc));
        } else {
            updateServerState();
        }
    }

    @Override
    public void onDisable() {
        if (!waitingForStop) {
            mc.thePlayer.setPositionAndRotation(x, y, z, rotation[0], rotation[1]);

            if (serverPlayer != null) {
                mc.theWorld.removeEntityFromWorld(serverPlayer.getEntityId());
            }

            mc.thePlayer.capabilities.isFlying = false;
            mc.thePlayer.onGround = true;
            MoveUtils.stop();
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (waitingForStop) {
            if (mc.thePlayer.onGround && MoveUtils.getSpeed() == 0) {
                updateServerState();
                waitingForStop = false;
            }
        } else if (fly.get()) {
            mc.thePlayer.motionY = 0.0D;
            mc.thePlayer.capabilities.setFlySpeed(0.05F);
            mc.thePlayer.capabilities.isFlying = true;
            mc.thePlayer.noClip = noClip.get();
            mc.thePlayer.onGround = false;
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (!waitingForStop) {
            if (e.isPre()) {
                e.setX(x);
                e.setY(y);
                e.setZ(z);
                e.setYaw(rotation[0]);
                e.setPitch(rotation[1]);
                e.setOnGround(true);
                e.setSprinting(false);
            } else {
                mc.thePlayer.rotationYawHead = mc.thePlayer.rotationYaw;
                mc.thePlayer.rotationPitchHead = mc.thePlayer.rotationPitch;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.OUTGOING && !waitingForStop) {
            if (e.getPacket() instanceof C0CPacketInput c0c) {
                c0c.setForwardSpeed(0);
                c0c.setStrafeSpeed(0);
                c0c.setJumping(false);
                c0c.setSneaking(false);
            }

            if (e.getPacket() instanceof C0APacketAnimation || e.getPacket() instanceof C02PacketUseEntity || e.getPacket() instanceof C07PacketPlayerDigging || e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
                e.setCancelled(true);
            }
        }
    }

    private void updateServerState() {
        x = mc.thePlayer.posX;
        y = mc.thePlayer.posY;
        z = mc.thePlayer.posZ;
        rotation = mc.thePlayer.getRotation();

        serverPlayer = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        serverPlayer.setPositionAndRotation(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYawHead, mc.thePlayer.rotationPitchHead);
        serverPlayer.setSprinting(mc.thePlayer.isSprinting());
        serverPlayer.setInvisible(mc.thePlayer.isInvisible());
        serverPlayer.setSneaking(mc.thePlayer.isSneaking());

        mc.theWorld.addEntityToWorld(serverPlayer.getEntityId(), serverPlayer);
    }
}
