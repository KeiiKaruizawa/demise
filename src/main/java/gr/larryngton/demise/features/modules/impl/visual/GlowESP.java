package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "GlowESP", description = "ESP but glow. (doesn't really work well rn)")
public class GlowESP extends Module {
    @EventTarget
    public void onRender3D(Render3DEvent e) {
        List<EntityPlayer> players = new ArrayList<>();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.isDead) continue;
            if (!RenderUtils.isBBInFrustum(player.getEntityBoundingBox())) continue;
            if (player == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) continue;
            players.add(player);
        }

        if (!players.isEmpty()) {
            Demise.INSTANCE.getShaderManager().addToShadowQueue3D(() -> players.forEach(entity -> mc.getRenderManager().renderEntityStaticNoShadow(entity, mc.timer.partialTicks, false)));
        }
    }
}
