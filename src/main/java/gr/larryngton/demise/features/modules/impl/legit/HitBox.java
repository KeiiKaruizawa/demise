package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.MouseOverEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "HitBox", description = "Expands your target's hitbox.")
public class HitBox extends Module {
    private final SliderValue expand = new SliderValue("Expand", 0.5f, -0.5f, 1, 0.1f, this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(String.valueOf(expand.get()));
    }

    @EventTarget
    public void onMouseOver(MouseOverEvent e) {
        e.setExpand(expand.get());
    }
}