package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.misc.StaticTickEvent;
import gr.larryngton.demise.events.impl.misc.TickEvent;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.player.MoveEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.SimulatedPlayer;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import org.apache.commons.lang3.Range;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TimerRange", description = "Abuses balance in order to be unpredictable to your target.")
public class TimerRange extends Module {
    private final BoolValue smartBalance = new BoolValue("Smart balance", false, this);
    private final SliderValue balanceRange = new SliderValue("Balance range", 8, 3, 15, 0.1f, this, () -> !smartBalance.get());
    private final SliderValue minBalanceRange = new SliderValue("Min balance range", 3, 0, 15, 0.1f, this, () -> !smartBalance.get());
    private final SliderValue maxBalance = new SliderValue("Max balance", 8, 1, 40, 1, this);
    private final BoolValue resetBalance = new BoolValue("Reset balance", false, this);
    private final SliderValue delay = new SliderValue("Delay", 50, 0, 1000, 50, this);
    private final SliderValue tickRange = new SliderValue("Tick range", 3f, 0.1f, 8f, 0.1f, this);
    private final SliderValue minRange = new SliderValue("Min range", 2.5f, 0.1f, 8f, 0.1f, this);
    private final SliderValue stopRange = new SliderValue("Stop range", 2.5f, 0.1f, 8f, 0.1f, this);
    private final SliderValue maxTicks = new SliderValue("Max ticks", 4, 1, 20, this);
    private final BoolValue legacySkip = new BoolValue("Legacy skip", false, this);
    private final SliderValue gameTimer = new SliderValue("Game timer", 2, 1, 20, 0.1F, this, legacySkip::get);
    private final BoolValue allowEarlyBreak = new BoolValue("Allow early break", false, this);
    private final BoolValue prioritiseCrits = new BoolValue("Prioritise crits", false, this);
    private final BoolValue renderPredictedSelfPos = new BoolValue("Render predicted self pos", false, this);

    public TimerRange() {
        delay.setDescription("Delay between teleports.");
    }

    private final TimerUtil timer = new TimerUtil();
    private final List<PlayerUtils.PredictProcess> selfPrediction = new ArrayList<>();
    private EntityLivingBase target;
    private int ticksToSkip;
    public static int balance;
    public static boolean working;
    private int skippedTick;
    private final TimerUtil lastBalanceLossTimer = new TimerUtil();
    private final TimerUtil decayTimer = new TimerUtil();

    @Override
    public void onEnable() {
        balance = 0;
    }

    @Override
    public void onDisable() {
        target = null;
        selfPrediction.clear();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(String.valueOf(ticksToSkip));

        target = PlayerUtils.getTarget(Double.MAX_VALUE);
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        selfPrediction.clear();

        SimulatedPlayer simulatedSelf = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);

        simulatedSelf.rotationYaw = RotationHandler.currentRotation != null ? RotationHandler.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < maxTicks.get(); i++) {
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

    @EventTarget
    public void onTick(TickEvent e) {
        balance--;
    }

    @EventTarget
    public void onStaticTick(StaticTickEvent e) {
        balance++;
    }

    @EventTarget
    public void onGame(GameEvent e) {
        if (target == null || selfPrediction.isEmpty() || mc.theWorld == null || mc.thePlayer == null) {
            working = false;
            return;
        }

        boolean balancing;

        if (balanceCriteria()) {
            if (balance < maxBalance.get()) {
                working = true;
                balancing = true;
                lastBalanceLossTimer.reset();
            } else {
                balancing = false;
            }
        } else {
            balancing = false;
        }

        if (decayTimer.hasTimeElapsed(1000) && lastBalanceLossTimer.hasTimeElapsed(5000) && resetBalance.get()) {
            balance = Math.max(0, balance - 1);
            decayTimer.reset();
        }

        if (timer.hasTimeElapsed(delay.get()) && shouldStart()) {
            skippedTick = 0;
            boolean skipped = false;

            if (!legacySkip.get()) {
                while (skippedTick < ticksToSkip) {
                    working = true;
                    try {
                        skippedTick++;
                        mc.runTick();
                        skipped = true;
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } else {
                mc.timer.timerSpeed = gameTimer.get();
                skipped = true;
            }

            if (skipped) {
                timer.reset();
            }
        } else if (mc.timer.timerSpeed == gameTimer.get()) {
            mc.timer.timerSpeed = 1;
        }

        if (!balancing) {
            working = false;
        }
    }

    private boolean balanceCriteria() {
        if (!smartBalance.get()) {
            return Range.between(minBalanceRange.get(), balanceRange.get()).contains((float) PlayerUtils.getDistanceToEntityBox(target));
        } else {
            return Range.between(3.5, 8.0).contains(selfPrediction.get(selfPrediction.size() - 1).position.distanceTo(target.getPositionVector())) ||
                    (target.hurtTime > 3 && ((!PlayerUtils.hittableBy(target) || PlayerUtils.getCustomDistanceToEntityBox(target.getPositionEyes(1), mc.thePlayer) > 3) || mc.thePlayer.hurtTime != 0));
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        balance = 0;
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (renderPredictedSelfPos.get() && mc.gameSettings.thirdPersonView != 0) {
            double x = selfPrediction.get(selfPrediction.size() - 1).position.xCoord - mc.getRenderManager().viewerPosX;
            double y = selfPrediction.get(selfPrediction.size() - 1).position.yCoord - mc.getRenderManager().viewerPosY;
            double z = selfPrediction.get(selfPrediction.size() - 1).position.zCoord - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, true, false, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1, 100), true).getRGB());
        }
    }

    private boolean shouldStart() {
        boolean picked = false;

        for (PlayerUtils.PredictProcess predictProcess : selfPrediction) {
            if (criteria(predictProcess.tick)) {
                ticksToSkip = predictProcess.tick + 1;
                picked = true;

                AxisAlignedBB entityBoundingBox = target.getHitbox();

                double predictedSelfDistance = PlayerUtils.getDistToTargetFromMouseOver(selfPrediction.get(predictProcess.tick).position.add(0, mc.thePlayer.getEyeHeight(), 0), mc.thePlayer.getLook(1), target, entityBoundingBox);

                if (predictProcess.fallDistance > 0 && prioritiseCrits.get()) {
                    break;
                }

                if (Range.between(tickRange.get(), tickRange.get() - 0.1f).contains((float) predictedSelfDistance) && allowEarlyBreak.get()) {
                    break;
                }
            }
        }

        if (!picked) {
            ticksToSkip = (int) maxTicks.get();
        }

        return criteria(ticksToSkip - 1);
    }

    private boolean criteria(int tick) {
        double predictedTargetDistance = PlayerUtils.getCustomDistanceToEntityBox(PlayerUtils.getPosFromAABB(target.getHitbox()).add(0, target.getEyeHeight(), 0), mc.thePlayer);
        double predictedSelfDistance = PlayerUtils.getDistToTargetFromMouseOver(selfPrediction.get(tick).position.add(0, mc.thePlayer.getEyeHeight(), 0), mc.thePlayer.getLook(1), target, target.getHitbox());

        return predictedSelfDistance < predictedTargetDistance &&
                predictedSelfDistance <= tickRange.get() &&
                predictedSelfDistance > minRange.get() &&
                PlayerUtils.getDistanceToEntityBox(target) >= stopRange.get() &&
                mc.thePlayer.canEntityBeSeen(target) &&
                target.canEntityBeSeen(mc.thePlayer) &&
                !selfPrediction.get(tick).isCollidedHorizontally &&
                !mc.thePlayer.isCollidedHorizontally &&
                balance >= tick &&
                target.hurtTime <= 3;
    }

    public boolean skipTick() {
        return working || skippedTick < 0;
    }

    public boolean freezeAnim() {
//        if (skippedTick != 0) {
//            if (!firstAnimation) {
//                firstAnimation = true;
//                return false;
//            }
//            return true;
//        }
        return false;
    }
}