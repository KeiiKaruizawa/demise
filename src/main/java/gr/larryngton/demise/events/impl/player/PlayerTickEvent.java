package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class PlayerTickEvent extends CancellableEvent {
    public State state;

    public enum State {
        PRE,
        POST
    }
}
