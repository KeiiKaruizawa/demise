package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AngleEvent;
import gr.larryngton.demise.events.impl.player.LClickMouseEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.SpoofSlotUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationManager;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.*;
import org.apache.commons.lang3.Range;

//todo lol too bored to do ts
@ModuleInfo(name = "AutoProjectile", description = "Automatically throws projectiles at your target.")
public class AutoProjectile extends Module {
    private final DoubleSliderValue range = new DoubleSliderValue("Range", 3, 4.5f, 1, 8, 0.1f, this);
    private final SliderValue useDelayOnSwitch = new SliderValue("Use delay on switch", 2, 0, 10, 1, this);
    private final SliderValue maxWaitDelay = new SliderValue("Max wait delay", 2, 0, 20, 1, this);
    private final SliderValue maxRethrowDelay = new SliderValue("Max rethrow delay", 2, 0, 20, 1, this);
    private final SliderValue switchBackDelay = new SliderValue("Switchback delay", 2, 0, 10, 1, this);
    private final SliderValue fov = new SliderValue("Fov", 90, 0, 360, 1, this);
    private final BoolValue rotate = new BoolValue("Rotate", true, this);
    private final SliderValue basePrediction = new SliderValue("Base prediction", 1, 0, 8, 0.1F, this, rotate::get);
    private final RotationManager rotationManager = new RotationManager(this);
    private final BoolValue onlyOnKillAura = new BoolValue("Only on kill aura", false, this);
    public final BoolValue overrideAuraRots = new BoolValue("Override kill aura rots", false, this);

    private final TimerUtil rethrowTimer = new TimerUtil();
    private final TimerUtil delayTimer = new TimerUtil();
    private int oldSlot;
    private EntityLivingBase currentTarget;
    public boolean rotating;
    private boolean resetSpoofing;
    private final TimerUtil switchTimer = new TimerUtil();
    private final TimerUtil useTimer = new TimerUtil();
    private double predictionSize;

    @Override
    public void onEnable() {
        oldSlot = mc.thePlayer.inventory.currentItem;
    }

    @Override
    public void onDisable() {
        currentTarget = null;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        currentTarget = PlayerUtils.getTarget(range.getEnd(), "RayTrace");

        if (currentTarget == null || !mc.thePlayer.canEntityBeSeen(currentTarget) || mc.thePlayer.isUsingItem() || (!getModule(KillAura.class).isEnabled() && onlyOnKillAura.get())) {
            reset();
            return;
        }

        predictionSize = basePrediction.get() + Math.min(Math.max((PlayerUtils.getDistanceToEntityBox(currentTarget) - range.getMin()) * 3, 0), 8);

        double predictedBHVDist = PlayerUtils.getCustomDistanceToEntityBox(
                mc.thePlayer.getPositionEyes(1).add(mc.thePlayer.getMoveDeltaVector().flat().multiply(predictionSize)),
                currentTarget.getPositionEyes(1).add(currentTarget.getMoveDeltaVector().flat().multiply(predictionSize))
        );

        if (Range.between(this.range.getStart(), this.range.getEnd()).contains((float) predictedBHVDist) && RotationUtils.getRotationDifference(currentTarget) <= fov.get()) {
            if (delayTimer.hasTimeElapsed(maxWaitDelay.get() * 50L) || currentTarget.hurtTime <= 3) {
                int rod = findRod();

                if (rod == -1) {
                    return;
                }

                useRod(rod);
            }
        } else if (rethrowTimer.hasTimeElapsed(maxRethrowDelay.get() * 50L)) {
            reset();
        }
    }

    @EventTarget
    public void onLMouse(LClickMouseEvent e) {
        if (!delayTimer.hasTimeElapsed(50)) {
            e.setCancelled(true);
        }
    }

    private void reset() {
        if (!switchTimer.hasTimeElapsed(switchBackDelay.getInt() * 50L)) {
            return;
        }

        if (!resetSpoofing) {
            if (oldSlot != -1) {
                mc.thePlayer.inventory.currentItem = oldSlot;
            }

            SpoofSlotUtils.stopSpoofing();
            mc.playerController.syncCurrentPlayItem();
        }
        rethrowTimer.reset();
        oldSlot = mc.thePlayer.inventory.currentItem;
        resetSpoofing = true;
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (!getModule(KillAura.class).isEnabled() && onlyOnKillAura.get()) {
            rotating = false;
            return;
        }

        double range = PlayerUtils.getDistanceToEntityBox(currentTarget);

        rotating = rotate.get() && (KillAura.target == null || overrideAuraRots.get()) && range > this.range.getStart() && range <= this.range.getEnd() && mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemFishingRod;

        if (rotating) {
            rotationManager.setRotation(RotationUtils.faceTrajectory(currentTarget, (float) predictionSize, 0.03f, 2f));
        }
    }

    private int findRod() {
        for (int slot = 36; slot < 45; slot++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();

            if (stack != null && (stack.getItem() instanceof ItemSnowball || stack.getItem() instanceof ItemEgg)) {
                return slot;
            }
        }

        return -1;
    }

    private void useRod(int rod) {
        mc.thePlayer.inventory.currentItem = rod - 36;
        SpoofSlotUtils.startSpoofing(oldSlot);

        if (resetSpoofing) {
            useTimer.reset();
            resetSpoofing = false;
        }

        if (!useTimer.hasTimeElapsed(useDelayOnSwitch.getInt() * 50L)) {
            return;
        }

        KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());

        rethrowTimer.reset();
        switchTimer.reset();
        delayTimer.reset();
    }
}
