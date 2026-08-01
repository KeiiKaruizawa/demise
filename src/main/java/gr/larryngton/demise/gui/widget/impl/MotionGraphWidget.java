package gr.larryngton.demise.gui.widget.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// it can't get more Russian than this
public class MotionGraphWidget extends Widget {
    private final List<Double> speeds = new ArrayList<>();
    private final List<Double> averageSpeeds = new ArrayList<>();
    private static final int MAX_POINTS = 50;
    private float interpolatedMaxSpeed = 10;
    private float interpolatedTextY = renderY + 2;
    private double prevSpeed;
    private float interpolatedTextY1 = renderY + 2;

    public MotionGraphWidget() {
        super("Motion graph");
        this.width = 150;
        this.height = 50;
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
            prevSpeed = MathUtils.interpolateNoUpdateCheck((float) prevSpeed, (float) MoveUtils.getBPS(), 0.5f);
            speeds.add(prevSpeed);

            double speedsSum = 0;
            for (double val : speeds) {
                speedsSum += val;
            }

            averageSpeeds.add(speedsSum / speeds.size());

            if (speeds.size() > MAX_POINTS) {
                speeds.remove(0);
            }

            if (averageSpeeds.size() > MAX_POINTS) {
                averageSpeeds.remove(0);
            }
        }
    }

    @Override
    public void render() {
        float lastY = interpolatedTextY;
        float lastY1 = interpolatedTextY1;

        RectUtils.drawRound(renderX, renderY, width, height, 3, new Color(setting.bgColor(), true));

        float gridSpacing = width / 20;
        for (int i = 1; i < 20; i++) {
            float x = renderX + (gridSpacing * i);
            RenderUtils.drawLine(x, renderY, x, renderY + height, 0.5f, new Color(100, 100, 100, 75).getRGB());
        }

        gridSpacing = height / 10;
        for (int i = 1; i < 10; i++) {
            float y = renderY + (gridSpacing * i);
            RenderUtils.drawLine(renderX, y, renderX + width, y, 0.5f, new Color(100, 100, 100, 75).getRGB());
        }

        if (!speeds.isEmpty()) {
            float xStep = width / (MAX_POINTS - 1f);

            //average
            for (int i = 0; i < averageSpeeds.size() - 1; i++) {
                float x1 = renderX + (i * xStep);
                float x2 = renderX + ((i + 1) * xStep);
                float y1 = renderY + height - (float) (averageSpeeds.get(i) / interpolatedMaxSpeed * height);
                float y2 = renderY + height - (float) (averageSpeeds.get(i + 1) / interpolatedMaxSpeed * height);

                lastY1 = y2;

                RenderUtils.drawLine(x1, y1, x2, y2, 1, new Color(100, 100, 100).getRGB());
            }

            //current
            float maxSpeed = 10;

            for (double speed : speeds) {
                maxSpeed = Math.max(maxSpeed, (float) speed + 2);
            }
            maxSpeed = Math.max(maxSpeed, 1f);

            interpolatedMaxSpeed = MathUtils.interpolate(interpolatedMaxSpeed, maxSpeed, 0.75f);

            for (int i = 0; i < speeds.size() - 1; i++) {
                float x1 = renderX + (i * xStep);
                float x2 = renderX + ((i + 1) * xStep);
                float y1 = renderY + height - (float) (speeds.get(i) / interpolatedMaxSpeed * height);
                float y2 = renderY + height - (float) (speeds.get(i + 1) / interpolatedMaxSpeed * height);

                lastY = y2;

                RenderUtils.drawLine(x1, y1, x2, y2, 1, setting.color(i));
            }
        }

        String speedText1 = String.format("%.2f avg", averageSpeeds.get(averageSpeeds.size() - 1)) + " -";
        Fonts.interRegular.get(15).drawString(speedText1, renderX - 2 - Fonts.interRegular.get(15).getStringWidth(speedText1), interpolatedTextY1 - 2.1, new Color(150, 150, 150).getRGB());

        interpolatedTextY = MathUtils.interpolate(interpolatedTextY, lastY, 0.25f);

        String speedText = "- " + String.format("%.2f bps", MoveUtils.getBPS());
        Fonts.interRegular.get(15).drawString(speedText, renderX + width + 2, interpolatedTextY - 2.1, setting.color());

        interpolatedTextY1 = MathUtils.interpolate(interpolatedTextY1, lastY1, 0.25f);

        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> RectUtils.drawShaderRound(renderX, renderY, width, height, 3, Color.BLACK));
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Motion graph");
    }
}