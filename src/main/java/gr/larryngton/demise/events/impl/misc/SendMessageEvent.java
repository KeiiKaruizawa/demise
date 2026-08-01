package gr.larryngton.demise.events.impl.misc;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SendMessageEvent extends CancellableEvent {
    private String message;
}
