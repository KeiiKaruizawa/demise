package gr.larryngton.demise.features.anticheat.impl.movement.noslow;

import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.anticheat.impl.Check;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;

public class NoSlowA extends Check {
    private double buffer;

    public NoSlowA(PlayerData data) {
        super(data);
    }

    @Override
    public String getName() {
        return "NoSlow A";
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (isMove(packet) && ((S14PacketEntity) packet).getEntityId() == player.getEntityId()) {
            if (player.isUsingItem()) {
                if (player.isSprinting()) {
                    buffer++;

                    if (buffer > 5) {
                        flag("Sprinting while using an item");
                        buffer = 0;
                    }
                } else {
                    buffer--;
                }
            }
        }
    }
}
