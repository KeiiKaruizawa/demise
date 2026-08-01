package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AngleEvent;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.Render3DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationManager;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.world.pathfinding.MainPathFinder;
import net.minecraft.util.Vec3;

import java.util.ArrayList;

@ModuleInfo(name = "PathFinder", description = "Use the pathfind command to set the target position.")
public class PathFinderModule extends Module {
    private final BoolValue renderPath = new BoolValue("Render path", false, this);
    private final BoolValue onlyRenderPath = new BoolValue("Only render path", false, this, renderPath::get);
    private final BoolValue autoDisable = new BoolValue("Auto disable", false, this);
    private final RotationManager rotationManager = new RotationManager(this);

    public Vec3 targetPos = Vec3.ZERO;
    public ArrayList<Vec3> path;
    private final TimerUtil updateTimer = new TimerUtil();
    private Vec3 currentVec = Vec3.ZERO;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (updateTimer.hasTimeElapsed(250)) {
            Vec3 finalTargetPos = targetPos;
            new Thread(() -> path = MainPathFinder.computePath(PlayerUtils.simulatePlayer(1).position, finalTargetPos)).start();
            updateTimer.reset();
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (!onlyRenderPath.get()) {
            if (mc.thePlayer.isCollidedHorizontally) {
                e.setJumping(true);
            }

            if (RotationUtils.getAngleDifference(RotationHandler.currentRotation[0], RotationUtils.getRotations(currentVec)[0]) < 45) {
                e.setForward(1);
            }
        }
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (path == null || path.isEmpty()) {
            return;
        }

        if (autoDisable.get() && (mc.thePlayer.getDistance(targetPos) < 1 || (mc.thePlayer.getDistance(targetPos) < 5 && path.size() < 3))) {
            ChatUtils.sendMessageClient("Successfully reached the target destination.");
            setEnabled(false);
        }

        if (onlyRenderPath.get()) {
            return;
        }

        Vec3 targetVec = Vec3.ZERO;
        ArrayList<Vec3> pointsToRemove = new ArrayList<>();

        for (Vec3 vec : path) {
            if (mc.thePlayer.getDistance(vec) > 1) {
                //if (RotationUtils.getAngleDifference(RotationUtils.getRotations(currentVec)[0], RotationUtils.getRotations(vec)[0]) < 90) {
                targetVec = vec;
                break;
            }

            pointsToRemove.add(vec);
        }

        currentVec = targetVec;
        path.removeAll(pointsToRemove);
        rotationManager.setRotation(new float[]{RotationUtils.getRotations(targetVec)[0], 25});
    }

    @EventTarget
    public void onRender3DEvent(Render3DEvent e) {
        if (!renderPath.get() || path == null) return;

        Vec3 lastPoint = null;

        for (Vec3 point : path) {
            if (lastPoint != null) {
                RenderUtils.draw3DLine(lastPoint.xCoord, lastPoint.yCoord + 0.01, lastPoint.zCoord, point.xCoord, point.yCoord + 0.01, point.zCoord, getModule(Interface.class).color(), 1);
            }
            lastPoint = point;
        }
    }
}
