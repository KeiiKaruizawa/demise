package gr.larryngton.demise.events.impl.packet;

import gr.larryngton.demise.events.impl.CancellableEvent;
import gr.larryngton.demise.utils.packet.TimedPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PacketReleaseEvent extends CancellableEvent {
    TimedPacket timedPacket;
}
