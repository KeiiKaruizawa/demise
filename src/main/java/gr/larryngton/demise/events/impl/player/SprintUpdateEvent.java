package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SprintUpdateEvent extends CancellableEvent {
    private State state;

    public enum State {
        PRE,
        POST
    }
}
