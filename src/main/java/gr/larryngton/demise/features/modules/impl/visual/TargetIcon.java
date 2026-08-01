package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.render.GifRenderer;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjglx.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@ModuleInfo(name = "TargetIcon", description = "Render a image when attacking someone")
public class TargetIcon extends Module {
    private static final String[] ICON_NAMES = new String[]{
            "Weary", "Angry", "Sunglasses", "Vomit", "Heart", "Tung", "Boykisser", "Eatsuki", "Cat", "Sunna", "Scuba", "Scuba Transparent"
    };

    private final BoolValue onlyKillAura = new BoolValue("OnlyKillAura", false, this);
    private final BoolValue randomIcon = new BoolValue("RandomIcon", false, this, () -> ICON_NAMES.length > 1);
    private final ModeValue icon = new ModeValue("Icon", ICON_NAMES, "Weary", this, () -> !randomIcon.get());
    private final SliderValue iconSize = new SliderValue("Size", 80, 16, 256, this);
    private final SliderValue heightFraction = new SliderValue("Height", 0.85f, 0f, 1.5f, 0.01f, this);
    private final SliderValue offsetX = new SliderValue("OffsetX", 0, -200, 200, this);
    private final SliderValue offsetY = new SliderValue("OffsetY", -10, -200, 200, this);

    private final Random random = new Random();
    private final Map<String, Object> iconCache = new HashMap<>();

    private EntityLivingBase trackedEntity;
    private Object currentIcon;
    private long showUntil;

    @Override
    public void onDisable() {
        trackedEntity = null;
        currentIcon = null;
        showUntil = 0L;
    }

    private Object resolveIcon(String name) {
        return iconCache.computeIfAbsent(name, n -> {
            String path = "demise/img/icon/" + n.toLowerCase();

            try {
                return new GifRenderer(new ResourceLocation(path + ".gif"));
            } catch (Exception ex) {
                return new ResourceLocation(path + ".png");
            }
        });
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        Entity target = e.getTargetEntity();

        if (!(target instanceof EntityLivingBase entityLivingBase) || !entityLivingBase.isEntityAlive()) return;
        if (onlyKillAura.get() && !getModule(KillAura.class).isEnabled()) return;

        boolean isNewTarget = System.currentTimeMillis() >= showUntil || trackedEntity != target;

        if (isNewTarget || currentIcon == null) {
            String name = randomIcon.get() ? ICON_NAMES[random.nextInt(ICON_NAMES.length)] : icon.get();
            currentIcon = resolveIcon(name);
        }

        trackedEntity = entityLivingBase;
        showUntil = System.currentTimeMillis() + 1000L;
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (currentIcon == null || System.currentTimeMillis() >= showUntil) {
            trackedEntity = null;
            currentIcon = null;
            showUntil = 0L;
            return;
        }

        EntityLivingBase target = trackedEntity;

        if (target == null || !target.isEntityAlive() || target.isDead) {
            trackedEntity = null;
            currentIcon = null;
            showUntil = 0L;
            return;
        }

        double x = MathUtils.interpolate(target.prevPosX, target.posX);
        double y = MathUtils.interpolate(target.prevPosY, target.posY) + target.height * heightFraction.get();
        double z = MathUtils.interpolate(target.prevPosZ, target.posZ);

        Vector2f screenPos = RenderUtils.worldToScreen(x, y, z, new ScaledResolution(mc), false);
        if (screenPos == null) return;

        float size = iconSize.get();
        float x0 = screenPos.x + offsetX.get() - size * 0.5f;
        float y0 = screenPos.y + offsetY.get() - size * 0.5f;

        if (currentIcon instanceof GifRenderer gifRenderer) {
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableBlend();
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

            gifRenderer.drawTexture(x0, y0, size, size);
            gifRenderer.update();

            GlStateManager.disableBlend();
            GlStateManager.color(1F, 1F, 1F, 1F);
        } else {
            RenderUtils.drawImage((ResourceLocation) currentIcon, x0, y0, (int) size, (int) size);
        }
    }
}