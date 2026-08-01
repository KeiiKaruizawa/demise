package gr.larryngton.demise.gui.widget;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MouseMoveEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.widget.impl.*;
import gr.larryngton.demise.gui.widget.impl.customWidgets.ChatWidget;
import gr.larryngton.demise.gui.widget.impl.customWidgets.HotbarWidget;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class WidgetManager implements InstanceAccess {
    public List<Widget> widgetList = new ArrayList<>();
    private boolean lastGrabbed;
    private int alpha;
    private final TimerUtil alphaTimer = new TimerUtil();
    private int snapHelperAlpha;
    private int lastX, lastY;

    public WidgetManager() {
        INSTANCE.getEventManager().register(this);

        register(
                new MotionGraphWidget(),
                new ModuleListWidget(),
                new TargetHudWidget(),
                new KeystrokeWidget(),
                new InfoWidget(),
                new RadarWidget(),
                new DebugWidget(),
                new PotionHUDWidget(),
                new MouseStrokesWidget(),
                new TeamsWidget(),
                new HotbarWidget(),
                new ChatWidget(),
                new SpotifyWidget()
        );
    }

    public boolean loaded;

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        ScaledResolution sr = new ScaledResolution(mc);

        if (isInDragGui()) {
            if (lastGrabbed) {
                mc.mouseHelper.ungrabMouseCursor();
                lastGrabbed = false;
            }

            Widget draggingWidget = null;
            for (Widget widget : widgetList) {
                if (widget.shouldRender() && widget.dragging) {
                    draggingWidget = widget;
                    break;
                }
            }

            int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
            int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;

            if (snapHelperAlpha > 0) {
                float screenCenterX = sr.getScaledWidth() / 2F;
                float screenCenterY = sr.getScaledHeight() / 2F;

                int color = new Color(255, 255, 255, snapHelperAlpha).getRGB();
                RenderUtils.drawRect(screenCenterX - 0.25F, 0, 0.5F, sr.getScaledHeight(), color);
                RenderUtils.drawRect(0, screenCenterY - 0.25F, sr.getScaledWidth(), 0.5F, color);
            }

            float deltaX = (float) (mouseX - lastX) / sr.getScaledWidth();
            float deltaY = (float) (mouseY - lastY) / sr.getScaledHeight();
            double mouseVelo = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            lastX = mouseX;
            lastY = mouseY;

            for (Widget widget : widgetList) {
                if (widget.shouldRender()) {
                    widget.onDragGUI(mouseX, mouseY, (draggingWidget == null || draggingWidget == widget), Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && mouseVelo < 0.005);
                    if (widget.dragging) draggingWidget = widget;
                }
            }

            if (alphaTimer.hasTimeElapsed((long) 16.67)) {
                alpha = Math.min(alpha + 5, 125);

                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                    snapHelperAlpha = Math.min(snapHelperAlpha + 5, 150);
                } else {
                    snapHelperAlpha = Math.max(snapHelperAlpha - 5, 0);
                }

                alphaTimer.reset();
            }
        } else {
            if (!lastGrabbed && mc.currentScreen == null) {
                mc.mouseHelper.grabMouseCursor();
            }

            if (alphaTimer.hasTimeElapsed((long) 16.67)) {
                alpha = Math.max(alpha - 5, 0);
                snapHelperAlpha = Math.min(snapHelperAlpha - 5, 0);

                alphaTimer.reset();
            }

            lastGrabbed = Mouse.isGrabbed();
        }

        if (alpha > 0) {
            Fonts.interRegular.get(18).drawCenteredString("Use LMB to drag widgets around", sr.getScaledWidth() / 2f, 20, new Color(255, 255, 255, alpha).getRGB());
            Fonts.interRegular.get(18).drawCenteredString("Hold LSHIFT for widget snapping", sr.getScaledWidth() / 2f, 20 + Fonts.interRegular.get(18).getHeight() + 4, new Color(255, 255, 255, alpha).getRGB());
        }

        for (Widget widget : widgetList) {
            boolean flag = widget.shouldRender();

            if (flag && !widget.lastShouldRender) {
                Demise.INSTANCE.getEventManager().register(widget);
                widget.onEnable();
            }

            if (widget.lastShouldRender && !flag) {
                Demise.INSTANCE.getEventManager().unregister(widget);
                widget.onDisable();
            }

            widget.lastShouldRender = flag;

            if (flag) {
                widget.updatePos();
                widget.render();
            }
        }
    }

    @EventTarget
    public void onMouseMove(MouseMoveEvent e) {
        if (isInDragGui()) {
//            int mouseX = Mouse.getX();
//            int mouseY = Display.getHeight() - Mouse.getY();
//
//            if (mouseX >= mc.displayWidth - 1) {
//                Mouse.setCursorPosition(3, mouseY);
//                e.setDeltaX(0);
//                e.setDeltaY(e.getDeltaY() / 4);
//                return;
//            }
//
//            if (mouseX <= 1) {
//                Mouse.setCursorPosition(mc.displayWidth - 3, mouseY);
//                e.setDeltaX(0);
//                e.setDeltaY(e.getDeltaY() / 4);
//                return;
//            }
//
//            if (mouseY <= 1) {
//                Mouse.setCursorPosition(mouseX, mc.displayHeight - 3);
//                e.setDeltaX(e.getDeltaX() / 4);
//                e.setDeltaY(0);
//                return;
//            }
//
//            if (mouseY >= mc.displayHeight - 1) {
//                Mouse.setCursorPosition(mouseX, 3);
//                e.setDeltaX(e.getDeltaX() / 4);
//                e.setDeltaY(0);
//                return;
//            }

            e.setDeltaX(e.getDeltaX() / 4);
            e.setDeltaY(e.getDeltaY() / 4);
        }
    }

    public static boolean isInDragGui() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiChat;
    }

    private void register(Widget... widgets) {
        this.widgetList.addAll(Arrays.asList(widgets));
    }

    public <module extends Widget> module get(Class<? extends module> moduleClass) {
        Iterator<Widget> var2 = this.widgetList.iterator();
        Widget module;
        do {
            if (!var2.hasNext()) {
                return null;
            }
            module = var2.next();
        } while (module.getClass() != moduleClass);

        return (module) module;
    }
}