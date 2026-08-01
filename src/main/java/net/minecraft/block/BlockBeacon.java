package net.minecraft.block;

import gr.larryngton.demise.utils.math.ObjectPool;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.concurrent.CompletableFuture;

public class BlockBeacon extends BlockContainer {
    public BlockBeacon() {
        super(Material.glass, MapColor.diamondColor);
        this.setHardness(3.0F);
        this.setCreativeTab(CreativeTabs.tabMisc);
    }

    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBeacon();
    }

    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityBeacon) {
                playerIn.displayGUIChest((TileEntityBeacon) tileentity);
                playerIn.triggerAchievement(StatList.field_181730_N);
            }

        }
        return true;
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean isFullCube() {
        return false;
    }

    public int getRenderType() {
        return 3;
    }

    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        if (stack.hasDisplayName()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);

            if (tileentity instanceof TileEntityBeacon) {
                ((TileEntityBeacon) tileentity).setName(stack.getDisplayName());
            }
        }
    }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        TileEntity tileentity = worldIn.getTileEntity(pos);

        if (tileentity instanceof TileEntityBeacon) {
            ((TileEntityBeacon) tileentity).updateBeacon();
            worldIn.addBlockEvent(pos, this, 1, 0);
        }
    }

    public EnumWorldBlockLayer getBlockLayer() {
        return EnumWorldBlockLayer.CUTOUT;
    }

    public static void updateColorAsync(final World worldIn, final BlockPos glassPos) {
        CompletableFuture.runAsync(() -> {
            Chunk chunk = worldIn.getChunkFromBlockCoords(glassPos);

            var mutable = ObjectPool.MUTABLE_BLOCK_POS.get();
            for (int i = glassPos.getY() - 1; i >= 0; --i) {
                mutable.set(glassPos.getX(), i, glassPos.getZ());

                if (!chunk.canSeeSky(mutable)) {
                    break;
                }

                if (worldIn.getBlockState(mutable).getBlock() != Blocks.beacon) {
                    continue;
                }

                var copied = new BlockPos(mutable);
                ObjectPool.MUTABLE_BLOCK_POS.offer(mutable);

                ((WorldServer) worldIn).addScheduledTask(() -> {
                    TileEntity tileentity = worldIn.getTileEntity(copied);

                    if (tileentity instanceof TileEntityBeacon) {
                        ((TileEntityBeacon) tileentity).updateBeacon();
                        worldIn.addBlockEvent(copied, Blocks.beacon, 1, 0);
                    }
                });
                break;
            }
        });
    }
}
