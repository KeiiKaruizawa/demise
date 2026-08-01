package gr.larryngton.demise.events.impl.packet;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.Packet;

@Getter
@AllArgsConstructor
public class PacketEvent extends CancellableEvent {
    @Setter
    private Packet<?> packet;
    private final State state;

    public enum State {
        INCOMING,
        OUTGOING
    }
}
