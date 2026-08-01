package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.StaticTickEvent;
import gr.larryngton.demise.events.impl.misc.TickEvent;
import gr.larryngton.demise.events.impl.player.AngleEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import org.lwjglx.input.Keyboard;

@ModuleInfo(name = "Fly", description = "Allows you to fly.")
public class Fly extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla","Ground", "Motion", "Invalid placement", "ONCP"}, "Vanilla", this);
    private final SliderValue motion = new SliderValue("Motion", 0.21f, 0.01f, 1, 0.01f, this, () -> mode.is("Motion"));
    private final SliderValue speed = new SliderValue("Speed", 2, 1, 5, 0.1f, this, () -> mode.is("Vanilla"));
    private final BoolValue autoJump = new BoolValue("Auto jump", false, this, () -> mode.is("Ground"));
    private final SliderValue aBalance = new SliderValue("Balance", 30, 0, 100, 1, this, () -> mode.is("ONCP"));
    private final BoolValue vanillaKickBypass = new BoolValue("Vanilla kick bypass", false, this);

    private boolean down;
    private double startY;
    private int balance;
    private int previousTickCount;

    @Override
    public void onEnable() {
        startY = Math.floor(mc.thePlayer.posY);

        if (mode.is("ONCP")) {
            balance = 0;

            int dalbajob = mc.thePlayer.ticksExisted - previousTickCount;
            if (dalbajob > aBalance.getInt()) {
                balance = -aBalance.getInt();
            }

            previousTickCount = mc.thePlayer.ticksExisted;

            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }
        }
    }

    @Override
    public void onDisable() {
        switch (mode.get()) {
            case "Vanilla":
                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.capabilities.setFlySpeed(0.05F);
                break;
        }

        mc.timer.timerSpeed = 1;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());

        switch (mode.get()) {
            case "Vanilla":
                mc.thePlayer.motionY = 0.0D;
                mc.thePlayer.capabilities.setFlySpeed((float) (0.05000000074505806D * speed.get()));
                mc.thePlayer.capabilities.isFlying = true;
                break;
            case "Motion":
                mc.thePlayer.motionY = 0.0D;
                MoveUtils.strafe(motion.get());

                if (!MoveUtils.isMoving()) {
                    MoveUtils.stopXZ();
                }

                if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                    mc.thePlayer.motionY = 0.42;
                } else if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                    mc.thePlayer.motionY = -0.42;
                }
                break;
        }
    }

    @EventTarget
    public void onTick(TickEvent e) {
        balance++;
    }

    @EventTarget
    public void onStaticTick(StaticTickEvent e) {
        balance--;
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
            switch (mode.get()) {
                case "Ground":
                    if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                        if (Math.abs(mc.thePlayer.posY - Math.floor(mc.thePlayer.posY)) < 0.1) {
                            mc.thePlayer.onGround = true;
                            e.setOnGround(true);
                            mc.thePlayer.motionY = 0;

                            startY = Math.floor(mc.thePlayer.posY);
                        }
                    }

                    if (!mc.gameSettings.keyBindSneak.isKeyDown()) {
                        if (mc.thePlayer.posY <= startY) {
                            mc.thePlayer.onGround = true;
                            e.setOnGround(true);
                            mc.thePlayer.motionY = 0;

                            if (autoJump.get() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                                mc.thePlayer.jump();
                            }
                        }
                    } else {
                        startY = Math.floor(mc.thePlayer.posY);
                    }
                    break;
                case "Invalid placement":
                    if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                        startY = Math.floor(mc.thePlayer.posY);
                    }

                    if (Math.abs(mc.thePlayer.posY - startY) < (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) ? 0.08 : 0.15)) {
                        Vec3 pos = new Vec3(mc.thePlayer.posX, Math.floor(startY) - 1, mc.thePlayer.posZ);
                        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), new BlockPos(pos), EnumFacing.UP, pos);
                    }

                    if (mc.thePlayer.posY <= startY && mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
                        mc.thePlayer.jump();
                    }
                    break;
                case "ONCP":
                    mc.thePlayer.motionY = 0;
                    MoveUtils.strafe(0.221);

                    if (balance < 0) {
                        mc.timer.timerSpeed = 1 - balance;
                    } else {
                        mc.timer.timerSpeed = 1;
                    }

                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 1e-4, mc.thePlayer.posZ);
                    break;
            }

            if (mc.thePlayer.ticksExisted % 40 == 0 && vanillaKickBypass.get()) {
                if (!down) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 0.04, mc.thePlayer.posZ);
                    down = true;
                } else {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.04, mc.thePlayer.posZ);
                    down = false;
                }
            }
        }
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        switch (mode.get()) {
            case "Invalid placement":
                RotationHandler.setBasicRotation(new float[]{mc.thePlayer.rotationYaw, 90}, true, 80, 80);
                break;
        }
    }
}