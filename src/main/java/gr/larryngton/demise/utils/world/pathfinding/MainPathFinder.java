package gr.larryngton.demise.utils.world.pathfinding;

import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.world.BlockUtils;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;

public class MainPathFinder implements InstanceAccess {
    public static ArrayList<Vec3> computePath(Vec3 topFrom, Vec3 to) {
        topFrom = raiseUntilPassable(topFrom);

        PathFinder pathfinder = new PathFinder(topFrom, to);
        pathfinder.compute();

        ArrayList<Vec3> path = new ArrayList<>();
        List<Vec3> pfPath = pathfinder.getPath();

        Vec3 lastLoc = null;
        Vec3 lastDashLoc = null;

        for (int i = 0; i < pfPath.size(); i++) {
            Vec3 pathElm = pfPath.get(i);
            boolean isFirstOrLast = (i == 0 || i == pfPath.size() - 1);

            if (isFirstOrLast) {
                if (lastLoc != null) {
                    path.add(lastLoc.addVector(0.5, 0.0, 0.5));
                }
                path.add(pathElm.addVector(0.5, 0.0, 0.5));
                lastDashLoc = pathElm;
            } else {
                if (!canContinueStraight(lastDashLoc, pathElm)) {
                    path.add(lastLoc.addVector(0.5, 0.0, 0.5));
                    lastDashLoc = lastLoc;
                }
            }
            lastLoc = pathElm;
        }

        return path;
    }

    private static Vec3 raiseUntilPassable(Vec3 from) {
        Vec3 p = from;
        while (!BlockUtils.canPassThrough(new BlockPos(p))) {
            p = p.addVector(0, 1, 0);
        }
        return p;
    }

    private static boolean canContinueStraight(Vec3 from, Vec3 to) {
        if (from == null || to == null) return false;

        if (from.squareDistanceTo(to) > 25) return false;

        int smallX = (int) Math.min(from.xCoord, to.xCoord);
        int bigX = (int) Math.max(from.xCoord, to.xCoord);
        int smallY = (int) Math.min(from.yCoord, to.yCoord);
        int bigY = (int) Math.max(from.yCoord, to.yCoord);
        int smallZ = (int) Math.min(from.zCoord, to.zCoord);
        int bigZ = (int) Math.max(from.zCoord, to.zCoord);

        for (int x = smallX; x <= bigX; x++) {
            for (int y = smallY; y <= bigY; y++) {
                for (int z = smallZ; z <= bigZ; z++) {
                    if (!BlockUtils.isValid(x, y, z, false)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
