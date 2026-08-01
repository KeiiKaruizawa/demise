package gr.larryngton.demise.features.anticheat.impl.movement.noslow;

import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.anticheat.impl.Check;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;

public class NoSlowB extends Check {
    private double buffer;
    private boolean lastUsingItem;

    public NoSlowB(PlayerData data) {
        super(data);
    }

    @Override
    public String getName() {
        return "NoSlow B";
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (isMove(packet) && ((S14PacketEntity) packet).getEntityId() == player.getEntityId()) {
            boolean usingItem = player.getItemInUseDuration() > 5;

            if (!usingItem && lastUsingItem && player.hurtTime == 0) {
                if (data.getSpeedXZ() > 0.2) {
                    buffer += 2.5;

                    if (buffer > 5) {
                        flag("Moving too fast on item release", "Max: 0.2 Current: " + data.getSpeedXZ());
                    }
                } else {
                    buffer = Math.max(0, buffer - 0.25);
                }
            }

            lastUsingItem = usingItem;
        }
    }
}
