package gr.larryngton.demise.utils.world.pathfinding;

import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.world.BlockUtils;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.*;

public class PathFinder implements InstanceAccess {
    private final Vec3 startVec3Path;
    private final Vec3 endVec3Path;
    private final List<Vec3> path = new ArrayList<>();

    private final Map<BlockPos, PathHub> allHubs = new HashMap<>();
    private final PriorityQueue<PathHub> openQueue;

    private static final Vec3[] DIRECTIONS = new Vec3[]{
            new Vec3(1, 0, 0), new Vec3(-1, 0, 0),
            new Vec3(0, 0, 1), new Vec3(0, 0, -1)
    };

    public PathFinder(Vec3 startVec3Path, Vec3 endVec3Path) {
        this.startVec3Path = startVec3Path.floor();
        this.endVec3Path = endVec3Path.floor();

        this.openQueue = new PriorityQueue<>(Comparator.comparingDouble(h -> h.getSqDist() + h.getMaxCost()));
    }

    public List<Vec3> getPath() {
        return Collections.unmodifiableList(path);
    }

    public void compute() {
        compute(1000, 10);
    }

    public void compute(int loops, int depth) {
        path.clear();
        openQueue.clear();
        allHubs.clear();

        ArrayList<Vec3> initPath = new ArrayList<>();
        initPath.add(startVec3Path);

        PathHub startHub = new PathHub(startVec3Path, null, initPath,
                startVec3Path.squareDistanceTo(endVec3Path), 0.0, 0.0);

        addOrUpdateHub(startHub);

        int iterations = 0;
        loop:
        while (!openQueue.isEmpty() && iterations < loops) {
            int processedThisLayer = 0;

            List<PathHub> toProcess = new ArrayList<>();
            while (!openQueue.isEmpty() && processedThisLayer < depth) {
                PathHub hub = openQueue.poll();
                if (hub == null) break;
                toProcess.add(hub);
                processedThisLayer++;
            }

            for (PathHub current : toProcess) {
                allHubs.put(new BlockPos(current.getLoc()), current);
                for (Vec3 d : DIRECTIONS) {
                    Vec3 loc = current.getLoc().add(d).floor();
                    if (tryPutHub(current, loc, 0.0)) break loop;
                }

                Vec3 up = current.getLoc().addVector(0, 1, 0).floor();
                if (BlockUtils.isValid(up, false) && tryPutHub(current, up, 0.0)) break loop;

                Vec3 down = current.getLoc().addVector(0, -1, 0).floor();
                if (BlockUtils.isValid(down, false) && tryPutHub(current, down, 0.0)) break loop;
            }
            iterations++;
        }

        PathHub best = allHubs.values().stream()
                .min(Comparator.comparingDouble(h -> h.getSqDist() + h.getMaxCost()))
                .orElse(null);

        path.clear();
        if (best != null) {
            path.addAll(best.getPathway());
        }
    }

    private boolean tryPutHub(PathHub parent, Vec3 loc, double cost) {
        if (!BlockUtils.isValid(loc, false)) return false;
        BlockPos posKey = new BlockPos(loc);

        PathHub existing = allHubs.get(posKey);
        double totalCost = cost + (parent != null ? parent.getMaxCost() : 0.0);

        if ((loc.xCoord == endVec3Path.xCoord && loc.yCoord == endVec3Path.yCoord && loc.zCoord == endVec3Path.zCoord)
                || loc.squareDistanceTo(endVec3Path) <= 1) {
            this.path.clear();
            this.path.addAll(parent.getPathway());
            this.path.add(loc);
            return true;
        }

        if (existing == null) {
            ArrayList<Vec3> newPath = new ArrayList<>(parent.getPathway());
            newPath.add(loc);
            PathHub newHub = new PathHub(loc, parent, newPath, loc.squareDistanceTo(endVec3Path), cost, totalCost);
            addOrUpdateHub(newHub);
        } else if (existing.getCurrentCost() > cost) {
            ArrayList<Vec3> newPath = new ArrayList<>(parent.getPathway());
            newPath.add(loc);
            existing.setParentPathHub(parent);
            existing.setPathway(newPath);
            existing.setSqDist(loc.squareDistanceTo(endVec3Path));
            existing.setCurrentCost(cost);
            existing.setMaxCost(totalCost);
            openQueue.remove(existing);
            openQueue.offer(existing);
        }

        return false;
    }

    private void addOrUpdateHub(PathHub hub) {
        BlockPos key = new BlockPos(hub.getLoc());
        PathHub old = allHubs.get(key);
        if (old == null) {
            allHubs.put(key, hub);
            openQueue.offer(hub);
        } else if (hub.getCurrentCost() < old.getCurrentCost()) {
            allHubs.put(key, hub);
            openQueue.remove(old);
            openQueue.offer(hub);
        }
    }
}