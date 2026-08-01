package gr.larryngton.demise.gui.widget.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.utils.render.RectUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;

import java.awt.*;
import java.util.ArrayList;

public class InfoWidget extends Widget {
    public InfoWidget() {
        super("Info");
        this.x = 0.8848025f;
        this.y = 0.962963f;
    }

    @Override
    public void render() {
        draw(false);
        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> draw(true));
    }

    private void draw(boolean shader) {
        float x;

        int leftClicks = Demise.INSTANCE.getCPSTracker().getLeftClicks();
        int rightClicks = Demise.INSTANCE.getCPSTracker().getRightClicks();

        String str1 = Minecraft.getDebugFPS() + " FPS";
        String cpsText = leftClicks + " | " + rightClicks + " CPS";
        float width1 = Fonts.interRegular.get(15).getStringWidth(str1) + 7;
        String name = Demise.INSTANCE.getClientSettings().getUsername();
        float width2 = Fonts.interRegular.get(15).getStringWidth(name) + 6;
        float width3 = Fonts.interRegular.get(15).getStringWidth(cpsText) + 7;

        if (renderX < sr.getScaledWidth() / 2f) {
            x = renderX;
        } else {
            x = renderX + width - (width1 + width2 + width3 + 10);
        }

        float height = Fonts.interRegular.get(15).getHeight() + 5;
        float textY = (float) (renderY + Fonts.interRegular.get(15).getHeight() + 0.5 - Fonts.interRegular.get(15).getHeight() / 2f);
        float x1 = x + width3 + 5;
        float x2 = x1 + width1 + 5;

        this.height = height;

        if (!shader) {
            RectUtils.drawRound(x, renderY, width3, height, 3, new Color(setting.bgColor(), true));
            Fonts.interRegular.get(15).drawGradient(cpsText, x + 3, textY, (index) -> new Color(setting.color(index)));

            RectUtils.drawRound(x1, renderY, width1, height, 3, new Color(setting.bgColor(), true));
            Fonts.interRegular.get(15).drawGradient(str1, x1 + 3, textY, (index) -> new Color(setting.color(index)));

            RectUtils.drawRound(x2, renderY, width2, height, 3, new Color(setting.bgColor(), true));
            Fonts.interRegular.get(15).drawGradient(name, x2 + 3, textY, (index) -> new Color(setting.color(index)));
        } else {
            RectUtils.drawShaderRound(x, renderY, width3, height, 3, Color.black);
            RectUtils.drawShaderRound(x1, renderY, width1, height, 3, Color.black);
            RectUtils.drawShaderRound(x2, renderY, width2, height, 3, Color.black);
        }
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Info") && setting.infoMode.is("Default");
    }
}