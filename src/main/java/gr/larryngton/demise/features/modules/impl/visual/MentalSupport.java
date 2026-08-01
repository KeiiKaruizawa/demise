package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.PostRender2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

@ModuleInfo(name = "MentalSupport", description = "Keeps your sanity.", legit = true)
public class MentalSupport extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"1", "2", "Karuizawa"}, "1", this);
    private float interpolatedWifeX;

    @EventTarget
    public void onPostRender2D(PostRender2DEvent e) {
        if (mc.thePlayer != null && mc.theWorld != null) {
            ScaledResolution sr = new ScaledResolution(mc);

            float x = mode.is("1") ? 330 : 202;
            float targetX = mc.currentScreen != null ? sr.getScaledWidth() - x : sr.getScaledWidth() + (x / 2);
            interpolatedWifeX = MathUtils.interpolate(interpolatedWifeX, targetX, 0.4F);

            if (interpolatedWifeX == 0) {
                interpolatedWifeX = targetX;
            }

            x = interpolatedWifeX;

            switch (mode.get()) {
                case "1":
                    RenderUtils.drawImage(new ResourceLocation("demise/img/mikos/Miko-banner.png"), x, sr.getScaledHeight() - 169, 330, 169);
                    break;
                case "2":
                    RenderUtils.drawImage(new ResourceLocation("demise/img/mikos/Miko-banner-big.png"), x, sr.getScaledHeight() - 183, 202, 183);
                    break;
                case "Karuizawa":
                    RenderUtils.drawImage(new ResourceLocation("demise/img/karuizawa/karuizawa-banner.png"), x, sr.getScaledHeight() - 183, 190, 300);
                    break;
            }

            float finalX = x;
            Demise.INSTANCE.getShaderManager().addToBloomQueue(() -> {
                switch (mode.get()) {
                    case "1":
                        RenderUtils.drawImage(new ResourceLocation("demise/img/mikos/Miko-banner.png"), finalX, sr.getScaledHeight() - 169, 330, 169);
                        break;
                    case "2":
                        RenderUtils.drawImage(new ResourceLocation("demise/img/mikos/Miko-banner-big.png"), finalX, sr.getScaledHeight() - 183, 202, 183);
                        break;
                    case "Karuizawa":
                        RenderUtils.drawImage(new ResourceLocation("demise/img/karuizawa/karuizawa-banner.png"), finalX, sr.getScaledHeight() - 183, 190, 300);
                        break;
                }
            });
        }
    }
}
