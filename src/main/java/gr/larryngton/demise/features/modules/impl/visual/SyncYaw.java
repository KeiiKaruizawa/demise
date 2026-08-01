package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "SyncYaw", description = "Syncs body yaw to head yaw.", legit = true)
public class SyncYaw extends Module {

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        mc.thePlayer.renderYawOffset = mc.thePlayer.rotationYawHead;
    }
}