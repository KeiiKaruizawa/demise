package gr.larryngton.demise.gui.widget.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.impl.combat.AntiBot;
import gr.larryngton.demise.gui.font.FontRenderer;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.render.*;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TeamsWidget extends Widget {
    public TeamsWidget() {
        super("TeamsHUD");
    }

    public List<EntityPlayer> teammates = new ArrayList<>();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        // don't want to waste performance on here, so check every 2 seconds
        if (mc.thePlayer.ticksExisted % 40 == 0) {
            teammates.clear();
            teammates.add(mc.thePlayer);

            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (PlayerUtils.isInTeam(player) && !getModule(AntiBot.class).isBot(player) && player != mc.thePlayer) {
                    teammates.add(player);
                }
            }

            teammates.sort(Comparator.comparing(EntityPlayer::getName));
        }
    }

    @Override
    public void render() {
        float x = renderX;
        float y = renderY;

        for (EntityPlayer entity : teammates) {
            if (entity instanceof AbstractClientPlayer player) {
                if (player != mc.thePlayer) {
                    RectUtils.drawGradientHorizontal(x + width - 72, y + 2, 70, 20, 0, new Color(0, 0, 0, 0), new Color(0, 0, 0, 100));
                } else {
                    RectUtils.drawGradientHorizontal(x + width - 90, y + 8, 80, 10, 0, new Color(255, 255, 255, 0), new Color(255, 255, 255, 100));
                }

                renderPlayerHead(player.getLocationSkin(), x + width - 24, y, 24, 0);
                String str = player.getNameClear();
                FontRenderer fr = Fonts.genshin.get(16);

                float fx = x + width - 21 - fr.getStringWidth(str) - 10;
                float fy = y + (player == mc.thePlayer ? 11.5F : 8);

                fr.drawString(str, fx, fy, -1);

                if (player != mc.thePlayer) {
                    float healthPercentage = PlayerUtils.getActualHealth(player) / player.getMaxHealth();
                    float space = 0.3F;
                    RectUtils.drawRound(x + 39, y + 17, 100 * space, 1, 1, new Color(0, 0, 0, 150));
                    Color col = ColorUtils.interpolateColorC(Color.RED, Color.GREEN.darker(), healthPercentage);
                    RectUtils.drawRound(x + 39, y + 17, (100 * space) * MathHelper.clamp_float(healthPercentage, 0, 1), 1, 1, col);
                }

                y -= 27;
            }
        }

        height = 24;
    }

    private void renderPlayerHead(ResourceLocation resourceLocation, float x, float y, float size, float radius) {
        StencilUtils.initStencilToWrite();
        RenderUtils.drawRoundedRect(x, y, size, size, radius, -1);
        StencilUtils.readStencilBuffer(1);
        GLUtils.startBlend();
        mc.getTextureManager().bindTexture(resourceLocation);
        Gui.drawScaledCustomSizeModalRect(x, y, 16, 8, 8, 8, size, size, 64.0F, 64.0F);
        GLUtils.endBlend();
        StencilUtils.uninitStencilBuffer();

        float padding = 1.5F;
        StencilUtils.initStencilToWrite();
        RenderUtils.drawRoundedRect(x - padding, y - padding, size + padding * 2, size + padding * 2, radius, -1);
        StencilUtils.readStencilBuffer(1);
        GLUtils.startBlend();
        mc.getTextureManager().bindTexture(resourceLocation);
        Gui.drawScaledCustomSizeModalRect(x - padding, y - padding, 48, 8, 8, 8, size + padding * 2, size + padding * 2, 64.0F, 64.0F);
        GLUtils.endBlend();
        StencilUtils.uninitStencilBuffer();
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Teams hud");
    }
}