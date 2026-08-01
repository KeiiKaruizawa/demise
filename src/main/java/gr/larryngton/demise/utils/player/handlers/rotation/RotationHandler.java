package gr.larryngton.demise.utils.player.handlers.rotation;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.*;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.enums.AngleLimiterMode;
import gr.larryngton.demise.utils.player.handlers.rotation.enums.MovementCorrectionMode;
import gr.larryngton.demise.utils.player.handlers.rotation.enums.AverageLimitMode;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.MathHelper;

import java.util.*;

public class RotationHandler implements InstanceAccess {
    public static float[] currentRotation = new float[]{0, 0};
    public static float[] previousRotation;
    private static boolean enabled;
    public static float[] targetRotation;
    public static boolean silent;
    private static float hSpeed;
    private static float vSpeed;
    public static MovementCorrectionMode correction;
    public static boolean reset;
    private long lastFrameTime = System.nanoTime();
    private static double previousDeltaYaw = 0;
    private static double previousDeltaPitch = 0;
    private double yawAccum = 0;
    private double pitchAccum = 0;
    public static float directionYaw;
    private static double tickScale;
    private final TimerUtil updateTimer = new TimerUtil();
    private static double accelerationYaw;
    private static double accelerationPitch;
    public static float yawDiff = 0;
    public static float pitchDiff = 0;
    private final List<Float> yawDiffList = new ArrayList<>();
    private final List<Float> pitchDiffList = new ArrayList<>();
    private static float avgYawDiff = 0;
    private static float avgPitchDiff = 0;
    private float prevYaw;
    private float prevPitch;
    private static int maxDeltaHistorySize;
    private double lastAccelDeltaYaw;
    private double lastAccelDeltaPitch;
    private static AngleLimiterMode angleLimiter = AngleLimiterMode.None;
    private static boolean isAimAssist;
    private static boolean noS;
    private static boolean shouldLimit;
    private static AverageLimitMode averageLimitMode;
    private static float maxAverageYawDelta;
    private static float minYawMultiplierOnLimit;
    private static float maxYawMultiplierOnLimit;
    private final List<Double> speedList = new ArrayList<>();
    private static float stamina = 0.8F;
    private float lastYawDiff;

    public RotationHandler() {
        enabled = true;
        correction = MovementCorrectionMode.Silent;
        hSpeed = 180;
        vSpeed = 180;
        targetRotation = currentRotation = new float[]{0, 0};
        reset = true;
        silent = true;
        averageLimitMode = AverageLimitMode.None;
    }

    public static void setBasicRotation(float[] rotation, boolean correction, float hSpeed, float vSpeed) {
        setRotation(rotation,
                correction ? MovementCorrectionMode.Silent : MovementCorrectionMode.None,
                new float[]{hSpeed, vSpeed},
                hSpeed == vSpeed && hSpeed == 180 ? AngleLimiterMode.None : AngleLimiterMode.Linear,
                0,
                true,
                false,
                false,
                AverageLimitMode.None,
                0,
                0,
                0
        );
    }

    public static void setRotation(float[] rotation,
                                   MovementCorrectionMode correction,
                                   float[] speed,
                                   AngleLimiterMode angleLimiter,
                                   int maxDeltaHistorySize,
                                   boolean silent,
                                   boolean isAimAssist,
                                   boolean noS,
                                   AverageLimitMode averageLimitMode,
                                   float maxAverageYawDelta,
                                   float minYawMultiplierOnLimit,
                                   float maxYawMultiplierOnLimit
    ) {
        RotationHandler.targetRotation = rotation;
        RotationHandler.silent = silent;
        RotationHandler.hSpeed = speed[0];
        RotationHandler.vSpeed = speed[1];
        RotationHandler.correction = correction;
        RotationHandler.directionYaw = mc.thePlayer.rotationYaw;
        RotationHandler.maxDeltaHistorySize = maxDeltaHistorySize;
        RotationHandler.angleLimiter = angleLimiter;
        RotationHandler.isAimAssist = isAimAssist;
        RotationHandler.noS = noS;
        RotationHandler.averageLimitMode = averageLimitMode;
        RotationHandler.maxAverageYawDelta = maxAverageYawDelta;
        RotationHandler.minYawMultiplierOnLimit = minYawMultiplierOnLimit;
        RotationHandler.maxYawMultiplierOnLimit = maxYawMultiplierOnLimit;
        enabled = true;
    }

    @EventTarget
    private void onMove(MoveInputEvent e) {
        if (shouldRotate()) {
            if (correction == MovementCorrectionMode.Silent) {
                MoveUtils.fixMovement(e, currentRotation[0], enabled ? directionYaw : mc.thePlayer.rotationYaw, noS);
            }

            RotationHandler.directionYaw = mc.thePlayer.rotationYaw;
        }
    }

    @EventTarget
    private void onStrafe(StrafeEvent e) {
        if (correction != MovementCorrectionMode.None && shouldRotate()) {
            e.setYaw(currentRotation[0]);
        }
    }

    @EventTarget
    private void onJump(JumpEvent event) {
        if (correction != MovementCorrectionMode.None && shouldRotate()) {
            event.setYaw(currentRotation[0]);
        }
    }

    @EventTarget
    public void onLook(LookEvent e) {
        if (shouldRotate()) {
            e.rotation = currentRotation;
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        currentRotation = targetRotation = mc.thePlayer.getRotation();
        reset = true;
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof S08PacketPlayerPosLook s08) {
            float yaw = s08.getYaw();
            float pitch = s08.getPitch();

            if (s08.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y_ROT)) {
                yaw += currentRotation[0];
            }

            if (s08.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X_ROT)) {
                pitch += currentRotation[1];
            }

            currentRotation[0] = yaw;
            currentRotation[1] = pitch;
            prevYaw = currentRotation[0];
            prevPitch = currentRotation[1];
            previousDeltaYaw = previousDeltaPitch = avgYawDiff = avgPitchDiff = 0;
        }
    }

    @EventTarget
    public void onMouseMove(MouseMoveEvent e) {
        if (currentRotation == null) {
            currentRotation = mc.thePlayer.getRotation();
        }

        if (e.getState() == MouseMoveEvent.State.PRE) {
            if (lastFrameTime == 0L) {
                lastFrameTime = System.nanoTime();
            }

            long currentTime = System.nanoTime();
            double deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0;
            lastFrameTime = currentTime;
            deltaTime = Math.min(deltaTime, 1.0 / 40.0);
            tickScale = deltaTime / (1.0 / 60.0);
            return;
        }

        if (e.getState() == MouseMoveEvent.State.UPDATE) {
            if (enabled) {
                handleRotation(e, targetRotation);
            } else {
                if (RotationUtils.getRotationDifference(currentRotation, mc.thePlayer.getRotation()) < 0.1 || reset) {
                    if (!reset) {
                        float lastYaw = mc.thePlayer.rotationYaw;
                        mc.thePlayer.rotationYaw = currentRotation[0] + MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - currentRotation[0]);

                        float correctedDelta = mc.thePlayer.rotationYaw - lastYaw;
                        mc.thePlayer.prevRotationYaw += correctedDelta;
                        mc.thePlayer.renderArmYaw += correctedDelta;
                        mc.thePlayer.prevRenderArmYaw += correctedDelta;
                    }

                    currentRotation = mc.thePlayer.getRotation();
                    targetRotation = null;
                    reset = true;
                    previousDeltaYaw = previousDeltaPitch = avgYawDiff = avgPitchDiff = yawDiff = pitchDiff = 0;
                    lastAccelDeltaYaw = lastAccelDeltaPitch = 0;
                    prevYaw = currentRotation[0];
                    prevPitch = currentRotation[1];
                } else {
                    handleRotation(e, mc.thePlayer.getRotation());
                }
            }

            enabled = false;
        }

        if (e.getState() == MouseMoveEvent.State.AFTER_ANGLE && isAimAssist) {
            currentRotation = mc.thePlayer.getRotation();
        }
    }

    public static boolean shouldRotate() {
        return currentRotation != null && targetRotation != null;
    }

    private void handleRotation(MouseMoveEvent e, float[] target) {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 8.0F;

        double[] delta = limitRotations(target);

        if (!averageLimitMode.equals(AverageLimitMode.None)) {
            delta[0] = reduceYaw(delta[0]);
        }

        double f11 = f1 * 0.15;
        if (Math.abs(delta[0]) <= f11 && Math.abs(avgYawDiff) - f11 < 1e-2) {
            delta[0] = 0;
        }

        if (Math.abs(delta[1]) <= f11 && Math.abs(avgPitchDiff) - f11 < 1e-2) {
            delta[1] = 0;
        }

        float yawStep, pitchStep;

        if (!angleLimiter.equals(AngleLimiterMode.None)) {
            delta[0] = delta[0] * tickScale / f1 / 0.15D;
            delta[1] = delta[1] * tickScale / f1 / 0.15D;

            if (!Double.isFinite(delta[0])) delta[0] = 0;
            if (!Double.isFinite(delta[1])) delta[1] = 0;

            // store accumulators to maintain precision
            // without this, rotations will seem interpolated on high frame rate
            yawAccum += delta[0];
            pitchAccum += delta[1];

            int intYaw = (int) yawAccum;
            int intPitch = (int) pitchAccum;

            yawAccum -= intYaw;
            pitchAccum -= intPitch;

            if (!isAimAssist) {
                yawStep = intYaw * f1;
                pitchStep = intPitch * f1;
            } else {
                yawStep = intYaw;
                pitchStep = intPitch;
            }
        } else {
            delta[0] = delta[0] / f1 / 0.15D;
            delta[1] = delta[1] / f1 / 0.15D;

            if (!Double.isFinite(delta[0])) delta[0] = 0;
            if (!Double.isFinite(delta[1])) delta[1] = 0;

            if (!isAimAssist) {
                yawStep = ((int) delta[0]) * f1;
                pitchStep = ((int) delta[1]) * f1;
            } else {
                yawStep = (int) delta[0];
                pitchStep = (int) delta[1];
            }
        }

        if (isAimAssist) {
            e.setDeltaX(e.getDeltaX() + (int) yawStep);
            e.setDeltaY(e.getDeltaY() + (int) pitchStep);
        } else {
            if (!silent) {
                e.setDeltaX(0);
                e.setDeltaY(0);

                mc.thePlayer.rotationYaw = (float) ((double) mc.thePlayer.rotationYaw + (double) yawStep * 0.15D);
                mc.thePlayer.rotationPitch = (float) ((double) mc.thePlayer.rotationPitch - (double) pitchStep * 0.15D);
                mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90f, 90f);
                currentRotation = mc.thePlayer.getRotation();
            } else {
                currentRotation[0] = (float) ((double) currentRotation[0] + (double) yawStep * 0.15D);
                currentRotation[1] = (float) ((double) currentRotation[1] - (double) pitchStep * 0.15D);
                currentRotation[1] = MathHelper.clamp_float(currentRotation[1], -90f, 90f);
            }
        }

        reset = false;
    }

    private static double reduceYaw(double deltaYaw) {
        if (shouldLimit && maxDeltaHistorySize > 0) {
            switch (averageLimitMode) {
                case NCP:
                    deltaYaw = 0;
                    break;
                case Custom:
                    float percentageYaw = 1 - MathHelper.clamp_float((Math.abs(avgYawDiff) - maxAverageYawDelta) / maxAverageYawDelta, 0, 1);
                    float reduce = MathUtils.interpolateNoUpdateCheck(minYawMultiplierOnLimit, maxYawMultiplierOnLimit, percentageYaw);
                    deltaYaw *= reduce;
                    break;
            }
        }

        return deltaYaw;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (shouldRotate()) {
            yawDiff = prevYaw - currentRotation[0];
            pitchDiff = prevPitch - currentRotation[1];

            if (maxDeltaHistorySize == 0) {
                avgYawDiff = 0;
                avgPitchDiff = 0;
            } else {
                yawDiffList.add(yawDiff);
                pitchDiffList.add(pitchDiff);
                speedList.add(MoveUtils.getSpeed());

                int deltaSwitchTarget = 0;
                float yawDiffListSum = 0;
                for (float diffYaw : yawDiffList) {
                    yawDiffListSum += diffYaw;

                    if (Math.abs(diffYaw) > 30) {
                        deltaSwitchTarget++;
                    }
                }
                avgYawDiff = yawDiffListSum / yawDiffList.size();
                double averageSwitching = (double) deltaSwitchTarget / yawDiffList.size();

                float pitchDiffListSum = 0;
                for (float diffPitch : pitchDiffList) {
                    pitchDiffListSum += diffPitch;
                }
                avgPitchDiff = pitchDiffListSum / pitchDiffList.size();

                double speedListSum = 0;
                for (double speed : speedList) {
                    speedListSum += speed;
                }
                double avgSpeed = speedListSum / speedList.size();

                while (yawDiffList.size() > maxDeltaHistorySize) {
                    yawDiffList.remove(0);
                }

                while (pitchDiffList.size() > maxDeltaHistorySize) {
                    pitchDiffList.remove(0);
                }

                while (speedList.size() > 20) {
                    speedList.remove(0);
                }

                avgYawDiff = MathHelper.clamp_float(avgYawDiff, -180F, 180F);
                avgPitchDiff = MathHelper.clamp_float(avgPitchDiff, -90F, 90F);

                if (!averageLimitMode.equals(AverageLimitMode.None)) {
                    float avgYawAbs = Math.abs(avgYawDiff);

                    switch (averageLimitMode) {
                        case NCP:
                            EntityLivingBase target = PlayerUtils.getTarget(3);
                            if (target == null || PlayerUtils.getDistanceToEntityBox(target) > 1) {
                                double vl = 0;
                                if (avgYawAbs > 50) {
                                    vl += 30.0 * avgYawAbs / 180.0;
                                }

                                if (avgSpeed >= 0.0 && avgSpeed < 0.2D) {
                                    vl += 20.0 * (0.2 - avgSpeed) / 0.2;
                                }

                                if (averageSwitching > 0.0) {
                                    vl += 20.0 * averageSwitching;
                                }

                                vl += 30.0 * (150.0 - rand.nextGaussian(75, 37.5)) / 150.0;

                                shouldLimit = vl > 70;
                            } else {
                                shouldLimit = false;
                            }
                            break;
                        case Custom:
                            shouldLimit = avgYawAbs > maxAverageYawDelta;
                            break;
                    }
                }
            }

            prevYaw = currentRotation[0];
            prevPitch = currentRotation[1];

            if (angleLimiter.equals(AngleLimiterMode.Accelerated)) {
                if (Math.abs(avgYawDiff) > RotationHandler.hSpeed / 4) {
                    stamina = Math.max(0, stamina - (Math.abs(avgYawDiff) - (RotationHandler.hSpeed / 4)) / 1980);
                } else {
                    stamina = Math.min(1.0F, stamina + 0.004F);
                }
            }
        } else {
            yawDiffList.clear();
            pitchDiffList.clear();
            avgYawDiff = 0;
            avgPitchDiff = 0;
        }
    }

    private double[] limitRotations(float[] targetRot) {
        float yawDifference = RotationUtils.getAngleDifference(targetRot[0], currentRotation[0]);
        float pitchDifference = RotationUtils.getAngleDifference(targetRot[1], currentRotation[1]);

        double rotationDifference = Math.max(1e-6, Math.hypot(Math.abs(yawDifference), Math.abs(pitchDifference)));

        float hSpeed = (float) (RotationHandler.hSpeed + Math.abs(yawDiff * 0.4) + Math.abs(avgYawDiff * MathUtils.randomizeFloat(0.8F, 1.6F)));
        float vSpeed = (float) (RotationHandler.vSpeed + Math.abs(pitchDiff * 0.4) + Math.abs(avgPitchDiff * MathUtils.randomizeFloat(0.8F, 1.6F)));

        float unNormHSpeed = hSpeed;
        hSpeed /= 3;

        float unNormVSpeed = vSpeed;
        vSpeed /= 3;

        hSpeed = Math.min(hSpeed, 180);
        vSpeed = Math.min(vSpeed, 180);

        switch (angleLimiter) {
            case None:
                return new double[]{yawDifference, -pitchDifference};
            case Linear: {
                double straightLineYaw = Math.abs(yawDifference / rotationDifference) * hSpeed;
                double straightLinePitch = Math.abs(pitchDifference / rotationDifference) * vSpeed;

                return new double[]{
                        Math.max(-straightLineYaw, Math.min(straightLineYaw, yawDifference)),
                        -Math.max(-straightLinePitch, Math.min(straightLinePitch, pitchDifference))
                };
            }
            case Interpolated: {
                double straightLineYaw = Math.abs(yawDifference / rotationDifference) * MathHelper.clamp_float(MathHelper.clamp_float(Math.abs(yawDifference) / 180F, 0, 1) * hSpeed, MathUtils.randomizeFloat(4, 6), 180);
                double straightLinePitch = Math.abs(pitchDifference / rotationDifference) * MathHelper.clamp_float(MathHelper.clamp_float(Math.abs(pitchDifference) / 180F, 0, 1) * vSpeed, MathUtils.randomizeFloat(4, 6), 180);

                return new double[]{
                        Math.max(-straightLineYaw, Math.min(straightLineYaw, yawDifference)),
                        -Math.max(-straightLinePitch, Math.min(straightLinePitch, pitchDifference))
                };
            }
            case Accelerated: {
                if (yawDifference > hSpeed) {
                    if (Math.abs(avgYawDiff) > 12 && Math.abs(avgPitchDiff) < 1) {
                        float variance = Math.abs(avgYawDiff) / 90 * 50;
                        targetRot[1] += MathUtils.randomizeAround(variance);
                        pitchDifference = RotationUtils.getAngleDifference(targetRot[1], currentRotation[1]);
                    }
                }

                float staminaMultiplier = MathHelper.clamp_float(0.8F + stamina * 0.2F, 0.8F, 1);
                hSpeed *= staminaMultiplier;
                vSpeed *= staminaMultiplier;

                float decelStartYaw = unNormHSpeed / 4;
                float decelStartPitch = unNormVSpeed / 4;

                float percentageYawCurr = MathHelper.clamp_float((decelStartYaw - Math.abs(avgYawDiff)) / decelStartYaw, 0, 1);
                float percentageYawAvg = MathHelper.clamp_float((decelStartYaw - Math.abs(yawDifference)) / decelStartYaw, 0, 1);
                float percentageYaw = percentageYawCurr * percentageYawAvg;

                float percentagePitchCurr = MathHelper.clamp_float((decelStartPitch - Math.abs(avgPitchDiff)) / decelStartPitch, 0, 1);
                float percentagePitchAvg = MathHelper.clamp_float((decelStartPitch - Math.abs(pitchDifference)) / decelStartPitch, 0, 1);
                float percentagePitch = percentagePitchCurr * percentagePitchAvg;

                float multiYaw = Math.abs(yawDifference) / decelStartYaw;
                float multiPitch = Math.abs(pitchDifference) / decelStartPitch;

                multiYaw = MathHelper.clamp_float(multiYaw, 0.85F, 1);
                multiPitch = MathHelper.clamp_float(multiPitch, 0.85F, 1);

                float reducedHSpeed = hSpeed * multiYaw;
                float reducedVSpeed = vSpeed * multiPitch;

                hSpeed = MathUtils.interpolateNoUpdateCheck(hSpeed, reducedHSpeed, percentageYaw);
                vSpeed = MathUtils.interpolateNoUpdateCheck(vSpeed, reducedVSpeed, percentagePitch);

                float maxAccel = 1.8F;
//
//                float yawPercentage = MathHelper.clamp_float(((float) Math.abs(accelerationYaw - lastAccelDeltaYaw) - maxAccel) / maxAccel, 0, 1);
//                float pitchPercentage = MathHelper.clamp_float(((float) Math.abs(accelerationPitch - lastAccelDeltaPitch) - maxAccel) / maxAccel, 0, 1);
//                float reducedHSpeed1 = hSpeed *  0.9F;
//                float reducedVSpeed1 = vSpeed * 0.9F;
//                hSpeed = MathUtils.interpolateNoUpdateCheck(hSpeed, reducedHSpeed1, yawPercentage);
//                vSpeed = MathUtils.interpolateNoUpdateCheck(vSpeed, reducedVSpeed1, pitchPercentage);
//
                float percentage1 = MathHelper.clamp_float((Math.abs(currentRotation[0] - prevYaw) - RotationHandler.hSpeed * maxAccel) / Math.max(RotationHandler.hSpeed * maxAccel, 1e-6F), 0, 1);
                float percentage2 = MathHelper.clamp_float((Math.abs(avgYawDiff) - RotationHandler.hSpeed) / RotationHandler.hSpeed, 0, 1);
                float reducedHSpeed2 = hSpeed * 0.6F;
                hSpeed = MathUtils.interpolateNoUpdateCheck(hSpeed, reducedHSpeed2, percentage1 * percentage2);

                hSpeed = Math.min(hSpeed, 180);
                vSpeed = Math.min(vSpeed, 180);

                double straightLineYaw = Math.abs(yawDifference / rotationDifference) * hSpeed;
                double straightLinePitch = Math.abs(pitchDifference / rotationDifference) * vSpeed;
                double deltaYaw = MathHelper.clamp_double(yawDifference, -straightLineYaw, straightLineYaw);
                double deltaPitch = MathHelper.clamp_double(pitchDifference, -straightLinePitch, straightLinePitch);

                if (updateTimer.hasTimeElapsed((long) 16.67)) {
                    double velocityYaw = deltaYaw - previousDeltaYaw;
                    double velocityPitch = deltaPitch - previousDeltaPitch;

                    float baseYaw = (float) (Math.abs(yawDiff * 0.7) + Math.abs(avgYawDiff * MathUtils.randomizeFloat(0.8F, 1.6F))) / 180;
                    baseYaw = MathHelper.clamp_float(baseYaw, 0, 1);

                    float basePitch = (float) (Math.abs(pitchDiff * 0.7) + Math.abs(avgPitchDiff * MathUtils.randomizeFloat(0.8F, 1.6F))) / 180;
                    basePitch = MathHelper.clamp_float(basePitch, 0, 1);

                    float factorYaw = MathHelper.clamp_float(MathUtils.interpolateNoUpdateCheck(0.15F, 0.35F, smooth(baseYaw, 2)), 0, 0.35F);
                    float factorPitch = MathHelper.clamp_float(MathUtils.interpolateNoUpdateCheck(0.15F, 0.35F, smooth(basePitch, 2)), 0, 0.35F);

                    float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
                    float f1 = f * f * f * 8.0F;
                    if (Math.abs(lastAccelDeltaYaw) < f1 * 0.15) {
                        float incYaw = MathHelper.clamp_float(0.2F * (Math.abs(yawDifference) / 50F), 0, 1);
                        factorYaw = MathUtils.randomizeFloat(0.1F + incYaw, 0.5F + incYaw);
                    }

                    if (Math.abs(lastAccelDeltaPitch) < f1 * 0.15) {
                        float incPitch = MathHelper.clamp_float(0.2F * (Math.abs(pitchDifference) / 50F), 0, 1);
                        factorPitch = MathUtils.randomizeFloat(0.1F + incPitch, 0.5F + incPitch);
                    }

                    factorYaw = MathHelper.clamp_float(factorYaw, 0, 1);
                    factorPitch = MathHelper.clamp_float(factorPitch, 0, 1);

                    float distYaw = 1 - MathHelper.clamp_float(Math.abs(yawDifference) / RotationHandler.hSpeed * 2, 0, 1);
                    float distPitch = 1 - MathHelper.clamp_float(Math.abs(pitchDifference) / RotationHandler.vSpeed * 2, 0, 1);

                    float yawSpeedRatio = 1 - MathHelper.clamp_float(Math.abs(avgYawDiff) / RotationHandler.hSpeed / 2, 0, 1);
                    float pitchSpeedRatio = 1 - MathHelper.clamp_float(Math.abs(avgPitchDiff) / RotationHandler.vSpeed / 2, 0, 1);

                    float minDamping = 0.9F;

                    float yawProgress = MathUtils.interpolateNoUpdateCheck(minDamping, 1, 1 - (distYaw * yawSpeedRatio));
                    float pitchProgress = MathUtils.interpolateNoUpdateCheck(minDamping, 1, 1 - (distPitch * pitchSpeedRatio));

                    float yawDamping = MathHelper.clamp_float(smooth(yawProgress, 1), minDamping, 1);
                    float pitchDamping = MathHelper.clamp_float(smooth(pitchProgress, 1), minDamping, 1);

                    float dampFactor = Math.abs(targetRotation[0] - lastYawDiff) / 4.7F;
                    yawDamping = MathHelper.clamp_float(MathUtils.interpolateNoUpdateCheck(yawDamping, 1, dampFactor), minDamping, 1);
                    pitchDamping = MathHelper.clamp_float(MathUtils.interpolateNoUpdateCheck(pitchDamping, 1, dampFactor), minDamping, 1);
                    lastYawDiff = targetRotation[0];

                    float minYaw = RotationHandler.hSpeed / 2;
                    float minPitch = minYaw / 2;
                    if (Math.signum(accelerationYaw) != Math.signum(velocityYaw) && Math.abs(avgYawDiff) > minYaw) {
                        float blendedDelta = (float) MathUtils.interpolateNoUpdateCheck(Math.abs(deltaYaw), Math.abs(avgYawDiff), 0.73F);
                        float avgYawRatio = MathHelper.clamp_float((blendedDelta - minYaw) / minYaw, 0, 1);
                        float multi = MathUtils.interpolateNoUpdateCheck(0.8F, 0.9F, avgYawRatio);
                        yawDamping *= multi;
                    }

                    if (Math.signum(accelerationPitch) != Math.signum(velocityPitch) && Math.abs(avgPitchDiff) > minPitch) {
                        float blendedDelta = (float) MathUtils.interpolateNoUpdateCheck(Math.abs(deltaPitch), Math.abs(avgYawDiff), 0.73F);
                        float avgPitchRatio = MathHelper.clamp_float((blendedDelta - minPitch) / minPitch, 0, 1);
                        float multi = MathUtils.interpolateNoUpdateCheck(0.8F, 0.9F, avgPitchRatio);
                        pitchDamping *= multi;
                    }

                    accelerationYaw = MathUtils.interpolateNoUpdateCheck(previousDeltaYaw, deltaYaw, factorYaw);
                    accelerationPitch = MathUtils.interpolateNoUpdateCheck(previousDeltaPitch, deltaPitch, factorPitch);
                    accelerationYaw *= yawDamping;
                    accelerationPitch *= pitchDamping;

                    lastAccelDeltaYaw = accelerationYaw;
                    lastAccelDeltaPitch = accelerationPitch;

                    updateTimer.reset();
                }

                deltaYaw = accelerationYaw;
                deltaPitch = accelerationPitch;
                previousDeltaYaw = deltaYaw;
                previousDeltaPitch = deltaPitch;

                return new double[]{deltaYaw, -deltaPitch};
            }
        }

        return new double[]{0, 0};
    }

    private float smooth(float x, float f) {
        return (x * x) * f;
    }
}
