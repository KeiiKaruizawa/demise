package gr.larryngton.demise.gui.widget.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventPriority;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.handlers.clicking.ClickHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.MathHelper;
import org.lwjglx.input.Mouse;
import org.lwjglx.util.vector.Vector2f;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class MouseStrokesWidget extends Widget {
    private final Key LMB = new Key(new float[]{0, 60}, 27, mc.gameSettings.keyBindAttack.getKeyCode());
    private final Key RMB = new Key(new float[]{30, 60}, 27, mc.gameSettings.keyBindUseItem.getKeyCode());
    private final List<Key> keyList = Arrays.asList(LMB, RMB);
    private final Vector2f dotPos = new Vector2f(0, 0);
    private float[] rotDeltas = new float[]{0, 0};
    private float[] prevRot = new float[]{0, 0};
    private final TimerUtil rPressTimer = new TimerUtil();

    public MouseStrokesWidget() {
        super("Mouse strokes");
        this.width = 57;
        this.height = 77;
    }

    @Override
    public void render() {
        drawMouseStrokes(false);
        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> drawMouseStrokes(true));
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (RotationHandler.shouldRotate()) {
            rotDeltas = new float[]{-RotationHandler.yawDiff, -RotationHandler.pitchDiff};
        } else {
            rotDeltas[0] = mc.thePlayer.getRotation()[0] - prevRot[0];
            rotDeltas[1] = mc.thePlayer.getRotation()[1] - prevRot[1];
        }

        prevRot = mc.thePlayer.getRotation();
    }

    private void drawMouseStrokes(boolean shader) {
        if (!shader) {
            RectUtils.drawRound(renderX, renderY, width, 57, 6, new Color(setting.bgColor(), true));

            float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f1 = f * f * f * 8.0F;

            float yawDelta = rotDeltas[0];
            float pitchDelta = rotDeltas[1];

            float mouseDX = yawDelta / 0.15F * f1;
            float mouseDY = pitchDelta / 0.15F * f1;

            dotPos.x += mouseDX / 17;
            dotPos.y += mouseDY * ((float) mc.displayWidth / mc.displayHeight) / 17;
        } else {
            RectUtils.drawShaderRound(renderX, renderY, width, 57, 6, Color.black);
        }

        dotPos.x = MathHelper.clamp_float(dotPos.x, -width / 2, width / 2);
        dotPos.y = MathHelper.clamp_float(dotPos.y, -57f / 2, 57f / 2);

        float[] dot00 = new float[]{renderX + width / 2, renderY + 57f / 2};
        float[] dotXY = new float[]{dot00[0] + dotPos.x, dot00[1] + dotPos.y};

        RenderUtils.drawCircle(dotXY[0], dotXY[1], 0, 360, 2, 0.5f, false, setting.color());

        if (!shader) {
            dotPos.x = MathUtils.interpolate(dotPos.x, 0, 0.25f);
            dotPos.y = MathUtils.interpolate(dotPos.y, 0, 0.25f);
        }

        LMB.setName("LMB");
        RMB.setName("RMB");

        LMB.setPressed(Mouse.isButtonDown(0) || ClickHandler.clickingNow);

        RenderUtils.renderKeys(renderX, renderY, keyList, shader, 4);

        if (rPressTimer.hasTimeElapsed(50)) {
            RMB.setPressed(false);
        }
    }

    @EventTarget
    @EventPriority(-100)
    public void onPacket(PacketEvent e) {
        if (e.getState() != PacketEvent.State.OUTGOING) return;

        if (Mouse.isButtonDown(1) || e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            RMB.setPressed(true);
            rPressTimer.reset();
        }
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Mouse strokes");
    }
}