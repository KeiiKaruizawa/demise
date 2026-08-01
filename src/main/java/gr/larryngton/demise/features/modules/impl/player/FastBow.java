package gr.larryngton.demise.features.modules.impl.player;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.BowAimbot;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.packet.PacketUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBow;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.lwjglx.input.Mouse;

@ModuleInfo(name = "FastBow", description = "Makes you shoot bows faster.")
public class FastBow extends Module {
    private final SliderValue speed = new SliderValue("Speed", 32, 1, 100, 1, this);
    private final BoolValue trigger = new BoolValue("Trigger", true, this);

    public FastBow() {
        trigger.setDescription("Hood bypass");
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemBow && !Mouse.isButtonDown(1)) {
            mc.thePlayer.setItemInUseCount(0);
        }

        if (!Mouse.isButtonDown(1) || !(mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemBow)) {
            return;
        }

        mc.thePlayer.setItemInUseCount(72000 - 20);

        for (int i = 0; i < speed.get(); i++) {
            PacketUtils.sendPacket(new C03PacketPlayer(mc.thePlayer.onGround));
        }

        if (trigger.get()) {
            if (getModule(BowAimbot.class).isEnabled() && !BowAimbot.canShoot) return;
            mc.playerController.onStoppedUsingItem(mc.thePlayer);
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
        }
    }
}