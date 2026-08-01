package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.misc.MouseOverEvent;
import gr.larryngton.demise.events.impl.player.AngleEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.legit.AutoRod;
import gr.larryngton.demise.features.values.impl.*;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.packet.BlinkComponent;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.clicking.ClickManager;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationManager;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.aimpoint.JitterHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.aimpoint.PointFinder;
import gr.larryngton.demise.utils.player.handlers.rotation.predictions.PredictionEngine;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "KillAura", description = "Automatically attack targets.")
public class KillAura extends Module {
    public final SliderValue attackRange = new SliderValue("Attack range", 3, 1, 8, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 4, 1, 8, 0.1f, this);

    private final ClickManager clickManager = new ClickManager(this);

    public final BoolValue autoBlock = new BoolValue("AutoBlock", true, this);
    public final SliderValue autoBlockRange = new SliderValue("AutoBlock range", 3.5f, 1, 8, 0.1f, this, autoBlock::get);
    public final ModeValue autoBlockMode = new ModeValue("AutoBlock mode", new String[]{"Fake", "Vanilla", "Release", "Force", "NCP", "Slot", "Full release", "Blink"}, "Vanilla", this, autoBlock::get);
    private final SliderValue reblockTicks = new SliderValue("Reblock ticks", 0, 0, 5, 1, this, autoBlock::get);
    private final BoolValue interact = new BoolValue("Interact", false, this, () -> autoBlock.get() && autoBlockMode.is("Release") || autoBlockMode.is("Force") || autoBlockMode.is("Slot"));
    private final BoolValue alwaysRenderBlocking = new BoolValue("Always render blocking", false, this, autoBlock::get);

    private final RotationManager rotationManager = new RotationManager(this);

    private final SliderValue hitboxQuality = new SliderValue("Hitbox quality", 1, 0.01F, 1, 0.01F, this);

    private final SliderValue outOfRangeBuffer = new SliderValue("Out of range buffer", 0, -1, 1, 0.01f, this);
    private final ModeValue preferredBodyPart = new ModeValue("Preferred body part", new String[]{"None", "Head", "Torso", "Feet"}, "None", this);
    private final MultiBoolValue blacklistedBoxPositions = new MultiBoolValue("Blacklisted box positions", Arrays.asList(
            new BoolValue("Head", false),
            new BoolValue("Torso", false),
            new BoolValue("Feet", false),
            new BoolValue("Bad hitVec", false),
            new BoolValue("Heuristic", false)
    ), this);
    private final SliderValue badHitVecBuffer = new SliderValue("Bad hitVec buffer", 0.5f, 0.01f, 2, 0.01f, this, () -> blacklistedBoxPositions.isEnabled("Bad hitVec"));
    private final SliderValue heuristicBuffer = new SliderValue("Heuristic buffer", 0.1f, 0.01f, 2, 0.01f, this, () -> blacklistedBoxPositions.isEnabled("Heuristic"));

    private final BoolValue dynamicTrim = new BoolValue("Dynamic trim", false, this);
    private final SliderValue yTrim = new SliderValue("Y trim", 0, 0, 0.5f, 0.01f, this, () -> !dynamicTrim.get());
    private final SliderValue xzTrim = new SliderValue("XZ trim", 0, 0, 0.5f, 0.01f, this);
    private final SliderValue randAdd = new SliderValue("XZ rand add", 0, 0, 0.5F, 0.01F, this, dynamicTrim::get);

    private final BoolValue predictionEngine = new BoolValue("Prediction engine", false, this);
    private final BoolValue simulateReactionTime = new BoolValue("Simulate reaction time", false, this, predictionEngine::get);
    private final DoubleSliderValue reactionTime = new DoubleSliderValue("Reaction time", 0, 10, 0, 20, 1, this, () -> predictionEngine.get() && simulateReactionTime.get());
    private final SliderValue thresholdToApplyReactionTime = new SliderValue("Threshold to apply reaction time", 0.1f, 0.01f, 1, 0.01f, this, () -> predictionEngine.get() && simulateReactionTime.get());
    private final SliderValue thresholdForDirectionConfidence = new SliderValue("Threshold for direction confidence", 0.1f, 0.01f, 0.5f, 0.01f, this, () -> predictionEngine.get() && simulateReactionTime.get());
    private final SliderValue extraPrediction = new SliderValue("Extra reaction prediction", 1, 0, 1, 0.01f, this, () -> predictionEngine.get() && simulateReactionTime.get());

    private final BoolValue jitter = new BoolValue("Jitter", false, this);
    private final DoubleSliderValue jitterFactor = new DoubleSliderValue("Jitter factor", 0.5f, 0.5f, 0, 1, 0.01f, this, jitter::get);
    private final BoolValue interpolateJitterVec = new BoolValue("Interpolate jitter vec", false, this, jitter::get);

    private final ModeValue lookVec = new ModeValue("Look vec", new String[]{"Client", "Server", "Normalised"}, "Normalised", this);

    private final ModeValue offsetMode = new ModeValue("Offset mode", new String[]{"None", "Gaussian", "Noise", "Advanced"}, "None", this);

    private final ModeValue advancedBase = new ModeValue("Base", new String[]{"Gaussian", "Noise"}, "Gaussian", this, () -> offsetMode.is("Advanced"));

    private final SliderValue oChance = new SliderValue("Offset chance", 75, 1, 100, 1, this, () -> !offsetMode.is("None") && !offsetMode.is("Blacklist"));

    private final DoubleSliderValue yawFactor = new DoubleSliderValue("Yaw factor", 0.25f, 0.25f, 0, 1, 0.01f, this, () -> !offsetMode.is("None") && !offsetMode.is("Blacklist") && !offsetMode.is("Noise20"));
    private final DoubleSliderValue pitchFactor = new DoubleSliderValue("Pitch factor", 0.25f, 0.25f, 0, 1, 0.01f, this, () -> !offsetMode.is("None") && !offsetMode.is("Blacklist") && !offsetMode.is("Noise20"));
    private final BoolValue interpolateVec = new BoolValue("Interpolate vec", false, this, () -> !offsetMode.is("None") && !offsetMode.is("Blacklist") && !offsetMode.is("Noise20"));
    private final SliderValue amount = new SliderValue("Amount", 0.5f, 0.01f, 1, 0.01f, this, () -> interpolateVec.get() && interpolateVec.canDisplay());

    private final SliderValue tolerance = new SliderValue("Tolerance", 0.05f, 0.01f, 0.1f, 0.01f, this, () -> offsetMode.is("Advanced"));

    private final ModeValue targetMode = new ModeValue("Target selection mode", new String[]{"Single", "Switch"}, "Single", this);
    private final ModeValue targetPriority = new ModeValue("Target Priority", new String[]{"None", "Distance", "Health", "HurtTime", "RayTrace"}, "Distance", this, () -> targetMode.is("Single"));
    private final SliderValue targetSwitchDelay = new SliderValue("Target switch delay", 500, 50, 1000, 50, this, () -> targetMode.is("Switch"));

    private final BoolValue targetESP = new BoolValue("Target ESP", false, this);

    public KillAura() {
        predictionEngine.setDescription("Makes your aim look more legit in certain circumstances.");
        offsetMode.setDescription("Randomly offsets your current aim point.");
        interpolateVec.setDescription("Smooth out the offset by x amount.");
        targetESP.setDescription("Renders a cool circle around the current target.");
        hitboxQuality.setDescription("The target's hitbox is split into multiple points to allow for a better overall point finder. More quality means more points generated.");
    }

    private final TimerUtil lastSwitchTime = new TimerUtil();
    public static EntityLivingBase target;
    public static boolean isBlocking = false;
    public Vec3 currentVec;
    private double lastXOffset;
    private double lastYOffset;
    private double lastZOffset;
    private boolean shouldRandomize;
    private Vec3 offsetVec = Vec3.ZERO;
    private float[] normalisedRot;
    private double finalXZTrim;
    private double xzRandShrinkThing;
    private int ticks;
    public static boolean isRealBlocking;

    @Override
    public void onEnable() {
        lastSwitchTime.reset();
        currentVec = Vec3.ZERO;
        offsetVec = Vec3.ZERO;
        ticks = 0;
    }

    @Override
    public void onDisable() {
        if (isRealBlocking) {
            setBlocking(false, true);
        }

        isRealBlocking = false;
        isBlocking = false;
        release();
        target = null;
        PredictionEngine.reset();
        normalisedRot = null;
        currentVec = null;
        offsetVec = null;
        PointFinder.allHitboxPoints.clear();
        PointFinder.hitboxPoints.clear();
        PointFinder.invalidHitboxPoints.clear();
    }

    public EntityLivingBase findTarget() {
        List<EntityLivingBase> targets = PlayerUtils.getTargets(searchRange.get() + 0.4);

        if (targets.isEmpty()) {
            return null;
        }

        if (!targets.stream().filter(mc.thePlayer::canEntityBeSeen).toList().isEmpty()) {
            targets.removeIf(target -> !mc.thePlayer.canEntityBeSeen(target));
        }

        MovingObjectPosition cameraDirectionMOP = PlayerUtils.getMouseOver();
        targets.sort(Comparator.comparingDouble(entity -> switch (targetPriority.get()) {
            case "Distance" -> PlayerUtils.getDistanceToEntityBox(entity);
            case "Health" -> PlayerUtils.getActualHealth(entity);
            case "HurtTime" -> entity.hurtTime;
            case "RayTrace" -> PlayerUtils.getCustomDistanceToEntityBox(cameraDirectionMOP.hitVec, entity);
            default -> 0;
        }));

        return targets.get(0);
    }

    private EntityLivingBase findNextTarget() {
        List<EntityLivingBase> targets = PlayerUtils.getTargets(searchRange.get() + 0.4);
        boolean shouldCheckVisible = targets.isEmpty() || !targets.stream().filter(mc.thePlayer::canEntityBeSeen).toList().isEmpty();

        if (shouldCheckVisible) {
            targets.removeIf(target -> !mc.thePlayer.canEntityBeSeen(target));
        }

        if (targets.isEmpty()) {
            return null;
        }

        if (target != null) {
            return targets.get((targets.indexOf(target) + 1) % targets.size());
        } else {
            targets.sort(Comparator.comparingDouble(PlayerUtils::getDistanceToEntityBox));
            return targets.get(0);
        }
    }

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        setTag(targetMode.get());

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        switch (targetMode.get()) {
            case "Single":
                target = findTarget();
                break;
            case "Switch":
                if (lastSwitchTime.hasTimeElapsed(targetSwitchDelay.get()) || target == null || PlayerUtils.getDistanceToEntityBox(target) > searchRange.get()) {
                    target = findNextTarget();
                    lastSwitchTime.reset();
                }
                break;
        }

        if (target != null) {
            if (isTargetValid()) {
                clickManager.click(attackRange.get(), target);

                if (PlayerUtils.getDistanceToEntityBox(target) > autoBlockRange.get() && isRealBlocking) {
                    setBlocking(false, true);
                }
            } else {
                target = null;
            }
        } else {
            PredictionEngine.reset();
            normalisedRot = null;

            if (isRealBlocking) {
                setBlocking(false, true);
            }
            release();
        }
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (target != null) {
            AutoRod autoRod = getModule(AutoRod.class);
            if (autoRod.rotating && autoRod.overrideAuraRots.get() && autoRod.isEnabled()) {
                return;
            }

            if (PlayerUtils.getDistanceToEntityBox(target) <= searchRange.get()) {
                rotationManager.setRotation(getRotations());
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        shouldRandomize = rand.nextInt(100) <= oChance.get();
        xzRandShrinkThing = Math.random();
    }

    @EventTarget
    public void onRenderTick(Render3DEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (target != null && targetESP.get()) {
            RenderUtils.drawTargetCircle(target);
        }
    }

    private boolean isTargetValid() {
        return !target.isDead && !(PlayerUtils.getDistanceToEntityBox(target) > searchRange.get()) && target.getEntityWorld() == mc.thePlayer.getEntityWorld();
    }

    @EventTarget
    public void onMouseOver(MouseOverEvent e) {
        e.setRange(attackRange.get());
    }

    public boolean alwaysPreAttack(boolean willAttack) {
        if (canAutoBlock()) {
            if (autoBlockMode.is("Blink")) {
                if (ticks == 0) {
                    BlinkComponent.blinking = true;
                    setBlocking(false);
                    willAttack = false;
                }

                ticks++;
            }
        } else {
            setBlocking(false, true);
            release();
        }

        return willAttack;
    }

    public void preAttack() {
        if (canAutoBlock()) {
            switch (autoBlockMode.get()) {
                case "None", "Release", "Vanilla", "Full release", "Slot":
                    setBlocking(false);
                    break;
                case "NCP":
                    isRealBlocking = true;
                case "Fake":
                    isBlocking = true;
                    break;
                case "Force":
                    setBlocking(true);
                    break;
            }
        } else {
            setBlocking(false, true);
        }
    }

    public void postAttack() {
        if (canAutoBlock()) {
            switch (autoBlockMode.get()) {
                case "Release", "Full release", "Vanilla", "Slot":
                    setBlocking(true);
                    break;
                case "Blink":
                    if (ticks > 1) {
                        mc.playerController.isPlayerRightClickingOnEntity(mc.thePlayer, mc.objectMouseOver.entityHit, mc.objectMouseOver);
                        mc.playerController.interactWithEntitySendPacket(mc.thePlayer, mc.objectMouseOver.entityHit);

                        release();
                        setBlocking(true);
                    }
                    break;
            }
        } else {
            setBlocking(false, true);
            release();
        }
    }

    private void release() {
        if (BlinkComponent.blinking) {
            BlinkComponent.dispatch(true);
        }
        ticks = 0;
    }

    private void setBlocking(boolean state) {
        setBlocking(state, false);
    }

    private void setBlocking(boolean state, boolean reset) {
        if (mc.currentScreen != null) return;

        if (state != isRealBlocking) {
            boolean shouldInteract = autoBlockMode.is("Release") || autoBlockMode.is("NCP") || autoBlockMode.is("Force") || autoBlockMode.is("Blink") || autoBlockMode.is("Vanilla");

            if (interact.get() && shouldInteract) {
                mc.playerController.isPlayerRightClickingOnEntity(mc.thePlayer, mc.objectMouseOver.entityHit, mc.objectMouseOver);
                mc.playerController.interactWithEntitySendPacket(mc.thePlayer, mc.objectMouseOver.entityHit);
            }

            switch (autoBlockMode.get()) {
                case "Release", "NCP", "Force", "Blink", "Vanilla":
                    if (state && shouldBlock()) {
                        sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                        isRealBlocking = true;
                    } else {
                        sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        isRealBlocking = false;
                    }
                    break;
                case "Full release":
                    if (state && shouldBlock()) {
                        if (mc.thePlayer.ticksExisted % 2 == 0) {
                            mc.playerController.interactWithEntitySendPacket(mc.thePlayer, target);
                            sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                            isRealBlocking = true;
                        }
                    } else {
                        sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        isRealBlocking = false;
                    }
                    break;
                case "Slot":
                    if (state && shouldBlock()) {
                        sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        sendPacketNoEvent(new C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9));
                        sendPacketNoEvent(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                        sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                        isRealBlocking = true;
                    } else {
                        sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        isRealBlocking = false;
                    }
                    break;
                default:
                    if (state && shouldBlock()) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                        isRealBlocking = true;
                    } else {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                        isRealBlocking = false;
                    }
                    break;
            }

            isBlocking = state && shouldBlock();
        }

        if (alwaysRenderBlocking.get()) {
            isBlocking = !reset;
        }
    }

    private boolean shouldBlock() {
        return reblockTicks.get() == 0 || mc.thePlayer.ticksExisted % (reblockTicks.get() + 1) != 0;
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (autoBlockMode.is("NCP") && canAutoBlock()) {
            if (e.isPre()) {
                sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            }

            if (e.isPost()) {
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem());
            }
        }
    }

    private boolean canAutoBlock() {
        return PlayerUtils.isHoldingSword() && autoBlock.get() && target != null && PlayerUtils.getDistanceToEntityBox(target) <= autoBlockRange.get();
    }

    private float[] getRotations() {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1);

        double speedShrinkFactor = Math.min(xzTrim.get(), Math.max(MoveUtils.getSpeedPosBased(mc.thePlayer) * 0.5, MoveUtils.getSpeedPosBased(target) * 0.5)) + (xzRandShrinkThing * randAdd.get());
        finalXZTrim = dynamicTrim.get() ? speedShrinkFactor : xzTrim.get();
        double finalYTrim = dynamicTrim.get() ? Math.abs(mc.thePlayer.motionY) : yTrim.get();

        AxisAlignedBB boundingBox;
        if (predictionEngine.get()) {
            boundingBox = PredictionEngine.simulatePredictions(
                    target,
                    attackRange.get(),
                    simulateReactionTime.get(),
                    thresholdForDirectionConfidence.get(),
                    thresholdToApplyReactionTime.get(),
                    reactionTime.get(),
                    extraPrediction.get()
            );
        } else {
            boundingBox = target.getHitbox();
        }

        boundingBox = boundingBox.contract(finalXZTrim, finalYTrim, finalXZTrim);

        if (mc.thePlayer.getHitbox().intersectsWith(boundingBox)) {
            AxisAlignedBB aoao = mc.thePlayer.getHitbox();

            double overlapX = Math.min(aoao.maxX, boundingBox.maxX) - Math.max(aoao.minX, boundingBox.minX);
            double overlapZ = Math.min(aoao.maxZ, boundingBox.maxZ) - Math.max(aoao.minZ, boundingBox.minZ);

            boundingBox = boundingBox.contract(overlapX / 2.0, 0, overlapZ / 2.0);
        }

        AxisAlignedBB bb = boundingBox;

        PointFinder.findPoints(boundingBox, (int) (PointFinder.pointCount * hitboxQuality.get()));
        List<Vec3> hitboxPoints = PointFinder.hitboxPoints;

        double playerToTarget = mc.thePlayer.getDistance(target.getPrevPositionVector()) - mc.thePlayer.getDistance(target.getPositionVector());
        double targetToPlayer = target.getDistance(mc.thePlayer.getPrevPositionVector()) - target.getDistance(mc.thePlayer.getPositionVector());
        double pred = (playerToTarget + targetToPlayer) / 2;
        boolean outOfRange = PlayerUtils.getDistanceToEntityBox(target) > attackRange.get();

        double bbHeight = (bb.maxY - bb.minY);
        double headSize = bbHeight / 4.5F;
        double torsoSize = bbHeight / 2.75F;

        AxisAlignedBB head = new AxisAlignedBB(bb.minX, bb.maxY - headSize, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
        AxisAlignedBB torso = new AxisAlignedBB(bb.minX, bb.minY + torsoSize, bb.minZ, bb.maxX, bb.maxY - headSize, bb.maxZ);
        AxisAlignedBB feet = new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + torsoSize, bb.maxZ);

        boolean headHittable = false;
        boolean torsoHittable = false;
        boolean feetHittable = false;

        double range = attackRange.get() + outOfRangeBuffer.get() + pred + finalXZTrim;
        switch (preferredBodyPart.get()) {
            case "Head":
                for (Vec3 vec : head.getVertices()) {
                    if (eyes.distanceTo(vec) < range) {
                        headHittable = true;
                        break;
                    }
                }
                break;
            case "Torso":
                for (Vec3 vec : torso.getVertices()) {
                    if (eyes.distanceTo(vec) < range) {
                        torsoHittable = true;
                        break;
                    }
                }
                break;
            case "Feet":
                for (Vec3 vec : feet.getVertices()) {
                    if (eyes.distanceTo(vec) < range) {
                        feetHittable = true;
                        break;
                    }
                }
                break;
        }

        boolean finalHeadHittable = headHittable;
        boolean finalTorsoHittable = torsoHittable;
        boolean finalFeetHittable = feetHittable;

        hitboxPoints.removeIf(p -> {
            boolean invalid = !isValidPoint(p, bb, pred, outOfRange, finalHeadHittable, finalTorsoHittable, finalFeetHittable);
            if (invalid) PointFinder.invalidHitboxPoints.add(p);
            return invalid;
        });

        List<Vec3> allHitboxPoints = PointFinder.allHitboxPoints;
        allHitboxPoints.removeIf(p -> pointInBlacklistedPos(p, bb, finalHeadHittable, finalTorsoHittable, finalFeetHittable));

        Vec3 vec;
        Vec3 lookDir = (switch (lookVec.get()) {
            case "Normalised" ->
                    normalisedRot == null ? mc.thePlayer.getLook(1) : mc.thePlayer.getLookCustom(normalisedRot[0], normalisedRot[1]);
            case "Server" ->
                    mc.thePlayer.getLookCustom(RotationHandler.currentRotation[0], RotationHandler.currentRotation[1]);
            default -> mc.thePlayer.getLookCustom(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }).normalize();

        if (!hitboxPoints.isEmpty()) {
            hitboxPoints.sort((a, b) -> {
                double da = a.subtract(eyes).crossProduct(lookDir).lengthVector();
                double db = b.subtract(eyes).crossProduct(lookDir).lengthVector();
                return Double.compare(da, db);
            });

            if (blacklistedBoxPositions.isEnabled("Bad hitVec") || blacklistedBoxPositions.isEnabled("Heuristic")) {
                Vec3 bhv = hitboxPoints.stream()
                        .min(Comparator.comparingDouble(p -> p.distanceTo(eyes)))
                        .orElse(bb.getCenter());
//            float[] toBhv = RotationUtils.getRotations(bhv);

                if (blacklistedBoxPositions.isEnabled("Heuristic")) {
                    hitboxPoints.removeIf(p -> {
                                if (p.distanceTo(bhv) < heuristicBuffer.get()) return true;
                                if (Math.abs(p.yCoord - bhv.yCoord) < heuristicBuffer.get() / 2) return true;

//                            float[] toPoint = RotationUtils.getRotations(p);
//                            if (RotationUtils.getAngleDifference(toBhv[0], toPoint[0]) < heuristicBuffer.get() * 2) return true;
//                            if (Math.abs(p.xCoord - bhv.xCoord) < heuristicBuffer.get() / 2 || Math.abs(p.zCoord - bhv.zCoord) < heuristicBuffer.get() / 2) return true;
                                return false;
                            }
                    );
                }

                if (blacklistedBoxPositions.isEnabled("Bad hitVec")) {
                    hitboxPoints.removeIf(p -> p.distanceTo(bhv) > badHitVecBuffer.get());
                }
            }

            if (hitboxPoints.isEmpty()) {
                vec = getBackupVec(allHitboxPoints, eyes, lookDir);
            } else {
                vec = hitboxPoints.get(0);
            }
        } else {
            vec = getBackupVec(allHitboxPoints, eyes, lookDir);
        }

        normalisedRot = RotationUtils.getRotations(vec);

        float[] jitterOffset = new float[]{0, 0};

        if (jitter.get()) {
            jitterOffset = JitterHandler.calculateJitter(jitterFactor.random(), interpolateJitterVec.get());
        }

        double minXZ = -0.5;
        double maxXZ = 0.5;
        double minY = -0.5;
        double maxY = 0.5;

        double yawFactor = this.yawFactor.random();
        double pitchFactor = this.pitchFactor.random();

        double meanXZ = (minXZ + maxXZ) / 2;
        double stdDevXZ = (maxXZ - minXZ) / 4;
        double meanY = (minY + maxY) / 2;
        double stdDevY = (maxY - minY) / 4;

        if (shouldRandomize) {
            switch (offsetMode.get()) {
                case "Gaussian": {
                    double xOffset = ThreadLocalRandom.current().nextGaussian(meanXZ, stdDevXZ) * yawFactor;
                    double yOffset = ThreadLocalRandom.current().nextGaussian(meanY, stdDevY) * pitchFactor;
                    double zOffset = ThreadLocalRandom.current().nextGaussian(meanXZ, stdDevXZ) * yawFactor;

                    offsetVec = MathUtils.interpolate(offsetVec, new Vec3(xOffset, yOffset, zOffset), interpolateVec.get() ? amount.get() : 1);

                    lastXOffset = xOffset;
                    lastYOffset = yOffset;
                    lastZOffset = zOffset;
                }
                break;
                case "Noise": {
                    double xOffset = MathUtils.randomizeDouble(minXZ, maxXZ) * yawFactor;
                    double yOffset = MathUtils.randomizeDouble(minY, maxY) * pitchFactor;
                    double zOffset = MathUtils.randomizeDouble(minXZ, maxXZ) * yawFactor;

                    offsetVec = MathUtils.interpolate(offsetVec, new Vec3(xOffset, yOffset, zOffset), interpolateVec.get() ? amount.get() : 1);

                    lastXOffset = xOffset;
                    lastYOffset = yOffset;
                    lastZOffset = zOffset;
                }
                break;
                case "Advanced": {
                    Vec3 lastOffset = new Vec3(lastXOffset, lastYOffset, lastZOffset);

                    if (Math.abs(offsetVec.distanceTo(lastOffset)) < tolerance.get()) {
                        double xOffset = 0;
                        double yOffset = 0;
                        double zOffset = 0;

                        switch (advancedBase.get()) {
                            case "Gaussian":
                                xOffset = ThreadLocalRandom.current().nextGaussian(meanXZ, stdDevXZ) * yawFactor;
                                yOffset = ThreadLocalRandom.current().nextGaussian(meanY, stdDevY) * pitchFactor;
                                zOffset = ThreadLocalRandom.current().nextGaussian(meanXZ, stdDevXZ) * yawFactor;
                                break;
                            case "Noise":
                                xOffset = MathUtils.randomizeDouble(minXZ, maxXZ) * yawFactor;
                                yOffset = MathUtils.randomizeDouble(minY, maxY) * pitchFactor;
                                zOffset = MathUtils.randomizeDouble(minXZ, maxXZ) * yawFactor;
                                break;
                        }

                        offsetVec = MathUtils.interpolate(offsetVec, new Vec3(xOffset, yOffset, zOffset), interpolateVec.get() ? amount.get() : 1);

                        lastXOffset = xOffset;
                        lastYOffset = yOffset;
                        lastZOffset = zOffset;
                    }
                }
                break;
                default:
                    offsetVec = Vec3.ZERO;
            }
        } else {
            if (!offsetMode.is("None")) {
                offsetVec = MathUtils.interpolate(offsetVec, new Vec3(lastXOffset, lastYOffset, lastZOffset), interpolateVec.get() ? amount.get() : 1);
            } else {
                offsetVec = Vec3.ZERO;
            }
        }

        shouldRandomize = false;

        Vec3 targetVec = new Vec3(vec.xCoord, vec.yCoord, vec.zCoord).add(offsetVec);

        boolean notVisible = !mc.thePlayer.canPosBeSeen(targetVec) && mc.thePlayer.canPosBeSeen(targetVec.subtract(offsetVec));

        if (notVisible) {
            targetVec = targetVec.subtract(offsetVec);
        }

        targetVec.xCoord = MathHelper.clamp_double(targetVec.xCoord, bb.minX, bb.maxX);
        targetVec.yCoord = MathHelper.clamp_double(targetVec.yCoord, bb.minY, bb.maxY);
        targetVec.zCoord = MathHelper.clamp_double(targetVec.zCoord, bb.minZ, bb.maxZ);

        if (predictionEngine.get()) {
            targetVec = targetVec.add(target.getMoveDeltaVector().multiply(Math.random() * 0.7));
        }

        currentVec = targetVec;
        float[] rot = RotationUtils.getRotations(currentVec, eyes);

        if (!notVisible) {
            rot[0] += jitterOffset[0];
            rot[1] += jitterOffset[1];
        }

        return rot;
    }

    private Vec3 getBackupVec(List<Vec3> allHitboxPoints, Vec3 playerPos, Vec3 lookDir) {
        allHitboxPoints.sort((a, b) -> {
            double da = a.subtract(playerPos).crossProduct(lookDir).lengthVector();
            double db = b.subtract(playerPos).crossProduct(lookDir).lengthVector();
            return Double.compare(da, db);
        });

        return allHitboxPoints.get(0);
    }

    private boolean isValidPoint(Vec3 point, AxisAlignedBB bb, double pred, boolean outOfRange, boolean headHittable, boolean torsoHittable, boolean feetHittable) {
        return canSeePoint(point, bb, pred, outOfRange) && !pointInBlacklistedPos(point, bb, headHittable, torsoHittable, feetHittable);
    }

    private boolean pointInBlacklistedPos(Vec3 point, AxisAlignedBB bb, boolean headHittable, boolean torsoHittable, boolean feetHittable) {
        double bbHeight = bb.maxY - bb.minY;
        double headSize = bbHeight / 4.5F;
        double torsoSize = bbHeight / 2.75F;

        boolean head = point.yCoord <= bb.maxY && point.yCoord > bb.maxY - headSize;
        boolean torso = point.yCoord <= bb.maxY - headSize && point.yCoord >= bb.minY + torsoSize;
        boolean feet = point.yCoord >= bb.minY && point.yCoord < bb.minY + torsoSize;

        if (headHittable && !head) {
            return true;
        }

        if (torsoHittable && !torso) {
            return true;
        }

        if (feetHittable && !feet) {
            return true;
        }

        if (blacklistedBoxPositions.isEnabled("Head") && head) {
            return true;
        }

        if (blacklistedBoxPositions.isEnabled("Torso") && torso) {
            return true;
        }

        if (blacklistedBoxPositions.isEnabled("Feet") && feet) {
            return true;
        }

        if (blacklistedBoxPositions.isEnabled("Heuristic")) {
            return Math.abs(point.yCoord - bb.getCenter().yCoord - 0.2) < heuristicBuffer.get() / 2;
        }

        return false;
    }

    private boolean canSeePoint(Vec3 point, AxisAlignedBB bb, double pred, boolean outOfRange) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1);
        MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(eyePos, point, false, false, false);
        MovingObjectPosition movingobjectposition = bb.calculateIntercept(eyePos, point);

        if (movingobjectposition != null) {
            // point isn't visible
            if (movingobjectposition.hitVec.distanceTo(point) > 0.01) {
                return false;
            }
        }

        return hit == null && (eyePos.distanceTo(point) < attackRange.get() + outOfRangeBuffer.get() + pred + finalXZTrim || outOfRange);
    }
}