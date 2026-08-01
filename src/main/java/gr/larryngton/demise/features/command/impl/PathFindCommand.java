package gr.larryngton.demise.features.command.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.command.CommandExecutionException;
import gr.larryngton.demise.features.modules.impl.misc.PathFinderModule;
import gr.larryngton.demise.utils.misc.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PathFindCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"pathfind"};
    }

    @Override
    public void execute(String[] arguments) throws CommandExecutionException {
        if (arguments.length < 1) {
            throw new CommandExecutionException(getUsage());
        }

        PathFinderModule pathFinderModule = Demise.INSTANCE.getModuleManager().getModule(PathFinderModule.class);
        Minecraft mc = Minecraft.getMinecraft();

        if (arguments.length > 3 && arguments[1].equalsIgnoreCase("pos")) {
            if (arguments.length != 5) {
                throw new CommandExecutionException(getUsage());
            }

            double x = Double.parseDouble(arguments[2]);
            double y = Double.parseDouble(arguments[3]);
            double z = Double.parseDouble(arguments[4]);
            Vec3 vec = new Vec3(x, y, z);

            pathFinderModule.targetPos = vec;
            ChatUtils.sendMessageClient("Set target position to " + vec + ".");
        } else {
            if (arguments.length != 3) {
                throw new CommandExecutionException(getUsage());
            }

            if (arguments[1].equalsIgnoreCase("block")) {
                new Thread(() -> {
                    int chunkRadius = 16 * mc.gameSettings.renderDistanceChunks;

                    List<BlockPos> validBlocks = new ArrayList<>();

                    for (int x = (int) mc.thePlayer.posX - chunkRadius; x < (int) mc.thePlayer.posX + chunkRadius; x++) {
                        for (int y = 0; y < 255; y++) {
                            for (int z = (int) mc.thePlayer.posZ - chunkRadius; z < (int) mc.thePlayer.posZ + chunkRadius; z++) {
                                BlockPos pos = new BlockPos(x, y, z);
                                Block block = mc.theWorld.getBlockState(pos).getBlock();

                                if (block.getUnlocalizedName().replace("tile.", "").equalsIgnoreCase(arguments[2])) {
                                    validBlocks.add(pos);
                                }
                            }
                        }
                    }

                    if (!validBlocks.isEmpty()) {
                        validBlocks.sort(Comparator.comparingDouble(pos -> mc.thePlayer.getDistanceSqToCenter(pos)));
                        Vec3 bestPos = validBlocks.get(0).toVec3();

                        pathFinderModule.targetPos = bestPos;
                        ChatUtils.sendMessageClient("Set target position to " + bestPos + ".");
                    } else {
                        ChatUtils.sendMessageClient("Couldn't find given block.");
                    }
                }).start();
            } else if (arguments[1].equalsIgnoreCase("player")) {
                for (EntityPlayer player : mc.theWorld.playerEntities) {
                    if (player.getName().equalsIgnoreCase(arguments[2])) {
                        pathFinderModule.targetPos = player.getPositionVector();
                        ChatUtils.sendMessageClient("Set target position to " + player.getPositionVector() + ".");
                        return;
                    }
                }

                ChatUtils.sendMessageClient("Couldn't find given player.");
            }
        }
    }

    @Override
    public String getUsage() {
        return ".pathfind pos/player/block <x y z>/<player>/<unlocalised block name>";
    }
}
