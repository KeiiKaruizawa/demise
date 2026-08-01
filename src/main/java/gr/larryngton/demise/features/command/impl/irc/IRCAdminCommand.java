package gr.larryngton.demise.features.command.impl.irc;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.modules.impl.misc.IRC;
import gr.larryngton.demise.utils.misc.ChatUtils;

public class IRCAdminCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"ircadmin", "irca"};
    }

    @Override
    public void execute(final String[] arguments) {
        IRC irc = Demise.INSTANCE.getModuleManager().getModule(IRC.class);

        if (!irc.isEnabled()) {
            ChatUtils.sendMessageClient("IRC is disabled.");
            return;
        }

        if (arguments.length <= 1) {
            sendUsage();
            return;
        }

        if (arguments.length == 2) {
            sendUsage();
        } else {
            switch (arguments[1].toLowerCase()) {
                case "ban" -> IRC.client.banUser(arguments[2]);
                case "unban" -> IRC.client.unbanUser(arguments[2]);
            }
        }
    }

    private void sendUsage() {
        ChatUtils.sendMessageClient(getUsage());
    }

    @Override
    public String getUsage() {
        return "ircadmin <ban/unban> <username>";
    }
}