package gr.larryngton.demise.features.modules.impl.player;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.TickEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.utils.misc.SpoofSlotUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import net.minecraft.util.MovingObjectPosition;

@ModuleInfo(name = "AutoTool", description = "Automatically switches to the best tool when digging.")
public class AutoTool extends Module {
    private final BoolValue spoof = new BoolValue("Spoof", false, this);
    private final BoolValue switchBack = new BoolValue("Switch back", true, this, () -> !spoof.get());
    private final BoolValue notOnItemUse = new BoolValue("Not on item use", true, this);

    private int oldSlot;
    private boolean wasDigging;

    @Override
    public void onDisable() {
        if (wasDigging) {
            mc.thePlayer.inventory.currentItem = oldSlot;
            wasDigging = false;
        }
        SpoofSlotUtils.stopSpoofing();
    }

    @EventTarget
    public void onTick(TickEvent e) {
        boolean legit = mc.gameSettings.keyBindAttack.isKeyDown() && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && PlayerUtils.findTool(mc.objectMouseOver.getBlockPos()) != -1;
        boolean bedBreaker = getModule(BedBreaker.class).isEnabled() && BedBreaker.breaking && PlayerUtils.findTool(BedBreaker.targetPos) != -1;
        boolean itemUse = !notOnItemUse.get() || !mc.gameSettings.keyBindUseItem.isKeyDown();

        if ((legit || bedBreaker) && itemUse) {
            if (!wasDigging) {
                oldSlot = mc.thePlayer.inventory.currentItem;
                if (spoof.get()) {
                    SpoofSlotUtils.startSpoofing(oldSlot);
                }
            }
            mc.thePlayer.inventory.currentItem = legit ? PlayerUtils.findTool(mc.objectMouseOver.getBlockPos()) : PlayerUtils.findTool(BedBreaker.targetPos);
            wasDigging = true;
        } else if (wasDigging && (switchBack.get() || spoof.get())) {
            mc.thePlayer.inventory.currentItem = oldSlot;
            SpoofSlotUtils.stopSpoofing();
            wasDigging = false;
        } else {
            oldSlot = mc.thePlayer.inventory.currentItem;
        }
    }
}
