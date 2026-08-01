package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ColorValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.awt.*;

@ModuleInfo(name = "VisualAimPoint", description = "Rotations but different.", legit = true)
public class VisualAimPoint extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Dot", "Box"}, "Dot", this);
    private final BoolValue onlySilent = new BoolValue("Only silent", false, this);
    private final BoolValue notOnMiss = new BoolValue("Not on miss", false, this);
    private final SliderValue dotSize = new SliderValue("Size", 0.1f, 0.05f, 0.2f, 0.01f, this);
    private final BoolValue customColor = new BoolValue("Custom Color", false, this);
    private final ColorValue color = new ColorValue("Color", Color.white, this, customColor::get);

    private Vec3 pos, lastPos;

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (e.isPost() || mc.objectMouseOver == null || mc.objectMouseOver.hitVec == null) {
            return;
        }

        switch (mode.get()) {
            case "Dot": {
                mc.entityRenderer.getMouseOver(1);

                lastPos = pos;
                pos = mc.objectMouseOver.hitVec;
                break;
            }
            case "Box": {
                double distance = mc.objectMouseOver.hitVec.getDistanceAtEyeByVec(mc.thePlayer);
                final Vec3 vec31 = mc.thePlayer.getLook(1);
                final Vec3 vec32 = mc.thePlayer.getPositionEyes(1).addVector(vec31.xCoord * distance, vec31.yCoord * distance, vec31.zCoord * distance);

                lastPos = pos;

                pos = vec32;
                break;
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (onlySilent.get() && (!RotationHandler.shouldRotate() || !RotationHandler.silent)) {
            return;
        }

        switch (mode.get()) {
            case "Dot": {
                if (lastPos != null && pos != null) {
                    Vec3 interpolatedPosition = MathUtils.interpolate(lastPos, pos, e.partialTicks());

                    if (notOnMiss.get() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
                        return;
                    }

                    RenderUtils.renderBreadCrumb(interpolatedPosition, dotSize.get(), customColor.get() ? color.get() : new Color(getModule(Interface.class).color()));
                }
                break;
            }
            case "Box": {
                Vec3 vec = MathUtils.interpolate(lastPos, pos, e.partialTicks());

                final double x = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * e.partialTicks();
                final double y = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * e.partialTicks();
                final double z = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * e.partialTicks();

                double d = dotSize.get() / 2;

                AxisAlignedBB target = new AxisAlignedBB(vec.xCoord - d, vec.yCoord - d, vec.zCoord - d, vec.xCoord + d, vec.yCoord + d, vec.zCoord + d);
                AxisAlignedBB axis = new AxisAlignedBB(target.minX - x, target.minY - y, target.minZ - z, target.maxX - x, target.maxY - y, target.maxZ - z);

                if (notOnMiss.get() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
                    return;
                }

                RenderUtils.drawAxisAlignedBB(axis, true, false, customColor.get() ? color.get().getRGB() : getModule(Interface.class).color());
                break;
            }
        }
    }
}
