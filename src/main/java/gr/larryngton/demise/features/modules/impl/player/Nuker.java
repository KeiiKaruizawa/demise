package gr.larryngton.demise.features.modules.impl.player;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AngleEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.utils.player.ItemUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationManager;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockGlowstone;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

//todo: block whitelist / blacklist and merge with BedBreaker
@ModuleInfo(name = "Nuker", description = "Breaks certain blocks around you.")
public class Nuker extends Module {
    private final BoolValue sameY = new BoolValue("Same y", false, this);
    private final BoolValue respectRotation = new BoolValue("Respect rotation", false, this);
    private final ModeValue sortMode = new ModeValue("Sort mode", new String[]{"Relative hardness", "Reverse relative hardness", "Distance"}, "Relative hardness", this);
    private final BoolValue instant = new BoolValue("Instant break", false, this);
    private final RotationManager rotationManager = new RotationManager(this);
    private BlockPos targetBlock;
    private double startY;

    @Override
    public void onEnable() {
        startY = Math.floor(mc.thePlayer.posY);
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (targetBlock == null || mc.theWorld.getBlockState(targetBlock).getBlock() instanceof BlockAir || mc.thePlayer.getDistanceSqToCenter(targetBlock) > 16) {
            targetBlock = getNextBlock();
        }

        if (targetBlock != null) {
            if (!respectRotation.get() || PlayerUtils.blockRayTrace(targetBlock).typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                sendBlockBreak(targetBlock);
            }
        }
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (targetBlock != null) {
            rotationManager.setRotation(RotationUtils.getRotations(targetBlock));
        }
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
//        breaking = true;
    }

    private BlockPos getNextBlock() {
        List<Tuple<Block, BlockPos>> blockList = new ArrayList<>();

        for (double x = mc.thePlayer.posX - 4.5; x <= mc.thePlayer.posX + 4.5; x += 0.5) {
            for (double y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - 4.5; y <= mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + 4.5; y += 0.5) {
                for (double z = mc.thePlayer.posZ - 4.5; z <= mc.thePlayer.posZ + 4.5; z += 0.5) {
                    BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
                    Block block = mc.theWorld.getBlockState(pos).getBlock();

                    if (mc.thePlayer.getDistanceSqToCenter(pos) > 20.25 || (sameY.get() && pos.getY() != startY)) {
                        continue;
                    }

                    boolean shouldSkip = false;

                    // purpleprison antinuker george floyd bypass
                    if (mc.getCurrentServerData().serverIP.toLowerCase().contains("purple")) {
                        if (block instanceof BlockGlowstone) {
                            continue;
                        }

                        Vec3i dirVec = EnumFacing.fromAngle(RotationUtils.getRotations(pos)[0]).getOpposite().getDirectionVec();

                        for (int y1 = 0; y1 <= 1; y1++) {
                            BlockPos offsetPos = pos.add(dirVec.getX(), y1, dirVec.getZ());
                            Block offsetBlock = mc.theWorld.getBlockState(offsetPos).getBlock();

                            if (offsetBlock instanceof BlockGlowstone || offsetBlock.getBlockHardness(mc.theWorld, offsetPos) == -1) {
                                shouldSkip = true;
                                break;
                            }
                        }
                    }

                    if (!ItemUtils.blacklistedBlocks.contains(block) && block.getBlockHardness(mc.theWorld, pos) >= 0 && !shouldSkip) {
                        blockList.add(new Tuple<>(block, pos));
                    }
                }
            }
        }

        blockList.sort(Comparator.comparingDouble(block -> block.getSecond().distanceSq(mc.thePlayer.getPositionVector().toVec3i())));

        if (sortMode.is("Relative hardness")) {
            blockList.sort(Comparator.comparingDouble(block -> -block.getFirst().getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, block.getSecond())));
        } else if (sortMode.is("Reverse relative hardness")) {
            blockList.sort(Comparator.comparingDouble(block -> block.getFirst().getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, block.getSecond())));
        }

        return blockList.get(0).getSecond();
    }
}
