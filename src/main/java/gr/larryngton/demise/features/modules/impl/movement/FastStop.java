package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.MoveUtils;

@ModuleInfo(name = "FastStop", description = "Stop fast.")
public class FastStop extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Input", "Stop motion"}, "Input", this);
    private final SliderValue keepTicks = new SliderValue("Keep ticks", 1, 1, 3, 1, this);

    private int stopTicks;
    private float lastForward;
    private float lastStrafe;

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (e.getForward() == 0 && e.getStrafe() == 0) {
            if (stopTicks > 0) {
                if (mode.is("Stop motion")) {
                    MoveUtils.stopXZ();
                } else {
                    e.setForward(-lastForward);
                    e.setStrafe(-lastStrafe);
                }

                stopTicks--;
            }
        } else {
            stopTicks = keepTicks.getInt();
            lastForward = e.getForward();
            lastStrafe = e.getStrafe();
        }
    }
}
