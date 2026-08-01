package gr.larryngton.demise.utils.math;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.utils.InstanceAccess;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

public class MathUtils implements InstanceAccess {
    private static final TimerUtil lerpUpdateTimer = new TimerUtil();
    private static boolean updateLerp;

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        if (lerpUpdateTimer.hasTimeElapsed(10)) {
            updateLerp = true;
            lerpUpdateTimer.reset();
            return;
        }

        updateLerp = false;
    }

    public static double roundToHalf(double d) {
        return Math.round(d * 2) / 2.0;
    }

    public static float interpolate(float current, float target) {
        return interpolate(current, target, mc.timer.partialTicks);
    }

    public static float interpolate(float current, float target, float multiple) {
        if (multiple == mc.timer.partialTicks) {
            return current + (target - current) * multiple;
        }

        if (updateLerp) {
            return current + (target - current) * multiple;
        }

        return current;
    }

    public static float[] interpolate(float[] current, float[] target, float multiple) {
        if (multiple == mc.timer.partialTicks) {
            return new float[]{
                    current[0] + (target[0] - current[0]) * multiple,
                    current[1] + (target[1] - current[1]) * multiple
            };
        }

        if (updateLerp) {
            return new float[]{
                    current[0] + (target[0] - current[0]) * multiple,
                    current[1] + (target[1] - current[1]) * multiple
            };
        }

        return current;
    }

    public static double interpolate(double current, double target) {
        return interpolate(current, target, mc.timer.partialTicks);
    }

    public static double interpolate(double current, double target, float multiple) {
        return interpolate((float) current, (float) target, multiple);
    }

    public static Vec3 interpolate(Vec3 current, Vec3 target) {
        return interpolate(current, target, mc.timer.partialTicks);
    }

    public static Vec3 interpolate(Vec3 current, Vec3 target, float multiple) {
        if (multiple == mc.timer.partialTicks) {
            return new Vec3(
                    interpolate(current.xCoord, target.xCoord, multiple),
                    interpolate(current.yCoord, target.yCoord, multiple),
                    interpolate(current.zCoord, target.zCoord, multiple));
        }

        if (updateLerp) {
            return new Vec3(
                    interpolate(current.xCoord, target.xCoord, multiple),
                    interpolate(current.yCoord, target.yCoord, multiple),
                    interpolate(current.zCoord, target.zCoord, multiple));
        }

        return current;
    }

    public static AxisAlignedBB interpolate(AxisAlignedBB current, AxisAlignedBB target) {
        return interpolate(current, target, mc.timer.partialTicks);
    }

    public static AxisAlignedBB interpolate(AxisAlignedBB current, AxisAlignedBB target, float multiple) {
        if (multiple == mc.timer.partialTicks) {
            return new AxisAlignedBB(
                    interpolate(current.minX, target.minX, multiple),
                    interpolate(current.minY, target.minY, multiple),
                    interpolate(current.minZ, target.minZ, multiple),
                    interpolate(current.maxX, target.maxX, multiple),
                    interpolate(current.maxY, target.maxY, multiple),
                    interpolate(current.maxZ, target.maxZ, multiple)
            );
        }

        if (updateLerp) {
            return new AxisAlignedBB(
                    interpolate(current.minX, target.minX, multiple),
                    interpolate(current.minY, target.minY, multiple),
                    interpolate(current.minZ, target.minZ, multiple),
                    interpolate(current.maxX, target.maxX, multiple),
                    interpolate(current.maxY, target.maxY, multiple),
                    interpolate(current.maxZ, target.maxZ, multiple)
            );
        }

        return current;
    }

    public static float interpolateNoUpdateCheck(float current, float target, float multiple) {
        return current + (target - current) * multiple;
    }

    public static double interpolateNoUpdateCheck(double current, double target, double multiple) {
        return current + (target - current) * multiple;
    }

    public static Vec3 interpolateNoUpdateCheck(Vec3 current, Vec3 target, double multiple) {
        return new Vec3(
                interpolateNoUpdateCheck(current.xCoord, target.xCoord, multiple),
                interpolateNoUpdateCheck(current.yCoord, target.yCoord, multiple),
                interpolateNoUpdateCheck(current.zCoord, target.zCoord, multiple)
        );
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double PI = Math.PI;
        double output = 1.0 / Math.sqrt(2.0 * PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public static int randomizeInt(double min, double max) {
        return (int) randomizeDouble(min, max);
    }

    public static double randomizeDouble(double min, double max) {
        if (min == max) {
            return min;
        }

        if (Math.abs(min) < 1 || Math.abs(max) < 1) {
            return Math.random() * (max - min) + min;
        }

        return ThreadLocalRandom.current().nextDouble(min, max + 1);
    }

    public static float randomizeFloat(float min, float max) {
        return (float) randomizeDouble(min, max);
    }

    public static float randomizeAround(float val) {
        return (float) randomizeDouble(-val, val);
    }

    public static boolean inBetween(double min, double max, double value) {
        return value >= min && value <= max;
    }

    public static double wrappedDifference(double number1, double number2) {
        return Math.min(Math.abs(number1 - number2), Math.min(Math.abs(number1 - 360) - Math.abs(number2 - 0), Math.abs(number2 - 360) - Math.abs(number1 - 0)));
    }

    public static double round(final double value, final int places) {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static float interpolateRotation(float par1, float par2, float par3) {
        float f;

        for (f = par2 - par1; f < -180.0F; f += 360.0F) {
        }

        while (f >= 180.0F) {
            f -= 360.0F;
        }

        return par1 + par3 * f;
    }
}