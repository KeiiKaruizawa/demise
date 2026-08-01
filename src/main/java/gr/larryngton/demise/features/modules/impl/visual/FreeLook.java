package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "FreeLook", description = "Allows you to freely look around without changing your serverside rotation.", legit = true)
public class FreeLook extends Module {
    private int savedPerspective = 0;

    @Override
    public void onEnable() {
        savedPerspective = mc.gameSettings.thirdPersonView;
        mc.gameSettings.thirdPersonView = 1;
    }

    @Override
    public void onDisable() {
        mc.gameSettings.thirdPersonView = savedPerspective;
        // mc.gameSettings.thirdPersonView = 0;
    }
}