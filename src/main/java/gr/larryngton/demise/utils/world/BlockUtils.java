package gr.larryngton.demise.utils.world;

import gr.larryngton.demise.utils.InstanceAccess;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BlockUtils implements InstanceAccess {
    private static final Set<Class<? extends Block>> NON_PASSABLE_BLOCK_CLASSES = new HashSet<>(
            Arrays.asList(
                    BlockSlab.class,
                    BlockStairs.class,
                    BlockCactus.class,
                    BlockChest.class,
                    BlockEnderChest.class,
                    BlockSkull.class,
                    BlockPane.class,
                    BlockFence.class,
                    BlockWall.class,
                    BlockGlass.class,
                    BlockPistonBase.class,
                    BlockPistonExtension.class,
                    BlockPistonMoving.class,
                    BlockStainedGlass.class,
                    BlockTrapDoor.class,
                    BlockEndPortalFrame.class,
                    BlockEndPortal.class,
                    BlockBed.class,
                    BlockWeb.class,
                    BlockBarrier.class,
                    BlockLadder.class,
                    BlockCarpet.class,
                    BlockFlowerPot.class,
                    BlockFire.class,
                    BlockWeb.class
            )
    );

    public static boolean isNotPassable(BlockPos pos) {
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        if (b == null) return true;
        if (b.isFullBlock()) return true;
        if (b.getMaterial() == Material.lava) return true;

        for (Class<? extends Block> cls : NON_PASSABLE_BLOCK_CLASSES) {
            if (cls.isInstance(b)) return true;
        }
        return false;
    }

    public static boolean canWalkOn(BlockPos pos) {
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        return !(b instanceof BlockFence) && !(b instanceof BlockWall) && !(b instanceof BlockButton) && !(b instanceof BlockWeb) && isNotPassable(pos.down());
    }

    public static boolean canPassThrough(BlockPos pos) {
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        Material m = b.getMaterial();
        return m == Material.air
                || m == Material.plants
                || m == Material.vine
                || b == Blocks.ladder
                || b == Blocks.water
                || b == Blocks.flowing_water
                || b == Blocks.wall_sign
                || b == Blocks.standing_sign
                || !b.isFullCube()
                || b instanceof BlockButton;
    }

    public static boolean isValid(Vec3 loc, boolean checkGround) {
        return isValid(loc.xCoord, loc.yCoord, loc.zCoord, checkGround);
    }

    public static boolean isValid(double x, double y, double z, boolean checkGround) {
        BlockPos top = new BlockPos(x, y, z);
        BlockPos above = new BlockPos(x, y + 1, z);
        BlockPos below = new BlockPos(x, y - 1, z);

        return !isNotPassable(top)
                && !isNotPassable(above)
                && (isNotPassable(below) || !checkGround)
                && canWalkOn(below);
    }
}