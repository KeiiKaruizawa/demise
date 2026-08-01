package gr.larryngton.demise.events.impl.render;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ViewBobbingEvent extends CancellableEvent {
    private State state;

    public enum State {
        CameraTransform,
        Hand1,
        Hand2
    }
}
