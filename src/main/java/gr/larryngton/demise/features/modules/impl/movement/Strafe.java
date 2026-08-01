package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.MoveUtils;

@ModuleInfo(name = "Strafe", description = "Modifies strafe friction.")
public class Strafe extends Module {
    public final BoolValue ground = new BoolValue("Ground", true, this);
    public final SliderValue groundStrength = new SliderValue("Ground strength", 1, 0.01f, 1, 0.01f, this, ground::get);
    public final BoolValue air = new BoolValue("Air", true, this);
    public final SliderValue airStrength = new SliderValue("Air strength", 1, 0.01f, 1, 0.01f, this, air::get);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if ((mc.thePlayer.onGround && ground.get()) || (!mc.thePlayer.onGround && air.get())) {
            MoveUtils.strafe(MoveUtils.getSpeed(), mc.thePlayer.onGround ? groundStrength.get() : airStrength.get());
        }
    }
}