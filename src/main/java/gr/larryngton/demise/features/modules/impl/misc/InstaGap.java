package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.utils.misc.ChatUtils;

@ModuleInfo(name = "InstaGap", description = "What the fuck is a instagap? just be making bullshit now")
public class InstaGap extends Module {
    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.ticksExisted % 20 == 0) {
            ChatUtils.sendMessageClient("[52✫] [MVP+] sweatycup: blue instagap");

        }
    }
}