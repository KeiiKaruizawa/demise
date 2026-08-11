package gr.larryngton.demise.gui.widget.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.gui.font.FontRenderer;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.utils.animations.Animation;
import gr.larryngton.demise.utils.animations.Direction;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class ModuleListWidget extends Widget {
    private static final float xPadding = 7;
    private static final int yPadding = 5;
    public static float currX;
    public static float currY;

    public ModuleListWidget() {
        super("ModuleList");
        this.x = 0.89062506f;
        this.y = 0.009259259f;
    }

    @Override
    public void render() {
        List<Module> enabledModules = getEnabledModules();
        int middle = sr.getScaledWidth() / 2;
        float offset = 0;

        currX = renderX;
        currY = renderY;

        switch (setting.moduleListMode.get()) {
            case "New":
                this.height = getEnabledModules().size() * getModuleHeight();

                for (int i = 0; i < enabledModules.size(); i++) {
                    Module module = enabledModules.get(i);
                    float width = getModuleWidth(module);
                    float height = getModuleHeight();

                    RenderPosition position = calculateRenderPosition(module);

                    if (position.x < middle) {
                        RenderUtils.drawRect(position.x, position.y + offset, width, height, setting.bgColor());
                    } else {
                        RenderUtils.drawRect(position.x + this.width - width, position.y + offset, width, height, setting.bgColor());
                    }

                    float finalOffset = offset;
                    Demise.INSTANCE.getShaderManager().addToAllQueue(() -> {
                        if (position.x < middle) {
                            RenderUtils.drawRect(position.x, position.y + finalOffset, width, height, Color.black.getRGB());
                        } else {
                            RenderUtils.drawRect(position.x + this.width - width, position.y + finalOffset, width, height, Color.black.getRGB());
                        }
                    });

                    String text = Demise.INSTANCE.getClientSettings().applyCasing(module.getName()) + module.getTag();
                    float textY = position.y + offset + 4 + (yPadding / 2f) - 1.3f;
                    float textX = position.x - (width - xPadding) + this.width - (xPadding / 2f) - 0.5f;

                    if (position.x < middle) {
                        Fonts.interRegular.get(15).drawString(text, position.x + (xPadding / 2f), textY, setting.color(i));
                    } else {
                        Fonts.interRegular.get(15).drawString(text, textX, textY, setting.color(i));
                    }

                    offset = calculateNextOffset(module, height, offset);
                }
                break;
            case "Old":
                int margin = 2;

                for (int i = 0; i < enabledModules.size(); i++) {
                    Module module = enabledModules.get(i);
                    RenderPosition pos = calculateRenderPosition(module);
                    String casedName = Demise.INSTANCE.getClientSettings().applyCasing(module.getName());
                    float moduleX = (renderX > middle) ? pos.x + (width - mc.fontRendererObj.getStringWidth(casedName + module.getTag())) : pos.x;
                    float y = pos.y + offset;

                    mc.fontRendererObj.drawStringWithShadow(casedName, moduleX, y, setting.color(i));
                    mc.fontRendererObj.drawStringWithShadow(module.getTag(), moduleX + mc.fontRendererObj.getStringWidth(casedName), y, Color.lightGray.getRGB());
                    offset += (float) ((mc.fontRendererObj.FONT_HEIGHT + margin) * module.getAnimation().getOutput());
                }

                this.height = getEnabledModules().size() * (mc.fontRendererObj.FONT_HEIGHT + margin);
                break;
            case "Icarus": {
                float moduleY = 0;
                float gradientWidth = 1f;
                FontRenderer fontRenderer = Fonts.interRegular.get(17);
                this.height = getEnabledModules().size() * (getModuleHeight() - 3F);

                enabledModules.sort(Comparator.comparingDouble(ModuleListWidget::getModuleWidth).reversed());
                for (Module module : enabledModules) {
                    if (!module.getAnimation().finished(Direction.BACKWARDS)) {
                        float rectLength = (float) ((getModuleWidth(module) * module.getAnimation().getOutput()) - gradientWidth);
                        RectUtils.drawRound(sr.getScaledWidth() - rectLength - 3.3F, moduleY, rectLength + 20, getModuleHeight() - 4.35F, 1, new Color(21, 21, 21));
                        fontRenderer.drawString(Demise.INSTANCE.getClientSettings().applyCasing(module.getName()), sr.getScaledWidth() - rectLength - 2.2F, moduleY + fontRenderer.getHeight() / 2F - 2.4F, -1);
                        moduleY += getModuleHeight() - 3F;
                    }
                }
                RectUtils.drawGradientVertical(sr.getScaledWidth() - gradientWidth, 0, gradientWidth, moduleY + 0.5f, 1, new Color(253, 0, 144), new Color(125, 18, 255));
                break;
            }
            case "Adjust":
                FontRenderer fontRenderer = Fonts.tahoma.get(15);
                float width = sr.getScaledWidth();

                float moduleY = 0;

                int index = 0;

                enabledModules.sort(Comparator.comparingDouble(ModuleListWidget::getModuleWidth).reversed());
                float lastWidth = 0;
                for (Module module : enabledModules) {
                    String moduleName = Demise.INSTANCE.getClientSettings().applyCasing(module.getName()).replaceAll("([a-z])([A-Z])", "$1 $2");
                    String moduleTag = module.getTag().replaceAll(" ([a-z])", " $1".toUpperCase()).replace("ยง7", "");
                    moduleTag = Pattern.compile("(?<=\\s)[a-z]").matcher(moduleTag).replaceAll(r -> r.group().toUpperCase());
                    String name2 = moduleName + moduleTag;
                    float moduleWidth = fontRenderer.getStringWidth(name2) + 4.5f;
                    float moduleHeight = 10f;
                    float moduleX = width - moduleWidth;
                    float animation = (float) (module.getAnimation().getOutput() - 1f);
                    float anim = (float) module.getAnimation().getOutput();

                    if (setting.background.get()) {
                        RenderUtils.drawRect(moduleX - moduleWidth * animation, moduleY + moduleHeight * animation, moduleWidth, moduleHeight, new Color(25, 25, 25, 100).getRGB());

                        //line
                        switch (setting.lineMode.get()) {
                            case "Line": {
                                RenderUtils.drawRect(sr.getScaledWidth() - 0.5f, moduleY, 0.5f, moduleHeight, setting.color(index, 225));
                                moduleX -= 0.5f;
                                break;
                            }
                            case "Outline": {
                                RenderUtils.drawRect(moduleX - moduleWidth * animation - 0.5f, moduleY + moduleHeight * animation, 0.5f, moduleHeight + 0.5f, setting.color(index, 225));
                                if (lastWidth != 0) {
                                    float lineWidth = lastWidth - moduleWidth * (module.getAnimation().getDirection().equals(Direction.BACKWARDS) ? 1 : anim);
                                    RenderUtils.drawRect(moduleX - (lastWidth - moduleWidth), moduleY, lineWidth, 0.5f, setting.color(index, 225));
                                }
                                if (module == enabledModules.get(enabledModules.size() - 1)) {
                                    RenderUtils.drawRect(moduleX - moduleWidth * animation, moduleY + moduleHeight + moduleHeight * animation, moduleWidth, 0.5f, setting.color(index, 225));
                                }
                                break;
                            }
                        }
                    }
                    fontRenderer.drawStringWithShadow(moduleName, moduleX + 2f - moduleWidth * animation, moduleY + 3f + moduleHeight * animation, setting.color(index));
                    fontRenderer.drawStringWithShadow(moduleTag, moduleX + 2f + fontRenderer.getStringWidth(moduleName) - moduleWidth * animation, moduleY + 3f + moduleHeight * animation, Color.LIGHT_GRAY.getRGB());
                    moduleY += moduleHeight * anim;
                    index++;
                    lastWidth = moduleWidth;
                }
                break;
            case "Rise 5": {
                double moduleY1 = 0;
                this.height = getEnabledModules().size() * getModuleHeight();
                FontRenderer comfortaa = Fonts.comfortaa.get(18);

                for (int i = 0; i < enabledModules.size(); i++) {
                    Module module = enabledModules.get(i);
                    String name = Demise.INSTANCE.getClientSettings().applyCasing(module.getName());
                    RenderPosition pos = calculateRenderPosition(module);
                    pos.y += (float) moduleY1;
                    float moduleX = (renderX > middle) ? pos.x + (this.width - comfortaa.getStringWidth(name)) : pos.x;

                    int offsetY = 2;
                    int offsetX = 1;

                    RenderUtils.drawRect(moduleX - offsetX, (float) (pos.y - offsetY + 1 + 0.1), (float) (comfortaa.getStringWidth(name) + offsetX * 1.5 + 0.5), (float) (comfortaa.getHeight() + offsetY + 0.3), setting.bgColor());
                    Demise.INSTANCE.getShaderManager().addToAllQueue(() -> RenderUtils.drawRect(moduleX - offsetX, (float) (pos.y - offsetY + 1 + 0.1), (float) (comfortaa.getStringWidth(name) + offsetX * 1.5 + 0.5), (float) (comfortaa.getHeight() + offsetY + 0.3), Color.black.getRGB()));

                    comfortaa.drawStringWithShadow(name, moduleX + 0.5f, pos.y + 1 + 2.6, setting.color(i));
                    moduleY1 += getModuleHeight() * module.getAnimation().getOutput();
                }
            }
            break;
            case "Sigma": {
                double moduleY1 = 0;
                this.height = getEnabledModules().size() * getModuleHeight();
                FontRenderer helveticaNeue = Fonts.helveticaNeue.get(24);

                for (int i = 0; i < enabledModules.size(); i++) {
                    Module module = enabledModules.get(i);
                    String name = Demise.INSTANCE.getClientSettings().applyCasing(module.getName());
                    RenderPosition pos = calculateRenderPosition(module);
                    pos.y += (float) moduleY1;
                    float moduleX = (renderX > middle) ? pos.x + (this.width - helveticaNeue.getStringWidth(name)) : pos.x;

                    helveticaNeue.drawStringWithShadow(name, moduleX + 1f, pos.y + 1 + 2.6, setting.color(i));
                    moduleY1 += getModuleHeight() * module.getAnimation().getOutput();
                }
            }
            break;
            case "Custom": {
                boolean vanilla = setting.customFont.is("Vanilla");
                boolean showSuffix = setting.customShowSuffix.get();
                boolean suffixVanilla = setting.customSuffixFont.is("Vanilla");
                float size = setting.customTextSize.get();
                FontRenderer customFont = vanilla ? null : customFontEnum(setting.customFont.get()).get(size);
                FontRenderer suffixFont = suffixVanilla ? null : customFontEnum(setting.customSuffixFont.get()).get(size);
                float referenceHeight = Fonts.interRegular.get(size).getHeight();
                float vanillaScale = referenceHeight / (float) mc.fontRendererObj.FONT_HEIGHT;
                float fontHeight = vanilla ? referenceHeight : customFont.getHeight();
                int spacing = 2;
                float lastEdgeX = Float.NaN;

                this.height = getEnabledModules().size() * (fontHeight + spacing);

                for (int i = 0; i < enabledModules.size(); i++) {
                    Module module = enabledModules.get(i);
                    RenderPosition pos = calculateRenderPosition(module);
                    String casedName = Demise.INSTANCE.getClientSettings().applyCasing(module.getName());
                    String tag = Demise.INSTANCE.getClientSettings().applyCasing(module.getTag().replace("\u00A77", ""), setting.customSuffixCasing.get());

                    float nameWidth = vanilla ? mc.fontRendererObj.getStringWidth(casedName) * vanillaScale : customFont.getStringWidth(casedName);
                    float tagWidth = !showSuffix ? 0 : (suffixVanilla ? mc.fontRendererObj.getStringWidth(tag) * vanillaScale : suffixFont.getStringWidth(tag));
                    float fullWidth = nameWidth + tagWidth;
                    float moduleX = (renderX > middle) ? pos.x + (this.width - fullWidth) : pos.x;
                    float y = pos.y + offset;
                    float boxTop = y - 1f;
                    float boxHeight = fontHeight + 2f;

                    if (setting.customBackground.get()) {
                        RenderUtils.drawRect(moduleX - 2f, boxTop, fullWidth + 4f, boxHeight, setting.bgColor());
                        Demise.INSTANCE.getShaderManager().addToAllQueue(() -> RenderUtils.drawRect(moduleX - 2f, boxTop, fullWidth + 4f, boxHeight, Color.black.getRGB()));
                    }

                    switch (setting.lineMode.get()) {
                        case "Line" -> RenderUtils.drawRect(renderX > middle ? moduleX + fullWidth + 2f : moduleX - 2f, boxTop, 0.5f, boxHeight, setting.color(i, 225));
                        case "Outline" -> {
                            float edgeX = renderX > middle ? moduleX - 2f : moduleX + fullWidth + 1.5f;
                            float fixedX = renderX > middle ? moduleX + fullWidth + 1.5f : moduleX - 2f;
                            RenderUtils.drawRect(edgeX, boxTop, 0.5f, boxHeight, setting.color(i, 225));
                            RenderUtils.drawRect(fixedX, boxTop, 0.5f, boxHeight, setting.color(i, 225));

                            if (!Float.isNaN(lastEdgeX) && lastEdgeX != edgeX) {
                                RenderUtils.drawRect(Math.min(lastEdgeX, edgeX), boxTop, Math.abs(lastEdgeX - edgeX) + 0.5f, 0.5f, setting.color(i, 225));
                            }

                            if (i == 0) {
                                RenderUtils.drawRect(moduleX - 2f, boxTop, fullWidth + 4f, 0.5f, setting.color(i, 225));
                            }

                            if (i == enabledModules.size() - 1) {
                                RenderUtils.drawRect(moduleX - 2f, boxTop + boxHeight - 0.5f, fullWidth + 4f, 0.5f, setting.color(i, 225));
                            }

                            lastEdgeX = edgeX;
                        }
                    }

                    if (vanilla) {
                        GlStateManager.pushMatrix();
                        try {
                            GlStateManager.translate(moduleX, y, 0);
                            GlStateManager.scale(vanillaScale, vanillaScale, 1f);
                            mc.fontRendererObj.drawStringWithShadow(casedName, 0, 0, setting.color(i));
                        } finally {
                            GlStateManager.popMatrix();
                        }
                    } else {
                        customFont.drawStringWithShadow(casedName, moduleX, boxTop + customFont.getMiddleOfBox(boxHeight) + 2.6f, setting.color(i));
                    }

                    if (showSuffix && setting.customSuffixColor.get().getAlpha() > 0) {
                        int suffixColor = setting.customSuffixColor.get().getRGB();
                        if (suffixVanilla) {
                            GlStateManager.pushMatrix();
                            try {
                                GlStateManager.translate(moduleX + nameWidth, y, 0);
                                GlStateManager.scale(vanillaScale, vanillaScale, 1f);
                                mc.fontRendererObj.drawStringWithShadow(tag, 0, 0, suffixColor);
                            } finally {
                                GlStateManager.popMatrix();
                            }
                        } else {
                            suffixFont.drawStringWithShadow(tag, moduleX + nameWidth, boxTop + suffixFont.getMiddleOfBox(boxHeight) + 2.6f, suffixColor);
                        }
                    }

                    offset += (float) ((fontHeight + spacing) * module.getAnimation().getOutput());
                }
                break;
            }
        }
    }

    private static Fonts customFontEnum(String name) {
        return switch (name) {
            case "Inter Bold" -> Fonts.interBold;
            case "Inter Medium" -> Fonts.interMedium;
            case "Inter Regular" -> Fonts.interRegular;
            case "Inter Semi Bold" -> Fonts.interSemiBold;
            case "Inter Light" -> Fonts.interLight;
            case "Urbanist" -> Fonts.urbanist;
            case "Exhi" -> Fonts.exhi;
            case "Genshin" -> Fonts.genshin;
            case "Tahoma" -> Fonts.tahoma;
            case "Tahoma Bold" -> Fonts.tahomaBold;
            case "Comfortaa" -> Fonts.comfortaa;
            case "Helvetica Neue" -> Fonts.helveticaNeue;
            case "why the fuck would you use this" -> Fonts.esp;
            default -> Fonts.interRegular;
        };
    }

    public static List<Module> getEnabledModules() {
        List<Module> enabledModules = new ArrayList<>();
        for (Module module : INSTANCE.getModuleManager().getModules()) {
            if (module.isHidden()) {
                continue;
            }
            Animation moduleAnimation = module.getAnimation();
            moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
            if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;
            enabledModules.add(module);
        }
        enabledModules.sort(Comparator.comparing(ModuleListWidget::getModuleWidth).reversed());
        return enabledModules;
    }

    private static float getModuleWidth(Module module) {
        String casedName = Demise.INSTANCE.getClientSettings().applyCasing(module.getName());
        return switch (setting.moduleListMode.get()) {
            case "New" -> Fonts.interRegular.get(15).getStringWidth(casedName + module.getTag()) + xPadding;
            case "Old" -> mc.fontRendererObj.getStringWidth(casedName + module.getTag());
            case "Icarus" -> Fonts.interRegular.get(17).getStringWidth(casedName) + 3;
            case "Adjust" -> Fonts.tahoma.get(15).getStringWidth(casedName.replaceAll("([a-z])([A-Z])", "$1 $2") + Pattern.compile("(?<=\\s)[a-z]").matcher(module.getTag().replaceAll(" ([a-z])", " $1".toUpperCase())).replaceAll(r -> r.group().toUpperCase()));
            case "Rise 5" -> Fonts.comfortaa.get(18).getStringWidth(casedName) + 1 * 1.5F + 0.5F;
            case "Sigma" -> Fonts.helveticaNeue.get(24).getStringWidth(casedName) + 1 * 3F + 1F;
            case "Custom" -> {
                boolean vanilla = setting.customFont.is("Vanilla");
                boolean showSuffix = setting.customShowSuffix.get();
                boolean suffixVanilla = setting.customSuffixFont.is("Vanilla");
                float size = setting.customTextSize.get();
                float vanillaScale = Fonts.interRegular.get(size).getHeight() / (float) mc.fontRendererObj.FONT_HEIGHT;
                float nameWidth = vanilla ? mc.fontRendererObj.getStringWidth(casedName) * vanillaScale : customFontEnum(setting.customFont.get()).get(size).getStringWidth(casedName);
                String tag = Demise.INSTANCE.getClientSettings().applyCasing(module.getTag().replace("\u00A77", ""), setting.customSuffixCasing.get());
                float tagWidth = !showSuffix ? 0 : (suffixVanilla ? mc.fontRendererObj.getStringWidth(tag) * vanillaScale : customFontEnum(setting.customSuffixFont.get()).get(size).getStringWidth(tag));
                yield nameWidth + tagWidth + 4;
            }
            default -> 0;
        };
    }

    public static float getModuleHeight() {
        return switch (setting.moduleListMode.get()) {
            case "New" -> Fonts.interRegular.get(15).getHeight() + yPadding;
            case "Old" -> mc.fontRendererObj.FONT_HEIGHT + 2;
            case "Icarus" -> Fonts.interRegular.get(17).getHeight() + 4;
            case "Adjust" -> 10;
            case "Rise 5" -> Fonts.comfortaa.get(18).getHeight() + 2 + 0.3F;
            case "Sigma" -> Fonts.helveticaNeue.get(24).getHeight() + 1 + 3F;
            case "Custom" -> {
                boolean vanilla = setting.customFont.is("Vanilla");
                float size = setting.customTextSize.get();
                yield (vanilla ? Fonts.interRegular.get(size).getHeight() : customFontEnum(setting.customFont.get()).get(size).getHeight()) + 2;
            }
            default -> 0;
        };
    }

    private static class RenderPosition {
        float x, y;

        RenderPosition(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private RenderPosition calculateRenderPosition(Module module) {
        float localX = renderX;
        float localY = renderY;
        float width = getModuleWidth(module);

        float MOVE_IN_SCALE = 2.0f;

        if (x > 0.5) {
            localX += (float) Math.abs(module.getAnimation().getOutput() - 1.0) *
                    (MOVE_IN_SCALE + width);
        } else {
            localX -= (float) Math.abs((module.getAnimation().getOutput() - 1.0) *
                    (MOVE_IN_SCALE + width));
        }

        return new RenderPosition(localX, localY);
    }

    private float calculateNextOffset(Module module, float height, float offset) {
        return (float) (offset + ((module.getAnimation().getOutput()) * (height)));
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Module list");
    }
}