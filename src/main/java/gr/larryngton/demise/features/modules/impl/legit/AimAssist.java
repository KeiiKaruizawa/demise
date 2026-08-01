package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AngleEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.packet.PacketUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationManager;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.aimpoint.PointFinder;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.List;

// todo why the fuck did this randomly break
@ModuleInfo(name = "AimAssist", description = "Assists with aiming.")
public class AimAssist extends Module {
    private final SliderValue searchRange = new SliderValue("Search range", 4.5F, 1, 8, 0.1F, this);
    private final BoolValue onlyOnLMB = new BoolValue("Only on LMB", true, this);
    private final RotationManager rotationManager = new RotationManager(this);

    private EntityLivingBase target;

    @EventTarget
    public void onDisable() {
        target = null;
        PointFinder.allHitboxPoints.clear();
        PointFinder.hitboxPoints.clear();
        PointFinder.invalidHitboxPoints.clear();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        target = PlayerUtils.getTarget(searchRange.get(), "RayTrace");
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (target != null && (!onlyOnLMB.get() || GameSettings.isKeyDown(mc.gameSettings.keyBindAttack))) {
            rotationManager.setRotation(getRotations());
        }
    }

    private float[] getRotations() {
        Vec3 playerPos = mc.thePlayer.getPositionEyes(1);

        AxisAlignedBB boundingBox = target.getHitbox();
        PointFinder.findPoints(boundingBox);
        List<Vec3> hitboxPoints = PointFinder.hitboxPoints;

        hitboxPoints.removeIf(p -> {
            boolean invalid = !isValidPoint(p);
            if (invalid) PointFinder.invalidHitboxPoints.add(p);
            return invalid;
        });

        List<Vec3> allHitboxPoints = PointFinder.allHitboxPoints;
        Vec3 lookDir = mc.thePlayer.getVectorForRotation(mc.thePlayer.rotationPitch, mc.thePlayer.rotationYaw).normalize();
        Vec3 vec;

        if (!hitboxPoints.isEmpty()) {
            hitboxPoints.sort((a, b) -> {
                double da = a.subtract(playerPos).crossProduct(lookDir).lengthVector();
                double db = b.subtract(playerPos).crossProduct(lookDir).lengthVector();
                return Double.compare(da, db);
            });

            if (!hitboxPoints.isEmpty()) {
                vec = hitboxPoints.get(0);
            } else {
                vec = getBackupVec(allHitboxPoints, playerPos, lookDir);
            }
        } else {
            vec = getBackupVec(allHitboxPoints, playerPos, lookDir);
        }

        Vec3 targetVec = new Vec3(vec.xCoord, vec.yCoord, vec.zCoord);
        targetVec.xCoord = MathHelper.clamp_double(targetVec.xCoord, boundingBox.minX, boundingBox.maxX);
        targetVec.yCoord = MathHelper.clamp_double(targetVec.yCoord, boundingBox.minY, boundingBox.maxY);
        targetVec.zCoord = MathHelper.clamp_double(targetVec.zCoord, boundingBox.minZ, boundingBox.maxZ);

        return RotationUtils.getRotations(targetVec, playerPos);
    }

    private Vec3 getBackupVec(List<Vec3> allHitboxPoints, Vec3 playerPos, Vec3 lookDir) {
        allHitboxPoints.sort((a, b) -> {
            double da = a.subtract(playerPos).crossProduct(lookDir).lengthVector();
            double db = b.subtract(playerPos).crossProduct(lookDir).lengthVector();
            return Double.compare(da, db);
        });

        return allHitboxPoints.get(0);
    }

    private boolean isValidPoint(Vec3 point) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1);
        MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(eyePos, point, false, false, false);

        return hit == null;
    }
}
