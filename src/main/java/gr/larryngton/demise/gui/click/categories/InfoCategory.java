package gr.larryngton.demise.gui.click.categories;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.statistics.Stat;
import gr.larryngton.demise.gui.click.Component;
import gr.larryngton.demise.gui.click.ICategory;
import gr.larryngton.demise.gui.click.PanelGui;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Mouse;

import java.awt.*;

import static gr.larryngton.demise.gui.click.PanelGui.posX;
import static gr.larryngton.demise.gui.click.PanelGui.posY;

@Getter
@Setter
public class InfoCategory implements ICategory {
    private boolean isSelected;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float maxScroll = 0;
    private float x, y;
    private String name = "Info";
    private boolean isHovered;
    private Component cachedComponent;
    private int alpha;
    private float interpolatedY;
    private final TimerUtil alphaTimer = new TimerUtil();
    private float slideProgress = 0F;
    private float lastHeight;

    public InfoCategory(float x, float y) {
        this.x = x;
        this.y = y;
        this.isSelected = false;
        this.isHovered = false;
        this.interpolatedX = x;
    }

    public void initCategory() {
        slideProgress = 0;
    }

    public void render() {
        float x = this.x;

        if (isSelected) {
            x += 3;
            float width = Fonts.interRegular.get(18).getStringWidth(name);
            interpolatedLineWidth = MathUtils.interpolate(interpolatedLineWidth, width, 0.15f);
        } else {
            interpolatedLineWidth = MathUtils.interpolate(interpolatedLineWidth, 0, 0.15f);
        }

        if (isHovered) {
            x += 2.5f;
        }

        if (!PanelGui.dragging) {
            interpolatedX = MathUtils.interpolate(interpolatedX, x, 0.25f);
        } else {
            interpolatedX = x;
        }

        Fonts.interRegular.get(18).drawString(name, interpolatedX, y, Color.white.getRGB());
        RenderUtils.drawRect(interpolatedX, y + Fonts.interRegular.get(18).getHeight() - 2.6f, interpolatedLineWidth, 0.5f, Color.white.getRGB());
    }

    @Override
    public boolean isHovered(int mouseX, int mouseY) {
        boolean state = MouseUtils.isHovered(x, y, Fonts.interRegular.get(18).getStringWidth(getName()), Fonts.interRegular.get(18).getHeight(), mouseX, mouseY);
        isHovered = state;
        return state;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        if (targetScrollOffset > maxScroll) {
            targetScrollOffset = maxScroll;
        }

        handleScroll();
        float componentStartY = posY + 12 + Fonts.urbanist.get(35).getHeight();
        float viewHeight = 255;

        float totalHeight = lastHeight;

        maxScroll = Math.max(0, totalHeight - viewHeight);
        scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.25f);

        float componentOffsetY = componentStartY + 2;
        float y = componentOffsetY - scrollOffset;
        float x = this.x + 60;
        float width = 375;

        RenderUtils.scissor(x, PanelGui.posY + 12 + Fonts.urbanist.get(35).getHeight(), 375, 255, PanelGui.interpolatedScale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        slideProgress = MathUtils.interpolate(slideProgress, 1, 0.2f);

        lastHeight = 0;
        float slideOffset = (width / 4) * (1.0f - slideProgress);
        x += slideOffset;

        Fonts.urbanist.get(40).drawString(Demise.INSTANCE.getClientName(), x + 187.5 - (Fonts.urbanist.get(40).getStringWidth(Demise.INSTANCE.getClientName()) / 2F), y, -1);
        lastHeight += Fonts.urbanist.get(40).getHeight() + 3;
        y += Fonts.urbanist.get(40).getHeight() + 3;

        float fontSize = 15;
        for (Stat stat : Demise.INSTANCE.getStatManager().statList) {
            String str = "Total " + stat.getName().replace(stat.getName().charAt(0), String.valueOf(stat.getName().charAt(0)).toLowerCase().charAt(0)) + ": " + stat.getStatFormatted(false);
            Fonts.interRegular.get(fontSize).drawString(str, x + 187.5 - (Fonts.interRegular.get(fontSize).getStringWidth(str) / 2F), y, -1);
            lastHeight += Fonts.interRegular.get(fontSize).getHeight() + 2;
            y += Fonts.interRegular.get(fontSize).getHeight() + 2;
        }

        y += 4;

        for (Stat stat : Demise.INSTANCE.getStatManager().statList) {
            String str = "Session " + stat.getName().replace(stat.getName().charAt(0), String.valueOf(stat.getName().charAt(0)).toLowerCase().charAt(0)) + ": " + stat.getStatFormatted(true);
            Fonts.interRegular.get(fontSize).drawString(str, x + 187.5 - (Fonts.interRegular.get(fontSize).getStringWidth(str) / 2F), y, -1);
            lastHeight += Fonts.interRegular.get(fontSize).getHeight() + 2;
            y += Fonts.interRegular.get(fontSize).getHeight() + 2;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        String str = "Made by larryngton, continued by fern";
        Fonts.interRegular.get(15).drawString(str, posX + 450 - Fonts.interRegular.get(15).getStringWidth(str) - 3.5F, posY + 300.5f - Fonts.interRegular.get(14).getHeight() - Fonts.interRegular.get(15).getHeight() - 2, new Color(255, 255, 255, 208).getRGB());
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -25 : 25;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + scrollAmount, 0, maxScroll);
        }
    }
}