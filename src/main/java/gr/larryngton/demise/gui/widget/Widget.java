package gr.larryngton.demise.gui.widget;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.gui.widget.impl.customWidgets.ChatWidget;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.render.ColorUtils;
import gr.larryngton.demise.utils.render.MouseUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjglx.input.Mouse;

import java.awt.*;

public abstract class Widget implements InstanceAccess {
    @Expose
    @SerializedName("name")
    public String name;
    @Expose
    @SerializedName("x")
    public float x;
    @Expose
    @SerializedName("y")
    public float y;
    protected float renderX, renderY;
    public float width;
    public float height;
    public boolean dragging;
    private int dragX, dragY;
    protected ScaledResolution sr;
    protected static Interface setting = INSTANCE.getModuleManager().getModule(Interface.class);
//    private float accumX;
//    private float accumY;
    private static final float snapThreshold = 5F;
    private static final float edgeSnapSpace = 5F;
    protected boolean lastShouldRender;
    private float interpolatedOffsetY;

    public Widget(String name) {
        this.name = name;
        this.x = 0F;
        this.y = 0F;
        this.width = 100F;
        this.height = 100F;
    }

    public abstract void render();

    public void updatePos() {
        sr = new ScaledResolution(mc);

        renderX = x * sr.getScaledWidth();
        renderY = y * sr.getScaledHeight();

        if (!(this instanceof ChatWidget)) {
            float targetY = 0;
            if (renderY + height >= sr.getScaledHeight() - 17 && GuiNewChat.getChatOpen()) {
                targetY = (renderY + height) - (sr.getScaledHeight() - 17) + 14;
            }

            interpolatedOffsetY = MathUtils.interpolate(interpolatedOffsetY, targetY, 0.25F);
            renderY -= interpolatedOffsetY;
        }
    }

    public final void onDragGUI(int mouseX, int mouseY, boolean drag, boolean snap) {
        boolean hovering = MouseUtils.isHovered(renderX, renderY, width, height, mouseX, mouseY);

        if (hovering) {
            if (dragging) {
                RectUtils.drawRoundOutline(renderX - 1, renderY - 1, width + 2, height + 2, 4, 1, new Color(0, 0, 0, 0), Color.WHITE);
            } else {
                RectUtils.drawRoundOutline(renderX - 1, renderY - 1, width + 2, height + 2, 4, 1, new Color(0, 0, 0, 0), new Color(125, 125, 125));
            }

            if (Mouse.isButtonDown(0) && !dragging && drag) {
                dragging = true;
                dragX = mouseX;
                dragY = mouseY;
            }
        } else {
            int c = ColorUtils.colorSwitch(new Color(100, 100, 100), new Color(50, 50, 50), 2000.0F, 0, 75L, 1).getRGB();
            RectUtils.drawRoundOutline(renderX - 1, renderY - 1, width + 2, height + 2, 4, 1, new Color(0, 0, 0, 0), new Color(c, true));
        }

        if (!Mouse.isButtonDown(0)) dragging = false;

        if (dragging) {
            float deltaX = (float) (mouseX - dragX) / sr.getScaledWidth();
            float deltaY = (float) (mouseY - dragY) / sr.getScaledHeight();

            x += deltaX;
            y += deltaY;
//            accumX += deltaX;
//            accumY += deltaY;

            dragX = mouseX;
            dragY = mouseY;

            if (snap) {
                applySnapping(deltaX, deltaY);
            }

//            if (renderX < 0F) {
//                x = renderX = 0F;
//            }
//
//            if (renderX > sr.getScaledWidth() - width) {
//                float fixedX = sr.getScaledWidth() - width;
//                x = fixedX / sr.getScaledWidth();
//                renderX = fixedX;
//            }
//
//            if (renderY < 0F) {
//                y = renderY = 0F;
//            }
//
//            if (renderY > sr.getScaledHeight() - height) {
//                float fixedY = sr.getScaledHeight() - height;
//                y = fixedY / sr.getScaledHeight();
//                renderY = fixedY;
//            }
        }
    }

    private void applySnapping(float deltaX, float deltaY) {
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        float centerX = screenW / 2F;
        float centerY = screenH / 2F;

        float widgetCenterX = renderX + width / 2F;
        float widgetCenterY = renderY + height / 2F;

//        float lastX = x;
//        float lastY = y;

        float edgeSnapSpaceX = edgeSnapSpace / screenW;
        float edgeSnapSpaceY = edgeSnapSpace / screenH;

        float left = renderX;
        float right = renderX + width;
        float top = renderY;
        float bottom = renderY + height;

        if (Math.abs(widgetCenterX - centerX) < snapThreshold) {
            x = (centerX - width / 2F) / screenW;
        }

        if (Math.abs(widgetCenterY - centerY) < snapThreshold) {
            y = (centerY - height / 2F) / screenH;
        }

        if (Math.abs(left) < snapThreshold) {
            x = edgeSnapSpaceX;
        }

        if (Math.abs(right - screenW) < snapThreshold) {
            x = (screenW - width) / screenW - edgeSnapSpaceX;
        }

        if (Math.abs(top) < snapThreshold) {
            y = edgeSnapSpaceY;
        }

        if (Math.abs(bottom - screenH) < snapThreshold) {
            y = (screenH - height) / screenH - edgeSnapSpaceY;
        }

        for (Widget other : Demise.INSTANCE.getWidgetManager().widgetList) {
            if (!other.shouldRender() || other == this) continue;

            float oLeft = other.renderX;
            float oRight = other.renderX + other.width;
            float oTop = other.renderY;
            float oBottom = other.renderY + other.height;

            boolean verticalOverlap = bottom > oTop && top < oBottom;
            boolean horizontalOverlap = right > oLeft && left < oRight;

            if (verticalOverlap && Math.abs(right - oLeft) < snapThreshold) {
                x = (oLeft - width) / screenW - edgeSnapSpaceX;
            }

            if (verticalOverlap && Math.abs(left - oRight) < snapThreshold) {
                x = oRight / screenW + edgeSnapSpaceX;
            }

            if (horizontalOverlap && Math.abs(bottom - oTop) < snapThreshold) {
                y = (oTop - height) / screenH - edgeSnapSpaceY;
            }

            if (horizontalOverlap && Math.abs(top - oBottom) < snapThreshold) {
                y = oBottom / screenH + edgeSnapSpaceY;
            }
        }

        renderX = x * sr.getScaledWidth();
        renderY = y * sr.getScaledHeight();

//        if (x == lastX) {
//            accumX -= deltaX;
//        }
//
//        if (y == lastY) {
//            accumY -= deltaY;
//        }
    }

    public <M extends Module> M getModule(Class<M> clazz) {
        return Demise.INSTANCE.getModuleManager().getModule(clazz);
    }

    public abstract boolean shouldRender();

    public void onEnable() {
    }

    public void onDisable() {
    }
}
