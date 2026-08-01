package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import org.lwjglx.input.Keyboard;

@ModuleInfo(name = "ClickGUI", description = "Just guess.", key = Keyboard.KEY_RSHIFT, legit = true)
public class ClickGUI extends Module {

    @Override
    public void onEnable() {
        mc.displayGuiScreen(INSTANCE.getPanelGui());
        setEnabled(false);
    }
}