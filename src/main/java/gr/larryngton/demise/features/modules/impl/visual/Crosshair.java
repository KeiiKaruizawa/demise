package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name = "Crosshair", description = "Shitty crosshair.")
public class Crosshair extends Module {
    private float lastX, lastY;

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        ScaledResolution sr = new ScaledResolution(mc);

        float x = sr.getScaledWidth() / 2F;
        float y = sr.getScaledHeight() / 2F - 4.75F;

        lastX = MathUtils.interpolate(lastX, mc.mouseHelper.deltaX / 10F, 0.25F);
        lastY = MathUtils.interpolate(lastY, mc.mouseHelper.deltaY / 10F, 0.25F);
        x += lastX;
        y -= lastY;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, x);
        GL11.glScaled(1.2, 1.2, 1.2);
        Fonts.interBold.get(18).drawCenteredString(".", 0, 0, Demise.INSTANCE.getModuleManager().getModule(Interface.class).color());
        GL11.glPopMatrix();
    }
}