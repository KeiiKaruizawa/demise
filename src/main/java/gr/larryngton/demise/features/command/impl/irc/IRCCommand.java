package gr.larryngton.demise.features.command.impl.irc;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.modules.impl.misc.IRC;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.misc.StringUtils;

public class IRCCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"irc", "chat"};
    }

    @Override
    public void execute(final String[] arguments) {
        if (arguments.length > 1) {
            IRC irc = Demise.INSTANCE.getModuleManager().getModule(IRC.class);

            if (!irc.isEnabled()) {
                ChatUtils.sendMessageClient("IRC is disabled.");
                return;
            }

            if (!IRC.client.isConnected()) {
                ChatUtils.sendMessageClient("IRC is not connected to the server.");
                return;
            }

            String message = StringUtils.toCompleteString(arguments, 1);

            IRC.client.sendMessage(message);
        } else {
            ChatUtils.sendMessageClient("Usage " + getUsage());
        }
    }

    @Override
    public String getUsage() {
        return "irc/chat <message>";
    }
}
