package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.BlockAABBEvent;
import gr.larryngton.demise.events.impl.misc.TickEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "WallClimb", description = "Allows you to climb up walls.")
public class WallClimb extends Module {

    private final ModeValue mode = new ModeValue("Mode", new String[]{
            "Vanilla", "Ticks"}, "Vanilla", this);

    private final SliderValue speed = new SliderValue("Speed", 0.9f, 0.1f, 5.0f, 0.1f, this, () -> mode.is("Ticks") || mode.is("Vanilla"));
    private final SliderValue ticks = new SliderValue("Ticks", 0, 2, 10, 1, this, () -> mode.is("Ticks"));

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null) return;

        if (!mc.thePlayer.isCollidedHorizontally) return;

        switch (mode.get()) {
            case "Ticks":
                if (mc.thePlayer.ticksExisted % ticks.get() != 0) return;
                mc.thePlayer.motionX = speed.get() * 0.2;
                break;

            case "Vanilla":
                mc.thePlayer.motionX = speed.get() * 0.2;
                break;
        }
    }
}
