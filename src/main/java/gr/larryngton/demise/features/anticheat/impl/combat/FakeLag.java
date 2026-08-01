package gr.larryngton.demise.features.anticheat.impl.combat;

import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.anticheat.impl.Check;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;

import java.util.ArrayList;
import java.util.List;

public class FakeLag extends Check {
    private final List<Long> s14Times = new ArrayList<>();
    private boolean susLag;
    private long lastAvgTime;
    private boolean startLag;
    private long possibleDelay;
    private int packetsInARow;

    public FakeLag(PlayerData data) {
        super(data);
    }

    @Override
    public String getName() {
        return "FakeLag";
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (isMove(packet) && ((S14PacketEntity) packet).getEntityId() == player.getEntityId() && data.getTimeSinceLastS14() < 10000) {
            if (susLag) {
                // packets start sending from lag
                if (Math.abs(data.getTimeSinceLastS14() - lastAvgTime) < lastAvgTime * 0.4) {
                    startLag = true;
                    susLag = false;
                }
                return;
            }

            if (s14Times.size() >= 15 && !startLag && data.getLastSpeedXZ() > 0.1) {
                long avgTime = 0;
                for (long a : s14Times) {
                    avgTime += a;
                }
                lastAvgTime = avgTime /= s14Times.size();
                possibleDelay = data.getTimeSinceLastS14() - avgTime;

                // started lagging
                if (possibleDelay > 200) {
                    susLag = true;
                    return;
                }
            }

            if (startLag) {
                // lagging again, either a legit or just shit client
                if (data.getTimeSinceLastS14() > 150) {
                    startLag = false;
                    packetsInARow = 0;
                }

                // releasing all packets
                if (data.getLastSpeedXZ() > 0.12 && data.getTimeSinceLastS14() < 25) {
                    packetsInARow++;
                } else {
                    packetsInARow = 0;
                }

                int expectedBurst = (int) Math.ceil((double) possibleDelay / lastAvgTime * 0.65);
                if (packetsInARow >= expectedBurst) {
                    // releasing near player
                    for (EntityPlayer player1 : mc.theWorld.playerEntities) {
                        if (player != player1 && player.getDistanceSqToEntity(player1) < 20.25) {
                            flag("Releasing accumulated packets near players", "Delay: " + possibleDelay);
                            break;
                        }
                    }

                    startLag = false;
                    susLag = false;
                    packetsInARow = 0;
                }
            }

            s14Times.add(data.getTimeSinceLastS14());
            while (s14Times.size() > 20) {
                s14Times.remove(0);
            }
        }
    }
}