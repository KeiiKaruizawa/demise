package gr.larryngton.demise.features.command.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.command.CommandExecutionException;
import gr.larryngton.demise.utils.misc.ChatUtils;

import static gr.larryngton.demise.utils.InstanceAccess.mc;

public class FriendCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"friend", "f", "fr"};
    }

    @Override
    public void execute(String[] arguments) throws CommandExecutionException {
        if (arguments.length == 1) {
            throw new CommandExecutionException(this.getUsage());
        }
        String lowerCase = arguments[1].toLowerCase();
        if (arguments.length == 2) {
            switch (lowerCase) {
                case "clear":
                    ChatUtils.sendMessageClient("Cleared the friends list.");
                    Demise.INSTANCE.getFriendManager().getFriends().clear();
                    break;
                case "list":
                    if (!Demise.INSTANCE.getFriendManager().getFriends().isEmpty()) {
                        ChatUtils.sendMessageClient("Friends §7(§f" + Demise.INSTANCE.getFriendManager().getFriends().size() + "§7)§f: §a" + Demise.INSTANCE.getFriendManager().getFriendsName());
                        break;
                    }
                    ChatUtils.sendMessageClient("The friends list is empty.");
                    break;
                default:
                    throw new CommandExecutionException(this.getUsage());
            }
        } else {
            if (arguments.length != 3) {
                throw new CommandExecutionException(this.getUsage());
            }
            if (arguments[2].contains(mc.thePlayer.getName())) {
                ChatUtils.sendMessageClient("§c§lNO");
                return;
            }
            String lowerCase2 = arguments[1].toLowerCase();
            switch (lowerCase2) {
                case "add":
                    ChatUtils.sendMessageClient("§b" + arguments[2] + " §7has been §2added to the friends list.");
                    Demise.INSTANCE.getFriendManager().add(arguments[2]);
                    break;
                case "remove":
                    ChatUtils.sendMessageClient("§b" + arguments[2] + " §7has been §4removed from the friends list.");
                    Demise.INSTANCE.getFriendManager().remove(arguments[2]);
                    break;
                default:
                    throw new CommandExecutionException(this.getUsage());
            }
        }
    }

    @Override
    public String getUsage() {
        return "friend add <name> | remove <name> | list | clear";
    }
}