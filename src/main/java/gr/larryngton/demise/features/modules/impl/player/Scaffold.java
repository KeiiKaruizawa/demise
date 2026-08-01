package gr.larryngton.demise.features.modules.impl.player;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventPriority;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.player.*;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.modules.impl.movement.Speed;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.features.values.impl.*;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.SpoofSlotUtils;
import gr.larryngton.demise.utils.player.ItemUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.clicking.ScaffoldClickHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationManager;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.aimpoint.PointFinder;
import gr.larryngton.demise.utils.render.RenderUtils;
import lombok.AllArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.lwjglx.input.Keyboard;

import java.awt.*;
import java.util.*;
import java.util.List;

@ModuleInfo(name = "Scaffold", description = "Automatically places blocks bellow you.")
public class Scaffold extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Breezily", "Telly", "Snap"}, "Normal", this);
    private final SliderValue straightTellyTicks = new SliderValue("Straight telly ticks", 4, 0, 5, this, () -> mode.is("Telly"));
    private final SliderValue diagonalTellyTicks = new SliderValue("Diagonal telly ticks", 2, 0, 5, this, () -> mode.is("Telly"));
    private final BoolValue instantRotateBack = new BoolValue("Instant rotate back", false, this, () -> mode.is("Telly") || mode.is("Snap"));
    private final ModeValue rotations = new ModeValue("Rotations", new String[]{"Normal", "Test-normal", "Center", "GodBridge", "Custom"}, "Normal", this, () -> !mode.is("Breezily"));

    private final ModeValue rotationBase = new ModeValue("Rotation base", new String[]{"Nearest to current", "Sideways", "Reverse", "Rounded"}, "Nearest", this, () -> rotations.is("Normal") && rotations.canDisplay());

    private final BoolValue staticify = new BoolValue("Static-ify", false, this, () -> !rotations.is("Normal") && !rotations.is("Center") && rotations.canDisplay());

    private final SliderValue customYawLeft = new SliderValue("Left yaw", 180, 0, 360, 1, this, () -> rotations.is("Custom") && rotations.canDisplay());
    private final SliderValue customYawRight = new SliderValue("Right yaw", 180, 0, 360, 1, this, () -> rotations.is("Custom") && rotations.canDisplay());
    private final SliderValue customYawDiagonal = new SliderValue("Diagonal yaw", 180, 0, 360, 1, this, () -> rotations.is("Custom") && rotations.canDisplay());
    private final SliderValue customPitchStraight = new SliderValue("Straight pitch", 80, -90, 90, 1, this, () -> rotations.is("Custom") && staticify.get() && rotations.canDisplay());
    private final SliderValue customPitchDiagonal = new SliderValue("Diagonal pitch", 80, -90, 90, 1, this, () -> rotations.is("Custom") && staticify.get() && rotations.canDisplay());

    private final SliderValue customYawLeftJump = new SliderValue("Left jumping yaw", 180, 0, 360, 1, this, () -> rotations.is("Custom") && rotations.canDisplay());
    private final SliderValue customYawRightJump = new SliderValue("Right jumping yaw", 180, 0, 360, 1, this, () -> rotations.is("Custom") && rotations.canDisplay());
    private final SliderValue customYawDiagonalJump = new SliderValue("Diagonal jumping yaw", 180, 0, 360, 1, this, () -> rotations.is("Custom") && rotations.canDisplay());
    private final SliderValue customPitchStraightJump = new SliderValue("Straight jumping pitch", 80, -90, 90, 1, this, () -> rotations.is("Custom") && staticify.get() && rotations.canDisplay());
    private final SliderValue customPitchDiagonalJump = new SliderValue("Diagonal jumping pitch", 80, -90, 90, 1, this, () -> rotations.is("Custom") && staticify.get() && rotations.canDisplay());

    private final BoolValue clutch = new BoolValue("Clutch", false, this);
    private final ModeValue clutchRotMode = new ModeValue("Clutch rotation mode", new String[]{"Normal", "Center"}, "Center", this, clutch::get);
    private final MultiBoolValue instantRotsCriteria = new MultiBoolValue("Instant rot criteria", Arrays.asList(
            new BoolValue("Always", false),
            new BoolValue("On miss", false),
            new BoolValue("Predict", false)
    ), this, clutch::get);

    private final RotationManager rotationManager = new RotationManager(this);

    private final SliderValue yawUpdateDistance = new SliderValue("Yaw update distance", 0.0F, 0.0F, 0.5F, 0.01F, this);
    private final SliderValue pitchUpdateDistance = new SliderValue("Pitch update distance", 0.0F, 0.0F, 0.5F, 0.01F, this);

    public final ModeValue sprintMode = new ModeValue("Sprint mode", new String[]{"Normal", "Silent", "Ground", "Air", "Omni", "None"}, "Normal", this);
    private final ModeValue speedMode = new ModeValue("Speed mode", new String[]{"Tick", "Exempt", "Diagonal", "None"}, "None", this);

    public final BoolValue swing = new BoolValue("Swing", true, this);
    private final BoolValue simulateClicking = new BoolValue("Simulate clicking", false, this);
    private final DoubleSliderValue cps = new DoubleSliderValue("CPS", 12, 16, 1, 40, 1, this, simulateClicking::get);
    private final ModeValue randomisationMode = new ModeValue("Randomisation mode", new String[]{"Stabilised", "Even", "Jitter"}, "Stabilised", this, simulateClicking::get);
    private final BoolValue allowWrongPlace = new BoolValue("Allow wrong place", false, this, simulateClicking::get);
    private final BoolValue clickOnMiss = new BoolValue("Click on miss", false, this, simulateClicking::get);

    private final BoolValue placeDelay = new BoolValue("Use place delay", false, this, () -> !simulateClicking.get());
    private final DoubleSliderValue delay = new DoubleSliderValue("Delay", 0, 0, 0, 10, 1, this, () -> !simulateClicking.get() && placeDelay.get());
    private final DoubleSliderValue delayInAir = new DoubleSliderValue("Delay in air", 0, 0, 0, 10, 1, this, () -> !simulateClicking.get() && placeDelay.get());

    private final ModeValue rayTraceMode = new ModeValue("Raytrace mode", new String[]{"None", "Basic", "Normal", "Strict"}, "None", this, () -> !simulateClicking.get());

    private final SliderValue minStackSizeToPlace = new SliderValue("Min stack size to place", 1, 1, 32, 1, this);
    private final ModeValue ledgeMode = new ModeValue("Ledge mode", new String[]{"None", "Sneak", "Jump", "Pause", "Breezily"}, "None", this);
    private final BoolValue onlySneakOnDiagonal = new BoolValue("Only sneak on diagonal", false, this, () -> !ledgeMode.is("None") && !ledgeMode.is("Sneak"));
    private final BoolValue sneakOnRotationDiffOnEnable = new BoolValue("Sneak on rotation diff when enabling", false, this, () -> !ledgeMode.is("None"));

    private final BoolValue optimiseDirection = new BoolValue("Optimise direction", false, this);

    private final BoolValue keepY = new BoolValue("Keep y", false, this);
    private final BoolValue onlySpeed = new BoolValue("Only speed", false, this, keepY::get);

    private final BoolValue targetBlockESP = new BoolValue("Target block esp", false, this);
    private final BoolValue shader = new BoolValue("Shader esp", false, this, targetBlockESP::get);

    private final BoolValue jump = new BoolValue("Jump", false, this);
    private final SliderValue blocksToJump = new SliderValue("Blocks to jump", 6, 1, 15, this, jump::get);
    private final BoolValue onlyStraight = new BoolValue("Only straight", true, this, jump::get);

    private final BoolValue sneak = new BoolValue("Sneak", false, this);
    private final SliderValue blocksToSneak = new SliderValue("Blocks to sneak", 7, 1, 15, this, sneak::get);
    private final SliderValue maxSneakTime = new SliderValue("Max sneak time", 500, 50, 1000, 50, this, sneak::get);
    private final BoolValue onlySneakOnGround = new BoolValue("Only sneak on ground", true, this, sneak::get);

    private final ModeValue tower = new ModeValue("Tower", new String[]{"None", "Vanilla", "Pull down", "NCP", "Air jump", "Polar"}, "None", this);
    private final SliderValue pullDownMotion = new SliderValue("Pull down moving motion", 0.95f, 0.5f, 1, 0.01f, this, () -> tower.is("Pull down"));
    private final SliderValue pullDownTick = new SliderValue("Pull down tick", 3, 2, 5, 1, this, () -> tower.is("Pull down"));

    public Scaffold() {
        staticify.setDescription("Makes your pitch static when enabled.");
        rayTraceMode.setDescription("None: places blocks bellow you without any checks, Normal: checks if you are aiming at the target block before placing, Strict: checks if you are aiming at the target block and at the correct face before placing.");
        rotationBase.setDescription("Scaffold will aim at the nearest valid point to the specified base.");
    }

    private double onGroundY;
    private int oldSlot = 0;
    public static int blocksPlaced;
    private int tellyTicks;
    public PlaceData data;
    private boolean isOnRightSide;
    private float yaw, pitch;
    private boolean shaderCalled;
    private float[] prevCalc;
    private PlayerUtils.PredictProcess selfPrediction;
    private final TimerUtil sneakTimer1 = new TimerUtil();
    private boolean firstUpdate;
    private final TimerUtil bufferTimer = new TimerUtil();
    private final TimerUtil placeTimer = new TimerUtil();
    private int nextPlaceDelay;
    private int nextPlaceDelayAir;
    private boolean firstTick;
    private int lastDirection;
    private boolean enableSneak;
    private long lastPlace;

    private static final EnumFacing[] FACINGS = {
            EnumFacing.EAST, EnumFacing.WEST,
            EnumFacing.NORTH, EnumFacing.SOUTH,
            EnumFacing.UP
    };

    private static final BlockPos[] OFFSETS = {
            new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(0, -1, 0)
    };

    @Override
    public void onEnable() {
        oldSlot = mc.thePlayer.inventory.currentItem;
        onGroundY = Math.floor(mc.thePlayer.posY);
        firstTick = true;
//        lastAimVec = mc.objectMouseOver.hitVec;
        enableSneak = sneakOnRotationDiffOnEnable.get() && sneakOnRotationDiffOnEnable.canDisplay();
    }

    @Override
    public void onDisable() {
        mc.thePlayer.inventory.currentItem = oldSlot;
        SpoofSlotUtils.stopSpoofing();

        blocksPlaced = 0;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        mc.thePlayer.omniSprint = false;
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        mc.thePlayer.inventory.currentItem = getBlockSlot();
        SpoofSlotUtils.startSpoofing(oldSlot);

        if (mc.thePlayer.onGround || mc.thePlayer.hurtTime != 0) {
            onGroundY = Math.floor(mc.thePlayer.posY);
        }

        boolean keepY = (this.keepY.get() && (!onlySpeed.get() || getModule(Speed.class).isEnabled())) && !mc.gameSettings.keyBindJump.isKeyDown();
        double posY = !keepY ? Math.floor(mc.thePlayer.posY) : onGroundY;

        if (towering()) {
            onGroundY = posY = Math.floor(mc.thePlayer.posY);
        }

        data = findBlock(new BlockPos(mc.thePlayer.posX, posY - 1, mc.thePlayer.posZ));

        if (mode.is("Telly")) {
            tellyTicks = MoveUtils.isMovingStraight() ? straightTellyTicks.getInt() : diagonalTellyTicks.getInt();
        }

        switch (sprintMode.get()) {
            case "Omni":
                mc.thePlayer.omniSprint = true;
            case "Normal", "Silent":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                break;
            case "Ground":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), mc.thePlayer.onGround);

                if (!mc.thePlayer.onGround) {
                    mc.thePlayer.setSprinting(false);
                }
                break;
            case "Air":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), !mc.thePlayer.onGround);

                if (mc.thePlayer.onGround) {
                    mc.thePlayer.setSprinting(false);
                }
                break;
            case "None":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
                mc.thePlayer.setSprinting(false);
                break;
        }

        if (tower.canDisplay() && !tower.is("None") && towering()) {
            blocksPlaced = 0;
        }

        mc.entityRenderer.getMouseOver(1);

        boolean rotateCheckYaw;
        int direction = Math.round(MoveUtils.getYawFromKeybind() / 45) * 45;
        AxisAlignedBB axis = mc.thePlayer.getEntityBoundingBox().offset(new Vec3(0, onGroundY - mc.thePlayer.getPositionVector().yCoord, 0));

        if (firstTick || (direction != lastDirection && !MoveUtils.getOnGroundPosBased(axis, 0.25))) {
            rotateCheckYaw = true;
            updateRotations(true, true);
            lastDirection = direction;
        } else {
            rotateCheckYaw = !MoveUtils.getOnGroundPosBased(axis, yawUpdateDistance.get());
            updateRotations(rotateCheckYaw, !MoveUtils.getOnGroundPosBased(axis, pitchUpdateDistance.get()));
        }

        boolean rotBack = mc.thePlayer.offGroundTicks >= tellyTicks;

        if (tellyTicks == 0) {
            rotBack = !mc.thePlayer.onGround;
        }

        float[] yPRot = new float[]{yaw, pitch};
        if ((rotateCheckYaw || !mode.is("Snap")) && (!mode.is("Telly") || mode.is("Telly") && rotBack)) {
            if (bufferTimer.hasTimeElapsed(50)) {
                rotationManager.setRotation(yPRot);
            } else {
                RotationHandler.setBasicRotation(yPRot, !rotationManager.getMoveCorrection().is("None"), 90, 90);
            }
        } else {
            if (instantRotateBack.get()) {
                RotationHandler.setBasicRotation(new float[]{MoveUtils.getYawFromKeybind(), 75}, !rotationManager.getMoveCorrection().is("None"), 180, 180);
                bufferTimer.reset();
            } else {
                rotationManager.setRotation(new float[]{MoveUtils.getYawFromKeybind(), 75});
            }
        }

        if (enableSneak && RotationUtils.getRotationDifference(yPRot, RotationHandler.currentRotation) < 5) {
            enableSneak = false;
        }
    }

    private void updateRotations(boolean updateYaw, boolean updatePitch) {
        float initialYaw = 0, initialPitch = 0;
        float[] rotationHandlerCurrentRotation = Objects.requireNonNullElse(RotationHandler.currentRotation, mc.thePlayer.getRotation());

        if (!mode.is("Breezily")) {
            switch (rotations.get()) {
                case "Test-normal": {
                    if (!getModule(KillAura.class).isEnabled()) {
                        Vec3 playerPos = mc.thePlayer.getPositionEyes(1);

                        PointFinder.findPoints(getTargetBlockPlane(data.blockPos.toVec3()));
                        List<Vec3> hitboxPoints = PointFinder.hitboxPoints;

                        if (!hitboxPoints.isEmpty()) {
                            Vec3 lookDir = mc.thePlayer.getLookCustom(yaw, pitch).normalize();

                            hitboxPoints.sort((a, b) -> {
                                double da = a.subtract(playerPos).crossProduct(lookDir).lengthVector();
                                double db = b.subtract(playerPos).crossProduct(lookDir).lengthVector();
                                return Double.compare(da, db);
                            });

                            float[] targetRot = RotationUtils.getRotations(hitboxPoints.get(0));
                            initialYaw = targetRot[0];
                            initialPitch = targetRot[1];
                        } else {
                            float[] targetRot = getNearestRotation(new float[]{rotationHandlerCurrentRotation[0], rotationHandlerCurrentRotation[1]});
                            initialYaw = targetRot[0];
                            initialPitch = targetRot[1];
                        }
                    } else {
                        float[] targetRot = getNearestRotation(new float[]{rotationHandlerCurrentRotation[0], rotationHandlerCurrentRotation[1]});
                        initialYaw = targetRot[0];
                        initialPitch = targetRot[1];
                    }
                }
                break;
                case "Normal": {
                    float targetYaw = rotationHandlerCurrentRotation[0];

                    switch (rotationBase.get()) {
                        case "Sideways":
                            float movingYaw = MoveUtils.isMoving() ? MoveUtils.getYawFromKeybind() - 180 : mc.thePlayer.rotationYaw - 180;

                            if (mc.thePlayer.onGround) {
                                isOnRightSide = Math.floor(mc.thePlayer.posX + Math.cos(Math.toRadians(movingYaw)) * 0.5) != Math.floor(mc.thePlayer.posX) ||
                                        Math.floor(mc.thePlayer.posZ + Math.sin(Math.toRadians(movingYaw)) * 0.5) != Math.floor(mc.thePlayer.posZ);
                            }

                            targetYaw = MoveUtils.isMovingStraight() ? (movingYaw + (isOnRightSide ? 45 : -45)) : movingYaw;
                            break;
                        case "Reverse":
                            targetYaw = MoveUtils.getYawFromKeybind() - 180;
                            break;
                        case "Rounded":
                            float yaw = MoveUtils.getYawFromKeybind() - 180;
                            targetYaw = Math.round(yaw / 45) * 45;
                    }

                    float[] targetRot = getNearestRotation(new float[]{targetYaw, rotationHandlerCurrentRotation[1]});
                    initialYaw = targetRot[0];
                    initialPitch = targetRot[1];
                }
                break;
                case "Center": {
                    Vec3 hitVec = RotationUtils.getVec3(data.blockPos, data.sideHit);

                    initialYaw = RotationUtils.getRotations(hitVec)[0];
                    initialPitch = RotationUtils.getRotations(hitVec)[1];
                }
                break;
                case "GodBridge": {
                    float movingYaw = MoveUtils.isMoving() ? MoveUtils.getYawFromKeybind() - 180 : mc.thePlayer.rotationYaw - 180;

                    if (mc.thePlayer.onGround) {
                        isOnRightSide = Math.floor(mc.thePlayer.posX + Math.cos(Math.toRadians(movingYaw)) * 0.5) != Math.floor(mc.thePlayer.posX) ||
                                Math.floor(mc.thePlayer.posZ + Math.sin(Math.toRadians(movingYaw)) * 0.5) != Math.floor(mc.thePlayer.posZ);
                    }

                    float yaw = MoveUtils.isMovingStraight() ? (movingYaw + (isOnRightSide ? 45 : -45)) : movingYaw;

                    initialYaw = Math.round(yaw / 45) * 45;

                    float pitch = MoveUtils.isMoving() ? 75.7f : 90f;
                    initialPitch = staticify.get() ? pitch : getNearestRotation(new float[]{initialYaw, pitch})[1];
                }
                break;
                case "Custom": {
                    float movingYaw = MoveUtils.isMoving() ? MoveUtils.getYawFromKeybind() - 180 : mc.thePlayer.rotationYaw - 180;

                    if (mc.thePlayer.onGround) {
                        isOnRightSide = Math.floor(mc.thePlayer.posX + Math.cos(Math.toRadians(movingYaw)) * 0.5) != Math.floor(mc.thePlayer.posX) ||
                                Math.floor(mc.thePlayer.posZ + Math.sin(Math.toRadians(movingYaw)) * 0.5) != Math.floor(mc.thePlayer.posZ);
                    }

                    float yaw = MoveUtils.isMovingStraight() ? (isOnRightSide ? customYawRight.get() : customYawLeft.get()) : customYawDiagonal.get();
                    float pitch = MoveUtils.isMovingStraight() ? customPitchStraight.get() : customPitchDiagonal.get();

                    if (mc.thePlayer.isJumping) {
                        yaw = MoveUtils.isMovingStraight() ? (isOnRightSide ? customYawRightJump.get() : customYawLeftJump.get()) : (customYawDiagonalJump.get());
                        pitch = MoveUtils.isMovingStraight() ? customPitchStraightJump.get() : customPitchDiagonalJump.get();
                    }

                    initialYaw = MoveUtils.getYawFromKeybind() - yaw;
                    initialPitch = staticify.get() ? pitch : getNearestRotation(new float[]{initialYaw, rotationHandlerCurrentRotation[1]})[1];
                }
                break;
            }
        } else {
            float yaw = MoveUtils.getYawFromKeybind() - 180;
            initialYaw = Math.round(yaw / 45) * 45;
            initialPitch = MoveUtils.isMoving() ? MoveUtils.isMovingStraight() ? 80 : 75.7f : 90;
        }

        float[] rotation = new float[]{initialYaw, initialPitch};

        if (clutch.get() && data != null) {
            MovingObjectPosition ray = PlayerUtils.rayTraceBlock(rotation, 4.5, 1);

            if (ray.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || (ray.getBlockPos() != null && !ray.getBlockPos().equalsBlockPos(data.blockPos))) {
                rotation = setClutchRot(rotation);
            }
        }

        if (updateYaw) {
            yaw = rotation[0];
        }

        if (updatePitch) {
            pitch = rotation[1];
        }

        firstTick = false;
    }

    private AxisAlignedBB getTargetBlockPlane(Vec3 block000) {
        double minX = block000.xCoord + 0.1;
        double minY = block000.yCoord + 0.1;
        double minZ = block000.zCoord + 0.1;
        double maxX = minX + 0.9;
        double maxY = minY + 0.9;
        double maxZ = minZ + 0.9;

        switch (data.sideHit) {
            case DOWN:
                maxY = minY;
                break;
            case UP:
                minY = maxY;
                break;
            case NORTH:
                maxZ = minZ;
                break;
            case SOUTH:
                minZ = maxZ;
                break;
            case WEST:
                maxX = minX;
                break;
            case EAST:
                minX = maxX;
                break;
        }

        return new AxisAlignedBB(
                minX, minY, minZ,
                maxX, maxY, maxZ
        );
    }

    private float[] setClutchRot(float[] initial) {
        float clutchYaw = 0, clutchPitch = 0;

        Vec3 hitVec = RotationUtils.getVec3(data.blockPos, data.sideHit);

        clutchPitch = switch (clutchRotMode.get()) {
            case "Normal" -> {
                clutchYaw = getNearestRotation(initial)[0];
                yield getNearestRotation(initial)[1];
            }
            case "Center" -> {
                clutchYaw = RotationUtils.getRotations(hitVec)[0];
                yield RotationUtils.getRotations(hitVec)[1];
            }
            default -> clutchPitch;
        };

        boolean isLeaningOffBlock = PlayerUtils.getBlock(data.blockPos.offset(data.sideHit)) instanceof BlockAir;
        boolean nextBlockIsAir = mc.theWorld.getBlockState(mc.thePlayer.getPosition().offset(EnumFacing.fromAngle(yaw), 1).down()).getBlock() instanceof BlockAir;

        boolean instantRotation = instantRotsCriteria.isEnabled("Predict") && isLeaningOffBlock && nextBlockIsAir;

        if (instantRotsCriteria.isEnabled("Always")) {
            instantRotation = true;
        }

        if (instantRotsCriteria.isEnabled("On miss")) {
            if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mc.objectMouseOver.getBlockPos().equalsBlockPos(data.blockPos)) {
                instantRotation = true;
            }
        }

        if (instantRotation) {
            rotationManager.instantRots = true;
        }

        return new float[]{clutchYaw, clutchPitch};
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPost()) {
            return;
        }

        switch (speedMode.get()) {
            case "Tick":
                if (mc.thePlayer.onGround) {
                    if (mc.thePlayer.ticksExisted % 15 == 0) {
                        mc.thePlayer.motionX *= 1.5 + Math.random() / 100D;
                        mc.thePlayer.motionZ *= 1.5 + Math.random() / 100D;
                    } else if (mc.thePlayer.ticksExisted - 2 % 15 == 0) {
                        mc.thePlayer.motionX *= 0.7;
                        mc.thePlayer.motionZ *= 0.7;
                    }
                }
                break;
            case "Exempt":
                if (mc.thePlayer.onGround) {
                    MoveUtils.strafe(mc.thePlayer.movementInput.moveStrafe == 0 ? 0.1591f : 0.1565f);
                }
                break;
            case "Diagonal":
                MoveUtils.useDiagonalSpeed();
                break;
        }

        if (towering() && PlayerUtils.blockNear(2)) {
            mc.thePlayer.jumpTicks = 0;

            switch (tower.get()) {
                case "Vanilla":
                    mc.thePlayer.motionY = 0.42;
                    break;
                case "NCP":
                    if (mc.thePlayer.posY % 1 <= 0.00153598) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                        mc.thePlayer.motionY = 0.42F;
                    } else if (mc.thePlayer.posY % 1 < 0.1 && mc.thePlayer.offGroundTicks != 0) {
                        mc.thePlayer.motionY = 0;
                        mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                    }
                    break;
                case "Pull down":
                    mc.thePlayer.motionX *= pullDownMotion.get();
                    mc.thePlayer.motionZ *= pullDownMotion.get();

                    if (mc.thePlayer.offGroundTicks == pullDownTick.getInt()) {
                        mc.thePlayer.motionY = -0.0784000015258789;
                    }
                    break;
                case "Air jump":
                    if (mc.thePlayer.ticksExisted % 4 == 0) {
                        mc.thePlayer.motionY = 0.42F;
                        e.setOnGround(true);
                    }
                    break;
                case "Polar":
                    double ground = mc.thePlayer.posY - MoveUtils.findGround(mc.thePlayer);

                    // i have no fucking idea why this works
                    if (!MoveUtils.isMovingMotion(mc.thePlayer) && mc.thePlayer.motionY < 0 && ground < 1.26) {
                        mc.thePlayer.motionY -= 0.091F;
                    }
                    break;
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        rotationManager.instantRots = false;

        if (!mode.is("Telly") || mode.is("Telly") && mc.thePlayer.offGroundTicks >= tellyTicks) {
            place(data.blockPos, data.sideHit, RotationUtils.getVec3(data.blockPos, data.sideHit));
        }
    }

    @EventTarget
    public void onGame(GameEvent e) {
        if (simulateClicking.get()) {
            ScaffoldClickHandler.initHandler(cps.getStart(), cps.getEnd(), ScaffoldClickHandler.RandomisationMode.valueOf(randomisationMode.get()), allowWrongPlace.get(), clickOnMiss.get());
        }
    }

    @EventTarget
    public void onMovementInput(MoveInputEvent e) {
        if (mode.is("Breezily")) {
            boolean isLeaningOffBlock = PlayerUtils.getBlock(data.blockPos.offset(data.sideHit)) instanceof BlockAir;
            if (isLeaningOffBlock) {
                breezily(e);
            }
        }

        if (jump.get() && MoveUtils.isMoving() && blocksPlaced % blocksToJump.get() == 0) {
            if (!onlyStraight.get() || MoveUtils.isMovingStraight()) {
                e.setJumping(true);
            }
        }

        if (mode.is("Telly")) {
            if (mc.thePlayer.onGround && !isEnabled(Speed.class) && MoveUtils.isMoving()) {
                e.setJumping(mc.thePlayer.isSprinting() || tellyTicks == 0);
            }
        }

        if (!ledgeMode.is("None") && MoveUtils.isMoving() && mc.thePlayer.onGround) {
            if (enableSneak) {
                e.setSneaking(true);
            }

            if (RotationUtils.getRotationDifference(RotationHandler.currentRotation, new float[]{yaw, pitch}) < 20) {
                Vec3 pp = selfPrediction.position;
                AxisAlignedBB predictedBB = new AxisAlignedBB(pp.xCoord - 0.5, pp.yCoord - 0.5, pp.zCoord - 0.5, pp.xCoord + 0.5, pp.yCoord + 0.5, pp.zCoord + 0.5);
                if (!MoveUtils.getOnGroundPosBased(predictedBB, 0.15)) {
                    String mode = !onlySneakOnDiagonal.get() || MoveUtils.isMovingStraight() ? ledgeMode.get() : "Sneak";

                    switch (mode) {
                        case "Sneak" -> e.setSneaking(true);
                        case "Jump" -> e.setJumping(true);
                        case "Pause" -> e.setCancelled(true);
                        case "Breezily" -> breezily(e);
                    }
                }
            }
        }

        if (sneak.get()) {
            if (!mc.thePlayer.onGround && onlySneakOnGround.get()) {
                return;
            }

            if (blocksToSneak.get() == 0) {
                firstUpdate = false;
            }

            if (blocksPlaced % blocksToSneak.get() == 0) {
                if (!firstUpdate) {
                    sneakTimer1.reset();
                    firstUpdate = true;
                }
            } else if (sneakTimer1.hasTimeElapsed(maxSneakTime.getInt())) {
                firstUpdate = false;
            }

            if (!sneakTimer1.hasTimeElapsed(maxSneakTime.getInt())) {
                e.setSneaking(true);
            }
        }
    }

    private void breezily(MoveInputEvent e) {
        BlockPos b = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
        if (MoveUtils.isMoving() && MoveUtils.isMovingStraight()) {
            switch (mc.thePlayer.getHorizontalFacing(yaw).getOpposite()) {
                case EAST -> {
                    if (b.getZ() + 0.5 > mc.thePlayer.posZ) {
                        e.setStrafe(1);
                    } else {
                        e.setStrafe(-1);
                    }
                }
                case WEST -> {
                    if (b.getZ() + 0.5 < mc.thePlayer.posZ) {
                        e.setStrafe(1);
                    } else {
                        e.setStrafe(-1);
                    }
                }
                case SOUTH -> {
                    if (b.getX() + 0.5 < mc.thePlayer.posX) {
                        e.setStrafe(1);
                    } else {
                        e.setStrafe(-1);
                    }
                }
                case NORTH -> {
                    if (b.getX() + 0.5 > mc.thePlayer.posX) {
                        e.setStrafe(1);
                    } else {
                        e.setStrafe(-1);
                    }
                }
            }
        }
    }

    @EventTarget
    @EventPriority(-100)
    public void onHighPriorityMoveInput(MoveInputEvent e) {
        if (optimiseDirection.get() && MoveUtils.isMoving() && MoveUtils.isMovingStraight()) {
            float yaw = Math.round(MoveUtils.getYawFromKeybind() / 45) * 45;
            BlockPos b = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
            float calculatedYaw = RotationUtils.getRotations(b.offset(EnumFacing.fromAngle(yaw)))[0];

            if (rotationManager.getMoveCorrection().is("Silent")) {
                RotationHandler.directionYaw = calculatedYaw;
            } else if (rotationManager.getMoveCorrection().is("None")) {
                MoveUtils.fixMovement(e, mc.thePlayer.rotationYaw, calculatedYaw);
            }
        }
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        selfPrediction = PlayerUtils.simulatePlayer(1, true);
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (targetBlockESP.get() && (!shaderCalled || !shader.get())) {
            RenderUtils.renderBlock(data.blockPos.offset(data.sideHit), getModule(Interface.class).color(0, 100), false, true);
        }

        shaderCalled = false;
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (targetBlockESP.get() && shader.get()) {
            Demise.INSTANCE.getShaderManager().addToAllQueue(() -> RenderUtils.draw2DCube(data.blockPos.offset(data.sideHit), Color.black.getRGB()));
        }

        shaderCalled = true;
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        setEnabled(false);
    }

    public boolean towering() {
        return tower.canDisplay() && Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
    }

    private int getBlockSlot() {
        int slot = mc.thePlayer.inventory.currentItem;

        if (getBlockCount() == 0) {
            return slot;
        }

        boolean containsValidBlockStack = hotbarContainsValidBlockStackSize();
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            if (itemStack != null) {
                if (itemStack.stackSize < minStackSizeToPlace.getInt() && containsValidBlockStack) {
                    continue;
                }

                if (itemStack.getItem() instanceof ItemBlock block && !ItemUtils.blacklistedBlocks.contains(block.getBlock())) {
                    slot = i;
                }
            }
        }

        return slot;
    }

    private boolean hotbarContainsValidBlockStackSize() {
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            if (itemStack != null) {
                if (itemStack.stackSize < minStackSizeToPlace.getInt()) {
                    continue;
                }

                return true;
            }
        }

        return false;
    }

    public int getBlockCount() {
        int blockCount = 0;

        for (int i = 36; i < 45; ++i) {
            if (!mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) continue;

            ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (!(is.getItem() instanceof ItemBlock && !ItemUtils.blacklistedBlocks.contains(((ItemBlock) is.getItem()).getBlock()))) {
                continue;
            }

            blockCount += is.stackSize;
        }

        return blockCount;
    }

    private boolean isPlaceable(Block block, BlockPos blockPos, EnumFacing facing) {
        return !isInteractable(block, blockPos) /*&& ScaffoldClickHandler.testForRightClickBasic(blockPos, facing)*/;
    }

    private boolean isInteractable(Block block, BlockPos blockPos) {
//        float[] rot = RotationUtils.getRotations(blockPos);
//        MovingObjectPosition ray = PlayerUtils.rayTraceBlock(rot, 8, 1);
//
//        float f = (float) (ray.hitVec.xCoord - (double) blockPos.getX());
//        float f1 = (float) (ray.hitVec.yCoord - (double) blockPos.getY());
//        float f2 = (float) (ray.hitVec.zCoord - (double) blockPos.getZ());
//
//        return block.onBlockActivated(mc.theWorld, ray.getBlockPos(), mc.theWorld.getBlockState(blockPos), mc.thePlayer, ray.sideHit, f, f1, f2);
        return false;
    }

    private void place(BlockPos pos, EnumFacing facing, Vec3 hitVec) {
        mc.entityRenderer.getMouseOver(1);
        MovingObjectPosition ray = mc.objectMouseOver;
        BlockPos targetPos = pos;
        Vec3 targetHitVec = hitVec;
        boolean shouldPlace = false;

        switch (rayTraceMode.get()) {
            case "None":
                shouldPlace = true;
                break;
            case "Basic":
                if (RotationUtils.getRotationDifference(RotationHandler.currentRotation, RotationUtils.getRotations(hitVec)) < 45 || ray.getBlockPos().equalsBlockPos(pos)) {
                    shouldPlace = true;
                }
            case "Normal":
                if (ray.getBlockPos().equalsBlockPos(pos)) {
                    targetPos = ray.getBlockPos();
                    shouldPlace = true;
                }
                break;
            case "Strict":
                if (ray.getBlockPos().equalsBlockPos(pos) && ray.sideHit.equals(facing)) {
                    targetPos = ray.getBlockPos();
                    targetHitVec = ray.hitVec;
                    shouldPlace = true;
                }
                break;
        }

        if (shouldPlace && ScaffoldClickHandler.testForRightClickBasic(targetPos, facing)) {
            if (placeTimer.hasTimeElapsed((mc.thePlayer.onGround ? nextPlaceDelay : nextPlaceDelayAir) * 50L) || !placeDelay.get()) {
                if (!simulateClicking.get()) {
                    if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), targetPos, facing, targetHitVec)) {
                        if (!swing.get()) {
                            sendPacket(new C0APacketAnimation());
                        } else {
                            mc.thePlayer.swingItem();
                        }
                        blocksPlaced++;
                        nextPlaceDelay = (int) delay.random();
                        nextPlaceDelayAir = (int) delayInAir.random();
                    }
                } // handled inside ScaffoldClickHandler
            }
        } else {
            placeTimer.reset();
        }
    }

    private PlaceData findBlock(BlockPos pos) {
        PlaceData immediateCheck = checkPosition(pos);
        if (immediateCheck != null) {
            return immediateCheck;
        }

        Vec3 predictedPos = mc.thePlayer.getPositionVector().add(mc.thePlayer.getMoveDeltaVector());
        predictedPos.yCoord = Math.floor(Math.min(mc.thePlayer.posY, predictedPos.yCoord));

        PlaceData best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for (int x = -6; x <= 6; x++) {
            for (int y = -6; y <= 0; y++) {
                for (int z = -6; z <= 6; z++) {
                    mutableBlockPos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);

                    PlaceData data = checkPosition(mutableBlockPos);
                    if (data == null) {
                        continue;
                    }

                    double dist = mutableBlockPos.distanceToNearestSq(predictedPos);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = data;
                    }
                }
            }
        }

        return best;
    }

    private PlaceData checkPosition(BlockPos blockPos) {
        World world = mc.theWorld;

        for (int i = 0; i < OFFSETS.length; i++) {
            BlockPos newPos = blockPos.add(OFFSETS[i]);
            Block block = world.getBlockState(newPos).getBlock();

            EnumFacing facing = FACINGS[i];
            if (block != Blocks.air && isPlaceable(block, newPos, facing)) {
                return new PlaceData(facing, newPos);
            }
        }

        return null;
    }

    private float[] getNearestRotation(float[] currentRot) {
        BlockPos blockPos = data.blockPos;
        EnumFacing face = data.sideHit;

        if (prevCalc != null && isValidRay(prevCalc, blockPos, face)) {
            return prevCalc;
        }

        float[] rotationHandlerCurrentRotation = Objects.requireNonNullElse(RotationHandler.currentRotation, mc.thePlayer.getRotation());
        if (isValidRay(rotationHandlerCurrentRotation, blockPos, face)) {
            return rotationHandlerCurrentRotation;
        }

        float[] bestRot = null;
        float bestDiff = Float.MAX_VALUE;

        List<Vec3> candidates = getCandidatePoints(blockPos, face);

        for (Vec3 pos : candidates) {
            float[] rot = RotationUtils.getRotations(pos);
            if (Float.isNaN(rot[0]) || Float.isNaN(rot[1])) continue;

            float diff = RotationUtils.getRotationDifference(currentRot, rot);
            if (diff < bestDiff) {
                if (isValidRay(rot, blockPos, face)) {
                    bestRot = rot;
                    bestDiff = diff;
                }
            }
        }

        float[] rot = bestRot != null ? bestRot : RotationUtils.getRotations(RotationUtils.getVec3(blockPos, face));
        prevCalc = rot;
        return rot;
    }

    private boolean isValidRay(float[] rot, BlockPos pos, EnumFacing face) {
        MovingObjectPosition ray = PlayerUtils.rayTraceBlock(rot, 4.5, 1);
        return ray != null && ray.getBlockPos().equalsBlockPos(pos) && ray.sideHit.equals(face);
    }

    private List<Vec3> getCandidatePoints(BlockPos blockPos, EnumFacing face) {
        List<Vec3> list = new ArrayList<>();
        Vec3i normal = face.getDirectionVec();
        double fx = normal.getX() > 0 ? 0.99 : 0.01;
        double fy = normal.getY() > 0 ? 0.99 : 0.01;
        double fz = normal.getZ() > 0 ? 0.99 : 0.01;

        double start = 0.1, end = 0.9;
        for (double u = start; u <= end; u += 0.01) {
            for (double v = start; v <= end; v += 0.01) {
                double x = blockPos.getX() + (normal.getX() != 0 ? fx : u);
                double y = blockPos.getY() + (normal.getY() != 0 ? fy : v);
                double z = blockPos.getZ() + (normal.getZ() != 0 ? fz : (normal.getX() == 0 ? u : v));

                list.add(new Vec3(x, y, z));
            }
        }

        return list;
    }

    @AllArgsConstructor
    public static class PlaceData {
        public EnumFacing sideHit;
        public BlockPos blockPos;
    }
}
