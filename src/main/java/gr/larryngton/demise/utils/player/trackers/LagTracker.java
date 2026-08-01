package gr.larryngton.demise.utils.player.trackers;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.TimerUtil;

import java.awt.*;

public class LagTracker implements InstanceAccess {
    public static final TimerUtil timer = new TimerUtil();
    private static final TimerUtil timer1 = new TimerUtil();

    public boolean isLagging() {
        boolean elapsed = timer.hasTimeElapsed(250);
        if (elapsed) {
            timer1.reset();
        }

        return elapsed && timer1.hasTimeElapsed(75);
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            timer.reset();
        }
    }
}