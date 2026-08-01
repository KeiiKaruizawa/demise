package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "NoHitDelay", description = "Removes Minecraft's hit delay.", legit = true)
public class NoHitDelay extends Module {

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        mc.leftClickCounter = 0;
    }
}