package gr.larryngton.demise.utils.render.shader.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.gui.mainmenu.GuiMainMenu;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.render.GifRenderer;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.render.shader.ShaderUtils;
import gr.larryngton.demise.utils.render.video.VideoPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.optifine.CustomPanorama;
import net.optifine.CustomPanoramaProperties;
import org.bytedeco.javacv.FrameGrabber;
import org.lwjgl.opengl.GL11;
import org.lwjglx.util.glu.Project;

import static net.minecraft.client.gui.Gui.zLevel;
import static net.minecraft.client.gui.GuiScreen.height;
import static net.minecraft.client.gui.GuiScreen.width;

public class MenuBackground implements InstanceAccess {
    private static final ShaderUtils mainmenu = new ShaderUtils("mainmenu");
    public static float panoramaTimer;
    private static float smoothedX;
    private static float smoothedY;
    public static float xOffSet;
    public static float yOffSet;
    private static final ResourceLocation[] titlePanoramaPaths = new ResourceLocation[]{new ResourceLocation("textures/gui/title/background/panorama_0.png"), new ResourceLocation("textures/gui/title/background/panorama_1.png"), new ResourceLocation("textures/gui/title/background/panorama_2.png"), new ResourceLocation("textures/gui/title/background/panorama_3.png"), new ResourceLocation("textures/gui/title/background/panorama_4.png"), new ResourceLocation("textures/gui/title/background/panorama_5.png")};
    private static ResourceLocation backgroundTexture;
    private static GuiScreen lastGui;
    private static final GifRenderer renderer = new GifRenderer(new ResourceLocation("demise/img/gif/big ben yahoo.gif"));
    private static boolean yahoo;

    public static void draw(long initTime) {
        if (mc.currentScreen != lastGui) {
            DynamicTexture viewportTexture = new DynamicTexture(256, 256);
            backgroundTexture = mc.getTextureManager().getDynamicTextureLocation("background", viewportTexture);
            lastGui = mc.currentScreen;

            if (mc.currentScreen instanceof GuiMainMenu) {
                // 1 in 10k chance to see the server owner
                yahoo = Math.random() < 0.0001;
            } else {
                yahoo = false;
            }
        }

        ScaledResolution sr = new ScaledResolution(mc);

        if (yahoo) {
            renderer.drawTexture(0, 0, sr.getScaledWidth(), sr.getScaledHeight());
            renderer.update();
            return;
        }

        switch (Demise.INSTANCE.getClientSettings().background.get()) {
            case "Minecraft":
                GlStateManager.disableAlpha();
                renderSkybox(mc.timer.partialTicks);
                GlStateManager.enableAlpha();
                break;
            case "Shader":
                mainmenu.init();
                mainmenu.setUniformf("TIME", (float) (System.currentTimeMillis() - initTime) / 1000);
                mainmenu.setUniformf("RESOLUTION", (float) ((double) sr.getScaledWidth() * sr.getScaleFactor()), (float) ((double) sr.getScaledHeight() * sr.getScaleFactor()));
                ShaderUtils.drawFixedQuads();
                mainmenu.unload();
                break;
            case "Static":
                RenderUtils.drawImage(new ResourceLocation("demise/img/backgrounds/background.png"), 0, 0, sr.getScaledWidth(), sr.getScaledHeight());
                break;
            case "Video":
                try {
                    VideoPlayer.render(0, 0, sr.getScaledWidth(), sr.getScaledHeight());
                } catch (FrameGrabber.Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case "Rise 5":
                if (!Demise.INSTANCE.getClientSettings().fullTint.get()) {
                    RenderUtils.color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color());
                    Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("demise/img/backgrounds/rise5.png"));
                }

                float scale = 1.66F;
                float amount = 1500;

                if (panoramaTimer % 100 == 0) {
                    xOffSet = (float) (Math.random() - 0.5F) * amount;
                    yOffSet = (float) (Math.random() - 0.5F) * amount;
                }

                smoothedX = (smoothedX * 250 + xOffSet) / 259;
                smoothedY = (smoothedY * 250 + yOffSet) / 259;

                if (!Demise.INSTANCE.getClientSettings().fullTint.get()) {
                    Gui.drawModalRectWithCustomSizedTexture(0, 0, sr.getScaledWidth() / scale + smoothedX - 150, sr.getScaledHeight() / scale + smoothedY - 100, sr.getScaledWidth(), height, sr.getScaledWidth() * scale, sr.getScaledHeight() * scale);
                } else {
                    RenderUtils.drawImage(new ResourceLocation("demise/img/backgrounds/rise5gray.png"), (float) 0, (float) 0, (int) (sr.getScaledWidth() * scale), (int) (sr.getScaledHeight() * scale), (int) (sr.getScaledWidth() / scale + smoothedX - 150), (int) (sr.getScaledHeight() / scale + smoothedY - 100), Demise.INSTANCE.getModuleManager().getModule(Interface.class).color());
                }
                break;
        }
    }

    private static void renderSkybox(float partialTicks) {
        mc.getFramebuffer().unbindFramebuffer();
        GlStateManager.viewport(0, 0, 256, 256);
        drawPanorama(partialTicks);
        rotateAndBlurSkybox();
        int i = 3;
        CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();

        if (custompanoramaproperties != null) {
            i = custompanoramaproperties.getBlur3();
        }

        for (int j = 0; j < i; ++j) {
            rotateAndBlurSkybox();
            rotateAndBlurSkybox();
        }

        mc.getFramebuffer().bindFramebuffer(true);
        GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
        float f2 = width > height ? 120.0F / (float) width : 120.0F / (float) height;
        float f = (float) height * f2 / 256.0F;
        float f1 = (float) width * f2 / 256.0F;
        int k = width;
        int l = height;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(0.0D, l, zLevel).tex(0.5F - f, 0.5F + f1).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(k, l, zLevel).tex(0.5F - f, 0.5F - f1).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(k, 0.0D, zLevel).tex(0.5F + f, 0.5F - f1).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(0.0D, 0.0D, zLevel).tex(0.5F + f, 0.5F + f1).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        tessellator.draw();
    }

    private static void drawPanorama(float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int i = 8;
        int j = 64;
        CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();

        if (custompanoramaproperties != null) {
            j = custompanoramaproperties.getBlur1();
        }

        for (int k = 0; k < j; ++k) {
            GlStateManager.pushMatrix();
            float f = ((float) (k % i) / (float) i - 0.5F) / 64.0F;
            float f1 = ((float) (k / i) / (float) i - 0.5F) / 64.0F;
            float f2 = 0.0F;
            GlStateManager.translate(f, f1, f2);
            GlStateManager.rotate(MathHelper.sin((panoramaTimer + partialTicks) / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-(panoramaTimer + partialTicks) * 0.1F, 0.0F, 1.0F, 0.0F);

            for (int l = 0; l < 6; ++l) {
                GlStateManager.pushMatrix();

                switch (l) {
                    case 1 -> GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                    case 2 -> GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                    case 3 -> GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                    case 4 -> GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                    case 5 -> GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                }

                ResourceLocation[] aresourcelocation = titlePanoramaPaths;

                if (custompanoramaproperties != null) {
                    aresourcelocation = custompanoramaproperties.getPanoramaLocations();
                }

                mc.getTextureManager().bindTexture(aresourcelocation[l]);
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                int i1 = 255 / (k + 1);
                worldrenderer.pos(-1.0D, -1.0D, 1.0D).tex(0.0D, 0.0D).color(255, 255, 255, i1).endVertex();
                worldrenderer.pos(1.0D, -1.0D, 1.0D).tex(1.0D, 0.0D).color(255, 255, 255, i1).endVertex();
                worldrenderer.pos(1.0D, 1.0D, 1.0D).tex(1.0D, 1.0D).color(255, 255, 255, i1).endVertex();
                worldrenderer.pos(-1.0D, 1.0D, 1.0D).tex(0.0D, 1.0D).color(255, 255, 255, i1).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
            }

            GlStateManager.popMatrix();
            GlStateManager.colorMask(true, true, true, false);
        }

        worldrenderer.setTranslation(0.0D, 0.0D, 0.0D);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
    }

    private static void rotateAndBlurSkybox() {
        mc.getTextureManager().bindTexture(backgroundTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, 256, 256);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.colorMask(true, true, true, false);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        GlStateManager.disableAlpha();
        int i = 3;
        int j = 3;
        CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();

        if (custompanoramaproperties != null) {
            j = custompanoramaproperties.getBlur2();
        }

        for (int k = 0; k < j; ++k) {
            float f = 1.0F / (float) (k + 1);
            int l = width;
            int i1 = height;
            float f1 = (float) (k - i / 2) / 256.0F;
            worldrenderer.pos(l, i1, zLevel).tex(0.0F + f1, 1.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos(l, 0.0D, zLevel).tex(1.0F + f1, 1.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos(0.0D, 0.0D, zLevel).tex(1.0F + f1, 0.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos(0.0D, i1, zLevel).tex(0.0F + f1, 0.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.colorMask(true, true, true, true);
    }
}