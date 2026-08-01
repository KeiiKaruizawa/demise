package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.TimerManipulationEvent;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.player.MoveEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.legit.BackTrack;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.SimulatedPlayer;
import gr.larryngton.demise.utils.player.handlers.clicking.ClickHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TickBase", description = "Abuses tick manipulation in order to be unpredictable to your target.")
public class TickBase extends Module {
    private final SliderValue delay = new SliderValue("Delay", 50, 0, 1000, 50, this);
    private final SliderValue tickRange = new SliderValue("Tick range", 3f, 0.1f, 8f, 0.1f, this);
    private final SliderValue minRange = new SliderValue("Min range", 2.5f, 0.1f, 8f, 0.1f, this);
    private final SliderValue stopRange = new SliderValue("Stop range", 2.5f, 0.1f, 8f, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 7f, 0.1f, 15, 0.1f, this);
    private final SliderValue maxTick = new SliderValue("Max ticks", 4, 1, 20, this);
    private final BoolValue prioritiseCrits = new BoolValue("Prioritise crits", false, this);
    private final BoolValue chatDebug = new BoolValue("Chat Debug", false, this);

    public TickBase() {
        delay.setDescription("Delay between teleports.");
        tickRange.setDescription("Max range to tp into.");
        minRange.setDescription("Min range to tp into.");
        prioritiseCrits.setDescription("Prioritise ticks where you will deal critical hits.");
    }

    private final TimerUtil timer = new TimerUtil();
    private long shifted, previousTime;
    private final List<PlayerUtils.PredictProcess> selfPrediction = new ArrayList<>();
    private EntityLivingBase target;
    private int ticksToSkip;

    @Override
    public void onEnable() {
        shifted = 0;
        previousTime = 0;
    }

    @Override
    public void onDisable() {
        target = null;
        selfPrediction.clear();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(String.valueOf(ticksToSkip));
        target = PlayerUtils.getTarget(searchRange.get());
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        shifted = 0;
    }

    @EventTarget
    public void onTimerManipulation(TimerManipulationEvent e) {
        if (target == null || selfPrediction.isEmpty()) {
            previousTime = e.getTime();
            e.setTime(e.getTime() - shifted);
            shifted = 0;
            return;
        }

        if (shouldStart() && timer.hasTimeElapsed(delay.get())) {
            shifted += e.getTime() - previousTime;
        } else {
            float lastShifted = shifted;
            shifted = 0;

            if (lastShifted > 0 && chatDebug.get()) {
                ChatUtils.sendMessageClient("reset bal accum");
            }
        }

        if (shifted >= ticksToSkip * 50L) {
            shifted = 0;
            timer.reset();
        }

        previousTime = e.getTime();
        e.setTime(e.getTime() - shifted);
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        selfPrediction.clear();

        SimulatedPlayer simulatedSelf = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);

        simulatedSelf.rotationYaw = RotationHandler.currentRotation != null ? RotationHandler.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < maxTick.get(); i++) {
            simulatedSelf.tick();

            PlayerUtils.PredictProcess predictProcess = new PlayerUtils.PredictProcess(
                    simulatedSelf.getPos(),
                    simulatedSelf.fallDistance,
                    simulatedSelf.onGround,
                    simulatedSelf.isCollidedHorizontally
            );

            predictProcess.tick = i;

            selfPrediction.add(predictProcess);
        }
    }

    private Vec3 getPredictedTargetPos(int tick) {
        return target.getPositionVector().add(target.getMoveDeltaVector().multiply(tick));
    }

    private boolean shouldStart() {
        // skipping ticks is useless here
        if (mc.objectMouseOver.entityHit == target) {
            return false;
        }

        boolean picked = false;

        double bestScore = Double.MAX_VALUE;
        int bestTick = -1;
        double bestDist = 27.6;

        for (PlayerUtils.PredictProcess predictProcess : selfPrediction) {
            if (!criteria(predictProcess.tick)) continue;
            picked = true;

            AxisAlignedBB entityBoundingBox = target.getHitbox().offset(getPredictedTargetPos(ticksToSkip).subtract(target.getPositionVector()));
            double predictedSelfDistance = PlayerUtils.getDistToTargetFromMouseOver(selfPrediction.get(predictProcess.tick).position.add(0, mc.thePlayer.getEyeHeight(), 0), mc.thePlayer.getLook(1), target, entityBoundingBox);
            double score = Math.abs(predictedSelfDistance - tickRange.get());

            if (prioritiseCrits.get() && predictProcess.fallDistance > 0) {
                score -= 1.0;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTick = predictProcess.tick;
                bestDist = predictedSelfDistance;
            }
        }

        if (bestDist != 27.6 && chatDebug.get()) {
            ChatUtils.sendMessageClient("tick dist is " + bestDist);
        }

        ticksToSkip = bestTick + 1;

        if (!picked) {
            ticksToSkip = (int) maxTick.get();
        }

        return criteria(ticksToSkip - 1);
    }

    private boolean criteria(int tick) {
        AxisAlignedBB entityBoundingBox = target.getHitbox().offset(getPredictedTargetPos(tick + 1).subtract(target.getPositionVector()));

        Vec3 predictedSelfEyes = selfPrediction.get(tick).position.add(0, mc.thePlayer.getEyeHeight(), 0);
        Vec3 predictedTargetEyes = getPredictedTargetPos(tick + 1).add(0, target.getEyeHeight(), 0);

        double predictedSelfDistanceMouseOver = PlayerUtils.getDistToTargetFromMouseOver(predictedSelfEyes, mc.thePlayer.getLook(1), target, entityBoundingBox);
        double predictedSelfDistanceBHV = PlayerUtils.getCustomDistanceToEntityBox(predictedSelfEyes, predictedTargetEyes);

        double predictedSelfDistance = predictedSelfDistanceMouseOver > 8 ? predictedSelfDistanceBHV : predictedSelfDistanceMouseOver;
        double predictedTargetDistance = PlayerUtils.getCustomDistanceToEntityBox(predictedTargetEyes, mc.thePlayer);

        boolean willClickAtTick = true;

        if (ClickHandler.isInitialized()) {
            int mod = ClickHandler.getClickPattern().size();

            if (mod != 0) {
                Integer clickAtTick = ClickHandler.getClickPattern().get(tick % mod);
                willClickAtTick = clickAtTick == null || clickAtTick > 0;
            }
        }

        return target.hurtTime - tick <= 1 &&
                predictedSelfDistance < predictedTargetDistance &&
                predictedSelfDistance <= tickRange.get() &&
                predictedSelfDistance > minRange.get() &&
                predictedSelfDistance <= searchRange.get() &&
                PlayerUtils.getDistanceToEntityBox(target) >= stopRange.get() &&
                mc.thePlayer.canEntityBeSeen(target) &&
                !selfPrediction.get(tick).isCollidedHorizontally &&
                !mc.thePlayer.isCollidedHorizontally &&
                willClickAtTick;
    }
}
