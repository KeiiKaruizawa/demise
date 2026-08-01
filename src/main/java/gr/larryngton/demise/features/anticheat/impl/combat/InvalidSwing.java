package gr.larryngton.demise.features.anticheat.impl.combat;

import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.anticheat.impl.Check;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0BPacketAnimation;

public class InvalidSwing extends Check {
    private double buffer;

    public InvalidSwing(PlayerData data) {
        super(data);
    }

    @Override
    public String getName() {
        return "Invalid swing";
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (packet instanceof S0BPacketAnimation s0b && s0b.getAnimationType() == 0 && s0b.getEntityID() == player.getEntityId()) {
            if (player.getItemInUseDuration() > 2) {
                buffer++;

                if (buffer > 2) {
                    flag("Swinging on item use");
                    buffer = 0;
                }
            } else {
                buffer = Math.max(buffer - 0.2, 0);
            }
        }
    }
}
