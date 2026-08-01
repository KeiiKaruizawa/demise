package gr.larryngton.demise.features.modules.impl.player;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AngleEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationManager;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "BedBreaker", description = "Automatically breaks beds around you.")
public class BedBreaker extends Module {
    private final BoolValue breakSurrounding = new BoolValue("Break surrounding", false, this);
    private final BoolValue instant = new BoolValue("Instant break", false, this);
    private final RotationManager rotationManager = new RotationManager(this);

    private BlockPos bedPos;
    private BlockPos cachedBedPos;
    private BlockPos weakestSurrounding;
    public static BlockPos targetPos;
    public static boolean breaking;

    @Override
    public void onDisable() {
        bedPos = cachedBedPos = weakestSurrounding = targetPos = null;
        breaking = false;
        mc.playerController.resetBlockRemoving();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (searchCriteria()) {
            bedPos = getBedPos();
            if (breakSurrounding.get() && bedPos != null) {
                weakestSurrounding = getWeakestBlockNearBed();
            } else {
                weakestSurrounding = null;
            }
        }

        if (bedPos != null) {
            targetPos = weakestSurrounding == null ? bedPos : weakestSurrounding;

            if (PlayerUtils.blockRayTrace(targetPos).typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                sendBlockBreak(targetPos);
            }
        } else {
            targetPos = null;
            breaking = false;

            if (mc.playerController.currentBlock == cachedBedPos) {
                mc.playerController.resetBlockRemoving();
            }
        }
    }

    private boolean searchCriteria() {
        boolean a = bedPos == null ||
                !(mc.theWorld.getBlockState(bedPos).getBlock() instanceof BlockBed) ||
                MathHelper.sqrt_double(mc.thePlayer.getDistanceSqToCenter(bedPos)) > 4.5;

        if (breakSurrounding.get()) {
            return a || (weakestSurrounding == null ||
                    mc.theWorld.getBlockState(weakestSurrounding).getBlock() instanceof BlockAir ||
                    MathHelper.sqrt_double(mc.thePlayer.getDistanceSqToCenter(weakestSurrounding)) > 4.5);
        }

        return a;
    }

    private void sendBlockBreak(BlockPos pos) {
        if (instant.get()) {
            sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, mc.objectMouseOver.sideHit));
            sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, mc.objectMouseOver.sideHit));
            mc.playerController.onPlayerDestroyBlock(pos);
        } else {
            mc.playerController.onPlayerDamageBlock(pos, mc.objectMouseOver.sideHit);
        }
        sendPacket(new C0APacketAnimation());
        breaking = true;
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (targetPos != null) {
            rotationManager.setRotation(RotationUtils.getRotations(targetPos));
        }
    }

    private BlockPos getBedPos() {
        BlockPos bedPos = null;
        double nearestPos = Double.MAX_VALUE;
        for (double x = mc.thePlayer.posX - 4.5; x <= mc.thePlayer.posX + 4.5; x++) {
            for (double y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - 4.5; y <= mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + 4.5; y++) {
                for (double z = mc.thePlayer.posZ - 4.5; z <= mc.thePlayer.posZ + 4.5; z++) {
                    BlockPos pos = new BlockPos((int) x, (int) y, (int) z);

                    if (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed && pos.distanceSq(mc.thePlayer.getPositionVector().toVec3i()) < nearestPos) {
                        bedPos = pos;
                        cachedBedPos = pos;
                        nearestPos = pos.distanceSq(mc.thePlayer.getPositionVector().toVec3i());
                    }
                }
            }
        }

        return bedPos;
    }

    private BlockPos getWeakestBlockNearBed() {
        List<Tuple<BlockPos, Block>> surroundingBlocks = new ArrayList<>();

        for (double x = bedPos.getX() - 1; x <= bedPos.getX() + 1; x++) {
            // i don't fucking know why but bedPos.getY() + 1 is still bedPos
            for (double y = bedPos.getY(); y <= bedPos.getY() + 2; y++) {
                for (double z = bedPos.getZ() - 1; z <= bedPos.getZ() + 1; z++) {
                    int dx = (int) Math.abs(x - bedPos.getX());
                    int dy = (int) Math.abs(y - bedPos.getY());
                    int dz = (int) Math.abs(z - bedPos.getZ());

                    // skip diagonal blocks to the bed
                    if (dx + dy + dz > 1) {
                        continue;
                    }

                    BlockPos blockPos = new BlockPos((int) x, (int) y, (int) z);
                    Block block = mc.theWorld.getBlockState(blockPos).getBlock();

                    // can break bed, return null so no excess blocks get broken
                    if (block instanceof BlockAir) {
                        return null;
                    }

                    // don't want to break the other part of the bed lol
                    if (block instanceof BlockBed) {
                        continue;
                    }

                    surroundingBlocks.add(new Tuple<>(blockPos, block));
                }
            }
        }

        surroundingBlocks.sort(Comparator.comparingDouble(tuple -> -tuple.getSecond().getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, tuple.getFirst())));
        return surroundingBlocks.get(0).getFirst();
    }
}