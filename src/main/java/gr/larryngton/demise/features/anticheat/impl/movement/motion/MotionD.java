package gr.larryngton.demise.features.anticheat.impl.movement.motion;

import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.anticheat.impl.Check;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;

public class MotionD extends Check {
    public MotionD(PlayerData data) {
        super(data);
    }

    @Override
    public String getName() {
        return "Motion D";
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (isMove(packet) && ((S14PacketEntity) packet).getEntityId() == player.getEntityId() && data.getTicksSinceLastVelocity() > 40 && data.getTimeSinceLastS14() < 150) {
            double predictedX = data.getLastDeltaX() * 0.9100000262260437;
            double predictedZ = data.getLastDeltaZ() * 0.9100000262260437;
            double differenceX = data.getDeltaX() - predictedX;
            double differenceZ = data.getDeltaZ() - predictedZ;
            double difference = Math.hypot(differenceX, differenceZ);
            difference /= 1.3;
            difference -= 0.026;

            if (difference > 0.0075 && data.getSpeedXZ() > 0.25 && !data.isRoughlyGround() && !data.isNearBlock()) {
                flag("Invalid strafing", "Motion: " + (difference - 0.0075));
            }
        }
    }
}
