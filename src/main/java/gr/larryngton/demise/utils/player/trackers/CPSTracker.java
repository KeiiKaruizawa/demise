package gr.larryngton.demise.utils.player.trackers;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.Event;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.utils.InstanceAccess;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;

import java.util.ArrayList;

public class CPSTracker implements InstanceAccess {
    private static final ArrayList<Long> leftClicks = new ArrayList<>();
    private static final ArrayList<Long> rightClicks = new ArrayList<>();

    public int getLeftClicks() {
        return leftClicks.size();
    }

    public int getRightClicks() {
        return rightClicks.size();
    }

    @EventTarget
    public void onGame(GameEvent e) {
        long time = System.currentTimeMillis();
        leftClicks.removeIf(aLong -> aLong + 1000L < time);
        rightClicks.removeIf(aLong -> aLong + 1000L < time);
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() != PacketEvent.State.OUTGOING) return;

        if (e.getPacket() instanceof C0APacketAnimation) {
            leftClicks.add(System.currentTimeMillis());
            Demise.INSTANCE.getEventManager().call(new LClickEvent());
        }

        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            rightClicks.add(System.currentTimeMillis());
        }
    }

    public static class LClickEvent implements Event {
    }
}
