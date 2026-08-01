package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class MotionEvent extends CancellableEvent {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public boolean onGround;
    public boolean sprinting;
    public State state;

    public MotionEvent(State state) {
        this.state = state;
    }

    public enum State {
        PRE,
        POST
    }

    public boolean isPre() {
        return state.equals(State.PRE);
    }

    public boolean isPost() {
        return state.equals(State.POST);
    }
}