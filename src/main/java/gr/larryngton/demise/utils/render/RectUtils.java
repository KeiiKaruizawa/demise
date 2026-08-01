package gr.larryngton.demise.utils.render;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.features.modules.impl.visual.Shaders;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.render.shader.ShaderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class RectUtils implements InstanceAccess {
    private static final ShaderUtils roundedShader = new ShaderUtils("roundedRect");
    private static final ShaderUtils roundedOutlineShader = new ShaderUtils("roundRectOutline");
    private static final ShaderUtils roundedGradientShader = new ShaderUtils("roundedRectGradient");
    private static final ShaderUtils gradientShader = new ShaderUtils("gradient");
    private static Shaders shadersModule;

    public static void drawRound(float x, float y, float width, float height, float radius, Color color) {
        drawRound(x, y, width, height, radius, color, false);
    }

    public static void drawRound(float x, float y, float width, float height, float radius, Color color, boolean noGlass) {
        if (!noGlass) {
//            Color color1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() / 1.5F));
//            Color lerpColor = ColorUtils.interpolateColorC(color, color1, 0.5F);
//            drawGradientRound(x, y, width, height, radius, lerpColor, color1, color, lerpColor);
            drawRound(x, y, width, height, radius, false, color);

            Color fadedCol = new Color(0, 0, 0, 0);
            Color bloomCol = new Color(127, 127, 127, 6);

            float maxWidth = Math.min(width, 5);
            float maxHeight = Math.min(height, 5);

            RectUtils.drawGradientHorizontal(x, y, maxWidth, height, radius, bloomCol, fadedCol);
            RectUtils.drawGradientHorizontal(x + width - maxWidth, y, maxWidth, height, radius, fadedCol, bloomCol);
            RectUtils.drawGradientVertical(x, y, width, maxHeight, radius, bloomCol, fadedCol);
            RectUtils.drawGradientVertical(x, y + height - maxHeight, width, maxHeight, radius, fadedCol, bloomCol);

            maxWidth /= 10;
            maxHeight /= 10;

            RectUtils.drawGradientHorizontal(x, y, maxWidth, height, radius, bloomCol, fadedCol);
            RectUtils.drawGradientHorizontal(x + width - maxWidth, y, maxWidth, height, radius, fadedCol, bloomCol);
            RectUtils.drawGradientVertical(x, y, width, maxHeight, radius, bloomCol, fadedCol);
            RectUtils.drawGradientVertical(x, y + height - maxHeight, width, maxHeight, radius, fadedCol, bloomCol);
        } else {
            drawRound(x, y, width, height, radius, false, color);
        }
    }

    public static void drawShaderRound(float x, float y, float width, float height, float radius, Color color) {
        x += 0.2F;
        y += 0.2F;
        width -= 0.4F;
        height -= 0.4F;
//        radius -= 0.5F;

        if (shadersModule == null) {
            if (Demise.INSTANCE.getModuleManager() != null) {
                Shaders shaders = Demise.INSTANCE.getModuleManager().getModule(Shaders.class);

                if (shaders != null) {
                    shadersModule = shaders;
                }
            }
        } else if (shadersModule.isEnabled() && shadersModule.bloom.get()) {
            Color c = new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color());
//            color = new Color(c.getRed(), c.getGreen(), c.getBlue(), 200);
            color = ColorUtils.interpolateColorC(color, c, 0.5F);
        }

        drawRound(x, y, width, height, radius, color, true);
    }

    public static void drawGradientHorizontal(float x, float y, float width, float height, float radius, Color left, Color right) {
        drawGradientRound(x, y, width, height, radius, left, left, right, right);
    }

    public static void drawGradientHorizontal2(float x, float y, float width, float height, float radius, Color left, Color right) {
        drawGradientRound(x+1, y+1, width-2, height-2, radius, left, left, right, right);
    }

    public static void drawGradientVertical(float x, float y, float width, float height, float alpha, Color top, Color bottom) {
        drawGradientRound(x, y, width, height, alpha, bottom, top, bottom, top);
    }

    public static void drawGradientRound(float x, float y, float width, float height, float radius, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        RenderUtils.setAlphaLimit(0);
        RenderUtils.resetColor();
        GLUtils.startBlend();
        roundedGradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);
        //Top left
        roundedGradientShader.setUniformf("color1", topLeft.getRed() / 255f, topLeft.getGreen() / 255f, topLeft.getBlue() / 255f, topLeft.getAlpha() / 255f);
        // Bottom Left
        roundedGradientShader.setUniformf("color2", bottomLeft.getRed() / 255f, bottomLeft.getGreen() / 255f, bottomLeft.getBlue() / 255f, bottomLeft.getAlpha() / 255f);
        //Top Right
        roundedGradientShader.setUniformf("color3", topRight.getRed() / 255f, topRight.getGreen() / 255f, topRight.getBlue() / 255f, topRight.getAlpha() / 255f);
        //Bottom Right
        roundedGradientShader.setUniformf("color4", bottomRight.getRed() / 255f, bottomRight.getGreen() / 255f, bottomRight.getBlue() / 255f, bottomRight.getAlpha() / 255f);
        ShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedGradientShader.unload();
        GLUtils.endBlend();
    }

    public static void drawRound(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        RenderUtils.resetColor();
        GLUtils.startBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderUtils.setAlphaLimit(0);
        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        ShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedShader.unload();
        GLUtils.endBlend();
    }

    public static void drawRoundOutline(float x, float y, float width, float height, float radius, float outlineThickness, Color color, Color outlineColor) {
        RenderUtils.resetColor();
        GLUtils.startBlend();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderUtils.setAlphaLimit(0);
        roundedOutlineShader.init();

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.getScaleFactor());
        roundedOutlineShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        roundedOutlineShader.setUniformf("outlineColor", outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f, outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f);

        ShaderUtils.drawQuads(x - (2 + outlineThickness), y - (2 + outlineThickness), width + (4 + outlineThickness * 2), height + (4 + outlineThickness * 2));
        roundedOutlineShader.unload();
        GLUtils.endBlend();
    }

    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, ShaderUtils roundedTexturedShader) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        roundedTexturedShader.setUniformf("location", x * sr.getScaleFactor(),
                (Minecraft.getMinecraft().displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        roundedTexturedShader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        roundedTexturedShader.setUniformf("radius", radius * sr.getScaleFactor());
    }

    public static void drawHGradientRect(float x, float y, float width, float height, Color left, Color right) {
        RenderUtils.setAlphaLimit(0);
        RenderUtils.resetColor();
        GLUtils.startBlend();
        gradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, 0, gradientShader);
        //Top left
        gradientShader.setUniformf("color1", left.getRed() / 255f, left.getGreen() / 255f, left.getBlue() / 255f, left.getAlpha() / 255f);
        // Bottom Left
        gradientShader.setUniformf("color2", left.getRed() / 255f, left.getGreen() / 255f, left.getBlue() / 255f, left.getAlpha() / 255f);
        //Top Right
        gradientShader.setUniformf("color3", right.getRed() / 255f, right.getGreen() / 255f, right.getBlue() / 255f, right.getAlpha() / 255f);
        //Bottom Right
        gradientShader.setUniformf("color4", right.getRed() / 255f, right.getGreen() / 255f, right.getBlue() / 255f, right.getAlpha() / 255f);
        ShaderUtils.drawQuads(x, y, width, height);
        gradientShader.unload();
        GLUtils.endBlend();
    }
}
