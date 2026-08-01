package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

@ModuleInfo(name = "InvMove", description = "Allows you to move in Guis.")
public class InvMove extends Module {
    private final BoolValue safe = new BoolValue("Safe", false, this);

    private final KeyBinding[] keyBindings = new KeyBinding[]{
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindRight,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindJump
    };

    @Override
    public void onDisable() {
        for (KeyBinding keyBinding : this.keyBindings) {
            KeyBinding.setKeyBindState(keyBinding.getKeyCode(), false);
        }
    }

    @EventTarget
    private void onUpdate(UpdateEvent e) {
        if (!(mc.currentScreen instanceof GuiChat)) {
            if (safe.get() && (mc.currentScreen instanceof GuiContainer)) return;

            for (KeyBinding keyBinding : this.keyBindings) {
                KeyBinding.setKeyBindState(keyBinding.getKeyCode(), GameSettings.isKeyDown(keyBinding));
            }
        }
    }
}