package gr.larryngton.demise.utils.player.handlers.rotation;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.modules.impl.legit.AimAssist;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.player.handlers.rotation.enums.AngleLimiterMode;
import gr.larryngton.demise.utils.player.handlers.rotation.enums.MovementCorrectionMode;
import gr.larryngton.demise.utils.player.handlers.rotation.enums.AverageLimitMode;
import lombok.Getter;
import net.minecraft.util.MathHelper;

@Getter
public class RotationManager implements InstanceAccess {
    final SliderValue yawRotationSpeed;
    final SliderValue pitchRotationSpeed;
    ModeValue moveCorrection;
    BoolValue silent;
    final ModeValue angleLimiter;
    final SliderValue maxDeltaHistorySize;
    BoolValue noS;
    final ModeValue averageLimitMode;
    final SliderValue maxAverageYawDelta;
    final DoubleSliderValue yawMultiplierOnLimit;

    private final Module module;
    boolean isAimAssist;
    boolean isKillAura;

    public RotationManager(Module module) {
        this.module = module;

        isAimAssist = module instanceof AimAssist;
        isKillAura = module instanceof KillAura;

        if (!isAimAssist) {
            silent = new BoolValue("Silent", true, module);
        }
        angleLimiter = new ModeValue("Angle limiter", new String[]{"Linear", "Accelerated", "Interpolated", "None"}, "Linear", module);
        yawRotationSpeed = new SliderValue("Yaw speed", 180, 1, 180, module, () -> !angleLimiter.is("None"));
        pitchRotationSpeed = new SliderValue("Pitch speed", 180, 1, 180, module, () -> !angleLimiter.is("None"));
        maxDeltaHistorySize = new SliderValue("Max delta history size", 20, 0, 20, 1, module);
        averageLimitMode = new ModeValue("Average yaw limiter mode", new String[]{"NCP", "Custom", "None"}, "None", module, () -> maxDeltaHistorySize.getInt() > 0);
        maxAverageYawDelta = new SliderValue("Max average yaw delta", 90, 1, 180, module, () -> averageLimitMode.canDisplay() && averageLimitMode.is("Custom"));
        yawMultiplierOnLimit = new DoubleSliderValue("Yaw multiplier on limit ", 0.1F, 1, 0, 1, 0.01F, module, () -> averageLimitMode.canDisplay() && averageLimitMode.is("Custom"));

        if (!isAimAssist) {
            moveCorrection = new ModeValue("Movement correction", new String[]{"Silent", "Strict", "None"}, "None", module);
            noS = new BoolValue("No S", false, module, () -> moveCorrection.is("Silent"));
        }
    }

    public boolean instantRots = false;

    public void setRotation(float[] targetRotation) {
        float hSpeed = yawRotationSpeed.get();
        float vSpeed = pitchRotationSpeed.get();

        if (!instantRots) {
            hSpeed = MathHelper.clamp_float(hSpeed, 1, 180);
            vSpeed = MathHelper.clamp_float(vSpeed, 1, 180);
        } else {
            hSpeed = vSpeed = 180;
        }

        RotationHandler.setRotation(targetRotation,
                isAimAssist ? MovementCorrectionMode.None : MovementCorrectionMode.valueOf(moveCorrection.get()),
                new float[]{hSpeed, vSpeed},
                AngleLimiterMode.valueOf(angleLimiter.get()),
                maxDeltaHistorySize.getInt(),
                !isAimAssist && silent.get(),
                isAimAssist,
                noS.get(),
                AverageLimitMode.valueOf(averageLimitMode.get()),
                maxAverageYawDelta.get(),
                yawMultiplierOnLimit.getStart(),
                yawMultiplierOnLimit.getEnd()
        );
    }
}