package gr.larryngton.demise.events.impl.misc;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.IChatComponent;

@Getter
@Setter
@AllArgsConstructor
public class PrintChatMessageEvent extends CancellableEvent {
    private IChatComponent chatComponent;
}
