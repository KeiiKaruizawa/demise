package gr.larryngton.demise.gui.widget.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.modules.impl.misc.Opsec;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.gui.font.FontRenderer;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.gui.widget.WidgetManager;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.render.ColorUtils;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjglx.util.vector.Vector2f;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TargetHudWidget extends Widget {
    private final TargetHud targetHudRenderer = new TargetHud();

    public TargetHudWidget() {
        super("Target hud");
        this.x = 0.44270813f;
        this.y = 0.6333332f;
    }

    private float interpolatedX;
    private float interpolatedY;
    private boolean rightSide;
    public static float interpolatedScale;
    public static float targetScale;
    public EntityLivingBase target;
    private final TimerUtil attackTimer = new TimerUtil();

    @EventTarget
    public void onGameEvent(GameEvent e) {
        KillAura aura = getModule(KillAura.class);

        if (!WidgetManager.isInDragGui()) {
            if (aura.isEnabled() && KillAura.target instanceof EntityPlayer) {
                targetScale = 1;
                target = KillAura.target;
            } else {
                if (attackTimer.hasTimeElapsed(1000)) {
                    targetScale = 0;
                    if (interpolatedScale < 0.01F) {
                        target = null;
                    }
                }
            }
        } else if (target == null) {
            targetScale = 1;
            target = mc.thePlayer;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        KillAura aura = getModule(KillAura.class);

        if ((!aura.isEnabled() || KillAura.target == null) && e.getTargetEntity() instanceof EntityPlayer) {
            targetScale = 1;
            target = (EntityLivingBase) e.getTargetEntity();
            attackTimer.reset();
        }
    }

    @Override
    public void render() {
        if (target != null) {
            switch (setting.targetHudMode.get()) {
                case "New":
                    this.height = 37;
                    this.width = 120;
                    break;
                case "Old":
                    String TargetName = Demise.INSTANCE.getModuleManager().getModule(Opsec.class).isEnabled() && target == mc.thePlayer ? Opsec.name : target.getDisplayName().getFormattedText();

                    double initialDiff = BigDecimal.valueOf((mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) - (PlayerUtils.getActualHealth(target) + target.getAbsorptionAmount())).setScale(2, RoundingMode.FLOOR).doubleValue();

                    if (mc.thePlayer != null) {
                        String status = initialDiff >= 0 ? " §aW" : " §cL";
                        TargetName = TargetName + status;
                    }

                    float current$minX;
                    float current$maxX;
                    float current$minY;
                    float current$maxY;

                    final ScaledResolution scaledResolution = new ScaledResolution(mc);
                    final int padding = 8;
                    final int n3 = mc.fontRendererObj.getStringWidth(TargetName) + padding + 20;
                    final float n4 = scaledResolution.getScaledWidth() / 2f - n3 / 2f + x;
                    final float n5 = scaledResolution.getScaledHeight() / 2f + 15 + y;
                    current$minX = n4 - padding;
                    current$minY = n5 - padding;
                    current$maxX = n4 + n3;
                    current$maxY = n5 + (mc.fontRendererObj.FONT_HEIGHT + 5) - 6 + padding;

                    this.width = current$maxX - current$minX;
                    this.height = current$maxY - current$minY + 8;
                    break;
                case "Novoline":
                    width = 28 + mc.fontRendererObj.getStringWidth(target.getName()) + 40;
                    height = 37;
                    break;
                case "Exhibition":
                    FontRenderer fr = Fonts.exhi.get(18);
                    width = (float) (fr.getStringWidth(target.getName()) > 70.0f ? (double) (124.0f + fr.getStringWidth(target.getName()) - 70.0f) : 124.0);
                    height = 43;
                    break;
                case "Adjust":
                    this.width = Math.max(100, Fonts.tahoma.get(16).getStringWidth(target.getName()) + 28);
                    this.height = 30;
                    break;
                case "Rise 5":
                    this.width = 129;
                    this.height = 48;
                    break;
            }

            renderTargetHUD((EntityPlayer) target);
        }
    }

    private void renderTargetHUD(EntityPlayer target) {
        if (target != null) {
            float x, y;

            if (!setting.trackTarget.get()) {
                x = renderX;
                y = renderY;
            } else {
                Vector2f calcPos = getPos(target);
                x = calcPos.x;
                y = calcPos.y;
            }

            targetHudRenderer.update(x, y, target, setting.targetHudMode.get());
            targetHudRenderer.render();
        }
    }

    private Vector2f getPos(Entity target) {
        float x = (float) MathUtils.interpolate(target.prevPosX, target.posX);
        float y = (float) MathUtils.interpolate(target.prevPosY, target.posY) + target.height - setting.offsetY.get() / 100;
        float z = (float) MathUtils.interpolate(target.prevPosZ, target.posZ);

        Vector2f pos = RenderUtils.worldToScreen(x, y, z, sr, false);

        if (pos == null) {
            interpolatedX = MathUtils.interpolate(interpolatedX, renderX, setting.interpolation.get());
            interpolatedY = MathUtils.interpolate(interpolatedY, renderY, setting.interpolation.get());

            return new Vector2f(interpolatedX, interpolatedY);
        }

        Vector2f rPos = new Vector2f(pos.x, pos.y);
        float rPosX = rPos.x + target.width - width / 2F;

        if (setting.offsetX.get() > 0) {
            double center = sr.getScaledWidth() / 2F;
            if (Math.abs(rPos.x - center) > width / 2F) {
                rightSide = rPos.x < center;
            }

            if (rightSide) {
                rPosX += setting.offsetX.get();
            } else {
                rPosX -= setting.offsetX.get();
            }
        }

        float rPosY = rPos.y;

        if (interpolatedX == 0 && interpolatedY == 0) {
            interpolatedX = rPosX;
            interpolatedY = rPosY;
        } else {
            interpolatedX = MathUtils.interpolate(interpolatedX, rPosX, setting.interpolation.get());
            interpolatedY = MathUtils.interpolate(interpolatedY, rPosY, setting.interpolation.get());
        }

        return new Vector2f(interpolatedX, interpolatedY);
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Target hud");
    }
}

@Getter
@Setter
class TargetHud implements InstanceAccess {
    private float x, y, width, height;
    private EntityPlayer target;
    private EntityPlayer lastTarget;
    private Color avgTargetSkinColor = Color.BLACK;
    private Interface setting = INSTANCE.getModuleManager().getModule(Interface.class);
    private final DecimalFormat decimalFormat = new DecimalFormat("0.0");
    private String mode;
    private Color interpolatedBloomColor = new Color(0, 0, 0, 0);
    private Color interpolatedStatusOutlineColor = new Color(0, 0, 0, 0);
    private final TimerUtil itsTheJews = new TimerUtil();
    private float healthDiff;
    private int lastTicksExisted;
    private float lastSelfHealth;
    private float lastTargetHealth;
    private ItemStack lastTargetStack;
    private float displayHealth;
    private final TimerUtil animTimer = new TimerUtil();

    public void update(float x, float y, EntityPlayer target, String mode) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.mode = mode;

        if (target != lastTarget) {
            healthDiff = getHealthDiff();

            if (target instanceof AbstractClientPlayer player) {
                BufferedImage image = getBufferedImage(player);

                if (image != null) {
                    avgTargetSkinColor = ColorUtils.getAverageColor(image);
                } else {
                    avgTargetSkinColor = Color.BLACK;
                }
            }
            lastTarget = target;
        }

        if (mc.thePlayer.ticksExisted != lastTicksExisted && mc.thePlayer.ticksExisted % 2 == 0) {
            float selfHealth = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
            float targetHealth = PlayerUtils.getActualHealth(target) + target.getAbsorptionAmount();
            ItemStack targetStack = target.getHeldItem();

            if (lastSelfHealth != selfHealth || lastTargetHealth != targetHealth || (lastTargetStack == null || targetStack == null) ? lastTargetStack != targetStack : !lastTargetStack.isItemEqual(targetStack)) {
                healthDiff = getHealthDiff();
                lastSelfHealth = selfHealth;
                lastTargetHealth = targetHealth;
                lastTargetStack = targetStack;
            }

            lastTicksExisted = mc.thePlayer.ticksExisted;
        }
    }

    public void render() {
        setWidth(INSTANCE.getWidgetManager().get(TargetHudWidget.class).width);
        setHeight(INSTANCE.getWidgetManager().get(TargetHudWidget.class).height);
        GlStateManager.pushMatrix();

        if (!mode.equals("Adjust")) {
            TargetHudWidget.interpolatedScale = MathUtils.interpolate(TargetHudWidget.interpolatedScale, TargetHudWidget.targetScale, 0.25f);
            GlStateManager.translate(x + width / 2F, y + height / 2F, 0);
            GlStateManager.scale(TargetHudWidget.interpolatedScale, TargetHudWidget.interpolatedScale, TargetHudWidget.interpolatedScale);
            GlStateManager.translate(-(x + width / 2F), -(y + height / 2F), 0);
        }

        switch (mode) {
            case "New": {
                float healthPercentage = PlayerUtils.getActualHealth(target) / target.getMaxHealth();
                float absorptionPercentage = target.getAbsorptionAmount() / target.getMaxHealth();
                float space = width - 43F;

                target.healthAnimation.animate(space * MathHelper.clamp_float(healthPercentage, 0, 1), 50);
                target.absorptionAnimation.animate(space * MathHelper.clamp_float(absorptionPercentage, 0, 1), 50);
                RectUtils.drawRound(x, y, width, height, 7, new Color(setting.bgColor(), true));

                RectUtils.drawRound(x + 38.5F, y + 28, space, 4, 2, new Color(0, 0, 0, 150));

                if (target.healthAnimation.getOutput() > 0.1) {
                    RectUtils.drawGradientHorizontal(x + 38.5F, y + 28, target.healthAnimation.getOutput(), 4, 2, new Color(setting.color()), new Color(setting.color((int) target.healthAnimation.getOutput())));
                }

                if (target.absorptionAnimation.getOutput() > 0.1) {
                    RectUtils.drawGradientHorizontal(x + 38.5F, y + 28, target.absorptionAnimation.getOutput(), 4, 2, ColorUtils.interpolateColorC(new Color(setting.color()), Color.YELLOW, 0.5F), ColorUtils.interpolateColorC(new Color(setting.color((int) target.absorptionAnimation.getOutput())), Color.YELLOW, 0.5F));
                }

                String text = String.valueOf(BigDecimal.valueOf(PlayerUtils.getActualHealth(target) + target.getAbsorptionAmount()).setScale(2, RoundingMode.FLOOR).doubleValue());
                double diff = BigDecimal.valueOf((mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) - (PlayerUtils.getActualHealth(target) + target.getAbsorptionAmount())).setScale(2, RoundingMode.FLOOR).doubleValue();
                String strDiff;

                if (diff > 0) {
                    strDiff = "+" + diff;
                } else if (diff < 0) {
                    strDiff = String.valueOf(diff);
                } else {
                    strDiff = "±" + diff;
                }

                Fonts.interSemiBold.get(13).drawString(text + "HP", x + 37, y + 17, Color.lightGray.getRGB());
                Fonts.interSemiBold.get(13).drawString(strDiff, x + 115 - Fonts.interSemiBold.get(13).getStringWidth(strDiff), y + 17, Color.lightGray.getRGB());
                Fonts.interSemiBold.get(18).drawString(target.getName(), x + 37, y + 6, -1);

                float calc = healthDiff;
                boolean winning = healthDiff >= 0;

                if (itsTheJews.hasTimeElapsed((long) 16.67)) {
                    Color bloomCol = winning ? avgTargetSkinColor : ColorUtils.interpolateColorC(avgTargetSkinColor, Color.RED, Math.min(-calc / 20F, 0.4F));
                    interpolatedBloomColor = ColorUtils.interpolateColorC(interpolatedBloomColor, bloomCol, 0.2F);

                    Color outlineCol = ColorUtils.interpolateColorC(new Color(255, 0, 0, 0), Color.RED, Math.min(-calc / 20F, 0.4F));
                    Color targetCol = !winning ? outlineCol : new Color(255, 0, 0, 0);
                    interpolatedStatusOutlineColor = ColorUtils.interpolateColorC(interpolatedStatusOutlineColor, targetCol, 0.2F);

                    itsTheJews.reset();
                }

                RectUtils.drawRound(x + 2.55F, y + 2.55F, 32.1F, 32.1F, 5, interpolatedStatusOutlineColor);

                float hurtTime = (target.hurtTime == 0 ? 0 : target.hurtTime - mc.timer.renderPartialTicks) * 0.3F;
                RenderUtils.renderPlayerHead(target, x + 2.5F, y + 2.5F, 32, 10, ColorUtils.interpolateColorC(Color.white, new Color(255, 150, 150, 255), hurtTime));

                Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(x + width / 2F, y + height / 2F, 0);
                    GlStateManager.scale(TargetHudWidget.interpolatedScale, TargetHudWidget.interpolatedScale, TargetHudWidget.interpolatedScale);
                    GlStateManager.translate(-(x + width / 2F), -(y + height / 2F), 0);

                    RectUtils.drawShaderRound(x, y, width, height, 7, new Color(setting.bgColor()));

                    GlStateManager.popMatrix();
                });

                Demise.INSTANCE.getShaderManager().addToBloomQueue(() -> {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(x + width / 2F, y + height / 2F, 0);
                    GlStateManager.scale(TargetHudWidget.interpolatedScale, TargetHudWidget.interpolatedScale, TargetHudWidget.interpolatedScale);
                    GlStateManager.translate(-(x + width / 2F), -(y + height / 2F), 0);

                    if (target.healthAnimation.getOutput() > 0.1) {
                        RectUtils.drawGradientHorizontal(x + 38.5F, y + 28, target.healthAnimation.getOutput(), 4, 2, new Color(setting.color(), false), new Color(setting.color((int) target.healthAnimation.getOutput()), false));
                    }

                    if (target.absorptionAnimation.getOutput() > 0.1) {
                        RectUtils.drawGradientHorizontal(x + 38.5F, y + 28, target.absorptionAnimation.getOutput(), 4, 2, ColorUtils.interpolateColorC(new Color(setting.color()), Color.YELLOW, 0.5F), ColorUtils.interpolateColorC(new Color(setting.color((int) target.absorptionAnimation.getOutput())), Color.YELLOW, 0.5F));
                    }

                    RenderUtils.drawRoundedRect(x + 2.5F, y + 2.5F, 32, 32, 10, ColorUtils.interpolateColorC(interpolatedBloomColor, new Color(255, 150, 150, 255), hurtTime).getRGB());

                    GlStateManager.popMatrix();
                });
            }
            break;
            case "Old": {
                // actual shitcode from old demise
                String TargetName = Demise.INSTANCE.getModuleManager().getModule(Opsec.class).isEnabled() && target == mc.thePlayer ? Opsec.name : target.getDisplayName().getFormattedText();
                float health = MathHelper.clamp_float(PlayerUtils.getActualHealth(target) / target.getMaxHealth(), 0, 1);
                String TargetHealth = String.format("%.1f", PlayerUtils.getActualHealth(target)) + " §c❤ ";

                String status = (health <= (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / mc.thePlayer.getMaxHealth()) ? " §aW" : " §cL";
                TargetName = TargetName + status;

                float current$minX, current$minY, current$maxX, current$maxY;

                int padding = 8;
                float n3 = mc.fontRendererObj.getStringWidth(TargetName) + padding + 20;
                float n4 = x;
                float n5 = y;
                current$minX = n4;
                current$minY = n5;
                current$maxX = n4 + n3 + padding;
                current$maxY = n5 + (mc.fontRendererObj.FONT_HEIGHT + 5) - 6 + padding * 2;

                RenderUtils.drawCustomRect(current$minX, current$minY, current$maxX, current$maxY + 7, (Color.black.getRGB() & 0xFFFFFF) | 60 << 24);

                float n13 = current$minX + 6 + 27;
                float n14 = current$maxX - 2;
                float n15 = current$maxY + 0.45f;
                float healthBar = (float) (n14 + (n13 - n14) * (1.0 - ((health < 0.01) ? 0 : health)));
                if (healthBar - n13 < 1) {
                    healthBar = n13;
                }

                RenderUtils.drawCustomRect(n13 - 1, n15, healthBar - 1, n15 + 4, setting.color());

                mc.fontRendererObj.drawString(TargetName, (n4 + 24) + padding, n5 - 4 + padding, (new Color(220, 220, 220, 255).getRGB() & 0xFFFFFF) | 255 << 24, true);
                mc.fontRendererObj.drawString(TargetHealth, (n4 + 24) + padding, n5 + 6 + padding, (new Color(220, 220, 220, 255).getRGB() & 0xFFFFFF) | 255 << 24, true);

                float targetX = current$minX + 2.65f;
                float targetY = current$minY + 2.65f;
                GlStateManager.color(1, 1, 1, 1);
                RenderUtils.renderPlayerHead(target, targetX, targetY, 26.5f, 0);
            }
            break;
            case "Novoline": {
                float healthPercentage = PlayerUtils.getActualHealth(target) / target.getMaxHealth();
                float space = width - height - 4.5f;

                target.healthAnimation.animate(space * MathHelper.clamp_float(healthPercentage, 0, 1), 75);

                RenderUtils.drawRect(x - 1, y - 1, width + 2, height + 2, new Color(29, 29, 29, 255).getRGB());
                RenderUtils.drawRect(x, y, width, height, new Color(40, 40, 40, 255).getRGB());
                RenderUtils.renderPlayerHead(target, x + 0.5f, y + 0.5f, height - 1f, 0);

                RenderUtils.drawRect(x + 2f + height, y + height - 19.5f, space, 10.8f, new Color(0, 0, 0, 50).getRGB());
                RenderUtils.drawRect(x + 2f + height, y + height - 19.5f, target.healthAnimation.getOutput(), 10.8f, new Color(Demise.INSTANCE.getClientSettings().colorMode.is("Breath") ? Demise.INSTANCE.getClientSettings().mainColor.get().getRGB() : setting.color()).darker().getRGB());
                RenderUtils.drawRect(x + 2f + height, y + height - 19.5f, space * MathHelper.clamp_float(PlayerUtils.getActualHealth(target) / target.getMaxHealth(), 0, 1), 10.8f, Demise.INSTANCE.getClientSettings().colorMode.is("Breath") ? Demise.INSTANCE.getClientSettings().mainColor.get().getRGB() : setting.color());

                float percentage = (PlayerUtils.getActualHealth(target) / target.getMaxHealth()) * 100;

                String text = new DecimalFormat("0.0").format(percentage).replace(",", ".") + "%";
                mc.fontRendererObj.drawStringWithShadow(text, (int) (x + 39f + space / 2 - mc.fontRendererObj.getStringWidth(text) / 2f), y + 19f, -1);
                mc.fontRendererObj.drawStringWithShadow(target.getName(), x + 40, y + 4, -1);
            }
            break;
            case "Adjust": {
                float opacity = 1f;
                float hurtTime = (target.hurtTime == 0 ? 0 : target.hurtTime - mc.timer.renderPartialTicks) * 0.3f;
                float realHealth = PlayerUtils.getActualHealth(target);

                String h = realHealth + target.getAbsorptionAmount() < mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount() ? "+" : "-";
                String healthDiff = realHealth + target.getAbsorptionAmount() == mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount() ? "" : h + (realHealth + target.getAbsorptionAmount() < mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount() ? String.valueOf(MathUtils.round((mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) - (realHealth + target.getAbsorptionAmount()), 1)) : String.valueOf(MathUtils.round((realHealth + target.getAbsorptionAmount()) - (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()), 1)));

                RenderUtils.drawRect(x, y, width, height, ColorUtils.applyOpacity(new Color(25, 25, 25, 150), opacity).getRGB());

                RenderUtils.renderPlayerHead(target, x + 2.5f, y + 2.5f, 20, 0, ColorUtils.applyOpacity(ColorUtils.interpolateColorC(Color.white, new Color(255, 171, 171, 255), hurtTime), opacity));

                target.healthAnimation.animate((width - 5f) * MathHelper.clamp_float(realHealth / target.getMaxHealth(), 0, 1), 10);
                target.healthAnimationSlow.animate((width - 5f) * MathHelper.clamp_float(realHealth / target.getMaxHealth(), 0, 1), 35);
                target.absorptionAnimation.animate((width - 5f) * MathHelper.clamp_float(target.getAbsorptionAmount() / target.getMaxHealth(), 0, 1), 10);

                RenderUtils.drawBorder(x + 2.5f, y + 25f, width - 5f, 2.5f, 0.5f, ColorUtils.applyOpacity(new Color(50, 50, 50, 100), opacity).getRGB());
                RenderUtils.drawRect(x + 2.5f, y + 25f, width - 5f, 2.5f, ColorUtils.applyOpacity(new Color(25, 25, 25, 150), opacity).getRGB());
                RectUtils.drawHGradientRect(x + 2.5f, y + 25f, target.healthAnimationSlow.getOutput(), 2.5f, ColorUtils.applyOpacity(setting.getColors()[0].darker().darker(), opacity), ColorUtils.applyOpacity(new Color(ColorUtils.interpolateColor2(setting.getColors()[0].darker().darker(), setting.getColors()[1].darker().darker(), realHealth / target.getMaxHealth())), opacity));
                RectUtils.drawHGradientRect(x + 2.5f, y + 25f, target.healthAnimation.getOutput(), 2.5f, ColorUtils.applyOpacity(setting.getColors()[0], opacity), ColorUtils.applyOpacity(new Color(ColorUtils.interpolateColor2(setting.getColors()[0], setting.getColors()[1], realHealth / target.getMaxHealth())), opacity));
                RenderUtils.drawRect(x + 2.5f, y + 25f, target.absorptionAnimation.getOutput(), 2.5f, new Color(255, 255, 50).getRGB());

                int xOffset = 8;
                RenderHelper.enableGUIStandardItemLighting();
                if (this.target.getInventory() != null) {
                    GlStateManager.pushMatrix();
                    GlStateManager.disableAlpha();
                    GlStateManager.clear(256);
                    GlStateManager.enableBlend();
                    GlStateManager.scale(0.5, 0.5, 0.5);
                    GlStateManager.color(1f, 1f, 1f, opacity);

                    if (target.getCurrentEquippedItem() != null) {
                        RenderUtils.renderItemStack(target.getCurrentEquippedItem(), (x + 21f) / 0.5f + xOffset, (y + (height - 16f)) / 0.5f, false);
                        xOffset += 16;
                    } else {
                        xOffset -= 1;
                    }
                    for (int i = 3; i >= 0; --i) {
                        ItemStack armorStack = target.inventory.armorInventory[i];
                        if (armorStack != null && armorStack.getItem() instanceof ItemArmor) {
                            RenderUtils.renderItemStack(armorStack, (x + 21f) / 0.5f + xOffset, (y + (height - 16f)) / 0.5f, false);
                            xOffset += 16;
                        }
                    }

                    GlStateManager.disableBlend();
                    GlStateManager.scale(0.5, 0.5, 0.5);
                    GlStateManager.disableDepth();
                    GlStateManager.disableLighting();
                    GlStateManager.enableDepth();
                    GlStateManager.scale(2.0f, 2.0f, 2.0f);
                    GlStateManager.enableAlpha();
                    GlStateManager.popMatrix();
                }

                Fonts.tahoma.get(15).drawStringWithShadow(target.getName(), x + 24f, y + Fonts.tahoma.get(16).getHeight() / 2.75f + 0.5f, new Color(255, 255, 255, 200).getRGB());
                Fonts.tahoma.get(12).drawStringWithShadow(healthDiff, x + 1.0f + width - (Fonts.tahoma.get(12).getStringWidth(healthDiff) + 3f), y - 4f + height - Fonts.tahoma.get(12).getHeight(), new Color(255, 255, 255, 200).getRGB());
            }
            break;
            case "Exhibition": {
                FontRenderer fr = Fonts.exhi.get(18);

                GlStateManager.pushMatrix();
                GlStateManager.translate(x - 1, y + 5, 0);
                RenderUtils.skeetRect(0, -2.0, fr.getStringWidth(target.getName()) > 70.0f ? (double) (124.0f + fr.getStringWidth(target.getName()) - 70.0f) : 124.0, 38.0, 1.0);
                RenderUtils.skeetRectSmall(0.0f, -2.0f, 124.0f, 38.0f, 1.0);
                fr.drawStringWithShadow(target.getName(), 42.3f, 0.3f, -1);
                final float health = PlayerUtils.getActualHealth(target);
                final float healthWithAbsorption = PlayerUtils.getActualHealth(target) + target.getAbsorptionAmount();
                final float[] fractions = new float[]{0.0F, 0.5F, 1.0F};
                final Color[] colors = new Color[]{Color.RED, Color.YELLOW, Color.GREEN};
                final float progress = health / target.getMaxHealth();
                final Color healthColor = health >= 0.0f ? ColorUtils.blendColors(fractions, colors, progress).brighter() : Color.RED;
                double cockWidth = 0.0;
                cockWidth = MathUtils.round(cockWidth, 5);
                if (cockWidth < 50.0) {
                    cockWidth = 50.0;
                }
                final double healthBarPos = cockWidth * (double) progress;
                RenderUtils.drawCustomRect(42.5, 10.3, 103, 13.5, healthColor.darker().darker().darker().darker().getRGB());
                RenderUtils.drawCustomRect(42.5, 10.3, 53.0 + healthBarPos + 0.5, 13.5, healthColor.getRGB());
                if (target.getAbsorptionAmount() > 0.0f) {
                    RenderUtils.drawCustomRect(97.5 - (double) target.getAbsorptionAmount(), 10.3, 103.5, 13.5, new Color(137, 112, 9).getRGB());
                }
                RenderUtils.rectangleBordered(42.0, 9.8f, 54.0 + cockWidth, 14.0, 0.5, 0, Color.BLACK.getRGB());
                for (int dist = 1; dist < 10; ++dist) {
                    final double cock = cockWidth / 8.5 * (double) dist;
                    RenderUtils.drawCustomRect(43.5 + cock, 9.8, 43.5 + cock + 0.5, 14.0, Color.BLACK.getRGB());
                }
                GlStateManager.scale(0.5, 0.5, 0.5);
                final int distance = (int) mc.thePlayer.getDistanceToEntity(target);
                final String nice = "HP: " + (int) healthWithAbsorption + " | Dist: " + distance;
                mc.fontRendererObj.drawString(nice, 85.3f, 32.3f, -1, true);
                GlStateManager.scale(2.0, 2.0, 2.0);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                GlStateManager.enableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                if (target != null) {
                    GL11.glPushMatrix();
                    final List<ItemStack> stuff = new ArrayList<>();
                    int cock = -2;
                    for (int geraltOfNigeria = 3; geraltOfNigeria >= 0; --geraltOfNigeria) {
                        final ItemStack armor = target.getCurrentArmor(geraltOfNigeria);
                        if (armor != null) {
                            stuff.add(armor);
                        }
                    }
                    if (target.getHeldItem() != null) {
                        stuff.add(target.getHeldItem());
                    }

                    for (final ItemStack yes : stuff) {
                        if (Minecraft.getMinecraft().theWorld != null) {
                            RenderHelper.enableGUIStandardItemLighting();
                            cock += 16;
                        }
                        GlStateManager.pushMatrix();
                        GlStateManager.disableAlpha();
                        GlStateManager.clear(256);
                        GlStateManager.enableBlend();
                        Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(yes, cock + 28, 20);
                        Minecraft.getMinecraft().getRenderItem().renderItemOverlays(Minecraft.getMinecraft().fontRendererObj, yes, cock + 28, 20);
                        RenderUtils.renderEnchantText(yes, cock + 28, (20 + 0.5f), 0.5f);
                        GlStateManager.disableBlend();
                        GlStateManager.scale(0.5, 0.5, 0.5);
                        GlStateManager.disableDepth();
                        GlStateManager.disableLighting();
                        GlStateManager.enableDepth();
                        GlStateManager.scale(2.0f, 2.0f, 2.0f);
                        GlStateManager.enableAlpha();
                        GlStateManager.popMatrix();
                        yes.getEnchantmentTagList();
                    }
                    GL11.glPopMatrix();
                }
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
                GlStateManager.scale(0.31, 0.31, 0.31);
                GlStateManager.translate(73.0f, 102.0f, 40.0f);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                RenderUtils.drawPlayerModel(target.rotationYaw, target.rotationPitch, target);
                GlStateManager.popMatrix();
            }
            break;
            case "Rise 5": {
                double dist = mc.thePlayer.getDistanceToEntity(target);

                float x = this.x - 40;
                float y = this.y + 34;
                RectUtils.drawRound(x + 40, y - 34, 129, 48, 4, new Color(setting.bgColor(), true));

                float finalY = y;
                Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(this.x + width / 2F, this.y + height / 2F, 0);
                    GlStateManager.scale(TargetHudWidget.interpolatedScale, TargetHudWidget.interpolatedScale, TargetHudWidget.interpolatedScale);
                    GlStateManager.translate(-(this.x + width / 2F), -(this.y + height / 2F), 0);

                    RectUtils.drawRound(x + 40, finalY - 34, 129, 48, 4, Color.black);
                    GlStateManager.popMatrix();
                });

                float scaleOffset = (target.hurtTime - mc.timer.partialTicks) * 0.35F;
                scaleOffset = Math.max(0, scaleOffset);

                double offset = (target.hurtTime - mc.timer.partialTicks) * 23.0;
                offset = Math.max(0, offset);

                RenderUtils.renderPlayerHead(target, x + 44 + scaleOffset / 2F, y - 34 + 5 + scaleOffset / 2F, 30 - scaleOffset, 0, new Color(255, (int) (255 - offset), (int) (255 - offset)));

                float health = PlayerUtils.getActualHealth(target) + target.getAbsorptionAmount();
                health = Math.min(20, health);

                int speed = 6;
                if (animTimer.hasTimeElapsed(16.67)) {
                    displayHealth = (displayHealth * (speed - 1) + health) / speed;
                    animTimer.reset();
                }

                float offset1 = 6;
                float drawBarPosX = x + 38;

                if (displayHealth > 0.1) {
                    for (int i = 0; i < displayHealth * 4; i++) {
                        int color = setting.color((int) (i * 0.5));
                        Gui.drawRect(drawBarPosX + offset1, y + 5, drawBarPosX + 1 + offset1 * 1.25, y + 10, color);
                        offset1 += 1;
                    }
                }

                // this is what happens when you are trying to replicate a client's worse (actually better) font renderer
                y += 2.5F;
                Fonts.interLight.get(17).drawString(MathUtils.round(displayHealth, 1) + "", drawBarPosX + 2 + offset1 * 1.25, y + 2.5f, -1);
                Fonts.interLight.get(17).drawString("Name: " + target.getName(), x + 38 + 6 + 30 + 3, y - 34 + 5 + 15 - Fonts.interLight.get(17).getHeight(), Color.WHITE.hashCode());

                y -= 1;
                Fonts.interLight.get(17).drawString("Distance: " + MathUtils.round(dist, 1), x + 38 + 6 + 30 + 3, y - 34 + 5 + 15 + 2, Color.WHITE.hashCode());
            }
            break;
        }

        GlStateManager.popMatrix();
    }

    private float damage(final ItemStack stack, final Entity attacker, final Entity target) {
        float baseDamage = 1.0f;
        if (stack != null) {
            final Item item = stack.getItem();
            int sharpnessLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);

            if (item instanceof ItemSword sword) {
                baseDamage = sword.attackDamage;
            } else if (item instanceof ItemTool tool) {
                baseDamage = tool.damageVsEntity;
            }
            baseDamage += sharpnessLevel * 1.25f;
        }

        if (attacker instanceof EntityLivingBase livingAttacker) {
            int strengthLevel = livingAttacker.getActivePotionEffect(Potion.damageBoost) != null ? livingAttacker.getActivePotionEffect(Potion.damageBoost).getAmplifier() + 1 : 0;
            baseDamage += strengthLevel * 3.0f;
        }

        float totalDamage = baseDamage;

        if (target instanceof EntityLivingBase livingTarget) {
            int totalProtectionLevel = 0;
            for (int i = 0; i < 4; i++) {
                totalProtectionLevel += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, livingTarget.getCurrentArmor(i));
            }
            totalDamage *= (1 - livingTarget.getTotalArmorValue() * 0.04f);
            totalDamage *= (1 - totalProtectionLevel * 0.04f);

            int resistanceLevel = livingTarget.getActivePotionEffect(Potion.resistance) != null ? livingTarget.getActivePotionEffect(Potion.resistance).getAmplifier() + 1 : 0;
            totalDamage *= (1 - resistanceLevel * 0.2f);
        }
        return totalDamage;
    }

    private float getHealthDiff() {
        if (!(target instanceof AbstractClientPlayer)) {
            return 0;
        }

        float playerHealth = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
        float targetHealth = PlayerUtils.getActualHealth(target) + target.getAbsorptionAmount();

        for (int i = 0; i < 10 && playerHealth > 0 && targetHealth > 0; i++) {
            float playerDamage = damage(mc.thePlayer.getHeldItem(), mc.thePlayer, target);
            float targetDamage = damage(target.getHeldItem(), target, mc.thePlayer);

            targetHealth -= playerDamage;
            playerHealth -= targetDamage;
        }

        return playerHealth - targetHealth;
    }

    private BufferedImage getBufferedImage(AbstractClientPlayer player) {
        if (!player.hasSkin()) return null;

        ResourceLocation skinLoc = player.getLocationSkin();
        ITextureObject texture = Minecraft.getMinecraft()
                .getTextureManager()
                .getTexture(skinLoc);

        if (texture instanceof ThreadDownloadImageData data) {
            if (data.getBufferedImage() != null) {
                return data.getBufferedImage();
            }
        }

        return null;
    }
}