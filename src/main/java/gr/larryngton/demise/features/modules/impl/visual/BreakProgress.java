package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.animations.Animation;
import gr.larryngton.demise.utils.animations.Direction;
import gr.larryngton.demise.utils.animations.impl.DecelerateAnimation;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@ModuleInfo(name = "BreakProgress", description = "Renders your break progress in the HUD.", legit = true)
public class BreakProgress extends Module {
    private final Animation anim = new DecelerateAnimation(175, 1);

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        anim.setDirection(mc.playerController.curBlockDamageMP != 0 ? Direction.FORWARDS : Direction.BACKWARDS);
        if (mc.playerController.curBlockDamageMP == 0 && anim.isDone()) return;
        ScaledResolution sr = new ScaledResolution(mc);
        float x, y;
        float output = (float) anim.getOutput();
        int spacing = 3;
        String text = BigDecimal.valueOf(mc.playerController.curBlockDamageMP * 100).setScale(1, RoundingMode.FLOOR) + "%";
        float textWidth = Fonts.interBold.get(18).getStringWidth(text);

        float totalWidth = ((textWidth + spacing) + 6) * output;
        x = sr.getScaledWidth() / 2f - (totalWidth / 2f);
        y = sr.getScaledHeight() - (sr.getScaledHeight() / 2f - 20);
        float height = 20;
        GL11.glPushMatrix();
        RenderUtils.scissor((float) (x - 1.5), (float) (y - 1.5), totalWidth + 3, height + 3);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RectUtils.drawRound(x, y, totalWidth, height, 5, new Color(INSTANCE.getModuleManager().getModule(Interface.class).bgColor(), true));

        Fonts.interBold.get(18).drawString(text, x + 3 + spacing, y + height / 2F - Fonts.interBold.get(18).getHeight() / 2F + 2.5f, -1);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();
    }

    //todo: migrate to new shader system because this moonlight ahh code is fucking ass
//    @EventTarget
//    public void drawShader2D(ShaderEvent e) {
//        anim.setDirection(mc.playerController.curBlockDamageMP != 0 ? Direction.FORWARDS : Direction.BACKWARDS);
//        if (mc.playerController.curBlockDamageMP == 0 && anim.isDone()) return;
//        ScaledResolution sr = new ScaledResolution(mc);
//        float x, y;
//        float output = (float) anim.getOutput();
//        int spacing = 3;
//        String text = BigDecimal.valueOf(mc.playerController.curBlockDamageMP * 100).setScale(1, RoundingMode.FLOOR) + "%";
//        float textWidth = Fonts.interBold.get(18).getStringWidth(text);
//
//        float totalWidth = ((textWidth + spacing) + 6) * output;
//        x = sr.getScaledWidth() / 2f - (totalWidth / 2f);
//        y = sr.getScaledHeight() - (sr.getScaledHeight() / 2f - 20);
//        float height = 20;
//        GL11.glPushMatrix();
//        RenderUtils.scissor((float) (x - 1.5), (float) (y - 1.5), totalWidth + 3, height + 3);
//        GL11.glEnable(GL11.GL_SCISSOR_TEST);
//        RoundedUtils.drawShaderRound(x, y, totalWidth, height, 5, Color.black);
//        GL11.glDisable(GL11.GL_SCISSOR_TEST);
//        GL11.glPopMatrix();
//    }
}