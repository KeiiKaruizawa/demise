package gr.larryngton.demise.features.anticheat.impl.combat.autoclicker;

import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.anticheat.impl.Check;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0BPacketAnimation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AutoClickerA extends Check {
    private double buffer;
    private long lastSwing;
    private final List<Long> attacks = new ArrayList<>();
    private final List<Long> deviations = new ArrayList<>();
    private long started;

    public AutoClickerA(PlayerData data) {
        super(data);
    }

    @Override
    public String getName() {
        return "AutoClicker A";
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (packet instanceof S0BPacketAnimation s0b && s0b.getEntityID() == player.getEntityId() && s0b.getAnimationType() == 0) {
            attacks.add(System.currentTimeMillis() - lastSwing);

            if (attacks.size() >= 50) {
                double standardDeviation = standardDeviation(attacks);
                deviations.add((long) standardDeviation);
                attacks.clear();
            }

            if (deviations.size() >= 3) {
                double std = standardDeviation(deviations);

                long length = System.currentTimeMillis() - started;

                if (std < 25 && length < 4000) {
                    int vlAdd = std < 10 ? 2 : 1;
                    buffer += vlAdd;
                    if (buffer > 2) {
                        flag("Too low standard deviation", std + "");
                    }
                } else if (buffer > 0) {
                    buffer -= 0.1;
                    buffer *= 0.9;
                }

                started = System.currentTimeMillis();
                deviations.clear();
            }

            lastSwing = System.currentTimeMillis();
        }
    }

    private double standardDeviation(Collection<? extends Number> sd) {
        double sum = 0, newSum = 0;
        for (Number v : sd) {
            sum = sum + v.doubleValue();
        }
        double mean = sum / sd.size();
        for (Number v : sd) {
            newSum = newSum + (v.doubleValue() - mean) * (v.doubleValue() - mean);
        }
        return Math.sqrt(newSum / sd.size());
    }
}
