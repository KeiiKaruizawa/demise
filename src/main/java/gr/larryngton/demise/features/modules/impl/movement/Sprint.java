package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.player.Scaffold;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.utils.player.MoveUtils;
import net.minecraft.client.settings.KeyBinding;

@ModuleInfo(name = "Sprint", description = "Makes you automatically sprint.", legit = true)
public class Sprint extends Module {
    private final BoolValue omni = new BoolValue("Omni", false, this);
    public final BoolValue silent = new BoolValue("Silent", false, this);

    public Sprint() {
        silent.setDescription("Sprint clientside without sending sprint packets to the server.");
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!isEnabled(Scaffold.class)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }

        if (omni.get()) {
            mc.thePlayer.omniSprint = MoveUtils.isMoving();
        }
    }

    @Override
    public void onDisable() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        mc.thePlayer.omniSprint = false;
    }
}