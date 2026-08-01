package gr.larryngton.demise.gui.widget.impl.customWidgets;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.player.Scaffold;
import gr.larryngton.demise.features.modules.impl.visual.CustomWidgetsModule;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.gui.widget.WidgetManager;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.SpoofSlotUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static gr.larryngton.demise.gui.ingame.CustomWidgets.f;

public class HotbarWidget extends Widget {
    private float slotX;
    public static float interpolatedExperienceBar;
    private float lastExperience;
    private float lastExpLevel;
    private float interpolatedExperienceProgress;
    private final TimerUtil expTimer = new TimerUtil();
    public static float posX, posY;
    private float interpolatedWidgetWidth;
    private float interpolatedWidgetX;
    private float interpolatedWidgetY = sr == null ? 1080 : sr.getScaledHeight() - 80 - 5;

    public HotbarWidget() {
        super("Hotbar");
        this.x = 0.40572897F;
        this.y = 0.92777777F;
        this.width = 181;
        this.height = 34;
    }

    @Override
    public void render() {
        posX = renderX;
        posY = renderY;

        float renderY = this.renderY;

        //todo proper alignment when the hotbar is high on the screen
        if (y >= 0.5) {
            // INDIA HACK RISE CHATGPT
            renderY += 13;
        }

        float baseWidth = 181;

        // base bar
        float targetPos = renderX + SpoofSlotUtils.getSpoofedSlot() * 20;

        if (slotX == 0) slotX = targetPos;
        slotX = MathUtils.interpolate(slotX, targetPos, 0.25f);

        RectUtils.drawRound(renderX, renderY, baseWidth, 21, 7, new Color(getModule(Interface.class).bgColor(), true));
        RectUtils.drawRound(slotX, renderY, 21, 21, 7, new Color(getModule(Interface.class).bgColor(), true).darker());

        Gui.zLevel = f;

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();

        for (int j = 0; j < 9; ++j) {
            float k = renderX + (j * 20) + 2.5f;
            GuiIngame.renderHotbarItem(j, k, renderY + 2.5F, mc.timer.partialTicks, mc.thePlayer);
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();

        float finalRenderY = renderY;
        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> RectUtils.drawShaderRound(renderX, finalRenderY, baseWidth, 21, 7, Color.black));

        // exp bar
        if (interpolatedExperienceBar < this.renderY) {
            interpolatedExperienceBar = this.renderY;
        }

        RenderUtils.scissor(renderX - 9, renderY - 9, 190, 8);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        if (interpolatedExperienceBar == 0) {
            interpolatedExperienceBar = sr.getScaledHeight() + 10;
        }

        if ((lastExpLevel != mc.thePlayer.experienceLevel || lastExperience != mc.thePlayer.experience) || !expTimer.hasTimeElapsed(2000) || WidgetManager.isInDragGui()) {
            if (lastExpLevel != mc.thePlayer.experienceLevel || lastExperience != mc.thePlayer.experience) {
                expTimer.reset();
            }

            interpolatedExperienceBar = MathUtils.interpolate(interpolatedExperienceBar, renderY - 7, 0.25F);

            lastExperience = mc.thePlayer.experience;
            lastExpLevel = mc.thePlayer.experienceLevel;
        } else {
            interpolatedExperienceBar = MathUtils.interpolate(interpolatedExperienceBar, renderY + 10, 0.04F);
        }

        float expX = renderX;
        float width = baseWidth * mc.thePlayer.experience;
        interpolatedExperienceProgress = MathUtils.interpolate(interpolatedExperienceProgress, width, 0.25F);

        RectUtils.drawRound(expX, interpolatedExperienceBar, baseWidth, 2, 1, new Color(0, 0, 0, 150));
        RectUtils.drawGradientHorizontal(expX, interpolatedExperienceBar, interpolatedExperienceProgress, 2, 1, new Color(getModule(Interface.class).color()), new Color(getModule(Interface.class).color((int) interpolatedExperienceProgress)));

        float finalRenderY1 = renderY;
        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
            RenderUtils.scissor(renderX - 9, finalRenderY1 - 9, 190, 8);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RectUtils.drawShaderRound(expX, interpolatedExperienceBar, baseWidth, 2, 1, Color.black);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        });

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        drawItemWidget(renderX + 90.5F, false);
        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> drawItemWidget(renderX + 90.5F, true));
    }

    private void drawItemWidget(float i, boolean shader) {
        ItemStack heldItem = mc.thePlayer.getCurrentEquippedItem();
        int count = heldItem == null ? 0 : getModule(Scaffold.class).getBlockCount();
        String countStr = String.valueOf(count);
        float blockWH = 15;
        int spacing = 3;

        float totalWidth;
        String text;

        if (SpoofSlotUtils.isSpoofing()) {
            if (heldItem != null) {
                if (heldItem.getItem() instanceof ItemBlock) {
                    text = countStr;
                } else {
                    text = "";
                }

                float textWidth = Fonts.interMedium.get(18).getStringWidth(text);

                if (heldItem.getItem() instanceof ItemBlock) {
                    totalWidth = ((textWidth + blockWH + spacing) + 6);
                } else {
                    totalWidth = ((blockWH + spacing) + 3);
                }
            } else {
                text = "";
                totalWidth = ((Fonts.interMedium.get(18).getStringWidth(text) + blockWH + spacing) + 3);
            }
        } else {
            text = "";
            totalWidth = ((Fonts.interMedium.get(18).getStringWidth(text) + blockWH + spacing) + 3);
        }

        float height = 20;

        float renderX = (i - totalWidth / 2);
        float renderY = (this.renderY - 40);

        if (!shader) {
            if (SpoofSlotUtils.isSpoofing()) {
                interpolatedWidgetY = MathUtils.interpolate(interpolatedWidgetY, renderY, 0.25f);
            } else {
                interpolatedWidgetY = MathUtils.interpolate(interpolatedWidgetY, sr.getScaledHeight() + height, 0.25f);
            }

            interpolatedWidgetWidth = MathUtils.interpolate(interpolatedWidgetWidth, totalWidth, 0.25f);
            interpolatedWidgetX = MathUtils.interpolate(interpolatedWidgetX, renderX, 0.25f);
        }

//        RenderUtils.scissor(sr.getScaledWidth() / 2F - 90.5F, renderY - 5, 181, Math.min(sr.getScaledHeight() - 42, interpolatedExperienceBar - 12) - renderY - 5);
//        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        if (!shader) {
            RectUtils.drawRound(interpolatedWidgetX, interpolatedWidgetY, interpolatedWidgetWidth, height, 7, new Color(getModule(Interface.class).bgColor(), true));

            Fonts.interRegular.get(18).drawGradient(text, interpolatedWidgetX + 3 + blockWH + spacing, interpolatedWidgetY + height / 2F - Fonts.interMedium.get(18).getHeight() / 2F + 2.5f, (index) -> new Color(getModule(Interface.class).color(index)));

            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(heldItem, interpolatedWidgetX + 2, (interpolatedWidgetY + 9.5f - (blockWH / 2)));
            RenderHelper.disableStandardItemLighting();
        } else {
            RectUtils.drawShaderRound(interpolatedWidgetX, interpolatedWidgetY, interpolatedWidgetWidth, height, 7, Color.black);
        }

//        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public boolean shouldRender() {
        return getModule(CustomWidgetsModule.class).isEnabled() && getModule(CustomWidgetsModule.class).hotbar.get() && getModule(CustomWidgetsModule.class).hotbarMode.is("Default");
    }
}
