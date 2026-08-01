package gr.larryngton.demise.utils.player.handlers.rotation;

import gr.larryngton.demise.utils.InstanceAccess;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;

@UtilityClass
public class RotationUtils implements InstanceAccess {
    public float getAngleDifference(float a, float b) {
        return MathHelper.wrapAngleTo180_float(a - b);
    }

    public float getRotationDifference(final Entity entity) {
        float[] target = RotationUtils.getRotations(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        return (float) Math.hypot(Math.abs(getAngleDifference(target[0], RotationHandler.shouldRotate() ? RotationHandler.currentRotation[0] : mc.thePlayer.rotationYaw)), Math.abs(target[1] - (RotationHandler.shouldRotate() ? RotationHandler.currentRotation[1] : mc.thePlayer.rotationYaw)));
    }

    public float getRotationDifferenceClientRot(final Entity entity) {
        float[] target = RotationUtils.getRotations(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        return (float) Math.hypot(Math.abs(getAngleDifference(target[0], mc.thePlayer.rotationYaw)), Math.abs(target[1] - mc.thePlayer.rotationYaw));
    }

    public float getRotationDifference(final float[] a, final float[] b) {
        return (float) Math.hypot(Math.abs(getAngleDifference(a[0], b[0])), Math.abs(a[1] - b[1]));
    }

    public float[] getRotations(BlockPos blockPos, EnumFacing enumFacing) {
        return getRotations(blockPos, enumFacing, 0.25, 0.25);
    }

    public float[] getRotations(BlockPos blockPos) {
        return getRotations(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
    }

    public float[] getRotations(BlockPos blockPos, EnumFacing enumFacing, double xz, double y) {
        double d = blockPos.getX() + 0.5 - mc.thePlayer.posX + enumFacing.getFrontOffsetX() * xz;
        double d2 = blockPos.getZ() + 0.5 - mc.thePlayer.posZ + enumFacing.getFrontOffsetZ() * xz;
        double d3 = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - blockPos.getY() - enumFacing.getFrontOffsetY() * y;
        double d4 = MathHelper.sqrt_double(d * d + d2 * d2);
        float f = (float) (Math.atan2(d2, d) * 180.0 / Math.PI) - 90.0f;
        float f2 = (float) (Math.atan2(d3, d4) * 180.0 / Math.PI);
        return new float[]{MathHelper.wrapAngleTo180_float(f), f2};
    }

    public float[] getRotations(double rotX, double rotY, double rotZ, double startX, double startY, double startZ) {
        double x = rotX - startX;
        double y = rotY - startY;
        double z = rotZ - startZ;
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, dist) * 180.0 / Math.PI));
        return new float[]{yaw, pitch};
    }

    public float[] getRotations(Vec3 vec, Vec3 playerPos) {
        return getRotations(vec.xCoord, vec.yCoord, vec.zCoord, playerPos.xCoord, playerPos.yCoord, playerPos.zCoord);
    }

    public float[] getRotations(double posX, double posY, double posZ) {
        return getRotations(posX, posY, posZ, mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
    }

    public float[] getRotations(Vec3 vec) {
        return getRotations(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    public Vec3 getBestHitVec(final Entity entity) {
        final Vec3 positionEyes = mc.thePlayer.getPositionEyes(1);
        final AxisAlignedBB entityBoundingBox = entity.getHitbox();
        final double ex = MathHelper.clamp_double(positionEyes.xCoord, entityBoundingBox.minX, entityBoundingBox.maxX);
        final double ey = MathHelper.clamp_double(positionEyes.yCoord, entityBoundingBox.minY, entityBoundingBox.maxY);
        final double ez = MathHelper.clamp_double(positionEyes.zCoord, entityBoundingBox.minZ, entityBoundingBox.maxZ);
        return new Vec3(ex, ey, ez);
    }

    public Vec3 getBestHitVecCustom(AxisAlignedBB bb) {
        final Vec3 positionEyes = mc.thePlayer.getPositionEyes(1);
        final double ex = MathHelper.clamp_double(positionEyes.xCoord, bb.minX, bb.maxX);
        final double ey = MathHelper.clamp_double(positionEyes.yCoord, bb.minY, bb.maxY);
        final double ez = MathHelper.clamp_double(positionEyes.zCoord, bb.minZ, bb.maxZ);
        return new Vec3(ex, ey, ez);
    }

    public float[] faceTrajectory(Entity target, float predictSize, float gravity, float velocity) {
        double posX = target.posX + (target.posX - target.prevPosX) * predictSize - (mc.thePlayer.posX + (mc.thePlayer.posX - mc.thePlayer.prevPosX));
        double posY = target.getEntityBoundingBox().minY + (target.getEntityBoundingBox().minY - target.prevPosY) * predictSize + target.getEyeHeight() - 0.15 - (mc.thePlayer.getEntityBoundingBox().minY + (mc.thePlayer.posY - mc.thePlayer.prevPosY)) - mc.thePlayer.getEyeHeight();
        double posZ = target.posZ + (target.posZ - target.prevPosZ) * predictSize - (mc.thePlayer.posZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ));
        double posSqrt = Math.sqrt(posX * posX + posZ * posZ);

        velocity = Math.min((velocity * velocity + velocity * 2) / 3, 1f);

        float gravityModifier = 0.12f * gravity;

        return new float[]{
                (float) Math.toDegrees(Math.atan2(posZ, posX)) - 90f,
                (float) -Math.toDegrees(Math.atan((velocity * velocity - Math.sqrt(
                        velocity * velocity * velocity * velocity - gravityModifier * (gravityModifier * posSqrt * posSqrt + 2 * posY * velocity * velocity)
                )) / (gravityModifier * posSqrt)))
        };
    }

    public Vec3 getVec3(BlockPos pos, EnumFacing face) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        x += face.getFrontOffsetX() / 2.0D;
        z += face.getFrontOffsetZ() / 2.0D;
        y += face.getFrontOffsetY() / 2.0D;

        return new Vec3(x, y, z);
    }

    public float[] getSimpleRotationsToEntity(Entity entity) {
        Vec3 currentVec;
        AxisAlignedBB bb = entity.getHitbox();

        Vec3 boxCenter = bb.getCenter();
        Vec3 entityPos = new Vec3(boxCenter.xCoord, bb.minY, boxCenter.zCoord);

        currentVec = entityPos.add(0.0, entity.getEyeHeight(), 0.0);
        return getRotations(currentVec);
    }

    public float getFovToTarget(EntityLivingBase target) {
        double x = target.posX - mc.thePlayer.posX;
        double z = target.posZ - mc.thePlayer.posZ;
        float calcYaw = (float) (MathHelper.atan2(z, x) * 180.0 / Math.PI - 90.0);
        return Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - mc.thePlayer.rotationYaw));
    }
}