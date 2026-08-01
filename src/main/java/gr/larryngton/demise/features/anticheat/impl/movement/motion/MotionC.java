package gr.larryngton.demise.features.anticheat.impl.movement.motion;

import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.anticheat.impl.Check;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.potion.Potion;

public class MotionC extends Check {
    private double buffer;

    public MotionC(PlayerData data) {
        super(data);
    }

    @Override
    public String getName() {
        return "Motion C";
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (isMove(packet) && ((S14PacketEntity) packet).getEntityId() == player.getEntityId() && data.getTicksSinceLastVelocity() > 40 && data.getTimeSinceLastS14() < 150) {
            if (data.getSpeedXZ() - data.getLastSpeedXZ() < 10) {
                double speedMulti = 1;

                if (player.isPotionActive(Potion.moveSpeed)) {
                    int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
                    speedMulti = 1.0 * (amplifier + 1);
                }

                speedMulti++;
                if (data.getSpeedXZ() - data.getLastSpeedXZ() > 0.32 * speedMulti) {
                    buffer++;

                    if (buffer > 3) {
                        flag("Invalid acceleration", "Motion delta: " + (data.getSpeedXZ() - data.getLastSpeedXZ()) + ", max: " + (0.32 * speedMulti));
                    }
                } else if (data.getSpeedXZ() - data.getLastSpeedXZ() > 0) {
                    buffer = Math.max(buffer - 0.8, 0);
                }
            }
        }
    }
}
