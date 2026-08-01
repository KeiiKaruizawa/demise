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
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla"}, "Vanilla", this);

    private boolean phasing;

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