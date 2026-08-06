package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "CustomWidgets", description = "Overrides the vanilla HUD widgets.", legit = true)
public class CustomWidgetsModule extends Module {
    public final BoolValue hotbar = new BoolValue("Hotbar", true, this);
    public final ModeValue hotbarMode = new ModeValue("Hotbar mode", new String[]{"Default", "Rise 5", "Augustus"}, "Default", this, hotbar::get);
    public final BoolValue scoreboard = new BoolValue("Scoreboard", true, this);
    public final BoolValue chat = new BoolValue("Chat", true, this);
    public final BoolValue inventory = new BoolValue("Inventory", false, this);
    public final BoolValue tabOverlay = new BoolValue("Tab overlay", false, this);
    public final BoolValue worldFade = new BoolValue("World fade", false, this);
    public final BoolValue sessionInfo = new BoolValue("Session Information", false, this);
}
