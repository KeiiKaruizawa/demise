package gr.larryngton.demise.utils.packet;

import gr.larryngton.demise.utils.math.TimerUtil;
import lombok.Getter;
import net.minecraft.network.Packet;

@Getter
public class TimedPacket {
    private final Packet packet;
    private final TimerUtil time;
    private final long millis;

    public TimedPacket(Packet packet) {
        this.packet = packet;
        this.time = new TimerUtil();
        this.millis = System.currentTimeMillis();
    }
}