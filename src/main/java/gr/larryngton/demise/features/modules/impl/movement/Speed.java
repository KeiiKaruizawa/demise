package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.*;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import net.minecraft.block.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static gr.larryngton.demise.utils.player.MoveUtils.BUNNY_FRICTION;
import static gr.larryngton.demise.utils.player.MoveUtils.BUNNY_SLOPE;

@ModuleInfo(name = "Speed", description = "Makes you go faster.")
public class Speed extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Legit", "Strafe hop", "Custom", "NCP", "Verus", "Intave", "Vulcan", "Collide", "Mineland", "Polar", "Grim", "Tick", "Jokai", "Carpet", "Derp", "Azheng", "Jitter", "Vape"}, "Strafe hop", this);

    private final BoolValue abideFriction = new BoolValue("Abide friction", true, this, () -> mode.is("Custom"));
    private final SliderValue customSpeed = new SliderValue("Speed", 0.35f, 0, 1, 0.01f, this, () -> mode.is("Custom") && abideFriction.get());
    private final SliderValue randomizeSpeed = new SliderValue("Randomize speed", 0.0f, 0.0f, 10, 0.1f, this, () -> mode.is("Custom") && abideFriction.get());
    private final SliderValue motionMultiplier = new SliderValue("Motion multiplier", 1, 0, 5, 0.01f, this, () -> mode.is("Custom") && !abideFriction.get());
    private final SliderValue customJumpMotion = new SliderValue("Jump motion", 0.42f, 0, 1, 0.01f, this, () -> mode.is("Custom"));
    private final BoolValue pullDown = new BoolValue("PullDown", false, this, () -> mode.is("Custom"));
    private final SliderValue pullDownTick = new SliderValue("PullDown tick", 2, 0, 12, 1, this, () -> mode.is("Custom") && pullDown.get());
    private final SliderValue pullDownMotionMotion = new SliderValue("PullDown motion", 0.4f, 0, 1, 0.01f, this, () -> mode.is("Custom") && pullDown.get());
    private final SliderValue timerSpeed = new SliderValue("Timer speed", 1.0f, 0.1f, 10, 0.1f, this, () -> mode.is("Custom"));
    private final SliderValue timerRandom = new SliderValue("Timer random", 0.0f, 0.0f, 10, 0.1f, this, () -> mode.is("Custom"));

    private final BoolValue ground = new BoolValue("Ground strafe", true, this, () -> mode.is("Strafe hop") || mode.is("Custom"));
    private final SliderValue groundStrength = new SliderValue("Ground strength", 1, 0, 1, 0.01f, this, () -> (mode.is("Strafe hop") || mode.is("Custom")) && ground.get());
    private final BoolValue air = new BoolValue("Air strafe", true, this, () -> mode.is("Strafe hop") || mode.is("Custom"));
    private final SliderValue airStrength = new SliderValue("Air strength", 1, 0, 1, 0.01f, this, () -> (mode.is("Strafe hop") || mode.is("Custom")) && air.get());

    private final ModeValue ncpMode = new ModeValue("NCP mode", new String[]{"Low hop", "Fast hop", "Old hop", "7 tick", "BHop", "FBHop", "Max"}, "Fast hop", this, () -> mode.is("NCP"));
    private final SliderValue speedMulti = new SliderValue("Extra speed multiplier", 0.4f, 0f, 1f, 0.01f, this, () -> ncpMode.is("Old Hop") && ncpMode.canDisplay());

    private final ModeValue verusMode = new ModeValue("Verus mode", new String[]{"Low"}, "Low", this, () -> mode.is("Verus"));

    private final BoolValue customGravity = new BoolValue("Custom gravity", false, this, () -> mode.is("Intave"));
    private final ModeValue intaveMode = new ModeValue("Intave mode", new String[]{"Safe", "Fast"}, "Safe", this, () -> mode.is("Intave"));
    private final BoolValue timer = new BoolValue("Timer", false, this, () -> mode.is("Intave") && intaveMode.is("Fast"));
    private final SliderValue iBoostMulti = new SliderValue("Boost multiplier", 1, 0f, 1, 0.1f, this, () -> mode.is("Intave") && intaveMode.is("Safe"));

    private final SliderValue collideSpeed = new SliderValue("Collide speed", 0.08f, 0.01f, 0.08f, 0.01f, this, () -> mode.is("Collide"));

    private final ModeValue vulcanMode = new ModeValue("Vulcan mode", new String[]{"Hop", "Low"}, "Hop", this, () -> mode.is("Vulcan"));
    private final BoolValue allowAirStrafing = new BoolValue("Allow air strafing", false, this, () -> mode.is("Vulcan") && vulcanMode.is("Hop"));

    private final SliderValue ticks = new SliderValue("Ticks", 2, 1, 10, 1, this, () -> mode.is("Tick"));
    private final BoolValue onlyY = new BoolValue("Only Y", false, this, () -> mode.is("Tick"));

    private final BoolValue funny = new BoolValue("Funny", false, this, () -> mode.is("Polar"));

    private final ModeValue vapeMode = new ModeValue("Vape mode", new String[]{"Anticheat A", "Anticheat B"}, "Anticheat A", this, () -> mode.is("Vape"));

    private final BoolValue damageBoost = new BoolValue("Damage boost", false, this);
    private final SliderValue boostSpeed = new SliderValue("Boost speed", 0.5f, 0.01f, 1f, 0.01f, this, damageBoost::get);
    private final ModeValue yawOffsetMode = new ModeValue("Yaw offset", new String[]{"None", "Ground", "Air", "Constant"}, "Air", this);
    private final BoolValue minSpeedLimiter = new BoolValue("Min speed limiter", false, this);
    private final SliderValue minSpeed = new SliderValue("Min speed", 0.25f, 0, 1, 0.01f, this, minSpeedLimiter::get);
    private final SliderValue minMoveTicks = new SliderValue("Move ticks for limit", 15, 0, 40, 1, this, minSpeedLimiter::get);
    private final BoolValue printAirTicks = new BoolValue("Print airTicks", false, this);

    public Speed() {
        abideFriction.setDescription("True: move at a constant speed defined by the speed option, False: follow vanilla speed.");
        yawOffsetMode.setDescription("Ground: when on ground, aims at the dir you are moving at to strafe more, Air: automatically does 45deg strafes to gain more speed, Constant: always aims at the dir you are moving at.");
        funny.setDescription("Patched");
    }

    private int movingTicks;
    private final TimerUtil strafeTimer = new TimerUtil();
    private double yaw;
    private Vec3 predictedPos = Vec3.ZERO;
    private boolean state;
    private double speed;
    private double lastMotionY;
    private double lastSpeed;
    private int speedBuffer;
    private final TimerUtil timerTimer = new TimerUtil();
    private int level = 1;
    private double moveSpeed = 0.2873;
    private double lastDist;
    private int timerDelay;
    private float v;
    private int x = 0;

    private double z;
    private int parentX = 2;

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
        moveSpeed = getBaseMoveSpeed();
        level = 0;
    }

    @Override
    public void onEnable() {
        predictedPos = mc.thePlayer.getPositionVector();
        movingTicks = 0;
        level = !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, mc.thePlayer.motionY, 0.0)).isEmpty() || mc.thePlayer.isCollidedVertically ? 1 : 4;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        if (!MoveUtils.isMoving()) {
            movingTicks = 0;
            return;
        } else {
            if (!mode.is("Verus")) {
                // doing mc.thePlayer.jump() in places where minecraft doesn't will cause simulation flags on certain anticheats.
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), mc.thePlayer.onGround);
            }

            movingTicks++;
        }

        if (printAirTicks.get()) {
            ChatUtils.sendMessageClient("Air Ticks: " + mc.thePlayer.offGroundTicks);
        }

        if (MoveUtils.isMoving() && MoveUtils.getSpeed() < minSpeed.get() && movingTicks > minMoveTicks.get() && minSpeedLimiter.get()) {
            MoveUtils.strafe(minSpeed.get());
        }

        if (mc.thePlayer.hurtTime >= 9 && damageBoost.get()) {
            MoveUtils.strafe(boostSpeed.get());
        }

        switch (mode.get()) {
            case "NCP":
                switch (ncpMode.get()) {
                    case "Fast hop":
                        MoveUtils.strafe(MoveUtils.getSpeed() + 0.003);
                        mc.timer.timerSpeed = mc.thePlayer.ticksExisted % 5 == 0 ? 1.3F : 1;
                        break;
                    case "Old hop":
                        if (MoveUtils.isMoving() && !mc.thePlayer.isInWater()) {
                            double spd = 0.0025 * speedMulti.get();
                            double m = (float) (Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ) + spd);
                            MoveUtils.bop(m);
                        }

                        if (mc.thePlayer.offGroundTicks == 4) {
                            mc.thePlayer.motionY -= 0.09800000190734863;
                        }

                        if (MoveUtils.getSpeed() < 0.312866806998394775 && movingTicks > 15) {
                            MoveUtils.strafe(0.312866806998394775);
                        }

                        if (MoveUtils.isMoving()) {
                            float timerSpeed = (float) (1.337 - MoveUtils.getSpeed());

                            if (timerSpeed > 1.5) timerSpeed = 1.5f;
                            if (timerSpeed < 0.6) timerSpeed = 0.6f;
                            mc.timer.timerSpeed = timerSpeed;
                        }
                        break;
                    case "Low hop":
                        if (mc.thePlayer.offGroundTicks == 4 && mc.thePlayer.posY % 1.0 == 0.16610926093821377) {
                            mc.thePlayer.motionY = -0.09800000190734863;
                        }

                        if (mc.thePlayer.offGroundTicks >= 6) {
                            MoveUtils.strafe();
                        }

                        if (mc.thePlayer.onGround) {
                            MoveUtils.strafe(Math.max(0.281, MoveUtils.getSpeed()));
                        }

                        if (MoveUtils.getSpeedEffect() > 0 && mc.thePlayer.offGroundTicks == 3) {
                            mc.thePlayer.motionX *= 1.2;
                            mc.thePlayer.motionZ *= 1.2;
                        }
                        break;
                    case "7 tick":
                        MoveUtils.strafe();

                        switch (mc.thePlayer.offGroundTicks) {
                            case 4 -> mc.thePlayer.motionY -= 0.054f;
                            case 5 -> mc.thePlayer.motionY -= 0.1f;
                            case 6 -> mc.thePlayer.motionY -= 0.143f;
                            case 7 -> {
                                mc.thePlayer.motionX *= 0.995f;
                                mc.thePlayer.motionY -= 0.178f;
                                mc.thePlayer.motionZ *= 0.995f;
                            }
                        }
                        break;
                    case "Max":
                        MoveUtils.strafe(MoveUtils.getAllowedHDist());
                        break;
                }
                break;
            case "Intave":
                if (mc.thePlayer.isSprinting()) {
                    switch (intaveMode.get()) {
                        case "Safe":
                            if (mc.thePlayer.motionY > 0.003) {
                                mc.thePlayer.motionX *= 1f + (0.003 * iBoostMulti.get());
                                mc.thePlayer.motionX *= 1f + (0.003 * iBoostMulti.get());
                            }
                            break;
                        case "Fast":
                            switch (mc.thePlayer.offGroundTicks) {
                                case 1 -> {
                                    mc.thePlayer.motionX *= 1.005;
                                    mc.thePlayer.motionZ *= 1.005;
                                }
                                case 2, 3, 4, 5, 6 -> {
                                    mc.thePlayer.motionX *= 1.011;
                                    mc.thePlayer.motionZ *= 1.011;
                                }
                            }

                            if (mc.thePlayer.onGroundTicks == 1) {
                                mc.thePlayer.motionX *= 1.0045;
                                mc.thePlayer.motionZ *= 1.0045;
                            }

                            if (timer.get()) {
                                mc.timer.timerSpeed = 1.0075f;
                            }
                            break;
                    }
                }
                break;
            case "Vulcan":
                if (mc.thePlayer.onGround) {
                    MoveUtils.strafe();
                }

                switch (vulcanMode.get()) {
                    case "Hop":
                        if (!mc.thePlayer.onGround) {
                            if (allowAirStrafing.get()) {
                                // I straight up don't know why this sort of works
                                if (strafeTimer.hasTimeElapsed(500)) {
                                    yaw = Math.toRadians(Math.round(Math.toDegrees(MoveUtils.getDirection())));
                                    strafeTimer.reset();
                                }

                                MoveUtils.strafe(yaw, MoveUtils.getSpeed(), 0.5);
                            }
                        }

                        if (mc.thePlayer.posY - MoveUtils.findGround(mc.thePlayer) < 1.9) {
                            MoveUtils.strafe();

                            if (mc.thePlayer.motionY <= 0 && !mc.thePlayer.isCollidedHorizontally) {
                                // -= 4.xx is the max
                                mc.thePlayer.motionY -= 1;
                            }
                        }

                        if (MoveUtils.getSpeed() <= 0.221) {
                            MoveUtils.strafe(0.221);
                        }
                        break;
                    case "Low":
                        if (movingTicks > 5) {
                            if (mc.thePlayer.offGroundTicks <= 2) {
                                if (mc.thePlayer.posY - MoveUtils.findGround(mc.thePlayer) < 1.9) {
                                    mc.thePlayer.motionY -= 0.44;
                                }

                                double dir = Math.toDegrees(MoveUtils.getDirection());
                                if (Math.abs(dir - yaw) < 25) {
                                    if (strafeTimer.hasTimeElapsed(100)) {
                                        if (movingTicks > 20) {
                                            if (mc.thePlayer.movementInput.moveStrafe == 0) {
                                                MoveUtils.strafe(0.2645, 0.2607);
                                            } else {
                                                MoveUtils.strafe(0.264, 0.2741);
                                            }
                                        } else {
                                            MoveUtils.strafe(0.21, 0.2);
                                        }
                                    } else {
                                        //MoveUtils.strafe(MoveUtils.getSpeed() * 0.25);
                                    }
                                } else {
                                    strafeTimer.reset();
                                }

                                yaw = dir;
                            }
                        }
                        break;
                }
                break;
            case "Mineland":
                mc.timer.timerSpeed = 1.007f;

                float f = switch (mc.thePlayer.offGroundTicks) {
                    case 1 -> 1.015f;
                    case 2, 3, 4, 5, 6, 7, 8, 9 -> 1.005f;
                    case 11 -> 1.01f;
                    default -> 1;
                };

                mc.thePlayer.motionX *= f;
                mc.thePlayer.motionZ *= f;
                break;
            case "Polar":
                float f1 = mc.thePlayer.offGroundTicks == 1 ? 1.0020001F : 1.0030001F;
                mc.thePlayer.motionX *= f1;
                mc.thePlayer.motionZ *= f1;

                if (mc.thePlayer.offGroundTicks == 5) {
                    mc.thePlayer.motionY -= 0.008F;
                }
                break;
            case "Grim":
                switch (mc.thePlayer.offGroundTicks) {
                    case 1:
                        mc.thePlayer.motionX *= 1.0020001f;
                        mc.thePlayer.motionZ *= 1.0020001f;
                        break;
                    case 5:
                        mc.thePlayer.motionX *= 1.001f;
                        mc.thePlayer.motionZ *= 1.001f;
                        break;
                }
                break;
            case "Jitter":
                if (timerTimer.hasTimeElapsed(50)) {
                    state = !state;

                    if (state) {
                        mc.timer.timerSpeed = 0.95F;
                    } else {
                        mc.timer.timerSpeed = 1.05F;
                    }

                    timerTimer.reset();
                }
                break;
        }
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        switch (yawOffsetMode.get()) {
            case "Ground":
                if (mc.thePlayer.onGround) {
                    RotationHandler.setBasicRotation(new float[]{MoveUtils.getYawFromKeybind(), mc.thePlayer.rotationPitch}, true, 180, 180);
                }
                break;
            case "Air":
                if (!mc.thePlayer.onGround) {
                    RotationHandler.setBasicRotation(new float[]{mc.thePlayer.rotationYaw + 45, mc.thePlayer.rotationPitch}, true, 180, 180);
                }
                break;
            case "Constant":
                RotationHandler.setBasicRotation(new float[]{MoveUtils.getYawFromKeybind(), mc.thePlayer.rotationPitch}, true, 180, 180);
                break;
        }

        if (mode.is("Derp")) {
            RotationHandler.setBasicRotation(new float[]{MoveUtils.getYawFromKeybind() - (!mc.thePlayer.onGround ? 180 : 0), mc.thePlayer.rotationPitch}, true, 180, 180);
        }
    }

    @EventTarget
    public void onGravity(GravityEvent e) {
        if (mode.is("Intave") && customGravity.get()) {
            e.setGravityDecrement(0.081);
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent e) {
        switch (mode.get()) {
            case "Collide":
                for (EntityPlayer player : mc.theWorld.playerEntities) {
                    if (player == mc.thePlayer) {
                        continue;
                    }

                    if (mc.thePlayer.getEntityBoundingBox().expand(1, 1, 1).intersectsWith(player.getEntityBoundingBox())) {
                        MoveUtils.moveFlying(collideSpeed.get());
                    }
                }
                break;
            case "NCP":
                if (ncpMode.is("BHop")) {
                    double hDist = MoveUtils.getAllowedHDist();

                    if (MoveUtils.isMoving()) {
                        switch (mc.thePlayer.offGroundTicks) {
                            case 0:
                                speed = hDist * 1.75;
                                break;
                            case 1:
                                speed -= (0.66 * (speed - hDist));
                                break;
                            default:
                                speed -= speed / BUNNY_FRICTION;
                        }
                    } else {
                        speed = hDist;
                    }

                    e.setSpeed(Math.max(speed, hDist), Math.random() / 2000);
                }
                break;
            case "Azheng":
                if (speedBuffer >= 12) {
                    float threshold = mc.thePlayer.onGround ? 0.31F : 0.341F;
                    int speedLevel = MoveUtils.getSpeedEffect();

                    Block block = mc.theWorld.getBlockState(mc.thePlayer.getPosition().down()).getBlock();
                    Block blockAbove = mc.theWorld.getBlockState(mc.thePlayer.getPosition().up().up()).getBlock();
                    threshold += block instanceof BlockSlime ? 0.07f : 0;
                    threshold += (mc.thePlayer.onGroundTicks < 5) ? speedLevel * 0.06f : speedLevel * 0.046f;

                    if (block instanceof BlockSlab) threshold *= 1.8f;
                    if (block instanceof BlockIce && mc.thePlayer.onGroundTicks < 5) threshold *= 1.7f;
                    if (!(blockAbove instanceof BlockAir) && mc.thePlayer.motionY != 0.0) {
                        threshold *= 2.0f;
                    }

                    MoveUtils.strafe(threshold);
                    speedBuffer--;
                } else if (MoveUtils.isMoving()) {
                    MoveUtils.strafe(1);
                    speedBuffer++;
                } else {
                    speedBuffer = Math.max(0, speedBuffer - 1);
                }
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
            switch (mode.get()) {
                case "Strafe hop":
                    if ((mc.thePlayer.onGround && ground.get()) || (!mc.thePlayer.onGround && air.get())) {
                        MoveUtils.strafe(MoveUtils.getSpeed(), mc.thePlayer.onGround ? groundStrength.get() : airStrength.get());
                    }
                    break;
                case "Custom": {
                    double randomConst = randomizeSpeed.get() < 0.1 ? 0.0 : MathUtils.randomizeDouble(0.0, randomizeSpeed.get() / 100);
                    double randomize = (rand.nextDouble() < 0.5 ? -randomConst : randomConst);

                    if ((mc.thePlayer.onGround && ground.get()) || (!mc.thePlayer.onGround && air.get())) {
                        if (abideFriction.get()) {
                            MoveUtils.strafe(customSpeed.get() + randomize, mc.thePlayer.onGround ? groundStrength.get() : airStrength.get());
                        } else {
                            MoveUtils.strafe(MoveUtils.getSpeed(), mc.thePlayer.onGround ? groundStrength.get() : airStrength.get());
                        }
                    }

                    if (!abideFriction.get()) {
                        mc.thePlayer.motionX *= motionMultiplier.get();
                        mc.thePlayer.motionZ *= motionMultiplier.get();
                    }

                    if (pullDown.get()) {
                        if (mc.thePlayer.offGroundTicks == pullDownTick.get()) {
                            mc.thePlayer.motionY = -pullDownMotionMotion.get();
                        }
                    }

                    float timerRandomConst = (float) (timerRandom.get() < 0.1 ? 0.0 : MathUtils.randomizeDouble(0.0, timerRandom.get() / 100));
                    float timerRandomize = (rand.nextDouble() < 0.5 ? -timerRandomConst : timerRandomConst);

                    boolean timerReset = timerSpeed.get() - 1.0 < 1E-3 && timerSpeed.get() - 1.0 > -1E-3;

                    mc.timer.timerSpeed = timerReset ? 1.0F : timerSpeed.get();
                    mc.timer.timerSpeed += timerRandomize;
                    break;
                }
                case "Tick":
                    state = !state;

                    if (state) {
                        predictedPos = PlayerUtils.simulatePlayer(ticks.getInt()).position;
                    } else {
                        if (!onlyY.get()) {
                            mc.thePlayer.setPosition(predictedPos.xCoord, predictedPos.yCoord, predictedPos.zCoord);
                        } else {
                            mc.thePlayer.setPosition(mc.thePlayer.posX, predictedPos.yCoord, mc.thePlayer.posZ);
                        }
                    }
                    break;
                case "Jokai":
                    if (!MoveUtils.isMoving()) {
                        speed = 0;
                    }

                    // yes i use the same calc as NCP on my ac LOLOL
                    double base = MoveUtils.getAllowedHDist();
                    int airTicks = mc.thePlayer.offGroundTicks - 1;
                    BlockPos groundPos = MoveUtils.findGroundB(mc.thePlayer).add(0, 1, 0);
                    Block ground = mc.theWorld.getBlockState(groundPos).getBlock();

                    switch (airTicks) {
                        case -1:
                            speed = base * (double) (ground.slipperiness / 0.6F);
                            break;
                        case 0:
                            speed = base * (double) (ground.slipperiness / 0.6F) * (lastMotionY == 0 && mc.thePlayer.motionY > 0 || mc.thePlayer.offGroundTicks == 1 ? 1.809954751131222 : 1);
                            speed -= (BUNNY_SLOPE * (speed - base));
                            break;
                        case 1:
                            speed -= speed / BUNNY_FRICTION;
                            break;
                        default:
                            if (airTicks == 3) {
                                speed *= 0.95;
                            }

                            speed -= speed / BUNNY_FRICTION;
                    }

                    if (mc.thePlayer.offGroundTicks >= 6) {
                        mc.thePlayer.motionY = -0.1 + MathUtils.randomizeAround(0.05F);
                    }

                    if (mc.thePlayer.posY - MoveUtils.findGround(mc.thePlayer) < 1.5 && !mc.thePlayer.onGround && mc.thePlayer.motionY < 0) {
                        speed += 0.2;
                        mc.thePlayer.motionY = Math.min(0, -0.03 + MathUtils.randomizeAround(0.02F));
                    }

                    double finalSpeed = speed;
                    while (finalSpeed - lastSpeed > 0.22) {
                        finalSpeed -= 0.05;
                    }

//                    if (finalSpeed >= 0.8) {
//                        finalSpeed = 0.8;
//                        finalSpeed += MathUtils.randomizeAround(0.1F);
//                    }

                    MoveUtils.strafe(finalSpeed);

                    lastMotionY = mc.thePlayer.motionY;
                    lastSpeed = speed;
                    break;
                case "NCP":
                    if (ncpMode.is("FBHop")) {
                        double xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX;
                        double zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
                        lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
                    }
                    break;
                case "Vape":
                    switch (vapeMode.get()) {
                        case "Anticheat A": {
                            double var1 = 0.2873F;
                            if (mc.thePlayer.isPotionActive(1) && mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getDuration() > 10) {
                                int var4 = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
                                var1 *= 1.0D + 0.15D * (double) (var4 + 1);
                            }

                            if ((mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0) && this.z > 0.0D && this.parentX > 0 && e.isOnGround()) {
                                if (this.x > 0) {
                                    mc.thePlayer.motionY += 0.004999999888241291D + 9.999999747378752E-5D * Math.random();
                                }

                                ++this.x;
                            }

                            double var3 = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
                            double var5 = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
                            this.z = 0.1;

                            if (this.parentX < 0) {
                                ++this.parentX;
                                this.z = 0.0D;
                            } else {
                                double d12;
                                boolean isMoving = mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0;
                                double var4 = d12 = var1;
                                double var6;
                                double var8;
                                float var12;

                                if (this.parentX == 1 && isMoving && mc.thePlayer.isCollidedVertically) {
                                    d12 = var1;
                                    this.x = 0;
                                } else if (this.parentX == 2 && isMoving && mc.thePlayer.isCollidedVertically/* && mc.thePlayer.q()*/) {
                                    var6 = this.x > 0 ? 0.424F + 0.0001F * Math.random() : 0.42F;
                                    if (mc.thePlayer.isPotionActive(8)) {
                                        var6 += (double) ((mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F);
                                    }

//                                event.setY(event.getY() + var6);
                                    mc.thePlayer.motionY = var6;

//                                vapeSpeedVar = var4 * 2.1475D;
                                    var8 = (double) mc.thePlayer.movementInput.moveForward;
                                    double var10 = (double) mc.thePlayer.movementInput.moveStrafe;
                                    var12 = mc.thePlayer.rotationYaw;

                                    if (var8 == 0.0D && var10 == 0.0D) {
//                                    thePlayer.d(thePlayer.ae() + 1.0D, thePlayer.g(), thePlayer.Y() + 1.0D);
//                                    thePlayer.d(thePlayer.x(), thePlayer.F(), thePlayer.aq());

                                        mc.thePlayer.motionX = 0.0D;
                                        mc.thePlayer.motionZ = 0.0D;

//                                    if (thePlayer.q()) {
//                                        this.v = var12;
//                                    }
                                    } else if (var8 != 0.0D) {
                                        if (var8 < 0.0D) {
                                            var12 -= 180.0F;
                                        }

                                        if (var10 > 0.0D) {
                                            var12 += var8 > 0.0D ? -43.51F : 43.51F;
                                        } else if (var10 < 0.0D) {
                                            var12 += var8 > 0.0D ? 43.51F : -43.51F;
                                        }
                                    } else if (var10 > 0.0D) {
                                        var12 += -88.58F;
                                    } else if (var10 < 0.0D) {
                                        var12 += 88.58F;
                                    }

                                    float var13 = MathHelper.wrapAngleTo180_float(-(this.v - var12));
                                    float var14 = 180.0F;
                                    if (Math.abs(var13) >= var14) {
                                        var13 = MathHelper.clamp_float(var13, -var14, var14);
                                    }

                                    this.v += var13;
                                } else if (this.parentX == 3) {
                                    var6 = 0.825D;
//                                if (thePlayer.b(fD.r())) {
//                                    if (thePlayer.a(fD.r()).g() == 0) {
//                                        var6 = 0.8D;
//                                    } else {
//                                        var6 = 0.75D;
//                                    }
//                                }

                                    var8 = var6 * (this.z - var4);
                                    d12 = this.z - var8;
                                } else {
                                    AxisAlignedBB bb = mc.thePlayer.getHitbox().offset(0, mc.thePlayer.motionY, 0);

                                    if ((mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty() || mc.thePlayer.isCollidedVertically) && this.x > 0) {
                                        this.parentX = mc.thePlayer.movementInput.moveForward == 0.0F && mc.thePlayer.movementInput.moveStrafe == 0.0F ? 0 : 1;
                                    }

                                    d12 = this.z - this.z / 159.0D;
                                }

                                d12 = Math.max(d12, var4);
                                if (this.parentX > 0) {
                                    var6 = mc.thePlayer.movementInput.moveForward;
                                    var8 = mc.thePlayer.movementInput.moveStrafe;
                                    float var18 = mc.thePlayer.rotationYaw;
                                    if (var6 == 0.0D && var8 == 0.0D) {
                                        mc.thePlayer.motionX = 0.0D;
                                        mc.thePlayer.motionZ = 0.0D;
                                    } else if (var6 != 0.0D) {
                                        if (var8 > 0.0D) {
                                            var18 += (float) (var6 > 0.0D ? -45 : 45);
                                        } else if (var8 < 0.0D) {
                                            var18 += (float) (var6 > 0.0D ? 45 : -45);
                                        }

                                        if (var6 > 0.0D) {
                                            var6 = 1.0D;
                                        } else if (var6 < 0.0D) {
                                            var6 = 1.0D;
                                            var18 -= 180.0F;
                                        }
                                    } else {
                                        if (var8 > 0.0D) {
                                            var18 += -90.0F;
                                        } else if (var8 < 0.0D) {
                                            var18 += 90.0F;
                                        }

                                        var6 = 1.0D;
                                    }

                                    float var11 = MathHelper.wrapAngleTo180_float(-(this.v - var18));
                                    var12 = 43.5F;
                                    if (Math.abs(var11) >= var12) {
                                        var11 = MathHelper.clamp_float(var11, -var12, var12);
                                    }

                                    this.v += var11;
                                    double var19 = Math.cos(Math.toRadians(this.v + 90.0F));
                                    double var15 = Math.sin(Math.toRadians(this.v + 90.0F));
                                    mc.thePlayer.motionX = var6 * d12 * var19;
                                    mc.thePlayer.motionZ = var6 * d12 * var15;

                                    if (mc.thePlayer.movementInput.moveForward != 0.0F || mc.thePlayer.movementInput.moveStrafe != 0.0F) {
                                        ++this.parentX;
                                    }
                                }
                            }

                            break;
                        }
                        case "Anticheat B": {
                            double var2 = 0.4199999868869781494140625;

                            if ((mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0) && mc.thePlayer.isCollidedVertically) {
                                mc.thePlayer.motionY = var2;
                            }

                            double var5 = mc.thePlayer.movementInput.moveForward;
                            double var7 = mc.thePlayer.movementInput.moveStrafe;
                            float var9 = mc.thePlayer.rotationYaw;

                            if (var5 == 0.0D && var7 == 0.0D) {
                                mc.thePlayer.motionX = 0.0D;
                                mc.thePlayer.motionZ = 0.0D;
                            } else if (var5 != 0.0D) {
                                if (var7 != 0.0D) {
                                    if (var7 > 0.0D) {
                                        var9 += var5 > 0.0D ? -45.0F : 45.0F;
                                        var7 = 0.0D;
                                    } else {
                                        var9 += var5 > 0.0D ? 45.0F : -45.0F;
                                        var7 = 0.0D;
                                    }
                                }

                                if (var5 > 0.0D) {
                                    var5 = 1.0D;
                                } else {
                                    var5 = -1.0D;
                                }
                            }

                            double var10 = Math.cos(Math.toRadians(var9 + 90.0F));
                            double var12 = Math.sin(Math.toRadians(var9 + 90.0F));
                            mc.thePlayer.motionX = var5 * var2 * var10 + var7 * var2 * var12;
                            mc.thePlayer.motionZ = var5 * var2 * var12 - var7 * var2 * var10;
                            break;
                        }
                    }
                    break;
            }
        }
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        switch (mode.get()) {
            case "Verus":
                if (verusMode.is("Low") && MoveUtils.isMoving() && !mc.thePlayer.isJumping) {
                    if (movingTicks % 12 == 0 && mc.thePlayer.onGround) {
                        // insane value unpatch
                        // from 0.69 to 0.68
                        MoveUtils.strafe(0.68 + Math.random() / 100);
                        e.setY(0.42F);
                        mc.thePlayer.motionY = -(mc.thePlayer.posY - (mc.thePlayer.posY - (mc.thePlayer.posY % 0.015625)));
                    } else {
                        if (mc.thePlayer.onGround) {
                            MoveUtils.strafe(1.01 + Math.random() / 100);
                        } else {
                            MoveUtils.strafe(0.41 + Math.random() / 100);
                        }
                    }
                }
                break;
            case "NCP":
                if (ncpMode.is("FBHop")) {
                    ++timerDelay;
                    timerDelay %= 5;
                    if (timerDelay != 0) {
                        mc.timer.timerSpeed = 1F;
                    } else {
                        if (MoveUtils.isMoving()) {
                            mc.timer.timerSpeed = 1.3F;
                            mc.thePlayer.motionX *= 1.0199999809265137;
                            mc.thePlayer.motionZ *= 1.0199999809265137;
                        }
                    }

                    if (mc.thePlayer.onGround && MoveUtils.isMoving()) {
                        level = 2;
                    }

                    if (round(mc.thePlayer.posY - (double) ((int) mc.thePlayer.posY)) == round(0.138)) {
                        mc.thePlayer.motionY -= 0.08;
                        e.setY(e.getY() - 0.09316090325960147);
                        mc.thePlayer.posY -= 0.09316090325960147;
                    }

                    if (level == 1 && (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f)) {
                        level = 2;
                        moveSpeed = 1.35 * getBaseMoveSpeed() - 0.01;
                    } else if (level == 2) {
                        level = 3;
                        mc.thePlayer.motionY = 0.399399995803833;
                        e.setY(0.399399995803833);
                        moveSpeed *= 2.149;
                    } else if (level == 3) {
                        level = 4;
                        double difference = 0.66 * (lastDist - getBaseMoveSpeed());
                        moveSpeed = lastDist - difference;
                    } else {
                        if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, mc.thePlayer.motionY, 0.0)).isEmpty() || mc.thePlayer.isCollidedVertically) {
                            level = 1;
                        }

                        moveSpeed = lastDist - lastDist / 159.0;
                    }

                    moveSpeed = Math.max(moveSpeed, getBaseMoveSpeed());
                    float forward = mc.thePlayer.movementInput.moveForward;
                    float strafe = mc.thePlayer.movementInput.moveStrafe;
                    float yaw = mc.thePlayer.rotationYaw;

                    if (forward == 0.0f && strafe == 0.0f) {
                        e.setX(0.0);
                        e.setZ(0.0);
                    } else if (forward != 0.0f) {
                        if (strafe >= 1.0f) {
                            yaw += (float) (forward > 0.0f ? -45 : 45);
                            strafe = 0.0f;
                        } else if (strafe <= -1.0f) {
                            yaw += (float) (forward > 0.0f ? 45 : -45);
                            strafe = 0.0f;
                        }
                        if (forward > 0.0f) {
                            forward = 1.0f;
                        } else if (forward < 0.0f) {
                            forward = -1.0f;
                        }
                    }

                    double mx2 = Math.cos(Math.toRadians(yaw + 90.0f));
                    double mz2 = Math.sin(Math.toRadians(yaw + 90.0f));
                    e.setX((double) forward * moveSpeed * mx2 + (double) strafe * moveSpeed * mz2);
                    e.setZ((double) forward * moveSpeed * mz2 - (double) strafe * moveSpeed * mx2);

                    mc.thePlayer.stepHeight = 0.6F;
                    if (forward == 0.0F && strafe == 0.0F) {
                        e.setX(0.0);
                        e.setZ(0.0);
                    }
                }
                break;
        }
    }

    private double getBaseMoveSpeed() {
        double baseSpeed = 0.2873;
        if(mc.thePlayer.isPotionActive(Potion.moveSpeed))
            baseSpeed *= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        return baseSpeed;
    }

    private double round(double value) {
        BigDecimal bigDecimal = new BigDecimal(value);
        bigDecimal = bigDecimal.setScale(3, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    @EventTarget
    public void onJump(JumpEvent e) {
        switch (mode.get()) {
            case "Intave":
                if (intaveMode.is("Fast")) {
                    e.setJumpoff(0.209f);
                }
                break;
            case "Custom":
                e.setMotionY((float) ((double) customJumpMotion.get()));
                break;
            case "NCP":
                if (ncpMode.is("7 tick")) {
                    e.setMotionY(0.411F);
                }

                if (ncpMode.is("BHop")) {
                    speed = MoveUtils.getAllowedHDist();
                }
                break;
            case "Polar":
                if (funny.get()) {
                    if (mc.thePlayer.posY % 0.5 != 0) {
                        if (mc.thePlayer.posY - (int) mc.thePlayer.posY == 0.875) {
                            e.setMotionY(0.0625F);
                            e.setSprint(false); // so you don't flag goofy ass hardcoded checks
                        } else {
                            e.setMotionY(0.6F);
                            e.setJumpoff(0.22F);
                        }
                    } else if (mc.thePlayer.posY % 1 != 0) {
                        e.setMotionY(0.0625F);
                        e.setSprint(false);
                    }
                }
                break;
            case "Carpet":
                e.setMotionY(0.0625F);
                break;
        }
    }
}