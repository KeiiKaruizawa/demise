package gr.larryngton.demise.features.modules.impl.legit;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ColorValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "LagCompAbuse", description = "Abuses lag compensation for increased reach.")
public class LagCompAbuse extends Module {
    private final SliderValue checkingReach = new SliderValue("Position check reach", 3, 1, 8, 0.1F, this);
    private final SliderValue maxPrevPositions = new SliderValue("Max prev positions", 5, 0, 20, 1, this);
    private final SliderValue maxPredictedPositions = new SliderValue("Max predicted positions", 1, 0, 20, 1, this);
    private final ColorValue boxColor = new ColorValue("Box color", new Color(0, 0, 0, 40), this);

    private final List<AxisAlignedBB> prevBBs = new ArrayList<>();
    private final List<AxisAlignedBB> predictedBBs = new ArrayList<>();
    private final List<AxisAlignedBB> bbs = new ArrayList<>();
    private EntityLivingBase target;

    @EventTarget
    public void onAttack(AttackEvent e) {
        target = (EntityLivingBase) e.getTargetEntity();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (target == null) {
            return;
        }

        prevBBs.add(target.getHitbox());

        double deltaX = (target.posX - target.lastTickPosX) * 2;
        double deltaZ = (target.posZ - target.lastTickPosZ) * 2;

        predictedBBs.clear();
        for (int i = 0; predictedBBs.size() < maxPredictedPositions.getInt(); i++) {
            predictedBBs.add(target.getHitbox().offset(deltaX * i, 0, deltaZ * i));
        }

        while (prevBBs.size() > maxPrevPositions.getInt()) {
            prevBBs.remove(0);
        }

        bbs.clear();
        bbs.addAll(predictedBBs);
        bbs.addAll(prevBBs);
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (target != null && !bbs.isEmpty()) {
            for (AxisAlignedBB bb : bbs) {
                AxisAlignedBB bb1 = bb.offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);
                RenderUtils.drawAxisAlignedBB(bb1, true, false, boxColor.get().getRGB());
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (target == null) {
            return;
        }

        if (e.getState() == PacketEvent.State.OUTGOING) {
            if (e.getPacket() instanceof C0APacketAnimation && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
                if (PlayerUtils.getDistanceToEntityBox(target) < 4.5) {
                    for (AxisAlignedBB bb : bbs) {
                        if (PlayerUtils.getDistToTargetFromMouseOver(mc.thePlayer.getPositionEyes(1), mc.thePlayer.getLook(1), target, bb) < checkingReach.get()) {
                            e.setCancelled(true);

                            if (ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                                sendPacketNoEvent(new C0APacketAnimation());
                                mc.playerController.attackEntity(mc.thePlayer, target);
                            } else {
                                mc.playerController.attackEntity(mc.thePlayer, target);
                                sendPacketNoEvent(new C0APacketAnimation());
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}
