package gr.larryngton.demise.features.command.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.command.Command;
import gr.larryngton.demise.features.modules.impl.movement.MotionModifier;
import gr.larryngton.demise.utils.misc.ChatUtils;

public class MotionModifierTickCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"motionmodifiertick", "tick"};
    }

    @Override
    public void execute(final String[] arguments) {
        if (arguments.length < 1) {
            ChatUtils.sendMessageClient(getUsage());
        } else {
            int val = Integer.parseInt(arguments[1]);
            Demise.INSTANCE.getModuleManager().getModule(MotionModifier.class).index = val;
            Demise.INSTANCE.getModuleManager().getModule(MotionModifier.class).indexP = val;
            ChatUtils.sendMessageClient("MotionModifier index has been set to: " + val);
        }
    }

    @Override
    public String getUsage() {
        return "motionmodifiertick/tick <tick>";
    }
}