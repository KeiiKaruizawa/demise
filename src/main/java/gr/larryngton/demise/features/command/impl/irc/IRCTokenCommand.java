package gr.larryngton.demise.features.command.impl.irc;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.modules.impl.misc.IRC;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.misc.StringUtils;
import moe.lasoleil.axochat4j.packet.c2s.C2SRequestJWTPacket;

public class IRCTokenCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"token"};
    }

    @Override
    public void execute(final String[] arguments) {
        IRC irc = Demise.INSTANCE.getModuleManager().getModule(IRC.class);

        switch (arguments[1].toLowerCase()) {
            case "set":
                if (arguments.length > 2) {
                    IRC.jwtToken = StringUtils.toCompleteString(arguments, 2);
                    irc.jwt.set(true);

                    if (irc.isEnabled()) {
                        irc.setEnabled(false);
                        irc.setEnabled(true);
                    }
                } else {
                    ChatUtils.sendMessageClient("token set <token>");
                }
                break;
            case "generate":
                if (!irc.isEnabled()) {
                    ChatUtils.sendMessageClient("IRC is disabled.");
                    return;
                }

                IRC.client.sendPacket(new C2SRequestJWTPacket());
                break;
        }
    }

    @Override
    public String getUsage() {
        return "token <set/generate>";
    }
}