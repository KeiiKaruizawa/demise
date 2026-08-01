package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;

@ModuleInfo(name = "NoInterpolation", description = "Disables entity interpolation.")
public class NoInterpolation extends Module {
    public final BoolValue onlyYaw = new BoolValue("Only Yaw", false, this);
}
