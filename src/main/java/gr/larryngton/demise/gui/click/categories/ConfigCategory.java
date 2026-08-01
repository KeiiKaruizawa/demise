package gr.larryngton.demise.gui.click.categories;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.gui.click.ICategory;
import gr.larryngton.demise.gui.click.PanelGui;
import gr.larryngton.demise.gui.click.components.ConfigComponent;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Mouse;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class ConfigCategory implements ICategory {
    private float x, y;
    private boolean isHovered, isSelected;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float maxScroll = 0;
    private String name = "Configs";
    private final List<ConfigComponent> configs = new ArrayList<>();

    public ConfigCategory(float x, float y) {
        if (Demise.INSTANCE.isLegit()) {
            return;
        }

        this.x = x;
        this.y = y;
        this.isSelected = false;
        this.isHovered = false;
        this.interpolatedX = x;

        List<String> cfgList = Demise.INSTANCE.getConfigManager().getConfigList();

        Collections.sort(cfgList);

        for (String config : cfgList) {
            // ez
            if (config.equals("elements")) {
                continue;
            }

            configs.add(new ConfigComponent(config));
        }
    }

    public void initCategory() {
        configs.forEach(ConfigComponent::initCategory);
    }

    @Override
    public boolean isHovered(int mouseX, int mouseY) {
        boolean state = MouseUtils.isHovered(x, y, Fonts.interRegular.get(18).getStringWidth(getName()), Fonts.interRegular.get(18).getHeight(), mouseX, mouseY);
        isHovered = state;
        return state;
    }

    public void initGui() {
        if (Demise.INSTANCE.isLegit()) {
            return;
        }

        configs.clear();

        List<String> cfgList = Demise.INSTANCE.getConfigManager().getConfigList();

        Collections.sort(cfgList);

        for (String config : cfgList) {
            configs.add(new ConfigComponent(config));
        }
    }

    public void render() {
        if (Demise.INSTANCE.isLegit()) {
            return;
        }

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

        if (isSelected) {
            handleScroll();

            float componentStartY = PanelGui.posY + 17 + Fonts.urbanist.get(35).getHeight();
            float viewHeight = 250;

            float totalHeight = 0;

            for (ConfigComponent ignored : configs) {
                totalHeight += 35;
            }

            maxScroll = Math.max(0, totalHeight - viewHeight);

            scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.25f);

            RenderUtils.scissor(0, componentStartY - 2, PanelGui.posX + 450, viewHeight, PanelGui.interpolatedScale);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            float componentOffsetY = componentStartY;
            for (ConfigComponent config : configs) {
                float moduleY = componentOffsetY - scrollOffset;
                config.setX(this.x + 60);
                config.setY(moduleY);
                config.render();
                config.setVisible(moduleY + 35 >= componentStartY && moduleY <= componentStartY + viewHeight);

                componentOffsetY += 35;
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        configs.forEach(configComponent -> configComponent.drawScreen(mouseX, mouseY));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        configs.forEach(configComponent -> configComponent.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        configs.forEach(configComponent -> configComponent.keyTyped(typedChar, keyCode));
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        configs.forEach(configComponent -> configComponent.mouseReleased(mouseX, mouseY, state));
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -25 : 25;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + scrollAmount, 0, maxScroll);
        }
    }
}