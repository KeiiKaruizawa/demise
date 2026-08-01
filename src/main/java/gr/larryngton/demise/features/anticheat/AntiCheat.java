package gr.larryngton.demise.features.anticheat;

import lombok.Getter;
import net.minecraft.network.Packet;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class AntiCheat {
    private final Map<UUID, PlayerData> playerMap = new ConcurrentHashMap<>();

    public AntiCheat() {
        CheckManager.setup();
    }

    public void onPacket(Packet<?> packet) {
        this.playerMap.values().forEach(playerData -> playerData.onPacket(packet));
    }

    public void incrementTick() {
        for (PlayerData data : playerMap.values()) {
            data.incrementTick();
        }
    }
}
