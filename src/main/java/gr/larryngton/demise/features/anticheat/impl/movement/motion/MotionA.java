package gr.larryngton.demise.features.anticheat.impl.movement.motion;

import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.anticheat.impl.Check;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;

public class MotionA extends Check {
    private double buffer;

    public MotionA(PlayerData data) {
        super(data);
    }

    @Override
    public String getName() {
        return "Motion A";
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (isMove(packet) && ((S14PacketEntity) packet).getEntityId() == player.getEntityId() && data.getTicksSinceLastVelocity() > 40 && data.getTimeSinceLastS14() < 150) {
            if (data.getSpeedXZ() > 0.8) {
                buffer += data.getSpeedXZ() - 0.8;

                if (buffer > 1) {
                    flag("Moving too fast", "Motion: " + data.getSpeedXZ());
                    buffer = 0;
                }
            } else {
                buffer *= 0.9;
            }
        }
    }
}
