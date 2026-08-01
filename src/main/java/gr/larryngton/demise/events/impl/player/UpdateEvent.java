package gr.larryngton.demise.events.impl.player;

import gr.larryngton.demise.events.impl.Event;

public final class UpdateEvent implements Event {
    private UpdateEvent() {
    }

    public static UpdateEvent INSTANCE = new UpdateEvent();
}