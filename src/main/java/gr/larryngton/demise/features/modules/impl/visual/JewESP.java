package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.render.GifRenderer;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import org.lwjglx.util.vector.Vector2f;

import java.util.ArrayList;

@ModuleInfo(name = "JewESP", description = "oy vey")
public class JewESP extends Module {
    private ArrayList<Entity> jews = new ArrayList<>();
    private final GifRenderer renderer = new GifRenderer(new ResourceLocation("demise/img/gif/tuff.gif"));

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        jews = getJews();
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        GlStateManager.pushMatrix();
        for (Entity jew : jews) {
            Vector2f renderPos = getPos(jew);

            if (renderPos == null) continue;

            renderer.drawTexture(renderPos.x, renderPos.y, renderer.getWidth() * 0.1f, renderer.getHeight() * 0.1f);
        }

        renderer.update();
        GlStateManager.popMatrix();
    }

    private Vector2f getPos(Entity target) {
        float x = (float) MathUtils.interpolate(target.prevPosX, target.posX);
        float y = (float) MathUtils.interpolate(target.prevPosY, target.posY) + target.height - 0.3f;
        float z = (float) MathUtils.interpolate(target.prevPosZ, target.posZ);

        Vector2f pos = RenderUtils.worldToScreen(x, y, z, new ScaledResolution(mc), false);

        if (pos == null) {
            return null;
        }

        Vector2f rPos = new Vector2f(pos.x, pos.y);

        float rPosX = rPos.x + target.width - (renderer.getWidth() * 0.1f) / 2;
        float rPosY = rPos.y;

        return new Vector2f(rPosX, rPosY);
    }

    private ArrayList<Entity> getJews() {
        ArrayList<Entity> jews = new ArrayList<>();

        for (Entity e : mc.theWorld.loadedEntityList) {
            if (e instanceof EntityVillager vey) {
                jews.add(vey);
                continue;
            }

            String n = e.getName();

            if (n.endsWith("berg") ||
                    n.endsWith("stein") ||
                    n.endsWith("thal") ||
                    n.endsWith("baum") ||
                    n.endsWith("witz") ||
                    n.endsWith("man") ||
                    n.endsWith("sky") ||
                    n.endsWith("blum") ||
                    n.endsWith("ler") ||
                    n.endsWith("feld") ||
                    n.endsWith("sohn")
            ) {
                jews.add(e);
            }
        }

        return jews;
    }
}