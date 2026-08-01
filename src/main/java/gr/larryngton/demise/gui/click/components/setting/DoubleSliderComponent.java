package gr.larryngton.demise.gui.click.components.setting;

import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.gui.click.Component;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.sound.SoundUtil;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DoubleSliderComponent extends Component {
    private final DoubleSliderValue setting;
    private float animStart;
    private float animEnd;
    private boolean draggingStart;
    private boolean draggingEnd;
    private final TimerUtil soundTimer = new TimerUtil();
    private float prevStart;
    private float prevEnd;

    public DoubleSliderComponent(DoubleSliderValue setting) {
        this.setting = setting;
        setHeight(Fonts.interRegular.get(15).getHeight() * 2 + Fonts.interRegular.get(15).getHeight() + 2);
        prevStart = setting.getStart();
        prevEnd = setting.getEnd();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        setDescription(setting.getDescription());

        prevStart = setting.getStart();
        prevEnd = setting.getEnd();

        Fonts.interRegular.get(15).drawString(setting.getName(), getX() + 4, getY() + 2.5f, -1);

        float barY = getY() + Fonts.interRegular.get(15).getHeight() + 2;
        float barX = getX() + 4;
        float barWidth = getWidth() - 8;

        float ratioStart = (setting.getStart() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float ratioEnd = (setting.getEnd() - setting.getMin()) / (setting.getMax() - setting.getMin());

        animStart = RenderUtils.animate(animStart, barWidth * ratioStart, 15);
        animEnd = RenderUtils.animate(animEnd, barWidth * ratioEnd, 15);

        float sliderXStart = barX + animStart;
        float sliderXEnd = barX + animEnd;

        RectUtils.drawRound(barX, barY, barWidth, 2, 1, Color.darkGray);

        float rangeX = Math.min(sliderXStart, sliderXEnd);
        float rangeWidth = Math.abs(sliderXEnd - sliderXStart);
        RectUtils.drawRound(rangeX, barY, rangeWidth, 2, 1, Color.white.darker().darker());

        RenderUtils.drawCircle(sliderXStart, barY + 1, 0, 360, 2, 0.1f, true, Color.white.getRGB());
        RenderUtils.drawCircle(sliderXEnd, barY + 1, 0, 360, 2, 0.1f, true, Color.white.getRGB());

        Fonts.interRegular.get(15).drawString(setting.getMin() + "", barX, getY() + Fonts.interRegular.get(15).getHeight() * 2 + 1, new Color(160, 160, 160).getRGB());
        Fonts.interRegular.get(15).drawCenteredString(setting.getStart() + " - " + setting.getEnd(), getX() + getWidth() / 2, getY() + Fonts.interRegular.get(15).getHeight() * 2 + 1, -1);
        Fonts.interRegular.get(15).drawString(setting.getMax() + "", getX() - 4 + getWidth() - Fonts.interRegular.get(15).getStringWidth(setting.getMax() + ""), getY() + Fonts.interRegular.get(15).getHeight() * 2 + 1, new Color(160, 160, 160).getRGB());

        if (draggingStart || draggingEnd) {
            float clampedRatio = Math.max(0, Math.min(1, (mouseX - getX()) / getWidth()));
            float rawValue = setting.getMin() + (setting.getMax() - setting.getMin()) * clampedRatio;

            BigDecimal valueBD = BigDecimal.valueOf(rawValue);
            BigDecimal incrementBD = BigDecimal.valueOf(setting.getIncrement());
            BigDecimal snapped = valueBD.divide(incrementBD, 0, RoundingMode.HALF_UP).multiply(incrementBD);
            float finalValue = snapped.setScale(getDecimalPoints(String.valueOf(setting.getIncrement())), RoundingMode.HALF_UP).floatValue();

            if (draggingStart) {
                finalValue = Math.min(finalValue, setting.getEnd());
                setting.setValueStart(finalValue);
            } else if (draggingEnd) {
                finalValue = Math.max(finalValue, setting.getStart());
                setting.setValueEnd(finalValue);
            }

            if (soundTimer.hasTimeElapsed(25) && (prevStart != setting.getStart() || prevEnd != setting.getEnd())) {
                SoundUtil.playSound("demise.tick");
                soundTimer.reset();
            }
        }
    }

    public static Integer getDecimalPoints(String n) {
        if (n.contains(".")) {
            return n.replaceAll(".*\\.(?=\\d?)", "").length();
        }
        return 0;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float barX = getX() + 4;
        float barWidth = getWidth() - 8;
        float handleStartX = barX + (barWidth * (setting.getStart() - setting.getMin()) / (setting.getMax() - setting.getMin()));
        float handleEndX = barX + (barWidth * (setting.getEnd() - setting.getMin()) / (setting.getMax() - setting.getMin()));
        float handleY = getY() + Fonts.interRegular.get(15).getHeight() + 3;
        float ratioEnd = (setting.getEnd() - setting.getMin()) / (setting.getMax() - setting.getMin());

        if (mouseButton == 0) {
            if (MouseUtils.isHovered(barX - 5, handleY - 2.5f, handleStartX - barX + 5, 5, mouseX, mouseY)) {
                draggingStart = true;
            } else if (MouseUtils.isHovered(handleEndX - 5, handleY - 2.5f, barWidth - ratioEnd + 5, 5, mouseX, mouseY)) {
                draggingEnd = true;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            draggingStart = false;
            draggingEnd = false;
        }
        super.mouseReleased(mouseX, mouseY, state);
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
