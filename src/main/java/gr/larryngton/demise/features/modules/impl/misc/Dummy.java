package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import net.minecraft.client.entity.EntityOtherPlayerMP;

@ModuleInfo(name = "Dummy", description = "Spawns a dummy player.")
public class Dummy extends Module {
    private EntityOtherPlayerMP dummy;

    @Override
    public void onEnable() {
        dummy = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        dummy.setPositionAndRotation(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        dummy.setSprinting(mc.thePlayer.isSprinting());
        dummy.setInvisible(mc.thePlayer.isInvisible());
        dummy.setSneaking(mc.thePlayer.isSneaking());

        mc.theWorld.addEntityToWorld(dummy.getEntityId(), dummy);
    }

//    @EventTarget
//    public void onRender2D(Render2DEvent e) {
//        dummy.rotationYawHead = interpolateRotation(dummy.prevRotationYawHead, dummy.rotationYawHead, e.partialTicks());
//        dummy.rotationPitchHead = interpolateRotation(dummy.prevRotationPitchHead, dummy.rotationPitchHead, e.partialTicks());
//    }
//
//    private float interpolateRotation(float par1, float par2, float par3) {
//        float f;
//
//        for (f = par2 - par1; f < -180.0F; f += 360.0F) {
//        }
//
//        while (f >= 180.0F) {
//            f -= 360.0F;
//        }
//
//        return par1 + par3 * f;
//    }
//
//    @EventTarget
//    public void onTick(TickEvent e) {
//        float f1 = MathHelper.wrapAngleTo180_float(dummy.rotationYaw - dummy.renderYawOffset);
//        float f2 = 50.0F;
//
//        if (Math.abs(f1) > f2) {
//            dummy.renderYawOffset = dummy.renderYawOffset + (f1 - Math.signum(f1) * f2);
//        }
//    }

    @Override
    public void onDisable() {
        if (dummy != null) {
            mc.theWorld.removeEntityFromWorld(dummy.getEntityId());
        }
        dummy = null;
    }
}
