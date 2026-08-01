package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationManager;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;

@ModuleInfo(name = "AutoWalk", description = "w a l k")
public class AutoWalk extends Module {
    private final BoolValue target = new BoolValue("Target player", false, this);
    private final BoolValue rotate = new BoolValue("Rotate", true, this, target::get);
    private final RotationManager rotationManager = new RotationManager(this);
    private final SliderValue minRange = new SliderValue("Min range", 1.5f, 0, 15, 0.1f, this, target::get);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!target.get()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
        } else {
            EntityLivingBase target = PlayerUtils.getTarget(Double.MAX_VALUE);
            if (target != null) {
                if (rotate.get()) {
                    rotationManager.setRotation(RotationUtils.getSimpleRotationsToEntity(target));
                }

                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), PlayerUtils.getDistanceToEntityBox(target) > minRange.get());
            }
        }
    }
}
