package gr.larryngton.demise.features.command.impl.irc;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.modules.impl.misc.IRC;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.packet.UserUtils;
import net.minecraft.client.Minecraft;

public class IRCLoginCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"irclogin"};
    }

    @Override
    public void execute(final String[] arguments) {
        IRC irc = Demise.INSTANCE.getModuleManager().getModule(IRC.class);

        if (!irc.isEnabled()) {
            ChatUtils.sendMessageClient("IRC is disabled.");
            return;
        }

        if (!IRC.client.isConnected()) {
            ChatUtils.sendMessageClient("IRC is not connected to the server.");
            return;
        }

        if (UserUtils.isValidTokenOffline(Minecraft.getMinecraft().session.getToken())) {
            IRC.client.loginMojang();
        } else {
            ChatUtils.sendMessageClient("You need a minecraft account to do this.");
        }
    }

    @Override
    public String getUsage() {
        return "irclogin";
    }
}
