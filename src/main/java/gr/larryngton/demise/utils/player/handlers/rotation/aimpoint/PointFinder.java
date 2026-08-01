package gr.larryngton.demise.utils.player.handlers.rotation.aimpoint;

import gr.larryngton.demise.utils.InstanceAccess;
import lombok.experimental.UtilityClass;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PointFinder implements InstanceAccess {
    public List<Vec3> hitboxPoints = new ArrayList<>();
    public List<Vec3> invalidHitboxPoints = new ArrayList<>();
    public List<Vec3> allHitboxPoints = new ArrayList<>();

    public static final int pointCount = 2048;
    private double lastCbrt;
    private double lastPointCount;

    public void findPoints(AxisAlignedBB bb) {
        findPoints(bb, pointCount);
    }

    public void findPoints(AxisAlignedBB bb, int pointCount) {
        hitboxPoints.clear();
        invalidHitboxPoints.clear();
        allHitboxPoints.clear();

        double cbrt = pointCount == lastPointCount ? lastCbrt : Math.cbrt(pointCount);
        lastPointCount = pointCount;
        lastCbrt = cbrt;

        double minX = bb.minX, minY = bb.minY, minZ = bb.minZ;
        double maxX = bb.maxX, maxY = bb.maxY, maxZ = bb.maxZ;

        double width = maxX - minX;
        double height = maxY - minY;
        double depth = maxZ - minZ;

        double total = width + height + depth;
        int stepsX = Math.max(2, (int) (cbrt * (width / total) * 3));
        int stepsY = Math.max(2, (int) (cbrt * (height / total) * 3));
        int stepsZ = Math.max(2, (int) (cbrt * (depth / total) * 3));

        double stepX = width / (stepsX - 1);
        double stepY = height / (stepsY - 1);
        double stepZ = depth / (stepsZ - 1);

        for (int i = 0; i < stepsX; i++) {
            for (int j = 0; j < stepsY; j++) {
                double x = minX + stepX * i;
                double y = minY + stepY * j;

                Vec3 p = new Vec3(x, y, minZ);
                hitboxPoints.add(p);
                allHitboxPoints.add(p);

                Vec3 p1 = new Vec3(x, y, maxZ);
                hitboxPoints.add(p1);
                allHitboxPoints.add(p1);
            }
        }

        for (int i = 0; i < stepsX; i++) {
            for (int k = 0; k < stepsZ; k++) {
                double x = minX + stepX * i;
                double z = minZ + stepZ * k;

                Vec3 p = new Vec3(x, minY, z);
                hitboxPoints.add(p);
                allHitboxPoints.add(p);

                Vec3 p1 = new Vec3(x, maxY, z);
                hitboxPoints.add(p1);
                allHitboxPoints.add(p1);
            }
        }

        for (int j = 0; j < stepsY; j++) {
            for (int k = 0; k < stepsZ; k++) {
                double y = minY + stepY * j;
                double z = minZ + stepZ * k;

                Vec3 p = new Vec3(minX, y, z);
                hitboxPoints.add(p);
                allHitboxPoints.add(p);

                Vec3 p1 = new Vec3(maxX, y, z);
                hitboxPoints.add(p1);
                allHitboxPoints.add(p1);
            }
        }
    }
}
