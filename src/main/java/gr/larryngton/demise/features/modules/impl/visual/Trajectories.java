package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.item.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;

@ModuleInfo(name = "Trajectories", description = "Renders throwable items' trajectories.", legit = true)
public class Trajectories extends Module {
    private final BoolValue onlyHolding = new BoolValue("Only holding", false, this);
    private final SliderValue maxPredictionTicks = new SliderValue("Max prediction ticks", 20, 1, 100, 1, this, () -> !onlyHolding.get());

    private final ArrayList<ArrayList<Vec3>> points = new ArrayList<>();

    @Override
    public void onDisable() {
        points.clear();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!onlyHolding.get()) {
            points.clear();
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityArrow || entity instanceof EntitySnowball || entity instanceof EntityEgg || entity instanceof EntityEnderPearl || entity instanceof EntityFireball) {
                    if (!entity.onGround && !entity.isInWater()) {
                        boolean flag = true;
                        int ticksInAir = 0;
                        double posX = entity.posX;
                        double posY = entity.posY;
                        double posZ = entity.posZ;
                        double motionX = entity.motionX;
                        double motionY = entity.motionY;
                        double motionZ = entity.motionZ;
                        ArrayList<Vec3> vec3s = new ArrayList<>();

                        for (int i = 0; i < maxPredictionTicks.getInt() && flag; i++) {
                            if (ticksInAir > 300) {
                                flag = false;
                            }
                            ++ticksInAir;
                            Vec3 vec3 = new Vec3(posX, posY, posZ);
                            Vec3 vec4 = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
                            MovingObjectPosition movingobjectposition = mc.theWorld.rayTraceBlocks(vec3, vec4);
                            if (movingobjectposition != null) {
                                flag = false;
                            }
                            posX += motionX;
                            posY += motionY;
                            posZ += motionZ;
                            float f2 = 0.99f;
                            if (entity instanceof EntityFireball) {
                                f2 = 0.95f;
                            }
                            float f3 = 0.03f;
                            if (entity instanceof EntityArrow) {
                                f3 = 0.05f;
                            } else if (entity instanceof EntityFireball) {
                                f3 = 0.0f;
                            }
                            if (entity instanceof EntityFireball entityFireball) {
                                motionX += entityFireball.accelerationX;
                                motionY += entityFireball.accelerationY;
                                motionZ += entityFireball.accelerationZ;
                            }
                            motionX *= f2;
                            motionY *= f2;
                            motionZ *= f2;
                            motionY -= f3;
                            vec3s.add(new Vec3(posX, posY, posZ));
                        }
                        points.add(vec3s);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (!onlyHolding.get() && !points.isEmpty()) {
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(2848);
            GL11.glDisable(3553);
            GlStateManager.disableCull();
            GL11.glDepthMask(false);

            GL11.glLineWidth(1.5F);
            for (ArrayList<Vec3> vec3s : points) {
                if (vec3s.size() > 1) {
                    Tessellator tessellator = Tessellator.getInstance();
                    WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    worldrenderer.begin(3, DefaultVertexFormats.POSITION);
                    for (int i = 0; i < vec3s.size(); i++) {
                        Vec3 vec3 = vec3s.get(i);
                        Color color = new Color(getModule(Interface.class).color(i));
                        GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 0.85f);
                        worldrenderer.pos((float) vec3.xCoord - mc.getRenderManager().renderPosX, (float) vec3.yCoord - mc.getRenderManager().renderPosY, (float) vec3.zCoord - mc.getRenderManager().renderPosZ).endVertex();
                    }
                    tessellator.draw();
                }
            }
            GL11.glLineWidth(1.0f);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glDepthMask(true);
            GlStateManager.enableCull();
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDisable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2848);
        }

        boolean bow;
        boolean potion;
        if (mc.thePlayer.getHeldItem() == null) {
            return;
        }
        if (!(mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSnowball) && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemEnderPearl) && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemEgg) && (!(mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion) || !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage()))) {
            return;
        }
        bow = (mc.thePlayer.getHeldItem().getItem() instanceof ItemBow);
        potion = (mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion);
        float throwingYaw = RotationHandler.currentRotation[0];
        float throwingPitch = RotationHandler.currentRotation[1];
        double posX = mc.getRenderManager().renderPosX - MathHelper.cos(throwingYaw / 180.0f * 3.141593f) * 0.16f;
        double posY = mc.getRenderManager().renderPosY + mc.thePlayer.getEyeHeight() - 0.1000000014901161;
        double posZ = mc.getRenderManager().renderPosZ - MathHelper.sin(throwingYaw / 180.0f * 3.141593f) * 0.16f;
        double motionX = -MathHelper.sin(throwingYaw / 180.0f * 3.141593f) * MathHelper.cos(throwingPitch / 180.0f * 3.141593f) * (bow ? 1.0 : 0.4);
        double motionY = -MathHelper.sin((throwingPitch - (potion ? 20 : 0)) / 180.0f * 3.141593f) * (bow ? 1.0 : 0.4);
        double motionZ = MathHelper.cos(throwingYaw / 180.0f * 3.141593f) * MathHelper.cos(throwingPitch / 180.0f * 3.141593f) * (bow ? 1.0 : 0.4);
        int var6 = 72000 - mc.thePlayer.getItemInUseCount();
        float power = var6 / 20.0f;
        power = (power * power + power * 2.0f) / 3.0f;
        if (power < 0.1) {
            return;
        }
        if (power > 1.0f) {
            power = 1.0f;
        }
        float distance = MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;
        double motion1 = (bow ? (power * 2.0f) : 1.0f) * (potion ? 0.5 : 1.5);
        motionX *= motion1;
        motionY *= motion1;
        motionZ *= motion1;
        GlStateManager.resetColor();
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2848);
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(1.5F);
        GL11.glBegin(3);
        boolean hasLanded = false;
        MovingObjectPosition landingPosition = null;

        for (int i = 0; !hasLanded && posY > 0.0; i++) {
            Vec3 present = new Vec3(posX, posY, posZ);
            Vec3 future = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
            MovingObjectPosition possibleLandingStrip = mc.theWorld.rayTraceBlocks(present, future, false, true, false);
            if (possibleLandingStrip != null) {
                if (possibleLandingStrip.typeOfHit != MovingObjectPosition.MovingObjectType.MISS) {
                    landingPosition = possibleLandingStrip;
                    hasLanded = true;
                }
            } else {
                Entity entityHit = this.getEntityHit(present, future);
                if (entityHit != null) {
                    landingPosition = new MovingObjectPosition(entityHit);
                    hasLanded = true;
                }
            }
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            float motionAdjustment = 0.99f;
            motionX *= motionAdjustment;
            motionY *= motionAdjustment;
            motionZ *= motionAdjustment;
            motionY -= (potion ? 0.05 : (bow ? 0.05 : 0.03));

            Color color = new Color(getModule(Interface.class).color(i));
            GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 0.85f);
            GL11.glVertex3d(posX - mc.getRenderManager().renderPosX, posY - mc.getRenderManager().renderPosY, posZ - mc.getRenderManager().renderPosZ);
        }

        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GL11.glDisable(2848);
        GL11.glDisable(3042);
        GlStateManager.resetColor();
        if (landingPosition != null && landingPosition.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            double n11 = -mc.getRenderManager().renderPosX;
            double n12 = -mc.getRenderManager().renderPosY;
            GL11.glTranslated(n11, n12, -mc.getRenderManager().renderPosZ);
            RenderUtils.drawAxisAlignedBB(landingPosition.entityHit.getEntityBoundingBox().expand(0.225, 0.125, 0.225).offset(0.0, 0.1, 0.0), true, new Color(235, 40, 40, 75).getRGB());
            double renderPosX = mc.getRenderManager().renderPosX;
            double renderPosY = mc.getRenderManager().renderPosY;
            GL11.glTranslated(renderPosX, renderPosY, mc.getRenderManager().renderPosZ);
        }
    }

    private ArrayList<EntityLivingBase> getEntities() {
        ArrayList<EntityLivingBase> ret = new ArrayList<>();
        for (Entity e : mc.theWorld.loadedEntityList) {
            if (e != mc.thePlayer && e instanceof EntityLivingBase ee) {
                ret.add(ee);
            }
        }
        return ret;
    }

    private Entity getEntityHit(Vec3 vecOrig, Vec3 vecNew) {
        for (EntityLivingBase o : this.getEntities()) {
            if (o != mc.thePlayer) {
                float expander = 0.2f;
                AxisAlignedBB bounding2 = o.getEntityBoundingBox().expand(expander, expander, expander);
                MovingObjectPosition possibleEntityLanding = bounding2.calculateIntercept(vecOrig, vecNew);
                if (possibleEntityLanding != null) {
                    return o;
                }
            }
        }
        return null;
    }
}