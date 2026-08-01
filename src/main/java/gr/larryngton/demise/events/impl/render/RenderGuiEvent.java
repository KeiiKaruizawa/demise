package gr.larryngton.demise.events.impl.render;

import gr.larryngton.demise.events.impl.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RenderGuiEvent implements Event {
    private State state;

    public enum State {
        PRE,
        POST
    }
}
