package net.minecraft.client.model;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.visual.ESP;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

public class ModelPlayer extends ModelBiped {
    public final ModelRenderer bipedLeftArmwear;
    public final ModelRenderer bipedRightArmwear;
    public final ModelRenderer bipedLeftLegwear;
    public final ModelRenderer bipedRightLegwear;
    public final ModelRenderer bipedBodyWear;
    public final ModelRenderer bipedCape;
    private final ModelRenderer bipedDeadmau5Head;
    private final boolean smallArms;

    public ModelPlayer(float p_i46304_1_, boolean p_i46304_2_) {
        super(p_i46304_1_, 0.0F, 64, 64);
        this.smallArms = p_i46304_2_;
        this.bipedDeadmau5Head = new ModelRenderer(this, 24, 0);
        this.bipedDeadmau5Head.addBox(-3.0F, -6.0F, -1.0F, 6, 6, 1, p_i46304_1_);

        this.bipedCape = new ModelRenderer(this, 0, 0);
        this.bipedCape.setTextureSize(64, 32);
        this.bipedCape.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1, p_i46304_1_);

        if (p_i46304_2_) {
            this.bipedLeftArm = new ModelRenderer(this, 32, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, p_i46304_1_);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.5F, 0.0F);
            this.bipedRightArm = new ModelRenderer(this, 40, 16);
            this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, p_i46304_1_);
            this.bipedRightArm.setRotationPoint(-5.0F, 2.5F, 0.0F);
            this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, p_i46304_1_ + 0.25F);
            this.bipedLeftArmwear.setRotationPoint(5.0F, 2.5F, 0.0F);
            this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
            this.bipedRightArmwear.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, p_i46304_1_ + 0.25F);
            this.bipedRightArmwear.setRotationPoint(-5.0F, 2.5F, 10.0F);
        } else {
            this.bipedLeftArm = new ModelRenderer(this, 32, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, p_i46304_1_);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
            this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, p_i46304_1_ + 0.25F);
            this.bipedLeftArmwear.setRotationPoint(5.0F, 2.0F, 0.0F);
            this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
            this.bipedRightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, p_i46304_1_ + 0.25F);
            this.bipedRightArmwear.setRotationPoint(-5.0F, 2.0F, 10.0F);
        }

        this.bipedLeftLeg = new ModelRenderer(this, 16, 48);
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, p_i46304_1_);
        this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
        this.bipedLeftLegwear = new ModelRenderer(this, 0, 48);
        this.bipedLeftLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, p_i46304_1_ + 0.25F);
        this.bipedLeftLegwear.setRotationPoint(1.9F, 12.0F, 0.0F);
        this.bipedRightLegwear = new ModelRenderer(this, 0, 32);
        this.bipedRightLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, p_i46304_1_ + 0.25F);
        this.bipedRightLegwear.setRotationPoint(-1.9F, 12.0F, 0.0F);
        this.bipedBodyWear = new ModelRenderer(this, 16, 32);
        this.bipedBodyWear.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, p_i46304_1_ + 0.25F);
        this.bipedBodyWear.setRotationPoint(0.0F, 0.0F, 0.0F);
    }

    public void setCapeAngles(AbstractClientPlayer player, float partialTicks) {
        double d0 = player.prevChasingPosX + (player.chasingPosX - player.prevChasingPosX) * partialTicks - (player.prevPosX + (player.posX - player.prevPosX) * partialTicks);
        double d1 = player.prevChasingPosY + (player.chasingPosY - player.prevChasingPosY) * partialTicks - (player.prevPosY + (player.posY - player.prevPosY) * partialTicks);
        double d2 = player.prevChasingPosZ + (player.chasingPosZ - player.prevChasingPosZ) * partialTicks - (player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks);

        player.cape.direction = MathUtils.interpolateRotation(player.cape.direction, (float) MoveUtils.getDirectionPosBased(player), partialTicks * 0.1F);

        double sinYawDir = MathHelper.sin(player.cape.direction * (float) Math.PI / 180.0F);
        double cosYawDir = -MathHelper.cos(player.cape.direction * (float) Math.PI / 180.0F);

        float f = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        double sinYaw = MathHelper.sin(f * (float) Math.PI / 180.0F);
        double cosYaw = -MathHelper.cos(f * (float) Math.PI / 180.0F);

        float lift = MathHelper.clamp_float((float) d1 * 40.0F, -6.0F, 180.0F);
        float forward = MathHelper.clamp_float((float) (d0 * sinYawDir + d2 * cosYawDir) * 100.0F, 1.0F, 165.5F);

        float camYaw = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * partialTicks;
        float walkingLift = MathHelper.sin((player.prevDistanceWalkedModified + (player.distanceWalkedModified - player.prevDistanceWalkedModified) * partialTicks) * 6.0F) * 32.0F * camYaw;

        float forwardPercentage = MathHelper.clamp_float(1 - (160.0F - forward) / 168.0F, 0.0F, 1.0F);
        float distanceWalkedToZeroPercentage = 1.25F - Math.abs(0.25F - (player.distanceWalkedModified - player.prevDistanceWalkedModified) / 0.25F);

        float windLift = ((float) player.getMoveDeltaVector().flat().lengthVector() * distanceWalkedToZeroPercentage * forwardPercentage * (float) (Math.random() * Math.random()) * 25);
        player.cape.windLift = MathUtils.interpolateNoUpdateCheck(player.cape.windLift, windLift, partialTicks * 0.1F);

        forward += walkingLift * 0.7F + player.cape.windLift;

        if (player.isSneaking()) {
            forward += 40.0F;
            GlStateManager.translate(0.0F, 0.142F, 0.0178F);
        }

//        float rotX = MathHelper.clamp_float((float) ((d0 * 1.5) * cosYaw - (d2 * 1.5) * sinYaw) * 100.0F, -100.0F, 100.0F) / 2.0F;
        float rotX = (float) (d0 * 1.5 * cosYaw - d2 * 1.5 * sinYaw) * 50.0F;
        float rotY = MathHelper.clamp_float(6.0F + (forward - 1.0F) / 2.0F + lift, 0.0F, 180.0F);

        float interpolatedRenderYawOffset = MathUtils.interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
        float renderYawOffsetDelta = player.renderYawOffset - interpolatedRenderYawOffset;

        player.cape.renderYawOffsetDelta = MathHelper.clamp_float(MathUtils.interpolateRotation(player.cape.renderYawOffsetDelta, renderYawOffsetDelta, partialTicks * 0.1F), -50.0F, 50.0F);
        rotX += player.cape.renderYawOffsetDelta * 1.2F;

        GlStateManager.rotate(rotY, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(rotX, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-rotX, 0.0F, 1.0F, 0.0F);
    }

    public void render(Entity entityIn, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float scale) {
        super.render(entityIn, p_78088_2_, p_78088_3_, p_78088_4_, p_78088_5_, p_78088_6_, scale);
        GlStateManager.pushMatrix();

        if (this.isChild) {
            float f = 2.0F;
            GlStateManager.scale(1.0F / f, 1.0F / f, 1.0F / f);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
        } else {
            if (entityIn.isSneaking()) {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }
        }
        this.bipedLeftLegwear.render(scale);
        this.bipedRightLegwear.render(scale);
        this.bipedLeftArmwear.render(scale);
        this.bipedRightArmwear.render(scale);
        this.bipedBodyWear.render(scale);

        GlStateManager.popMatrix();
    }

    public void renderDeadmau5Head(float p_178727_1_) {
        copyModelAngles(this.bipedHead, this.bipedDeadmau5Head);
        this.bipedDeadmau5Head.rotationPointX = 0.0F;
        this.bipedDeadmau5Head.rotationPointY = 0.0F;
        this.bipedDeadmau5Head.render(p_178727_1_);
    }

    public void renderCape(float p_178728_1_) {
        this.bipedCape.render(p_178728_1_);
    }

    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        copyModelAngles(this.bipedLeftLeg, this.bipedLeftLegwear);
        copyModelAngles(this.bipedRightLeg, this.bipedRightLegwear);
        copyModelAngles(this.bipedLeftArm, this.bipedLeftArmwear);
        copyModelAngles(this.bipedRightArm, this.bipedRightArmwear);
        copyModelAngles(this.bipedBody, this.bipedBodyWear);
    }

    public void renderRightArm() {
        this.bipedRightArm.render(0.0625F);
        this.bipedRightArmwear.render(0.0625F);
    }

    public void renderLeftArm() {
        this.bipedLeftArm.render(0.0625F);
        this.bipedLeftArmwear.render(0.0625F);
    }

    public void setInvisible(boolean invisible) {
        super.setInvisible(invisible);
        this.bipedLeftArmwear.showModel = invisible;
        this.bipedRightArmwear.showModel = invisible;
        this.bipedLeftLegwear.showModel = invisible;
        this.bipedRightLegwear.showModel = invisible;
        this.bipedBodyWear.showModel = invisible;
        this.bipedCape.showModel = invisible;
        this.bipedDeadmau5Head.showModel = invisible;
    }

    public void postRenderArm(float scale) {
        if (this.smallArms) {
            ++this.bipedRightArm.rotationPointX;
            this.bipedRightArm.postRender(scale);
            --this.bipedRightArm.rotationPointX;
        } else {
            this.bipedRightArm.postRender(scale);
        }
    }

    public void renderGlintCape(Entity var1, float var2, float var3, float var4, float var5, float var6, float var7) {
        super.render(var1, var2, var3, var4, var5, var6, var7);
        GlStateManager.pushMatrix();
        if (this.isChild) {
            float var8 = 2.0F;
            GlStateManager.scale(1.0F / var8, 1.0F / var8, 1.0F / var8);
            GlStateManager.translate(0.0F, 24.0F * var7, 0.0F);
            this.bipedCape.render(var7);
        } else {
            this.bipedCape.render(var7);
        }

        GlStateManager.popMatrix();
    }
}
