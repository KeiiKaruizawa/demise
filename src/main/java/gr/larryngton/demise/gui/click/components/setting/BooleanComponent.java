package gr.larryngton.demise.gui.click.components.setting;

import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.gui.click.Component;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.animations.Direction;
import gr.larryngton.demise.utils.animations.impl.SmoothStepAnimation;
import gr.larryngton.demise.utils.render.ColorUtils;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.sound.SoundUtil;

import java.awt.*;

public class BooleanComponent extends Component {
    private final BoolValue setting;
    private final SmoothStepAnimation toggleAnimation = new SmoothStepAnimation(175, 1);
    private final SmoothStepAnimation guideLineAnimation = new SmoothStepAnimation(175, 1);

    public BooleanComponent(BoolValue setting) {
        this.setting = setting;
        this.toggleAnimation.setDirection(Direction.BACKWARDS);
        setHeight(Fonts.interRegular.get(15).getHeight() + 4);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        setDescription(setting.getDescription());

        this.toggleAnimation.setDirection(this.setting.get() ? Direction.FORWARDS : Direction.BACKWARDS);

        Fonts.interRegular.get(15).drawString(setting.getName(), getX() + 4, getY() + 2.5f, -1);

        RectUtils.drawRound(getX() + getWidth() - 15.5f, getY() + 2, 13, 5, 2.25f, ColorUtils.interpolateColorC(new Color(128, 128, 128, 255), Color.white, (float) toggleAnimation.getOutput()).darker());
        RectUtils.drawRound(getX() + getWidth() - 15.5f + 8 * (float) toggleAnimation.getOutput(), getY() + 2f, 5, 5, 2.25f, Color.WHITE);

        float x = getX() + Fonts.interRegular.get(15).getStringWidth(setting.getName()) + 8;
        float y = getY() + 2F;
        float width = getWidth() - 19.5F - (Fonts.interRegular.get(15).getStringWidth(setting.getName()) + 8);
        float height = 5;
        boolean hovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
        guideLineAnimation.setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);
        Color col = ColorUtils.interpolateColorC(new Color(255, 255, 255, 0), new Color(255, 255, 255, 100), (float) guideLineAnimation.getOutput());
        RenderUtils.drawRect(x, y + 2.4F, width, 0.5F, col.getRGB());
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(getX() + getWidth() - 15.5F, getY() + 2, 13, 5, mouseX, mouseY) && mouseButton == 0) {
            this.setting.set(!this.setting.get());
            SoundUtil.playSound("demise.tick");
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean isVisible() {
        return this.setting.canDisplay();
    }

    @Override
    public boolean isChild() {
        return this.setting.isChild();
    }
}
