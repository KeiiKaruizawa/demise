package net.minecraft.client.renderer.entity.layers;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.visual.Cape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public class LayerCape implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer playerRenderer;

    public LayerCape(RenderPlayer playerRendererIn) {
        this.playerRenderer = playerRendererIn;
    }

    public void doRenderLayer(AbstractClientPlayer entitylivingbaseIn, float p_177141_2_, float p_177141_3_, float partialTicks, float p_177141_5_, float p_177141_6_, float p_177141_7_, float scale) {
        Cape cape = Demise.INSTANCE.getModuleManager().getModule(Cape.class);

        if (!entitylivingbaseIn.isInvisible() && (entitylivingbaseIn.hasPlayerInfo() && entitylivingbaseIn.isWearing(EnumPlayerModelParts.CAPE) && entitylivingbaseIn.getLocationCape() != null || entitylivingbaseIn.getName().equals(Minecraft.getMinecraft().getSession().getUsername()) && cape.isEnabled())) {
            if (cape.tint.get()) {
                GlStateManager.color(cape.color.get().getRed() / 255f, cape.color.get().getGreen() / 255f, cape.color.get().getBlue() / 255f, cape.color.get().getAlpha() / 255f);
            } else {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            if (entitylivingbaseIn.getName().equals(Minecraft.getMinecraft().getSession().getUsername()) && cape.isEnabled()) {
                switch (cape.capeMode.get()) {
                    case "Rise" -> {
                        switch (cape.riseMode.get()) {
                            case "Normal" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/rise/RiseCape.png"));
                            case "Red" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/rise/RiseCapeRed.png"));
                            case "Green" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/rise/RiseCapeGreen.png"));
                            case "Blue" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/rise/RiseCapeBlue.png"));
                            case "Dogshit" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/rise/RiseCapeGato.png"));
                        }
                    }
                    case "Other" -> {
                        switch (cape.otherMode.get()) {
                            case "Aesthetic drive" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/AestheticDrive.png"));
                            case "BEST CLIENT" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/BESTCLIENT.png"));
                            case "Firefox" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/Firefox.png"));
                            case "Larp" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/Larp.png"));
                            case "Novoline" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/Novoline.png"));
                            case "Polar" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/Polar.png"));
                            case "Tenacity" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/Tenacity.png"));
                            case "KKE" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/KKE.png"));
                            case "cp" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/cp.png"));
                            case "loli" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/loli.png"));
                            case "Grim" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/Grim.png"));
                            case "❤" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/other/karuizawa.png"));
                        }
                    }
                    case "Raven" -> {
                        switch (cape.ravenMode.get()) {
                            case "Anime" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/raven/anime.png"));
                            case "Aqua" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/raven/rvn_aqua.png"));
                            case "Green" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/raven/rvn_green.png"));
                            case "Purple" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/raven/rvn_purple.png"));
                            case "Red" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/raven/rvn_red.png"));
                            case "White" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/raven/rvn_white.png"));
                            case "Yellow" ->
                                    this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/raven/rvn_yellow.png"));
                        }
                    }
                    case "Minecraft" ->
                            this.playerRenderer.bindTexture(new ResourceLocation("demise/texture/cape/mc/" + cape.mcMode.get() + ".png"));
                }
            } else {
                this.playerRenderer.bindTexture(entitylivingbaseIn.getLocationCape());
            }

            if (cape.modernPhysics.get()) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.0F, 0.0F, 0.125F);

                if (this.playerRenderer.getMainModel() != null) {
                    this.playerRenderer.getMainModel().setCapeAngles(entitylivingbaseIn, partialTicks);
                }

                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                this.playerRenderer.getMainModel().bipedCape.render(0.0625F);
                GlStateManager.popMatrix();
            } else {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.0F, 0.0F, 0.125F);
                double d0 = entitylivingbaseIn.prevChasingPosX + (entitylivingbaseIn.chasingPosX - entitylivingbaseIn.prevChasingPosX) * (double) partialTicks - (entitylivingbaseIn.prevPosX + (entitylivingbaseIn.posX - entitylivingbaseIn.prevPosX) * (double) partialTicks);
                double d1 = entitylivingbaseIn.prevChasingPosY + (entitylivingbaseIn.chasingPosY - entitylivingbaseIn.prevChasingPosY) * (double) partialTicks - (entitylivingbaseIn.prevPosY + (entitylivingbaseIn.posY - entitylivingbaseIn.prevPosY) * (double) partialTicks);
                double d2 = entitylivingbaseIn.prevChasingPosZ + (entitylivingbaseIn.chasingPosZ - entitylivingbaseIn.prevChasingPosZ) * (double) partialTicks - (entitylivingbaseIn.prevPosZ + (entitylivingbaseIn.posZ - entitylivingbaseIn.prevPosZ) * (double) partialTicks);

                float f = entitylivingbaseIn.prevRenderYawOffset + (entitylivingbaseIn.renderYawOffset - entitylivingbaseIn.prevRenderYawOffset) * partialTicks;

                double d3 = MathHelper.sin(f * (float) Math.PI / 180.0F);
                double d4 = -MathHelper.cos(f * (float) Math.PI / 180.0F);
                float f1 = (float) d1 * 10.0F;
                f1 = MathHelper.clamp_float(f1, -6.0F, 32.0F);
                float f2 = (float) (d0 * d3 + d2 * d4) * 100.0F;
                float f3 = (float) (d0 * d4 - d2 * d3) * 100.0F;

                if (f2 < 0.0F) {
                    f2 = 0.0F;
                }

                if (f2 > 165.0F) {
                    f2 = 165.0F;
                }

                if (f1 < -5.0F) {
                    f1 = -5.0F;
                }

                float f4 = entitylivingbaseIn.prevCameraYaw + (entitylivingbaseIn.cameraYaw - entitylivingbaseIn.prevCameraYaw) * partialTicks;
                f1 = f1 + MathHelper.sin((entitylivingbaseIn.prevDistanceWalkedModified + (entitylivingbaseIn.distanceWalkedModified - entitylivingbaseIn.prevDistanceWalkedModified) * partialTicks) * 6.0F) * 32.0F * f4;

                if (entitylivingbaseIn.isSneaking()) {
                    f1 += 25.0F;
                    GlStateManager.translate(0.0F, 0.142F, -0.0178F);
                }

                GlStateManager.rotate(6.0F + f2 / 2.0F + f1, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(f3 / 2.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(-f3 / 2.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                this.playerRenderer.getMainModel().renderCape(0.0625F);
                GlStateManager.popMatrix();
            }
        }
    }

    public boolean shouldCombineTextures() {
        return false;
    }
}