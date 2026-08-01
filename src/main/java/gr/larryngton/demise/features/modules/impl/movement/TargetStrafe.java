package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventPriority;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.enums.MovementCorrectionMode;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjglx.input.Keyboard;

@ModuleInfo(name = "TargetStrafe", description = "Strafes around your target.")
public class TargetStrafe extends Module {
    private final SliderValue range = new SliderValue("Range", 1, 0.1f, 6, 0.1f, this);
    private final BoolValue behind = new BoolValue("Behind", true, this);
    private final BoolValue onlySpeed = new BoolValue("Only speed", false, this);

    public float yaw;
    private boolean left, colliding;
    public boolean active;
    public EntityLivingBase target;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!getModule(KillAura.class).isEnabled() || (onlySpeed.get() && !getModule(Speed.class).isEnabled())) {
            active = false;
            target = null;
            return;
        }

        active = true;
        target = KillAura.target;

        if (target == null) {
            return;
        }

        if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) {
            left = false;
        } else if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())) {
            left = true;
        }

        if (mc.thePlayer.isCollidedHorizontally || !PlayerUtils.isBlockUnder(5, false)) {
            if (!colliding) {
                left = !left;
            }
            colliding = true;
        } else {
            colliding = false;
        }

        float yaw;

        if (behind.get()) {
            yaw = target.rotationYaw + 180;
        } else {
            yaw = getYaw(mc.thePlayer, new Vec3(target.posX, target.posY, target.posZ)) + 135 * (left ? -1 : 1);
        }

        double posX = -MathHelper.sin((float) Math.toRadians(yaw)) * range.get() + target.posX;
        double posZ = MathHelper.cos((float) Math.toRadians(yaw)) * range.get() + target.posZ;

        yaw = getYaw(mc.thePlayer, new Vec3(posX, target.posY, posZ));

        this.yaw = yaw;
    }

    public static float getYaw(EntityPlayer from, Vec3 pos) {
        return from.rotationYaw + MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(pos.zCoord - from.posZ, pos.xCoord - from.posX)) - 90f - from.rotationYaw);
    }

    @EventTarget
    @EventPriority(-100)
    public void onHighPriorityMoveInput(MoveInputEvent e) {
        if (target != null) {
            if (RotationHandler.correction == MovementCorrectionMode.Silent) {
                RotationHandler.directionYaw = yaw;
            } else if (RotationHandler.correction == MovementCorrectionMode.None) {
                MoveUtils.fixMovement(e, mc.thePlayer.rotationYaw, yaw);
            }
        }
    }
}
