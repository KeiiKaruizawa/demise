package gr.larryngton.demise.features.modules.impl.player;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.packet.PacketUtils;
import net.minecraft.item.ItemFood;
import net.minecraft.network.play.client.C03PacketPlayer;

@ModuleInfo(name = "FastUse", description = "Allows you to consume items faster.")
public class FastUse extends Module {
    private final SliderValue packets = new SliderValue("Packets", 32, 1, 100, 1, this);
    private final SliderValue every = new SliderValue("Every", 2, 1, 50, 1, this);
    private int itemInUseCount;
    private int clientItemInUseCount;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!mc.thePlayer.isUsingItem() || !(mc.thePlayer.getItemInUse().getItem() instanceof ItemFood)) {
            itemInUseCount = 0;
            clientItemInUseCount = 0;
            return;
        }

        clientItemInUseCount++;

        if (clientItemInUseCount % every.getInt() == 0) {
            for (int i = 0; i < packets.get(); i++) {
                PacketUtils.sendPacket(new C03PacketPlayer(mc.thePlayer.onGround));
                itemInUseCount++;
            }
        }

        if (itemInUseCount >= 32) {
            mc.playerController.onStoppedUsingItem(mc.thePlayer);
            itemInUseCount = 0;
        }

        itemInUseCount++;
    }
}