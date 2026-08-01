package gr.larryngton.demise.features.modules.impl.misc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.Event;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.aimpoint.PointFinder;
import gr.larryngton.demise.utils.render.ColorUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "Debug", description = "debug lolololol")
public class Debug extends Module {
    public final BoolValue clickHandler = new BoolValue("Click handler", false, this);
    public final BoolValue simulatedReactionTime = new BoolValue("Simulated reaction time", false, this);
    public final BoolValue attackRange = new BoolValue("Attack range", false, this);
    private final BoolValue targetVec = new BoolValue("Target vec", false, this);
    private final BoolValue bhv = new BoolValue("BHV", false, this);
    private final BoolValue hitboxPoints = new BoolValue("Hitbox points", false, this);
    private final BoolValue aimData = new BoolValue("Aim data", false, this);
    private final BoolValue clickData = new BoolValue("Click data", false, this);

    public double range;
    private Vec3 pos, lastPos;
    private Vec3 pos1, lastPos1;
    private Vec3 bestHitVec;
    private final List<float[]> dataList = new ArrayList<>();
    private float[] lastRot;
    private List<Integer> clickList = new ArrayList<>();
    private int index;

    @EventTarget
    public void onDisable() {
        dataList.clear();
        clickList.clear();
        pos = null;
        lastPos = null;
        pos1 = null;
        lastPos1 = null;
        bestHitVec = null;
        lastRot = null;
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (attackRange.get()) {
            range = PlayerUtils.getDistanceToEntityBox(e.getTargetEntity());
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (aimData.get()) {
            ScaledResolution sr = new ScaledResolution(mc);
            Fonts.interRegular.get(18).drawCenteredStringWithShadow(String.valueOf(dataList.size()), sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f + 20, -1);
        }
    }

    @EventTarget
    public void on67Mouse(TurkeyMouseEvent e) {
        if (clickData.get()) {
            if (clickList.isEmpty()) {
                for (int i = 0; i < 20; i++) {
                    clickList.add(0);
                }
            }

            for (int i = 0; i < mc.gameSettings.keyBindAttack.pressTime; i++) {
                clickList.set(index, clickList.get(index) + 1);
            }

            index++;

            if (index == 20) {
                StringBuilder jew = new StringBuilder();

                for (Integer i : clickList) {
                    jew.append(i).append(" ");
                }

                ChatUtils.sendMessageClient(" " + jew);
                clickList.clear();
                index = 0;
            }
        }
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (e.isPost()) {
            return;
        }

        EntityLivingBase target = PlayerUtils.getTarget(8);

        if (aimData.get()) {
            float[] curr = mc.thePlayer.getRotation();

            if (RotationUtils.getRotationDifference(lastRot, curr) > 1 && target != null) {
                dataList.add(new float[]{lastRot[0] - curr[0], lastRot[1] - curr[1]});
            }
        } else {
            if (!dataList.isEmpty()) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                File file = new File(Demise.INSTANCE.getMainDir(), "rotation_deltas.json");
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(dataList, writer);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                dataList.clear();
            }
        }

        lastRot = mc.thePlayer.getRotation();

        KillAura killAura = getModule(KillAura.class);
        if (killAura.isEnabled() && killAura.currentVec != null) {
            if (targetVec.get()) {
                double distance = killAura.currentVec.getDistanceAtEyeByVec(mc.thePlayer);
                float[] rot = RotationUtils.getRotations(killAura.currentVec);
                final Vec3 vec31 = mc.thePlayer.getVectorForRotation(rot[1], rot[0]);
                final Vec3 vec32 = mc.thePlayer.getPositionEyes(1).addVector(vec31.xCoord * distance, vec31.yCoord * distance, vec31.zCoord * distance);

                lastPos = pos;

                pos = vec32;
            }

            if (bhv.get() && bestHitVec != null) {
                double distance = bestHitVec.getDistanceAtEyeByVec(mc.thePlayer);
                float[] rot = RotationUtils.getRotations(bestHitVec);
                final Vec3 vec31 = mc.thePlayer.getVectorForRotation(rot[1], rot[0]);
                final Vec3 vec32 = mc.thePlayer.getPositionEyes(1).addVector(vec31.xCoord * distance, vec31.yCoord * distance, vec31.zCoord * distance);

                lastPos1 = pos1;

                pos1 = vec32;
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        KillAura killAura = getModule(KillAura.class);

        if (KillAura.target != null) {
            bestHitVec = RotationUtils.getBestHitVec(KillAura.target);
        }

        double x = mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * e.partialTicks();
        double y = mc.thePlayer.prevPosY + (mc.thePlayer.posY - mc.thePlayer.prevPosY) * e.partialTicks();
        double z = mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * e.partialTicks();
        double d = 0.01;
        double f = 0.001;

        if (hitboxPoints.get() && PointFinder.hitboxPoints != null && PointFinder.invalidHitboxPoints != null && PointFinder.allHitboxPoints != null) {
            Vec3 bestHitVec = PointFinder.hitboxPoints.stream()
                    .min(Comparator.comparingDouble(p -> p.distanceTo(mc.thePlayer.getPositionEyes(1))))
                    .orElse(KillAura.target.getHitbox().getCenter());

            Vec3 worstHitVec = PointFinder.hitboxPoints.stream()
                    .max(Comparator.comparingDouble(p -> p.distanceTo(mc.thePlayer.getPositionEyes(1))))
                    .orElse(KillAura.target.getHitbox().getCenter());

            for (Vec3 vec : PointFinder.invalidHitboxPoints) {
                AxisAlignedBB target = new AxisAlignedBB(vec.xCoord - f, vec.yCoord - f, vec.zCoord - f, vec.xCoord + f, vec.yCoord + f, vec.zCoord + f);
                AxisAlignedBB axis = new AxisAlignedBB(target.minX - x, target.minY - y, target.minZ - z, target.maxX - x, target.maxY - y, target.maxZ - z);

//                RenderUtils.renderBreadCrumb(vec, 0.03F, Color.RED);
                RenderUtils.drawAxisAlignedBB(axis, true, false, Color.RED.getRGB());
            }

            for (Vec3 vec : PointFinder.hitboxPoints) {
                AxisAlignedBB target = new AxisAlignedBB(vec.xCoord - d, vec.yCoord - d, vec.zCoord - d, vec.xCoord + d, vec.yCoord + d, vec.zCoord + d);
                AxisAlignedBB axis = new AxisAlignedBB(target.minX - x, target.minY - y, target.minZ - z, target.maxX - x, target.maxY - y, target.maxZ - z);

                float multi = calculateDistancePercentage(vec, bestHitVec, worstHitVec);
//                RenderUtils.renderBreadCrumb(vec, 0.03F, ColorUtils.interpolateColorC(Color.RED, Color.GREEN, multi));
                RenderUtils.drawAxisAlignedBB(axis, true, false, ColorUtils.interpolateColor2(Color.GREEN, Color.RED, multi));
            }
        }

        if (killAura.isEnabled() && KillAura.target != null) {
            d = 0.05;

            if (bhv.get() && bestHitVec != null) {
                Vec3 vec = MathUtils.interpolate(lastPos1, pos1, e.partialTicks());

                AxisAlignedBB target = new AxisAlignedBB(vec.xCoord - d, vec.yCoord - d, vec.zCoord - d, vec.xCoord + d, vec.yCoord + d, vec.zCoord + d);
                AxisAlignedBB axis = new AxisAlignedBB(target.minX - x, target.minY - y, target.minZ - z, target.maxX - x, target.maxY - y, target.maxZ - z);

                RenderUtils.drawAxisAlignedBB(axis, true, false, Color.blue.getRGB());
            }

            if (targetVec.get() && pos != null) {
                Vec3 vec = MathUtils.interpolate(lastPos, pos, e.partialTicks());

                AxisAlignedBB target = new AxisAlignedBB(vec.xCoord - d, vec.yCoord - d, vec.zCoord - d, vec.xCoord + d, vec.yCoord + d, vec.zCoord + d);
                AxisAlignedBB axis = new AxisAlignedBB(target.minX - x, target.minY - y, target.minZ - z, target.maxX - x, target.maxY - y, target.maxZ - z);

                RenderUtils.drawAxisAlignedBB(axis, true, false, Color.white.getRGB());
            }
        }
    }

    private float calculateDistancePercentage(Vec3 point, Vec3 bhv, Vec3 whv) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1);
        double pointDistance = point.distanceTo(eyes);
        double bestDistance = bhv.distanceTo(eyes);
        double worstDistance = whv.distanceTo(eyes);
        return (float) MathHelper.clamp_double((pointDistance - bestDistance) / (worstDistance - bestDistance), 0.0, 1.0);
    }

    public static class TurkeyMouseEvent implements Event {
    }
}