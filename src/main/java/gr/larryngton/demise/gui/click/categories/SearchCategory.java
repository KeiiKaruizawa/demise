package gr.larryngton.demise.gui.click.categories;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.gui.click.ICategory;
import gr.larryngton.demise.gui.click.PanelGui;
import gr.larryngton.demise.gui.click.components.ModuleComponent;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.sound.SoundUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static gr.larryngton.demise.gui.click.PanelGui.posX;
import static gr.larryngton.demise.gui.click.PanelGui.posY;

@Getter
@Setter
public class SearchCategory implements ICategory {
    private boolean isSelected;
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float maxScroll = 0;
    private String filter = "";
    private boolean inputting;

    public SearchCategory() {
        this.isSelected = false;
    }

    public void initCategory() {
        moduleComponents.clear();
        for (Module module : Demise.INSTANCE.getModuleManager().getModules()) {
            if (filter.isEmpty() || module.getName().toLowerCase().contains(filter.toLowerCase())) {
                moduleComponents.add(new ModuleComponent(module));
            }
        }

        moduleComponents.forEach(ModuleComponent::initCategory);
    }

    @Override
    public boolean isHovered(int mouseX, int mouseY) {
        float watermarkWidth = Fonts.urbanist.get(35).getStringWidth(Demise.INSTANCE.getClientName()) + 2 + Fonts.urbanist.get(24).getStringWidth(Demise.INSTANCE.getVersion());
        float calcWidth = 450 - watermarkWidth - 19;

        return MouseUtils.isHovered(posX + watermarkWidth + 13, posY + 7, calcWidth, 20, mouseX, mouseY);
    }

    @Override
    public float getX() {
        return 0;
    }

    @Override
    public float getY() {
        return 0;
    }

    @Override
    public void setX(float x) {

    }

    @Override
    public void setY(float y) {

    }

    @Override
    public String getName() {
        return "Search";
    }

    public void render() {
        if (isSelected) {
            if (targetScrollOffset > maxScroll) {
                targetScrollOffset = maxScroll;
            }

            String thing = (System.currentTimeMillis() % 1000 > 500 ? "|" : "");
            String text = !filter.isEmpty() ? filter : "Search...";
            if (inputting) {
                text += thing;
            }

            drawText(text);

            handleScroll();

            float componentStartY = posY + 12 + Fonts.urbanist.get(35).getHeight();
            float viewHeight = 255;

            float totalHeight = 0;

            for (ModuleComponent module : moduleComponents) {
                totalHeight += module.getHeight() + 10;
            }

            maxScroll = Math.max(0, totalHeight - viewHeight);
            scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.25f);

            RenderUtils.scissor(0, componentStartY, posX + 450, viewHeight, PanelGui.interpolatedScale);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            float componentOffsetY = componentStartY + 2;
            for (ModuleComponent module : moduleComponents) {
                float moduleY = componentOffsetY - scrollOffset;
                module.setX(posX + 67);
                module.setY(moduleY);
                module.render();
                module.setVisible(moduleY + 35 >= componentStartY && moduleY <= componentStartY + viewHeight);
                module.setVisibleSetting(moduleY + module.getHeight() >= componentStartY && moduleY <= componentStartY + viewHeight);

                componentOffsetY += module.getHeight() + 10;
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void drawText(String text) {
        float watermarkWidth = Fonts.urbanist.get(35).getStringWidth(Demise.INSTANCE.getClientName()) + 2 + Fonts.urbanist.get(24).getStringWidth(Demise.INSTANCE.getVersion());
        int color = inputting ? new Color(193, 193, 193).getRGB() : new Color(119, 119, 119, 255).getRGB();
        Fonts.interRegular.get(18).drawString(text, posX + watermarkWidth + 18, posY + 7 + Fonts.interRegular.get(15).getHeight() - 2, color);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.drawScreen(mouseX, mouseY));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float watermarkWidth = Fonts.urbanist.get(35).getStringWidth(Demise.INSTANCE.getClientName()) + 2 + Fonts.urbanist.get(24).getStringWidth(Demise.INSTANCE.getVersion());
        float calcWidth = 450 - watermarkWidth - 19;
        inputting = MouseUtils.isHovered(posX + watermarkWidth + 13, posY + 7, calcWidth, 20, mouseX, mouseY) && mouseButton == 0;

        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (inputting) {
            String lastFilter = filter;

            if (keyCode == Keyboard.KEY_BACK) {
                if (!filter.isEmpty()) {
                    filter = filter.substring(0, filter.length() - 1);
                }
            }

            if (Character.isLetterOrDigit(typedChar) || keyCode == Keyboard.KEY_SPACE) {
                filter += typedChar;
            }

            if (!lastFilter.equals(filter)) {
                SoundUtil.playSound("demise.tick");
                scrollOffset = 0;
                initCategory();
            }
        }

        moduleComponents.forEach(moduleComponent -> moduleComponent.keyTyped(typedChar, keyCode));
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, state));
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -25 : 25;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + scrollAmount, 0, maxScroll);
        }
    }
}