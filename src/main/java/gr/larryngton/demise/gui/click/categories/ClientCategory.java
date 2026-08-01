package gr.larryngton.demise.gui.click.categories;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.features.values.Value;
import gr.larryngton.demise.features.values.impl.*;
import gr.larryngton.demise.gui.click.Component;
import gr.larryngton.demise.gui.click.ICategory;
import gr.larryngton.demise.gui.click.PanelGui;
import gr.larryngton.demise.gui.click.components.setting.*;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Mouse;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import static gr.larryngton.demise.gui.click.PanelGui.posY;

@Getter
@Setter
public class ClientCategory implements ICategory {
    private boolean isSelected;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float maxScroll = 0;
    private float x, y;
    private String name = "Client";
    private boolean isHovered;
    private Component cachedComponent;
    private int alpha;
    private float interpolatedY;
    private final TimerUtil alphaTimer = new TimerUtil();
    private float slideProgress = 0F;

    public ClientCategory(float x, float y) {
        this.x = x;
        this.y = y;
        this.isSelected = false;
        this.isHovered = false;
        this.interpolatedX = x;

        for (Value value : Demise.INSTANCE.getClientSettings().getValues()) {
            if (value instanceof BoolValue boolValue) {
                settings.add(new BooleanComponent(boolValue));
            }
            if (value instanceof ColorValue colorValue) {
                settings.add(new ColorPickerComponent(colorValue));
            }
            if (value instanceof SliderValue sliderValue) {
                settings.add(new SliderComponent(sliderValue));
            }
            if (value instanceof ModeValue modeValue) {
                settings.add(new ModeComponent(modeValue));
            }
            if (value instanceof MultiBoolValue multiBoolValue) {
                settings.add(new MultiBooleanComponent(multiBoolValue));
            }
            if (value instanceof TextValue textValue) {
                settings.add(new StringComponent(textValue));
            }
            if (value instanceof DoubleSliderValue doubleSliderValue) {
                settings.add(new DoubleSliderComponent(doubleSliderValue));
            }
        }
    }

    public void initCategory() {
        slideProgress = 0;
    }

    @Override
    public boolean isHovered(int mouseX, int mouseY) {
        boolean state = MouseUtils.isHovered(x, y, Fonts.interRegular.get(18).getStringWidth(getName()), Fonts.interRegular.get(18).getHeight(), mouseX, mouseY);
        isHovered = state;
        return state;
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
    public void drawScreen(int mouseX, int mouseY) {
        if (targetScrollOffset > maxScroll) {
            targetScrollOffset = maxScroll;
        }

        handleScroll();
        float componentStartY = posY + 12 + Fonts.urbanist.get(35).getHeight();
        float viewHeight = 255;

        float totalHeight = 0;

        for (Component component : settings) {
            if (component.isVisible()) {
                totalHeight += component.getHeight();
            }
        }

        maxScroll = Math.max(0, totalHeight - viewHeight);
        scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.25f);

        float yOffset = 0;
        Component hoveredComponent = null;

        float componentOffsetY = componentStartY + 2;
        float y = componentOffsetY - scrollOffset;
        float x = this.x + 60;
        float width = 375;

        RenderUtils.scissor(x, PanelGui.posY + 12 + Fonts.urbanist.get(35).getHeight(), 375, 255, PanelGui.interpolatedScale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        slideProgress = MathUtils.interpolate(slideProgress, 1, 0.2f);

        float slideOffset = (width / 4) * (1.0f - slideProgress);
        x += slideOffset;

        for (Component component : settings) {
            if (!component.isVisible()) continue;

            component.setX(component.isChild() ? x + 5 : x);
            component.setY(y + yOffset + 1);
            component.setWidth(component.isChild() ? width - 5 : width);

            component.drawScreen(mouseX, mouseY);

            if (component.isChild()) {
                RenderUtils.drawRect(x + 3.5f, component.getY() - 2.8f, 1, component.getHeight(), Color.gray.getRGB());
            }

            if (component.isHovered(mouseX, mouseY)) {
                hoveredComponent = component;
            }

            yOffset += component.getHeight();
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (hoveredComponent != null && hoveredComponent.isVisible() && mouseY <= PanelGui.posY + 12 + Fonts.urbanist.get(35).getHeight() + 255 && mouseY >= PanelGui.posY + 12 + Fonts.urbanist.get(35).getHeight() && !hoveredComponent.getDescription().isEmpty()) {
            cachedComponent = hoveredComponent;

            if (alphaTimer.hasTimeElapsed(10)) {
                alpha = Math.min(alpha + 10, 255);
                alphaTimer.reset();
            }

            interpolatedY = MathUtils.interpolate(interpolatedY, mouseY + 3 - (Fonts.interRegular.get(15).getHeight() + 5) / 2f, 0.15f);
        } else {
            if (alphaTimer.hasTimeElapsed(10)) {
                alpha = Math.max(alpha - 10, 0);
                alphaTimer.reset();
            }

            if (alpha == 0) {
                cachedComponent = null;
            }
        }

        if (alpha > 0 && cachedComponent != null) {
            String str = cachedComponent.getDescription();
            float x1 = this.x + width + 15;
            float floatA = (float) alpha;
            int maxWidth = Math.min(Fonts.interRegular.get(15).getStringWidth(str), 200);

            Color i = new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor(), true);
            Color finalColor = new Color(i.getRed(), i.getGreen(), i.getBlue(), (int) ((floatA / 255) * i.getAlpha()));

            ArrayList<String> lines = new ArrayList<>();

            StringBuilder currentLine = new StringBuilder();
            for (String word : str.split(" ")) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (Fonts.interRegular.get(15).getStringWidth(testLine) <= maxWidth) {
                    currentLine.setLength(0);
                    currentLine.append(testLine);
                } else {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                    currentLine.append(word);
                }
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }

            float lineHeight = Fonts.interRegular.get(15).getHeight() + 2;
            float totalHeight1 = lines.size() * lineHeight + 5;
            RectUtils.drawRound(x1, interpolatedY - 2.5f, maxWidth + 6, totalHeight1 - 2.5f, 5, finalColor);

            for (int j = 0; j < lines.size(); j++) {
                Fonts.interRegular.get(15).drawString(lines.get(j), x1 + 3, interpolatedY + 2.5f + j * lineHeight, new Color(255, 255, 255, alpha).getRGB());
            }

            Demise.INSTANCE.getShaderManager().addToShadowQueue(() -> RectUtils.drawShaderRound(x1, interpolatedY - 2.5f, maxWidth + 6, totalHeight1 - 2.5f, 5, new Color(0, 0, 0, alpha)));
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float componentStartY = PanelGui.posY + 12 + Fonts.urbanist.get(35).getHeight();

        if (mouseY > componentStartY && mouseY < componentStartY + 255) {
            settings.forEach(value -> {
                if (value.isVisible()) {
                    value.mouseClicked(mouseX, mouseY, mouseButton);
                }
            });
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        float componentStartY = PanelGui.posY + 12 + Fonts.urbanist.get(35).getHeight();

        if (mouseY > componentStartY && mouseY < componentStartY + 255) {
            settings.forEach(value -> {
                if (value.isVisible()) {
                    value.mouseReleased(mouseX, mouseY, state);
                }
            });
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        settings.forEach(value -> value.keyTyped(typedChar, keyCode));
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -25 : 25;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + scrollAmount, 0, maxScroll);
        }
    }
}