package net.minecraft.util;

import com.google.common.collect.AbstractIterator;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import lombok.Getter;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class BlockPos extends Vec3i {
    public static final BlockPos ORIGIN = new BlockPos(0, 0, 0);
    private static final int NUM_X_BITS = 1 + MathHelper.calculateLogBaseTwo(MathHelper.roundUpToPowerOfTwo(30000000));
    private static final int NUM_Z_BITS = NUM_X_BITS;
    private static final int NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS;
    private static final int Y_SHIFT = NUM_Z_BITS;
    private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;
    private static final long X_MASK = (1L << NUM_X_BITS) - 1L;
    private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
    private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;

    public BlockPos(int x, int y, int z) {
        super(x, y, z);
    }

    public BlockPos(double x, double y, double z) {
        super(x, y, z);
    }

    public BlockPos(Entity source) {
        this(source.posX, source.posY, source.posZ);
    }

    public BlockPos(Vec3 source) {
        this(source.xCoord, source.yCoord, source.zCoord);
    }

    public BlockPos(Vec3i source) {
        this(source.getX(), source.getY(), source.getZ());
    }

    public BlockPos add(double x, double y, double z) {
        return x == 0.0D && y == 0.0D && z == 0.0D ? this : new BlockPos((double) this.getX() + x, (double) this.getY() + y, (double) this.getZ() + z);
    }

    public BlockPos add(int x, int y, int z) {
        return x == 0 && y == 0 && z == 0 ? this : new BlockPos(this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    public BlockPos add(Vec3i vec) {
        return vec.getX() == 0 && vec.getY() == 0 && vec.getZ() == 0 ? this : new BlockPos(this.getX() + vec.getX(), this.getY() + vec.getY(), this.getZ() + vec.getZ());
    }

    public BlockPos subtract(Vec3i vec) {
        return vec.getX() == 0 && vec.getY() == 0 && vec.getZ() == 0 ? this : new BlockPos(this.getX() - vec.getX(), this.getY() - vec.getY(), this.getZ() - vec.getZ());
    }

    public BlockPos up() {
        return new BlockPos(this.getX(), this.getY() + 1, this.getZ());
    }

    public BlockPos up(int offset) {
        return offset == 0 ? this : new BlockPos(this.getX(), this.getY() + offset, this.getZ());
    }

    public BlockPos down() {
        return new BlockPos(this.getX(), this.getY() - 1, this.getZ());
    }

    public BlockPos down(int offset) {
        return offset == 0 ? this : new BlockPos(this.getX(), this.getY() - offset, this.getZ());
    }

    public BlockPos north() {
        return new BlockPos(this.getX(), this.getY(), this.getZ() - 1);
    }

    public BlockPos north(int offset) {
        return offset == 0 ? this : new BlockPos(this.getX(), this.getY(), this.getZ() - offset);
    }

    public BlockPos south() {
        return new BlockPos(this.getX(), this.getY(), this.getZ() + 1);
    }

    public BlockPos south(int offset) {
        return offset == 0 ? this : new BlockPos(this.getX(), this.getY(), this.getZ() + offset);
    }

    public BlockPos west() {
        return new BlockPos(this.getX() - 1, this.getY(), this.getZ());
    }

    public BlockPos west(int offset) {
        return offset == 0 ? this : new BlockPos(this.getX() - offset, this.getY(), this.getZ());
    }

    public BlockPos east() {
        return new BlockPos(this.getX() + 1, this.getY(), this.getZ());
    }

    public BlockPos east(int offset) {
        return offset == 0 ? this : new BlockPos(this.getX() + offset, this.getY(), this.getZ());
    }

    public BlockPos offset(EnumFacing direction) {
        return new BlockPos(this.getX() + direction.getFrontOffsetX(), this.getY() + direction.getFrontOffsetY(), this.getZ() + direction.getFrontOffsetZ());
    }

    public BlockPos offset(EnumFacing facing, int n) {
        return n == 0 ? this : new BlockPos(this.getX() + facing.getFrontOffsetX() * n, this.getY() + facing.getFrontOffsetY() * n, this.getZ() + facing.getFrontOffsetZ() * n);
    }

    public BlockPos crossProduct(Vec3i vec) {
        return new BlockPos(this.getY() * vec.getZ() - this.getZ() * vec.getY(), this.getZ() * vec.getX() - this.getX() * vec.getZ(), this.getX() * vec.getY() - this.getY() * vec.getX());
    }

    public long toLong() {
        return ((long) this.getX() & X_MASK) << X_SHIFT | ((long) this.getY() & Y_MASK) << Y_SHIFT | ((long) this.getZ() & Z_MASK);
    }

    public static BlockPos fromLong(long serialized) {
        int i = (int) (serialized << 64 - X_SHIFT - NUM_X_BITS >> 64 - NUM_X_BITS);
        int j = (int) (serialized << 64 - Y_SHIFT - NUM_Y_BITS >> 64 - NUM_Y_BITS);
        int k = (int) (serialized << 64 - NUM_Z_BITS >> 64 - NUM_Z_BITS);
        return new BlockPos(i, j, k);
    }

    public static Iterable<BlockPos> getAllInBox(BlockPos from, BlockPos to) {
        final BlockPos blockpos = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
        final BlockPos blockpos1 = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
        return new Iterable<>() {
            public @NotNull Iterator<BlockPos> iterator() {
                return new AbstractIterator<>() {
                    private BlockPos lastReturned = null;

                    protected BlockPos computeNext() {
                        if (this.lastReturned == null) {
                            this.lastReturned = blockpos;
                            return this.lastReturned;
                        } else if (this.lastReturned.equals(blockpos1)) {
                            return this.endOfData();
                        } else {
                            int i = this.lastReturned.getX();
                            int j = this.lastReturned.getY();
                            int k = this.lastReturned.getZ();

                            if (i < blockpos1.getX()) {
                                ++i;
                            } else if (j < blockpos1.getY()) {
                                i = blockpos.getX();
                                ++j;
                            } else if (k < blockpos1.getZ()) {
                                i = blockpos.getX();
                                j = blockpos.getY();
                                ++k;
                            }

                            this.lastReturned = new BlockPos(i, j, k);
                            return this.lastReturned;
                        }
                    }
                };
            }
        };
    }

    public static Iterable<BlockPos.MutableBlockPos> getAllInBoxMutable(BlockPos from, BlockPos to) {
        final BlockPos blockpos = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
        final BlockPos blockpos1 = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
        return new Iterable<>() {
            public @NotNull Iterator<BlockPos.MutableBlockPos> iterator() {
                return new AbstractIterator<>() {
                    private BlockPos.MutableBlockPos theBlockPos = null;

                    protected BlockPos.MutableBlockPos computeNext() {
                        if (this.theBlockPos == null) {
                            this.theBlockPos = new BlockPos.MutableBlockPos(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                            return this.theBlockPos;
                        } else if (this.theBlockPos.equals(blockpos1)) {
                            return this.endOfData();
                        } else {
                            int i = this.theBlockPos.getX();
                            int j = this.theBlockPos.getY();
                            int k = this.theBlockPos.getZ();

                            if (i < blockpos1.getX()) {
                                ++i;
                            } else if (j < blockpos1.getY()) {
                                i = blockpos.getX();
                                ++j;
                            } else if (k < blockpos1.getZ()) {
                                i = blockpos.getX();
                                j = blockpos.getY();
                                ++k;
                            }

                            this.theBlockPos.x = i;
                            this.theBlockPos.y = j;
                            this.theBlockPos.z = k;
                            return this.theBlockPos;
                        }
                    }
                };
            }
        };
    }

    public Vec3 toVec3() {
        return new Vec3(this.getX(), this.getY(), this.getZ());
    }

    public double distanceToNearest(Vec3 vec) {
        Vec3 pos = RotationUtils.getBestHitVecCustom(new AxisAlignedBB(this.getCenter()).expand(0.5, 0.5, 0.5));
        double xDist = Math.abs(pos.xCoord - vec.xCoord);
        double yDist = Math.abs(pos.yCoord - vec.yCoord);
        double zDist = Math.abs(pos.zCoord - vec.zCoord);
        return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
    }

    public double distanceToNearestSq(Vec3 vec) {
        Vec3 pos = RotationUtils.getBestHitVecCustom(new AxisAlignedBB(this.getCenter()).expand(0.5, 0.5, 0.5));
        double xDist = Math.abs(pos.xCoord - vec.xCoord);
        double yDist = Math.abs(pos.yCoord - vec.yCoord);
        double zDist = Math.abs(pos.zCoord - vec.zCoord);
        return xDist * xDist + yDist * yDist + zDist * zDist;
    }

    public Vec3 getCenter() {
        return new Vec3(getX() + 0.5, getY() + 0.5, getZ() + 0.5);
    }

    @Getter
    public static final class MutableBlockPos extends BlockPos {
        private int x;
        private int y;
        private int z;

        public MutableBlockPos() {
            this(0, 0, 0);
        }

        public MutableBlockPos(int x_, int y_, int z_) {
            super(0, 0, 0);
            this.x = x_;
            this.y = y_;
            this.z = z_;
        }

        public BlockPos.MutableBlockPos set(int xIn, int yIn, int zIn) {
            this.x = xIn;
            this.y = yIn;
            this.z = zIn;
            return this;
        }
    }

    public boolean equalsBlockPos(BlockPos blockPos) {
        if (blockPos == null) {
            return false;
        } else {
            return this.getX() == blockPos.getX() && this.getY() == blockPos.getY() && this.getZ() == blockPos.getZ();
        }
    }
}
