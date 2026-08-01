package gr.larryngton.demise.features.anticheat.impl.movement.motion;

import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.anticheat.impl.Check;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.util.BlockPos;

public class MotionB extends Check {
    private double buffer;

    public MotionB(PlayerData data) {
        super(data);
    }

    @Override
    public String getName() {
        return "Motion B";
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (isMove(packet) && ((S14PacketEntity) packet).getEntityId() == player.getEntityId() && data.getTicksSinceLastVelocity() > 40 && data.getTimeSinceLastS14() < 150) {
            Block blockAbove = mc.theWorld.getBlockState(new BlockPos(data.getPosition().add(0, player.getEyeHeight() + 1, 0))).getBlock();

            if (!data.isRoughlyGround() && blockAbove instanceof BlockAir && data.getDeltaY() < 0 && data.getLastDeltaY() < 0) {
                double expected = data.getLastDeltaY() - 0.08;
                expected *= 0.98;

                if (Math.abs(data.getDeltaY() - expected) > 0.1) {
                    buffer += 1;

                    if (buffer > 5) {
                        flag("Invalid gravity", "Expected " + expected + ", Received " + data.getDeltaY());
                        buffer = 0;
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.8);
                }
            }
        }
    }
}
