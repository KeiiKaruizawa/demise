package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.network.play.server.S3APacketTabComplete;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Plugins", description = "Displays what plugins a server uses in the chat.")
public class Plugins extends Module {
    private final TimerUtil timer = new TimerUtil();

    @Override
    public void onEnable() {
        sendPacket(new C14PacketTabComplete("/"));
        timer.reset();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (timer.hasTimeElapsed(5000L)) {
            ChatUtils.sendMessageClient("Failed to detect any plugins");
            toggle();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            if (e.getPacket() instanceof S3APacketTabComplete s3A) {
                List<String> pluginList = new ArrayList<>();

                for (String command : s3A.func_149630_c()) {
                    String[] pluginCommand = command.split(":");
                    if (pluginCommand.length > 1 && !pluginCommand[0].startsWith("/minecraft")) {
                        String plugin = pluginCommand[0].replace("/", "");

                        if (!pluginList.contains(plugin)) {
                            pluginList.add(plugin);
                        }
                    }
                }

                if (!pluginList.isEmpty()) {
                    ChatUtils.sendMessageClient("Plugins (" + pluginList.size() + "):");
                    pluginList.forEach(ChatUtils::sendMessageClient);
                } else {
                    ChatUtils.sendMessageClient("No plugins found");
                }

                toggle();
            }
        }
    }
}