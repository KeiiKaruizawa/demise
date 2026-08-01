package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "HurtCam", description = "Changes hurt angle.", legit = true)
public class HurtCam extends Module {
    public final SliderValue amount = new SliderValue("Amount", 0f, 0f, 1f, 0.05f, this);

}
