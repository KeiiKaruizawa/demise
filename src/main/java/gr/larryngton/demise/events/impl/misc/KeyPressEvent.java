package gr.larryngton.demise.events.impl.misc;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.Getter;

@Getter
public class KeyPressEvent extends CancellableEvent {
    private final int key;

    public KeyPressEvent(int key) {
        this.key = key;
    }
}
