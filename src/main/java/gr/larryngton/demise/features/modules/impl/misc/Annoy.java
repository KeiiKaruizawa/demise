package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.features.values.impl.TextValue;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.world.pathfinding.MainPathFinder;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.ArrayList;

@ModuleInfo(name = "Annoy", description = "Follows the target player around.")
public class Annoy extends Module {
    private final TextValue targetPlayer = new TextValue("Target", "eonian", this);
    private final ModeValue targetMode = new ModeValue("Target pos mode", new String[]{"On player", "Front", "Behind"}, "On top", this);
    private final SliderValue range = new SliderValue("Range", 3, 1, 6, 0.1f, this, () -> !targetMode.is("On player"));
    private final BoolValue renderPath = new BoolValue("Render path", false, this);

    private ArrayList<Vec3> path;
    private EntityPlayer target;

    @EventTarget
    public void onDisable() {
        path.clear();
        target = null;
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPost()) return;

        target = getTarget();

        if (target == null) return;

        NetHandlerPlayClient h = mc.getNetHandler();
        float pingSelf = h.getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();
        float pingTarget = h.getPlayerInfo(target.getUniqueID()).getResponseTime();
        float pingTotal = pingSelf + pingTarget;

        float totalPingToTicks = (pingTotal / 50F) + 5;

        Vec3 targetPos = target.getPositionVector().add(target.getMoveDeltaVector()).multiply(totalPingToTicks);
        targetPos.yCoord = target.posY;

        if (!targetMode.is("On player")) {
            float rotYaw = target.rotationYaw + (target.rotationYaw - target.prevRotationYaw) * totalPingToTicks;
            float dir = (float) Math.toRadians(targetMode.is("Front") ? rotYaw : rotYaw - 180);
            targetPos = new Vec3(
                    -MathHelper.sin(dir) * range.get() + targetPos.xCoord,
                    targetPos.yCoord,
                    MathHelper.cos(dir) * range.get() + targetPos.zCoord
            );
        }

        Vec3 finalTargetPos = targetPos;
        new Thread(() -> {
            path = MainPathFinder.computePath(mc.thePlayer.getPositionVector(), finalTargetPos);
            Vec3 lastCalcVec = path.get(path.size() - 1);

            path.set(path.size() - 1, finalTargetPos);

            for (Vec3 vec : path) {
                sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(vec.xCoord, vec.yCoord, vec.zCoord, false));
            }

            e.setX(lastCalcVec.xCoord);
            e.setY(lastCalcVec.yCoord);
            e.setZ(lastCalcVec.zCoord);
        }).start();

        // so you don't fucking die
        e.setOnGround(false);
    }

    private EntityPlayer getTarget() {
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.getName().equalsIgnoreCase(targetPlayer.get())) {
                return player;
            }
        }

        return null;
    }

    @EventTarget
    public void onRender3DEvent(Render3DEvent event) {
        if (!renderPath.get() || path == null || target == null) return;

        Vec3 lastPoint = null;

        for (Vec3 point : path) {
            if (lastPoint != null) {
                RenderUtils.draw3DLine(lastPoint.xCoord, lastPoint.yCoord + 0.01, lastPoint.zCoord, point.xCoord, point.yCoord + 0.01, point.zCoord, getModule(Interface.class).color(), 1);
            }
            lastPoint = point;
        }
    }
}