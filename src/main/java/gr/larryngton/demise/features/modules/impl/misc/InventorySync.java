package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

@ModuleInfo(name = "InventorySync", description = "Prevents inventory de-syncs.", legit = true)
public class InventorySync extends Module {
    public short action;

    @EventTarget
    public void onSendPacket(PacketEvent e) {
        if (e.getState() != PacketEvent.State.INCOMING) {
            return;
        }

        if (e.getPacket() instanceof S32PacketConfirmTransaction wrapper) {
            final Container inventory = mc.thePlayer.inventoryContainer;

            if (wrapper.getWindowId() == inventory.windowId) {
                this.action = wrapper.getActionNumber();

                if (this.action > 0 && this.action < inventory.transactionID) {
                    inventory.transactionID = (short) (this.action + 1);
                }
            }
        }
    }
}