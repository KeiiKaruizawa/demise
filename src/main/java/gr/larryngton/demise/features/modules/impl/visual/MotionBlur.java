package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.DisplayUpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;
import net.minecraft.util.ResourceLocation;

@ModuleInfo(name = "MotionBlur", description = "Blurs your motion. (??)", legit = true)
public class MotionBlur extends Module {
    public final SliderValue amount = new SliderValue("Amount", 0.5F, 0.01F, 0.99F, 0.01F, this);

    @EventTarget
    public void onDisplayUpdate(DisplayUpdateEvent e) {
        if (mc.theWorld != null) {
            if (mc.entityRenderer.getShaderGroup() == null) {
                mc.entityRenderer.loadShader(new ResourceLocation("minecraft", "shaders/post/motion_blur.json"));
            } else {
                mc.entityRenderer.getShaderGroup().listShaders.get(0).getShaderManager().getShaderUniform("Phosphor").set(1F - Math.min(amount.get(), 0.99F), 0F, 0F);
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.entityRenderer.isShaderActive()) {
            mc.entityRenderer.stopUseShader();
        }
    }
}
