package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.BlockAABBEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.gui.notification.NotificationType;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.packet.PacketUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import net.minecraft.block.BlockAir;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

@ModuleInfo(name = "Phase", description = "Allows you to phase through blocks.")
public class Phase extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Intave", "Polar"}, "Vanilla", this);

    private boolean phasing;
    private boolean handle;
    private BlockPos pos;
    private EnumFacing sideHit;

    @Override
    public void onEnable() {
        if (mode.is("Intave")) {
            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Intave Phase", "Sneak to move forward in blocks.", 10, new ScaledResolution(mc));
            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Intave Phase", "LMB to start.", 10, new ScaledResolution(mc));
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        switch (mode.get()) {
            case "Vanilla":
                phasing = false;

                final double rotation = Math.toRadians(mc.thePlayer.rotationYaw);

                final double x = Math.sin(rotation);
                final double z = Math.cos(rotation);

                if (mc.thePlayer.isCollidedHorizontally) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX - x * 0.005, mc.thePlayer.posY, mc.thePlayer.posZ + z * 0.005);
                    phasing = true;
                } else if (PlayerUtils.insideBlock()) {
                    sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX - x * 1.5, mc.thePlayer.posY, mc.thePlayer.posZ + z * 1.5, false));

                    mc.thePlayer.motionX *= 0.3D;
                    mc.thePlayer.motionZ *= 0.3D;

                    phasing = true;
                }
                break;
            case "Intave":
                boolean check = mc.gameSettings.keyBindAttack.isKeyDown() && mc.thePlayer.rotationPitch > 80;

                if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && check) {
                    pos = mc.objectMouseOver.getBlockPos();
                    sideHit = mc.objectMouseOver.sideHit;

                    if (!handle) {
                        handle = true;
                    }
                }

                if (handle) {
                    PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, sideHit));
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 0.0052, mc.thePlayer.posZ);
                }

                if (!check) {
                    handle = false;
                }

                if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK && handle) {
                    switch (mc.objectMouseOver.typeOfHit) {
                        case ENTITY -> ChatUtils.sendMessageClient("A fatass is blocking the way, can't phase");
                        case MISS -> ChatUtils.sendMessageClient("Stopped");
                    }

                    mc.thePlayer.jump();
                    handle = false;
                }

                handleInBlockMoveThingWhenSneaking();
                break;
        }
    }

    //todo figure out why this stopped working randomly
    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre() && mode.is("Polar") && PlayerUtils.blockNear(2) && mc.gameSettings.keyBindJump.isKeyDown()) {
            double ground = mc.thePlayer.posY - MoveUtils.findGround(mc.thePlayer);

            if (!MoveUtils.isMovingMotion(mc.thePlayer) && mc.thePlayer.motionY < 0 && ground < 1.26) {
                mc.thePlayer.motionY -= 0.091F;

                if (ground < 1.1) {
                    ChatUtils.sendMessageClient(true);
                    e.setOnGround(true);
                }
            }
        }

        handleInBlockMoveThingWhenSneaking();
    }

    private void handleInBlockMoveThingWhenSneaking() {
        if (mc.thePlayer.isSneaking()) {
            final double wDist = 0.00001D;
            final double aDist = 0.00001D;
            final double sDist = -0.00001D;
            final double dDist = -0.00001D;

            final double rotationn = Math.toRadians(mc.thePlayer.rotationYaw);

            if (mc.gameSettings.keyBindForward.isKeyDown()) {
                final double xx = Math.sin(rotationn) * wDist;
                final double zz = Math.cos(rotationn) * wDist;

                mc.thePlayer.setPosition(mc.thePlayer.posX - xx, mc.thePlayer.posY, mc.thePlayer.posZ + zz);
            }

            if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                final double xx = Math.sin(rotationn) * aDist;

                mc.thePlayer.setPosition(mc.thePlayer.posX + xx, mc.thePlayer.posY, mc.thePlayer.posZ);
            }

            if (mc.gameSettings.keyBindBack.isKeyDown()) {
                final double xx = Math.sin(rotationn) * sDist;
                final double zz = Math.cos(rotationn) * sDist;

                mc.thePlayer.setPosition(mc.thePlayer.posX - xx, mc.thePlayer.posY, mc.thePlayer.posZ + zz);
            }

            if (mc.gameSettings.keyBindRight.isKeyDown()) {
                final double zz = Math.sin(rotationn) * dDist;

                mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ + zz);
            }
        }
    }

    @EventTarget
    public void onBlockAABB(BlockAABBEvent e) {
        if (mode.is("Vanilla")) {
            if (e.getBlock() instanceof BlockAir && phasing) {
                final double x = e.getBlockPos().getX(), y = e.getBlockPos().getY(), z = e.getBlockPos().getZ();

                if (y < mc.thePlayer.posY) {
                    e.setBoundingBox(AxisAlignedBB.fromBounds(-15, -1, -15, 15, 1, 15).offset(x, y, z));
                }
            }
        }
    }
}