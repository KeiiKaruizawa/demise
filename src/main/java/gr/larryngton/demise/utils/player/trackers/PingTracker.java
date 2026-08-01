package gr.larryngton.demise.utils.player.trackers;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.packet.PacketUtils;
import lombok.Getter;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.network.play.server.S3APacketTabComplete;

public class PingTracker implements InstanceAccess {
    private static final TimerUtil updateTimer = new TimerUtil();
    private static long sendTime;
    @Getter
    private static int ping;
    private static boolean waitingForPong;

    public int getTickPing() {
        return (int) Math.ceil((float) ping / 50F);
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (updateTimer.hasTimeElapsed(Demise.INSTANCE.getClientSettings().pingUpdateDelay.getInt() * 1000L)) {
            PacketUtils.sendPacket(new C14PacketTabComplete(String.valueOf(mc.thePlayer.getName().charAt(0))));
            sendTime = System.currentTimeMillis();
            waitingForPong = true;
            updateTimer.reset();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            if (e.getPacket() instanceof S3APacketTabComplete && waitingForPong) {
                ping = Math.toIntExact(System.currentTimeMillis() - sendTime);
                waitingForPong = false;
            }
        }
    }
}
