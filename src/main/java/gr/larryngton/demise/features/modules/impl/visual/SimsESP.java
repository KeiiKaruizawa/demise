package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.friend.FriendManager;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;

// @author nazunavfx
@ModuleInfo(name = "SimsESP", description = "Sims funny.")
public final class SimsESP extends Module {
    private final BoolValue team = new BoolValue("Team color", true);

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        for (Entity e : mc.theWorld.loadedEntityList) {
            if (e instanceof EntityPlayer) {
                EntityPlayer entity = (EntityPlayer)e;

                if((entity != mc.thePlayer || mc.gameSettings.thirdPersonView != 0) && !entity.isInvisible()) {
                    float pTicks = mc.timer.renderPartialTicks;
                    double a = (entity.prevPosX + (entity.posX - entity.prevPosX) * pTicks)
                            - mc.getRenderManager().renderPosX;
                    double b = (entity.prevPosY + (entity.posY - entity.prevPosY) * pTicks)
                            - mc.getRenderManager().renderPosY + 0.5;
                    double c = (entity.prevPosZ + (entity.posZ - entity.prevPosZ) * pTicks)
                            - mc.getRenderManager().renderPosZ;

                    double pX = 0, pY = 0, pZ = 0;

                    GlStateManager.pushMatrix();
                    GlStateManager.translate(a, b, c);
                    float rot = ((entity.ticksExisted - 1) + (entity.ticksExisted - (entity.ticksExisted - 1)) * pTicks) * 5;
                    GlStateManager.rotate(rot, 0, 1, 0);
                    pre3D();

                    int color = new Color(255, 50, 50, 150).getRGB();
                    if (Demise.INSTANCE.getFriendManager().isFriend(entity.getName()) || entity == mc.thePlayer) {
                        color = new Color(0, 255, 0, 150).getRGB();
                    }

                    if (team.get() && PlayerUtils.isInTeam(entity)) {
                        color = new Color(0, 255, 0, 150).getRGB();
                    }

                    if (KillAura.target == entity && getModule(KillAura.class).isEnabled()) {
                        color = new Color(50, 50, 255, 150).getRGB();
                    }

                    RenderUtils.color(color);
                    GL11.glBegin(GL11.GL_TRIANGLES);
                    GL11.glVertex3d(pX - 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 2.5, pZ);
                    GL11.glVertex3d(pX, pY + 2, pZ - 0.25);

                    GL11.glVertex3d(pX - 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 2, pZ + 0.25);
                    GL11.glVertex3d(pX, pY + 2.5, pZ);

                    GL11.glVertex3d(pX + 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 2.5, pZ);
                    GL11.glVertex3d(pX, pY + 2, pZ + 0.25);

                    GL11.glVertex3d(pX + 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 2, pZ - 0.25);
                    GL11.glVertex3d(pX, pY + 2.5, pZ);

                    GL11.glVertex3d(pX, pY + 1.5, pZ);
                    GL11.glVertex3d(pX - 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 2, pZ - 0.25);

                    GL11.glVertex3d(pX - 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 1.5, pZ);
                    GL11.glVertex3d(pX, pY + 2, pZ + 0.25);

                    GL11.glVertex3d(pX + 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 2, pZ + 0.25);
                    GL11.glVertex3d(pX, pY + 1.5, pZ);

                    GL11.glVertex3d(pX, pY + 2, pZ - 0.25);
                    GL11.glVertex3d(pX + 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 1.5, pZ);
                    GL11.glEnd();

//                    glColor(getColorOpacity(color,200));
                    GL11.glBegin(GL11.GL_LINE_LOOP);
                    GL11.glVertex3d(pX - 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 2.5, pZ);
                    GL11.glVertex3d(pX + 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 1.5, pZ);
                    GL11.glEnd();

                    GL11.glBegin(GL11.GL_LINE_LOOP);
                    GL11.glVertex3d(pX, pY + 1.5, pZ);
                    GL11.glVertex3d(pX, pY + 2, pZ + 0.25);
                    GL11.glVertex3d(pX, pY + 2.5, pZ);
                    GL11.glVertex3d(pX, pY + 2, pZ - 0.25);
                    GL11.glEnd();

                    GL11.glBegin(GL11.GL_LINE_LOOP);
                    GL11.glVertex3d(pX, pY + 2, pZ + 0.25);
                    GL11.glVertex3d(pX + 0.25, pY + 2, pZ);
                    GL11.glVertex3d(pX, pY + 2, pZ - 0.25);
                    GL11.glVertex3d(pX - 0.25, pY + 2, pZ);

                    GL11.glEnd();

                    post3D();
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    private void post3D() {
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        RenderUtils.resetColor();
//        if (lighting && !GL11.glIsEnabled(GL11.GL_LIGHTING)) {
//            lighting = false;
//            GL11.glEnable(GL11.GL_LIGHTING);
//        }
    }

    private void pre3D() {
        GL11.glPushMatrix();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
    }
}