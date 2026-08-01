package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@ModuleInfo(name = "AutoMeow", description =  "Automeowtically meows for you!")
public class AutoMeow extends Module {
    private final DoubleSliderValue delay = new DoubleSliderValue("Delay", 500, 1000, 0, 10000, 50, this);
    private final DoubleSliderValue length = new DoubleSliderValue("Length", 5, 10, 1, 50, 1, this);

    private final TimerUtil meowTimer = new TimerUtil();
    private int nextDelay;
    private final List<String> baseMessages = Arrays.asList(
            "nya",
            "nya~",
            "mew",
            "meow",
            "mrrp",
            ":3",
            "meowmeow",
            "mow"
    );

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (meowTimer.hasTimeElapsed(nextDelay)) {
            mc.thePlayer.sendChatMessage(generateMessages((int) length.random()));

            nextDelay = (int) delay.random();
            meowTimer.reset();
        }
    }

    private String generateMessages(int count) {
        return ThreadLocalRandom.current()
                .ints(count, 0, baseMessages.size())
                .mapToObj(baseMessages::get)
                .collect(Collectors.joining(" "));
    }
}