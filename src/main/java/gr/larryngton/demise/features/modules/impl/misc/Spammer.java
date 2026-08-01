package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.features.values.impl.TextValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;

@ModuleInfo(name = "Spammer", description = "Spams.")
public class Spammer extends Module {
    private final TextValue string = new TextValue("String", "hi", this);
    private final SliderValue delay = new SliderValue("Delay", 5, 1, 100, 1, this);

    private final TimerUtil timer = new TimerUtil();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (timer.hasTimeElapsed(delay.get() * 50L)) {
            ChatUtils.sendMessageServer(string.get());
            timer.reset();
        }
    }
}
