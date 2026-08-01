package gr.larryngton.demise.features.command;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.SendMessageEvent;
import gr.larryngton.demise.features.command.impl.*;
import gr.larryngton.demise.features.command.impl.irc.*;
import gr.larryngton.demise.utils.misc.ChatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CommandManager {

    public static final CommandManager INSTANCE = new CommandManager();

    public static final String PREFIX = ".";

    public List<Command> cmd = new ArrayList<>();

    public CommandManager() {
        addCommands(
                new HelpCommand(),
                new ToggleCommand(),
                new BindCommand(),
                new HideCommand(),
                new FriendCommand(),
                new ConfigCommand(),
                new HWIDCommand(),
                new OnlineConfigCommand(),
                new JumpCommand(),
                new IRCCommand(),
                new IRCTokenCommand(),
                new IRCAdminCommand(),
                new IRCPrivateCommand(),
                new IRCLoginCommand(),
                new VClipCommand(),
                new MotionModifierTickCommand(),
                new PathFindCommand(),
                new LegitCommand(),
                new ReloadCommand()
        );

        Demise.INSTANCE.getModuleManager().getModules().forEach(module -> {
            if (!module.getValues().isEmpty()) cmd.add(new ModuleCommand(module, module.getValues()));
        });

        Demise.INSTANCE.getEventManager().register(this);
    }

    @EventTarget
    public void onSendMessageEvent(final SendMessageEvent event) {
        final String message = event.getMessage();
        if (message.startsWith(PREFIX)) {
            event.setCancelled(true);
            executeCommandString(message);
        }
    }

    private boolean executeCommandString(String rawText) {
        if (!rawText.startsWith(PREFIX)) {
            return false;
        }

        final String removedPrefix = rawText.substring(PREFIX.length());
        final String[] arguments = removedPrefix.split(" ");

        if (!removedPrefix.isEmpty() && arguments.length > 0) {
            for (final Command command : cmd) {
                if (command.getAliases() == null) continue;

                for (final String alias : command.getAliases()) {
                    if (alias.equalsIgnoreCase(arguments[0])) {
                        try {
                            command.execute(arguments);
                        } catch (CommandExecutionException e) {
                            ChatUtils.sendMessageClient("Invalid commands syntax. Hint: " + e.getMessage());
                        }
                        return true;
                    }
                }
            }
            ChatUtils.sendMessageClient("'" + arguments[0] + "' is not a command. Try '.help'");
            return true;
        } else {
            ChatUtils.sendMessageClient("No arguments were supplied. Try '.help'");
            return true;
        }
    }

    public boolean handle(String text) {
        return executeCommandString(text);
    }

    public void addCommands(Command... checks) {
        this.cmd.addAll(Arrays.asList(checks));
    }
}