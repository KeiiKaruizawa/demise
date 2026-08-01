package gr.larryngton.demise.features.command.impl.irc;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.modules.impl.misc.IRC;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.misc.StringUtils;

public class IRCPrivateCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"ircp", "pchat"};
    }

    @Override
    public void execute(final String[] arguments) {
        IRC irc = Demise.INSTANCE.getModuleManager().getModule(IRC.class);

        if (arguments.length > 2) {
            if (!irc.isEnabled()) {
                ChatUtils.sendMessageClient("IRC is disabled.");
                return;
            }

            if (!IRC.client.isConnected()) {
                ChatUtils.sendMessageClient("IRC is not connected to the server.");
                return;
            }

            IRC.client.sendPrivateMessage(arguments[1], StringUtils.toCompleteString(arguments, 2));
            ChatUtils.sendMessageClient("Message was successfully sent.");
        } else {
            ChatUtils.sendMessageClient(getUsage());
        }
    }

    @Override
    public String getUsage() {
        return "ircp/pchat <username> <message>";
    }
}
