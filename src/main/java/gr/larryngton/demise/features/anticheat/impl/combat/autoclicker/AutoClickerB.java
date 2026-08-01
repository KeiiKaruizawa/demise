package gr.larryngton.demise.features.anticheat.impl.combat.autoclicker;

import gr.larryngton.demise.features.anticheat.PlayerData;
import gr.larryngton.demise.features.anticheat.impl.Check;
import gr.larryngton.demise.utils.math.TimerUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0BPacketAnimation;

public class AutoClickerB extends Check {
    private final TimerUtil timer = new TimerUtil();
    private int clicks;

    public AutoClickerB(PlayerData data) {
        super(data);
    }

    @Override
    public String getName() {
        return "AutoClicker B";
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (packet instanceof S0BPacketAnimation s0b && s0b.getEntityID() == player.getEntityId() && s0b.getAnimationType() == 0) {
            if (clicks == 0) {
                timer.reset();
            }

            clicks++;

            if (timer.hasTimeElapsed(1000)) {
                if (clicks > 24) {
                    flag("Clicking too fast", clicks + "cps");
                }
                timer.reset();
                clicks = 0;
            }
        }
    }
}
