package gr.larryngton.demise.utils.player;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.features.modules.impl.movement.TargetStrafe;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import lombok.experimental.UtilityClass;
import net.minecraft.block.BlockAir;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;

import java.util.Arrays;

@UtilityClass
public class MoveUtils implements InstanceAccess {
    public static final double WALK_SPEED = 0.221;
    public static final double BUNNY_SLOPE = 0.66;
    public static final double MOD_SPRINTING = 1.3F;
    public static final double MOD_SNEAK = 0.3F;
    public static final double MOD_ICE = 2.5F;
    public static final double MOD_WEB = 0.105 / WALK_SPEED;
    public static final double JUMP_HEIGHT = 0.42F;
    public static final double BUNNY_FRICTION = 159.9F;
    public static final double Y_ON_GROUND_MIN = 0.00001;
    public static final double Y_ON_GROUND_MAX = 0.0626;
    public static final double LILYPAD_HEIGHT = 0.015625F;
    public static final double AIR_FRICTION = 0.9800000190734863D;
    public static final double WATER_FRICTION = 0.800000011920929D;
    public static final double LAVA_FRICTION = 0.5D;
    public static final double MOD_SWIM = 0.115F / WALK_SPEED;
    public static final double[] MOD_DEPTH_STRIDER = {
            1.0F,
            0.1645F / MOD_SWIM / WALK_SPEED,
            0.1995F / MOD_SWIM / WALK_SPEED,
            1.0F / MOD_SWIM,
    };

    public static final double UNLOADED_CHUNK_MOTION = -0.09800000190735147;
    public static final double HEAD_HITTER_MOTION = -0.0784000015258789;
    private float yaw;

    public boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.moveForward != 0F || mc.thePlayer.moveStrafing != 0F);
    }

    public boolean isMovingMotion(EntityLivingBase player) {
        return player != null && getSpeedPosBased(player) != 0;
    }

    public double getSpeed(EntityLivingBase player) {
        return Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
    }

    public double getSpeedPosBased(EntityLivingBase player) {
        double motionX = player.posX - player.prevPosX;
        double motionZ = player.posZ - player.prevPosZ;

        return Math.sqrt(motionX * motionX + motionZ * motionZ);
    }

    public double getSpeed() {
        return getSpeed(mc.thePlayer);
    }

    public void strafe() {
        strafe(getSpeed());
    }

    public void strafe(double speed) {
        strafe(speed, 1);
    }

    public void strafe(double speed, double strength) {
        strafe(getDirection(), speed, strength);
    }

    public void strafe(double yaw, double speed, double strength) {
        double motionX = mc.thePlayer.motionX;
        double motionZ = mc.thePlayer.motionZ;

        if (!isMoving()) return;

        mc.thePlayer.motionX = -Math.sin(yaw) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;

        mc.thePlayer.motionX = motionX + (mc.thePlayer.motionX - motionX) * strength;
        mc.thePlayer.motionZ = motionZ + (mc.thePlayer.motionZ - motionZ) * strength;
    }

    public void bop(double s) {
        double forward = mc.thePlayer.movementInput.moveForward;
        double strafe = mc.thePlayer.movementInput.moveStrafe;
        float yaw = mc.thePlayer.rotationYaw;
        if (forward == 0.0D && strafe == 0.0D) {
            mc.thePlayer.motionX = 0.0D;
            mc.thePlayer.motionZ = 0.0D;
        } else {
            if (forward != 0.0D) {
                if (strafe > 0.0D) {
                    yaw += (float) (forward > 0.0D ? -45 : 45);
                } else if (strafe < 0.0D) {
                    yaw += (float) (forward > 0.0D ? 45 : -45);
                }

                strafe = 0.0D;
                if (forward > 0.0D) {
                    forward = 1.0D;
                } else if (forward < 0.0D) {
                    forward = -1.0D;
                }
            }

            double rad = Math.toRadians(yaw + 90.0F);
            double sin = Math.sin(rad);
            double cos = Math.cos(rad);
            mc.thePlayer.motionX = forward * s * cos + strafe * s * sin;
            mc.thePlayer.motionZ = forward * s * sin - strafe * s * cos;
        }
    }

    public int getSpeedEffect(EntityLivingBase player) {
        return player.isPotionActive(Potion.moveSpeed) ? player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1 : 0;
    }

    public int getSpeedEffect() {
        return getSpeedEffect(mc.thePlayer);
    }

    public void stopXZ() {
        mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
    }

    public void stop() {
        mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
    }

    public double getBPS() {
        return getBPS(mc.thePlayer);
    }

    public double getBPS(EntityPlayer player) {
        if (player == null || player.ticksExisted < 1) {
            return 0.0;
        }
        return getDistance(player.lastTickPosX, player.lastTickPosZ) * (20.0f * mc.timer.timerSpeed);
    }

    public double getDistance(double x, double z) {
        double xSpeed = mc.thePlayer.posX - x;
        double zSpeed = mc.thePlayer.posZ - z;
        return MathHelper.sqrt_double(xSpeed * xSpeed + zSpeed * zSpeed);
    }

    public boolean isMovingStraight() {
        float direction = getYawFromKeybind() + 180;
        float movingYaw = Math.round(direction / 45) * 45;
        return movingYaw % 90 == 0f;
    }

    public double getBaseMoveSpeed(EntityPlayer player) {
        double baseSpeed = 0.2873;

        if (player.isPotionActive(Potion.moveSpeed)) {
            int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }

        if (player.isPotionActive(Potion.moveSlowdown)) {
            int amplifier = player.getActivePotionEffect(Potion.moveSlowdown).getAmplifier();
            baseSpeed *= 1.0 - 0.15 * (amplifier + 1);
        }

        return baseSpeed;
    }

    public double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public void fixMovement(MoveInputEvent e, float yaw, float camYaw) {
        fixMovement(e, yaw, camYaw, false);
    }

    public void fixMovement(MoveInputEvent e, float yaw, float camYaw, boolean noS) {
        // no need to fix shit
        if (e.getForward() == e.getStrafe() && e.getForward() == 0) {
            return;
        }

        // player simulation kinda breaks here soo
        if (mc.thePlayer.isOnLadder() || mc.thePlayer.isCollidedHorizontally) {
            fixMovementLegacy(e, yaw, camYaw);
            return;
        }

        int accuracy = 1;

        float forward = e.getForward();
        float strafe = e.getStrafe();

        MovementInput movementInput = new MovementInput(forward, strafe, e.isJumping(), e.isSneaking());
        SimulatedPlayer simulatedSelf = SimulatedPlayer.fromClientPlayer(movementInput);

        simulatedSelf.rotationYaw = camYaw;
        for (int i = 0; i < accuracy; i++) {
            simulatedSelf.tick();
        }

        Vec3 desiredPos = simulatedSelf.getPos();

        float bestForward = 0;
        float bestStrafe = 0;
        double bestDistance = Double.MAX_VALUE;

        for (float testForward = -1F; testForward <= 1F; testForward += 1F) {
            for (float testStrafe = -1F; testStrafe <= 1F; testStrafe += 1F) {
                if (noS && testForward == -1F && testStrafe == 0) {
                    continue;
                }

                MovementInput movementInput1 = new MovementInput(testForward, testStrafe, e.isJumping(), e.isSneaking());
                SimulatedPlayer simulatedSelf1 = SimulatedPlayer.fromClientPlayer(movementInput1);

                simulatedSelf1.rotationYaw = yaw;
                for (int i = 0; i < accuracy; i++) {
                    simulatedSelf1.tick();
                }

                // prevents you from falling on bridges
                if (simulatedSelf.posY != simulatedSelf1.posY) {
                    continue;
                }

                double dist = desiredPos.squareDistanceTo(simulatedSelf1.getPos());

                if (dist < bestDistance) {
                    bestForward = testForward;
                    bestStrafe = testStrafe;
                    bestDistance = dist;
                }
            }
        }

        // something went wrong, use the legacy moveFix as a backup
        if (bestForward == bestStrafe && bestForward == 0) {
            fixMovementLegacy(e, yaw, camYaw);
            return;
        }

        e.setForward(bestForward);
        e.setStrafe(bestStrafe);
    }

    private void fixMovementLegacy(MoveInputEvent event, float yaw, float camYaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();

        if (forward == 0 && strafe == 0) return;

        double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(camYaw, forward, strafe)));

        float closestForward = 0, closestStrafe = 0;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedForward == 0 && predictedStrafe == 0) continue;

                double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(yaw, predictedForward, predictedStrafe)));
                double difference = Math.abs(MathHelper.wrapAngleTo180_double(angle - predictedAngle));

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }

    public void moveFlying(double increase) {
        if (!MoveUtils.isMoving()) return;
        double yaw = getDirection();
        mc.thePlayer.motionX += -MathHelper.sin((float) yaw) * increase;
        mc.thePlayer.motionZ += MathHelper.cos((float) yaw) * increase;
    }

    public void useDiagonalSpeed() {
        KeyBinding[] gameSettings = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft};

        int[] down = {0};

        Arrays.stream(gameSettings).forEach(keyBinding -> down[0] = down[0] + (keyBinding.isKeyDown() ? 1 : 0));

        boolean active = down[0] == 1;

        if (!active) return;

        double groundIncrease = (0.1299999676734952 - 0.12739998266255503) + 1E-7 - 1E-8;
        double airIncrease = (0.025999999334873708 - 0.025479999685988748) - 1E-8;
        double increase = mc.thePlayer.onGround ? groundIncrease : airIncrease;

        moveFlying(increase);
    }

    public float getRawDirectionFromKeybind() {
        float moveForward = 0;
        float moveStrafe = 0;

        if (mc.gameSettings.keyBindForward.isKeyDown()) {
            moveForward++;
        }

        if (mc.gameSettings.keyBindBack.isKeyDown()) {
            moveForward--;
        }

        if (mc.gameSettings.keyBindLeft.isKeyDown()) {
            moveStrafe++;
        }

        if (mc.gameSettings.keyBindRight.isKeyDown()) {
            --moveStrafe;
        }

        if (moveForward == 1 && moveStrafe == 1) {
            yaw = 45F;
        } else if (moveForward == 1 && moveStrafe == -1) {
            yaw = -45F;
        } else if (moveForward == -1 && moveStrafe == 1) {
            yaw = 135F;
        } else if (moveForward == -1 && moveStrafe == -1) {
            yaw = -135F;
        } else if (moveForward == -1) {
            yaw = 180F;
        } else if (moveStrafe == 1) {
            yaw = 90F;
        } else if (moveStrafe == -1) {
            yaw = -90F;
        } else if (moveForward == 1) {
            yaw = 0F;
        }

        return yaw;
    }

    public float getYawFromKeybind() {
        return mc.thePlayer.rotationYaw - getRawDirectionFromKeybind();
    }

    public float getDirection() {
        float dir;

        TargetStrafe targetStrafe = Demise.INSTANCE.getModuleManager().getModule(TargetStrafe.class);

        if (targetStrafe.isEnabled() && targetStrafe.active && targetStrafe.target != null) {
            dir = targetStrafe.yaw;
        } else {
            dir = getYawFromKeybind();
        }

        return (float) Math.toRadians(dir);
    }

    public double getDirection(float moveForward, float moveStrafing, float rotationYaw) {
        if (moveForward < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (moveForward < 0) {
            forward = -0.5F;
        } else if (moveForward > 0) {
            forward = 0.5F;
        }

        if (moveStrafing > 0) {
            rotationYaw -= 70 * forward;
        }

        if (moveStrafing < 0) {
            rotationYaw += 70 * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public void holdS(MoveInputEvent e) {
        float forward = e.getForward();
        float strafe = e.getStrafe();

        double angle = MathHelper.wrapAngleTo180_double(RotationHandler.currentRotation[0] - 180);
        if (forward == 0 && strafe == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(MoveUtils.getDirection(predictedForward, predictedStrafe, mc.thePlayer.rotationYaw)));
                double difference = MathUtils.wrappedDifference(angle, predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        e.setForward(closestForward);
        e.setStrafe(closestStrafe);
    }

    public boolean getOnGroundPosBased(AxisAlignedBB initialBoundingBox, double xzShrink) {
        AxisAlignedBB p = initialBoundingBox.contract(xzShrink, 0, xzShrink);
        AxisAlignedBB hitbox = new AxisAlignedBB(p.minX, p.minY - 0.1, p.minZ, p.maxX, p.minY, p.maxZ);
        return mc.theWorld.checkBlockCollision(hitbox);
    }

    public boolean getOnGroundPosBased(EntityPlayer player) {
        AxisAlignedBB p = player.getHitbox();
        AxisAlignedBB hitbox = new AxisAlignedBB(p.minX, p.minY - 0.1, p.minZ, p.maxX, p.minY, p.maxZ);
        return mc.theWorld.checkBlockCollision(hitbox);
    }

    public double getDirectionPosBased(EntityLivingBase player) {
        double dx = player.posX - player.prevPosX;
        double dz = player.posZ - player.prevPosZ;

        if (dx == 0 && dz == 0) {
            return MathHelper.wrapAngleTo180_double(player.rotationYawHead);
        }

        double length = Math.sqrt(dx * dx + dz * dz);
        dx /= length;
        dz /= length;

        double angle = -Math.toDegrees(Math.atan2(dz, dx));
        angle += 90;

        return MathHelper.wrapAngleTo180_double(-angle);
    }

    public double findGround(EntityPlayer player) {
        return findGroundB(player).getY();
    }

    public BlockPos findGroundB(EntityPlayer player) {
        for (int i = 0; i < 50; i++) {
            BlockPos pos = new BlockPos(player.getPositionVector().floor().subtract(0, i, 0));
            if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockAir)) {
                return pos;
            }
        }

        return BlockPos.ORIGIN;
    }

    public int depthStriderLevel() {
        return EnchantmentHelper.getDepthStriderModifier(mc.thePlayer);
    }

    public boolean enoughMovementForSprinting() {
        return Math.abs(mc.thePlayer.moveForward) >= 0.8F || Math.abs(mc.thePlayer.moveStrafing) >= 0.8F;
    }

    public double getAllowedHDist() {
        double horizontalDistance;
        boolean useBaseModifiers = false;

        if (mc.thePlayer.isInWeb) {
            horizontalDistance = MOD_WEB * WALK_SPEED;
        } else if (PlayerUtils.inLiquid()) {
            horizontalDistance = MOD_SWIM * WALK_SPEED;

            final int depthStriderLevel = depthStriderLevel();
            if (depthStriderLevel > 0) {
                horizontalDistance *= MOD_DEPTH_STRIDER[depthStriderLevel];
                useBaseModifiers = true;
            }

        } else if (mc.thePlayer.isSneaking()) {
            horizontalDistance = MOD_SNEAK * WALK_SPEED;
        } else {
            horizontalDistance = WALK_SPEED;
            useBaseModifiers = true;
        }

        if (useBaseModifiers) {
            if (enoughMovementForSprinting()) {
                horizontalDistance *= MOD_SPRINTING;
            }

            if (mc.thePlayer.isPotionActive(Potion.moveSpeed) && mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getDuration() > 0) {
                horizontalDistance *= 1 + (0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1));
            }

            if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
                horizontalDistance = 0.29;
            }
        }

        return horizontalDistance;
    }

    public Vec3 getPredictedHurtPos(EntityLivingBase entity, EntityLivingBase attacker, double ticks) {
        double motionX = entity.posX - entity.prevPosX;
        double motionY = entity.posY - entity.prevPosY;
        double motionZ = entity.posZ - entity.prevPosZ;

        int i = 0;
        i = i + EnchantmentHelper.getKnockbackModifier(attacker);

        if (attacker.isSprinting()) {
            ++i;
        }

        motionX += -MathHelper.sin(attacker.rotationYawHead * (float) Math.PI / 180.0F) * (float) i * 0.5F;
        motionY += 0.1D;
        motionZ += MathHelper.cos(attacker.rotationYawHead * (float) Math.PI / 180.0F) * (float) i * 0.5F;

        return entity.getPositionEyes(1).add(motionX * ticks, motionY * ticks, motionZ * ticks);
    }

    public int getTicksForGround(EntityLivingBase player) {
        int ticks = 12;
        double posY = player.posY;
        double lastMotionY = player.getMoveDeltaVector().yCoord;

        for (int i = 0; i < 12; i++) {
            double motionY = (lastMotionY - 0.08) * 0.98;
            posY += motionY;

            if (MoveUtils.getOnGroundPosBased(player.getHitbox().offset(0, posY - player.posY, 0), 0)) {
                ticks = i;
                break;
            }
            lastMotionY = motionY;
        }

        return ticks;
    }
}