package gr.larryngton.demise.features.command.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.command.CommandExecutionException;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.utils.misc.ChatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ToggleCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"toggle", "t"};
    }

    @Override
    public void execute(final String[] arguments) throws CommandExecutionException {
        if (arguments.length != 2) {
            throw new CommandExecutionException(getUsage());
        }

        String targetModuleName = arguments[1];
        Module module = findModuleByName(targetModuleName);

        if (module == null) {
            throw new CommandExecutionException(getUsage());
        }

        toggleModuleAndNotify(module);
    }

    private Module findModuleByName(String moduleName) {
        return Demise.INSTANCE.getModuleManager().getModules().stream()
                .filter(module -> module.getName().replaceAll(" ", "").equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null);
    }

    private void toggleModuleAndNotify(Module module) {
        module.toggle();
        String status = module.isEnabled() ? "§AEnabled" : "§CDisabled.";
        ChatUtils.sendMessageClient(module.getName() + " has been " + status);
    }

    @Override
    public String getUsage() {
        return "toggle/t <module name>";
    }

    @Override
    public List<String> getArgumentSuggestions(String[] args)
    {
        if (args.length <= 1)
        {
            List<String> modlist = new ArrayList<>(Collections.emptyList());
            for (Module m : Demise.INSTANCE.getModuleManager().getModules()) {
                modlist.add(m.getName().replace(" ", ""));
            }
            return modlist;
        }
        return Collections.emptyList();
    }
}