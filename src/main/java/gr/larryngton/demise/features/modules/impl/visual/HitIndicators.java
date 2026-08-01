package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import org.lwjglx.util.vector.Vector2f;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "HitIndicators", description = "Displays damage dealt to entities in the hud.", legit = true)
public class HitIndicators extends Module {
    private final List<HitInfo> hitList = new ArrayList<>();

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase entityLivingBase) {
                float health = PlayerUtils.getActualHealth(entityLivingBase) + entityLivingBase.getAbsorptionAmount();
                float delta = entityLivingBase.lastHealth - health;

                if (delta > 0) {
                    Vec3 interpolatedPos = new Vec3(
                            MathUtils.interpolate(entityLivingBase.prevPosX, entityLivingBase.posX),
                            MathUtils.interpolate(entityLivingBase.prevPosY, entityLivingBase.posY) + entityLivingBase.getEyeHeight() / 2,
                            MathUtils.interpolate(entityLivingBase.prevPosZ, entityLivingBase.posZ)
                    );

                    interpolatedPos.xCoord += MathUtils.randomizeDouble(-0.5, 0.5);
                    interpolatedPos.yCoord += MathUtils.randomizeDouble(-0.25, 0.25);
                    interpolatedPos.zCoord += MathUtils.randomizeDouble(-0.5, 0.5);

                    hitList.add(new HitInfo(BigDecimal.valueOf(delta).setScale(1, RoundingMode.HALF_UP).floatValue(), interpolatedPos, System.currentTimeMillis(), entityLivingBase == mc.thePlayer));
                }

                entityLivingBase.lastHealth = health;
            }
        }

        for (HitInfo info : hitList) {
            Vec3 world = info.worldPos;
            Vector2f pos = RenderUtils.worldToScreen((float) world.xCoord, (float) world.yCoord, (float) world.zCoord, new ScaledResolution(mc), false);

            if (pos != null) {
                String str = String.valueOf(BigDecimal.valueOf(info.healthDelta).setScale(1, RoundingMode.HALF_UP).floatValue());
                Fonts.genshin.get(50 * info.scale).drawCenteredString(str, pos.x + info.scale, pos.y + info.scale, info.isSelf ? -1 : getModule(Interface.class).color((int) (info.addedTime % 500)));
            }
        }

        hitList.forEach(HitInfo::update);
        hitList.removeIf(l -> System.currentTimeMillis() - l.addedTime > 1000);
    }

    private static class HitInfo {
        public float healthDelta;
        public Vec3 worldPos;
        public long addedTime;
        private final double finalDriftPos;
        public float scale = 1.1F;
        public boolean isSelf;

        public HitInfo(float healthDelta, Vec3 worldPos, long addedTime, boolean isSelf) {
            this.healthDelta = healthDelta;
            this.worldPos = worldPos;
            this.addedTime = addedTime;
            finalDriftPos = worldPos.yCoord + 0.15;
            this.isSelf = isSelf;

            if (isSelf) {
                if (mc.thePlayer.fallDistance > 0.0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && !mc.thePlayer.isPotionActive(Potion.blindness) && mc.thePlayer.ridingEntity == null) {
                    scale = 2F;
                }
            }
        }

        public void update() {
            worldPos.yCoord = MathUtils.interpolate(worldPos.yCoord, finalDriftPos, 0.07F);
            scale = MathUtils.interpolate(scale, 0.5F, 0.35F);
        }
    }
}
