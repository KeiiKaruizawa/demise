package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;

@ModuleInfo(name = "JumpDelay", description = "Modifies your jump delay.", legit = true)
public class JumpDelay extends Module {
    private final SliderValue jumpDelay = new SliderValue("Jump delay", 0, 0, 20, 1, this);
    private final TimerUtil delay = new TimerUtil();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(String.valueOf(jumpDelay.get()));

        // if jumpTicks > 1 you will not be able to jump at all, so this is needed
        if (jumpDelay.get() > 1) {
            if (delay.hasTimeElapsed(jumpDelay.get() * 50)) {
                mc.thePlayer.jumpTicks = 0;
                delay.reset();
            } else {
                mc.thePlayer.jumpTicks = (int) jumpDelay.get();
            }
        } else {
            mc.thePlayer.jumpTicks = (int) jumpDelay.get();
        }
    }
}
