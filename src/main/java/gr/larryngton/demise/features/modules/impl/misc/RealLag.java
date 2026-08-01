package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.packet.LagUtils;

@ModuleInfo(name = "RealLag", description = "FakeLag, but real.")
public class RealLag extends Module {
    private final SliderValue ping = new SliderValue("Ping", 100, 0, 1000, 1, this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(String.valueOf(ping.get()));
        LagUtils.spoof(ping.getInt(), true, false, false, true, true, true, true);
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        LagUtils.disable();
        LagUtils.dispatch();
    }

    @Override
    public void onDisable() {
        LagUtils.disable();
        LagUtils.dispatch();
    }
}