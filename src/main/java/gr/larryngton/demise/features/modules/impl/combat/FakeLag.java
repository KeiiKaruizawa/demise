package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.packet.PacketReleaseEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.PreAttackEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.events.impl.render.RenderEntityEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.legit.BackTrack;
import gr.larryngton.demise.features.values.impl.*;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.packet.LagUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.render.RenderUtils;
import lombok.AllArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "FakeLag", description = "Abuses latency in order to be unpredictable to your target.")
public class FakeLag extends Module {
    private final BoolValue alwaysSpoof = new BoolValue("Always spoof", false, this);
    private final SliderValue searchRange = new SliderValue("Search range", 6, 1, 15, 0.1f, this, () -> !alwaysSpoof.get());
    private final DoubleSliderValue recoilTime = new DoubleSliderValue("Re-lag time", 50, 50, 0, 1000, this);
    private final SliderValue delay = new SliderValue("Delay ms", 100, 0, 1000, 10, this);
    private final SliderValue delayInCombat = new SliderValue("Delay in combat", 100, 0, 1000, 10, this);
    private final SliderValue extraRand = new SliderValue("Extra rand", 50, 0, 500, 10, this);
    private final SliderValue hurtTimeToStop = new SliderValue("HurtTime to stop", 0, 0, 10, 1, this);
    private final SliderValue predictionTicks = new SliderValue("Prediction ticks", 0, 0, 5, 0.1F, this);
    private final SliderValue maxServerCPS = new SliderValue("Max server CPS", 10, 0, 100, 1, this);
    private final BoolValue projectileDodge = new BoolValue("Projectile dodge", false, this);
    private final BoolValue realPos = new BoolValue("Display real pos", true, this);
    private final ModeValue renderMode = new ModeValue("Render mode", new String[]{"Box", "FakePlayer"}, "FakePlayer", this, realPos::get);
    private final ColorValue color = new ColorValue("Color", new Color(0, 0, 0, 100), this, () -> renderMode.is("Box"));
    private final BoolValue onlyKillAura = new BoolValue("Only on KillAura", false, this);

    private final BoolValue keepRange = new BoolValue("Keep range", false, this);
    private final SliderValue keepRangeDelay = new SliderValue("Keep range delay", 50, 0, 1000, 10, this, keepRange::get);

    public FakeLag() {
        alwaysSpoof.setDescription("Makes FakeLag ignore search range.");
        recoilTime.setDescription("Delay to lag again after stopping.");
    }

    public static boolean blinking = false;
    private final TimerUtil delayTimer = new TimerUtil();
    private final TimerUtil recoilTimer = new TimerUtil();
    private Vec3 onPacketXYZ = Vec3.ZERO;
    private Vec3 realXYZ = Vec3.ZERO;
    private Vec3 lastRealXYZ = Vec3.ZERO;
    private Vec3 interpolatedRealXYZ = Vec3.ZERO;
    public EntityLivingBase target;
    private int ms;
    private boolean dispatched;
    private final TimerUtil hurtTimer = new TimerUtil();
    private final TimerUtil nextLagAfterBackTrack = new TimerUtil();
    private int reLagTime;
    private final TimerUtil standbyTimer = new TimerUtil();
    private final TimerUtil attackTimer = new TimerUtil();
    private int nextRand;
    private int lagTicks;
    private final List<PositionSnapshot> positionSnapshots = new ArrayList<>();
    public static Vec3 realPosition = Vec3.ZERO;
    public static Vec3 realLastPos = Vec3.ZERO;
    private Vec3 anim = Vec3.ZERO;
    private boolean waitingForCPSReduce;
    private float serverCPS;

    @Override
    public void onEnable() {
        blinking = false;
        target = null;
        delayTimer.reset();
        recoilTimer.reset();

        onPacketXYZ = Vec3.ZERO;
        realXYZ = Vec3.ZERO;
        lastRealXYZ = Vec3.ZERO;
        interpolatedRealXYZ = Vec3.ZERO;
        realPosition = Vec3.ZERO;
        realLastPos = Vec3.ZERO;
        anim = Vec3.ZERO;
    }

    @Override
    public void onDisable() {
        LagUtils.disable();
        LagUtils.dispatch();
        lagTicks = 0;
        onPacketXYZ = null;
        realXYZ = null;
        lastRealXYZ = null;
        interpolatedRealXYZ = null;
        target = null;
        positionSnapshots.clear();
        realPosition = null;
        realLastPos = null;
        anim = null;
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPost()) {
            setTag(ms + " ms");

            target = PlayerUtils.getTarget(alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get() + 1);

            if (pauseCriteria()) {
                flush();
                return;
            }

            boolean shouldLag = shouldLag();

            if (shouldLag) {
                if (recoilTimer.hasTimeElapsed(reLagTime)) {
                    if (!attackTimer.hasTimeElapsed(5000)) {
                        ms = delayInCombat.getInt() + nextRand;
                    } else {
                        ms = delay.getInt() + nextRand / 2;
                    }

                    if (projectileDodge.get()) {
                        Tuple<Boolean, Integer> calc = getBestDodgeMS();

                        if (calc.getFirst()) {
                            ms = calc.getSecond();
                        }
                    }

                    ms = Math.max(0, ms);
                } else {
                    ms = 0;
                }
            }

            boolean isExtraSpoofing = false;

            if (keepRange.get() && !shouldLag) {
                List<PositionSnapshot> validSnapshots = new ArrayList<>();

                for (PositionSnapshot snapshot : positionSnapshots) {
                    if (PlayerUtils.getDistToTargetFromMouseOver(
                            snapshot.position.add(0, mc.thePlayer.getEyeHeight(), 0),
                            mc.thePlayer.getLookCustom(snapshot.rotation[0], snapshot.rotation[1]),
                            target,
                            target.getHitbox()) < 3 &&
                            PlayerUtils.getCustomDistanceToEntityBox(snapshot.position, target) > PlayerUtils.getDistanceToEntityBox(target)) {
                        validSnapshots.add(snapshot);
                    }
                }

                if (!validSnapshots.isEmpty()) {
                    validSnapshots.sort(Comparator.comparingDouble(snapshot -> PlayerUtils.getCustomDistanceToEntityBox(snapshot.position, target)));
                    Collections.reverse(validSnapshots);

                    PositionSnapshot firstSnapshot = validSnapshots.get(0);
                    spoof(ms = firstSnapshot.time, true);
                    onPacketXYZ = firstSnapshot.position;

                    isExtraSpoofing = true;
                } else {
                    flush();
                }
            }

            if (shouldLag) {
                spoof(ms, false);
            } else if (!isExtraSpoofing) {
                flush();
            }

            lastRealXYZ = realXYZ;
            realXYZ = onPacketXYZ;
        } else {
            positionSnapshots.add(new PositionSnapshot(0, mc.thePlayer.getPositionVector(), RotationHandler.currentRotation));

            for (PositionSnapshot snapshot : positionSnapshots) {
                snapshot.time += 50;
            }

            while (positionSnapshots.size() > (keepRange.get() ? keepRangeDelay.get() : ms) / 50L) {
                positionSnapshots.remove(0);
            }
        }
    }

    private void spoof(int ms, boolean keepRange) {
        dispatched = false;

        if (!keepRange) {
            if (standbyTimer.hasTimeElapsed(50)) {
                LagUtils.spoof(ms, true, false, false, false, true, true, true);
                blinking = true;

                if (ms > 0) {
                    lagTicks++;
                }
            } else {
                resetRenderPos();
            }
        } else {
            LagUtils.spoof(ms, true, true, true, true, false, false);
            blinking = true;
            lagTicks++;
        }
    }

    private void flush() {
        waitingForCPSReduce = false;

        float baseCPS = Demise.INSTANCE.getCPSTracker().getLeftClicks();
        serverCPS = baseCPS + (baseCPS * (ms / 50F));

        if (serverCPS > maxServerCPS.getInt() && !dispatched) {
            waitingForCPSReduce = true;
            spoof(ms, false);
//            ChatUtils.sendMessageClient("spoof extra for no cps fuckery");
            return;
        }

        resetRenderPos();

        if (!dispatched) {
            lagTicks = 0;
            LagUtils.disable();
            LagUtils.dispatch();
            standbyTimer.reset();
            dispatched = true;
            nextRand = (int) MathUtils.randomizeAround(extraRand.get());
        }

        recoilTimer.reset();
        delayTimer.reset();
        reLagTime = (int) recoilTime.random();
        blinking = false;
    }

    private void resetRenderPos() {
        realXYZ = onPacketXYZ = lastRealXYZ = interpolatedRealXYZ = mc.thePlayer.getPositionVector();
    }

    private boolean pauseCriteria() {
        boolean backtrackPause = getModule(BackTrack.class).isEnabled() && BackTrack.shouldLag;

        if (backtrackPause) {
            nextLagAfterBackTrack.reset();
        }

        return target == null ||
                (onlyKillAura.get() && !getModule(KillAura.class).isEnabled()) ||
                mc.thePlayer.isDead ||
                !nextLagAfterBackTrack.hasTimeElapsed(50) ||
                mc.thePlayer.isUsingItem();
    }

    private Tuple<Boolean, Integer> getBestDodgeMS() {
        Entity projectile = null;
        Vec3 vec = mc.thePlayer.getPositionVector();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityArrow && entity.ticksExisted <= 1) {
                continue;
            }

            AxisAlignedBB axis = mc.thePlayer.getHitbox().offset(realXYZ.subtract(vec)).expand(1, 1, 1);
            Vec3 predictedPos = entity.getPositionVector().add(entity.getMoveDeltaVector().multiply(2));

            if (entity instanceof EntityArrow arrow && !arrow.inGround && arrow.shootingEntity != mc.thePlayer) {
                if (axis.isVecInside(predictedPos)) {
                    projectile = arrow;
                    break;
                }
            }

            if (entity instanceof EntityThrowable throwable && throwable.getThrower() != mc.thePlayer) {
                if (axis.isVecInside(predictedPos)) {
                    projectile = throwable;
                    break;
                }
            }

            if (entity instanceof EntityFishHook fishHook && fishHook.caughtEntity == null) {
                if (axis.isVecInside(predictedPos)) {
                    projectile = fishHook;
                    break;
                }
            }
        }

        if (projectile == null) {
            return new Tuple<>(false, 0);
        }

        for (PositionSnapshot snapshot : positionSnapshots) {
            AxisAlignedBB axis = mc.thePlayer.getHitbox().offset(snapshot.position.subtract(vec));
            if (!axis.expand(1, 1, 1).isVecInside(projectile.getPositionVector().add(projectile.getMoveDeltaVector().multiply(2)))) {
                ChatUtils.sendMessageClient("best snapshot for " + projectile.getClass().getSimpleName() + " is of " + snapshot.time + "ms");
                return new Tuple<>(true, snapshot.time);
            }
        }

        ChatUtils.sendMessageClient("couldn't dodge");
        return new Tuple<>(false, 0);
    }

    @EventTarget
    public void onPacketRelease(PacketReleaseEvent e) {
        if (e.getTimedPacket().getPacket() instanceof C03PacketPlayer c03) {
            if (c03.getPositionX() != 0 && c03.getPositionY() != 0 && c03.getPositionZ() != 0) {
                onPacketXYZ = new Vec3(c03.getPositionX(), c03.getPositionY(), c03.getPositionZ());
            }
        }
    }

    private Vec3 getSelfPrediction(int prediction) {
        return mc.thePlayer.getMoveDeltaVector().flat().multiply(prediction);
    }

    private Vec3 getTargetPrediction(int prediction) {
        return target.getMoveDeltaVector().flat().multiply(prediction);
    }

    private boolean shouldLag() {
        if (target == null)
            return false;

        Vec3 selfPred = getSelfPrediction(predictionTicks.getInt());
        Vec3 targetPred = getTargetPrediction(predictionTicks.getInt());

        double playerToTarget = mc.thePlayer.getDistance(target.getPrevPositionVector()) - mc.thePlayer.getDistance(target.getPositionVector());
        double targetToPlayer = target.getDistance(mc.thePlayer.getPrevPositionVector()) - target.getDistance(mc.thePlayer.getPositionVector());

        if (Math.signum(playerToTarget) != Math.signum(targetToPlayer)) {
            selfPred.multiply(2);
            targetPred = Vec3.ZERO;
        }

        double predictedBHVDist = PlayerUtils.getCustomDistanceToEntityBox(
                mc.thePlayer.getPositionEyes(1).add(selfPred),
                target.getPositionEyes(1).add(targetPred)
        );

        boolean minRangeCheck = predictedBHVDist > 3 && mc.objectMouseOver.entityHit != target;
        boolean rangeCheck = alwaysSpoof.get() || PlayerUtils.getDistanceToEntityBox(target) <= searchRange.get();

        if (mc.thePlayer.hurtTime > hurtTimeToStop.get()) {
            hurtTimer.reset();
        }

        boolean selfHurtTimeCheck = hurtTimer.hasTimeElapsed(50);
        boolean distanceDiffCheck = mc.thePlayer.getDistanceToEntity(target) < mc.thePlayer.getCustomDistanceToEntity(onPacketXYZ, target) ||
                (mc.thePlayer.getDistanceToEntity(target) > 6 && mc.thePlayer.getCustomDistanceToEntity(onPacketXYZ, target) > 6);

        return minRangeCheck && rangeCheck && distanceDiffCheck && selfHurtTimeCheck && target.hurtTime <= 1;
    }

    @EventTarget
    public void onPreAttack(PreAttackEvent e) {
        attackTimer.reset();

        if (waitingForCPSReduce) {
            ChatUtils.sendMessageClient("Cancelled attack, " + serverCPS + " > " + maxServerCPS.getInt());
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING && keepRange.get() && realPos.get()) {
            realLastPos = realPosition;

            if (e.getPacket() instanceof S14PacketEntity s14PacketEntity) {
                if (s14PacketEntity.getEntityId() == target.getEntityId()) {
                    realPosition = realPosition.addVector(s14PacketEntity.getX() / 32.0D, s14PacketEntity.getY() / 32.0D, s14PacketEntity.getZ() / 32.0D);
                }
            } else if (e.getPacket() instanceof S18PacketEntityTeleport s18PacketEntityTeleport) {
                if (s18PacketEntityTeleport.getEntityId() == target.getEntityId()) {
                    realPosition = new Vec3(s18PacketEntityTeleport.getX() / 32D, s18PacketEntityTeleport.getY() / 32D, s18PacketEntityTeleport.getZ() / 32D);
                }

                if (s18PacketEntityTeleport.getEntityId() == mc.thePlayer.getEntityId()) {
                    dispatched = false;
                }
            }
        }
    }

    @EventTarget
    public void onRenderEntity(RenderEntityEvent e) {
        if (realPos.get() && renderMode.is("FakePlayer")) {
            if (lagTicks > 0 && blinking && mc.gameSettings.thirdPersonView != 0) {
                interpolatedRealXYZ = MathUtils.interpolate(lastRealXYZ, realXYZ);

                double x = interpolatedRealXYZ.xCoord - mc.getRenderManager().viewerPosX;
                double y = interpolatedRealXYZ.yCoord - mc.getRenderManager().viewerPosY;
                double z = interpolatedRealXYZ.zCoord - mc.getRenderManager().viewerPosZ;

                mc.getRenderManager().doRenderEntity(mc.thePlayer, x, y, z, mc.thePlayer.rotationYawHead, mc.timer.partialTicks, true, true);
            }

            if (target != null && keepRange.get()) {
                float x = (float) (realPosition.xCoord - mc.getRenderManager().viewerPosX);
                float y = (float) (realPosition.yCoord - mc.getRenderManager().viewerPosY);
                float z = (float) (realPosition.zCoord - mc.getRenderManager().viewerPosZ);

                anim = MathUtils.interpolate(anim, new Vec3(x, y, z));
                mc.getRenderManager().doRenderEntity(target, anim.xCoord, anim.yCoord, anim.zCoord, target.rotationYawHead, mc.timer.partialTicks, true);
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (realPos.get() && renderMode.is("Box")) {
            if (lagTicks > 0 && blinking && mc.gameSettings.thirdPersonView != 0) {
                interpolatedRealXYZ = MathUtils.interpolate(lastRealXYZ, realXYZ);

                double x = interpolatedRealXYZ.xCoord - mc.getRenderManager().viewerPosX;
                double y = interpolatedRealXYZ.yCoord - mc.getRenderManager().viewerPosY;
                double z = interpolatedRealXYZ.zCoord - mc.getRenderManager().viewerPosZ;

                Vec3 vec = MathUtils.interpolate(mc.thePlayer.getPrevPositionVector(), mc.thePlayer.getPositionVector());
                AxisAlignedBB axis = mc.thePlayer.getHitbox().offset(new Vec3(x - vec.xCoord, y - vec.yCoord, z - vec.zCoord));
                RenderUtils.drawAxisAlignedBB(axis, true, false, color.get().getRGB());
            }

            if (target != null && keepRange.get()) {
                float x = (float) (realPosition.xCoord - mc.getRenderManager().viewerPosX);
                float y = (float) (realPosition.yCoord - mc.getRenderManager().viewerPosY);
                float z = (float) (realPosition.zCoord - mc.getRenderManager().viewerPosZ);

                anim = MathUtils.interpolate(anim, new Vec3(x, y, z));
                RenderUtils.drawAxisAlignedBB(target.getHitbox().offset(anim.subtract(target.getPositionVector())), true, false, color.get().getRGB());
            }
        }
    }

    @AllArgsConstructor
    static class PositionSnapshot {
        int time;
        Vec3 position;
        float[] rotation;
    }
}