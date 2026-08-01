package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import net.minecraft.client.renderer.EntityRenderer;

@ModuleInfo(name = "ItemGlow", description = "Makes your held item glow.", legit = true)
public class ItemGlow extends Module {
    private final BoolValue colored = new BoolValue("Colored", false, this);

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (mc.gameSettings.thirdPersonView == 0) {
            if (colored.get()) {
                Demise.INSTANCE.getShaderManager().addToBloomQueue3D(() -> EntityRenderer.renderHand(mc.timer.partialTicks, 2));
            } else {
                Demise.INSTANCE.getShaderManager().addToShadowQueue3D(() -> EntityRenderer.renderHand(mc.timer.partialTicks, 2));
            }
        }
    }
}