package gr.larryngton.demise.utils.player.handlers.rotation.predictions;

import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.ArrayList;

@UtilityClass
public class PredictionEngine implements InstanceAccess {
    private float currentTargetOffset;
    private AxisAlignedBB lastTrackedAABB;
    private int ticksExisted;
    public int currentReactionTime;
    public final ArrayList<Double> previousTargetMotions = new ArrayList<>();
    public boolean lastReset;
    private final TimerUtil updateTimer = new TimerUtil();
    private Vec3 lastTrackedMoveDelta;

    public AxisAlignedBB simulatePredictions(EntityLivingBase currentTarget, float attackRange, boolean simulateReactionTime, float thresholdForDirectionConfidence, float thresholdToApplyReactionTime, float[] reactionTime, float extraPrediction) {
        lastReset = false;
        double dist = PlayerUtils.getDistanceToEntityBox(currentTarget);
        Vec3 prediction;
        AxisAlignedBB hitbox = currentTarget.getHitbox();

        boolean flag = false;

        double speed = MoveUtils.getSpeedPosBased(currentTarget);

        if (dist > attackRange) {
            currentTargetOffset = (float) (Math.min(Math.max((dist - attackRange) * 3, 0), 8));
            flag = true;
        }

        if (speed > 0.4) {
            double extra = Math.random() * MathUtils.randomizeFloat(0.9f, 1.1f);
            float target = (float) (-Math.min(Math.max(dist, 0), 8) + (rand.nextBoolean() ? extra : -extra));

            currentTargetOffset = MathUtils.interpolate(currentTargetOffset, target, 0.05f);
            flag = true;
        }

        if (mc.thePlayer.getHitbox().intersectsWith(currentTarget.getHitbox())) {
            currentTargetOffset = 3;
            flag = true;
        }

        prediction = currentTarget.getMoveDeltaVector().multiply(flag ? currentTargetOffset : 0).flat();

        if (simulateReactionTime) {
            if (lastTrackedAABB == null) {
                lastTrackedAABB = hitbox;
            }

            if (mc.thePlayer.ticksExisted != ticksExisted) {
                previousTargetMotions.add(currentTarget.getMoveDeltaVector().lengthVector());

                if (updateTimer.hasTimeElapsed((currentReactionTime + 1) * 50L)) {
                    lastTrackedAABB = hitbox;
                    lastTrackedMoveDelta = currentTarget.getMoveDeltaVector();

                    double averageMotion = 0;
                    for (Double motion : previousTargetMotions) {
                        averageMotion += motion;
                    }
                    averageMotion /= previousTargetMotions.size();
                    double motionPercentage = MathHelper.clamp_double(averageMotion * 2, 0, 1);

                    currentReactionTime = (int) MathUtils.interpolateNoUpdateCheck(reactionTime[0], reactionTime[1], motionPercentage);
                    updateTimer.reset();
                } else {
                    lastTrackedAABB = lastTrackedAABB.offset(lastTrackedMoveDelta);
                    hitbox = lastTrackedAABB;
                }

                while (previousTargetMotions.size() > 20) {
                    previousTargetMotions.remove(0);
                }
            }
        }

        ticksExisted = mc.thePlayer.ticksExisted;
        return hitbox.offset(prediction);
    }

    public void reset() {
        lastTrackedAABB = null;
        currentReactionTime = 0;
        previousTargetMotions.clear();
        lastReset = true;
    }
}