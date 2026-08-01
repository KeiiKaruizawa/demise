package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.utils.player.handlers.clicking.ClickManager;
import net.minecraft.client.settings.GameSettings;

@ModuleInfo(name = "AutoClicker", description = "Automatically clicks for you.")
public class AutoClicker extends Module {
    private final ClickManager clickManager = new ClickManager(this);

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        if (GameSettings.isKeyDown(mc.gameSettings.keyBindAttack)) {
            // hit delay is handled inside ClickHandler
            mc.leftClickCounter = 0;

            clickManager.click(3, null);
        }
    }
}
