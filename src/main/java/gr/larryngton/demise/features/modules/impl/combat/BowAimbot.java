package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.events.annotations.EventPriority;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AngleEvent;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationManager;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBow;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "BowAimbot", description = "Automatically aims and shoots bows.")
public class BowAimbot extends Module {
    private final SliderValue fov = new SliderValue("Fov", 90, 10, 180, 1, this);
    private final SliderValue predictSize = new SliderValue("Predict size", 2, 0.1f, 5, 0.1f, this);
    private final RotationManager rotationManager = new RotationManager(this);
    private final BoolValue autoRelease = new BoolValue("Auto release", false, this);
    public static boolean canShoot;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (autoRelease.get() && canShoot) {
            mc.playerController.onStoppedUsingItem(mc.thePlayer);
        }
    }


    @EventTarget
    public void onAngle(AngleEvent e) {
        canShoot = false;
        List<EntityLivingBase> targets = PlayerUtils.getTargets(500);

        targets.sort(Comparator.comparingDouble(RotationUtils::getFovToTarget));
        if (targets.isEmpty()) return;

        EntityLivingBase target = targets.get(0);
        if (target == null) return;

        if (RotationUtils.getFovToTarget(target) < fov.get() && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow bow && mc.thePlayer.isUsingItem()) {
            int var6 = 72000 - mc.thePlayer.getItemInUseCount();
            float power = var6 / 20.0f;
            power = (power * power + power * 2.0f) / 3.0f;
            if (power > 1.0f) {
                power = 1.0f;
            }

            float[] targetRot = RotationUtils.faceTrajectory(target, predictSize.get(), 0.05f, power);
            if (RotationUtils.getRotationDifference(RotationHandler.currentRotation, targetRot) < 0.05) {
                canShoot = true;
            }

            rotationManager.setRotation(targetRot);

        }
    }
}