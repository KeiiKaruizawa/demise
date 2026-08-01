package gr.larryngton.demise.features.modules.impl.combat;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.MultiBoolValue;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ModuleInfo(name = "AntiBot", description = "Makes Combat-related modules ignore bots.")
public class AntiBot extends Module {
    public final MultiBoolValue options = new MultiBoolValue("Options", Arrays.asList(
            new BoolValue("Tab", false),
            new BoolValue("Hypixel", false),
            new BoolValue("Simple", false)
    ), this);

    public final ArrayList<EntityPlayer> bots = new ArrayList<>();
    private static final String VALID_USERNAME_REGEX = "^[a-zA-Z0-9_]{1,16}+$";
    private List<BoolValue> lastOptionList = new ArrayList<>();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        List<BoolValue> lastBool = lastOptionList;
        lastOptionList = options.getToggled();

        if (!lastOptionList.equals(lastBool)) {
            bots.clear();
        }
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        bots.clear();
    }

    @Override
    public void onDisable() {
        bots.clear();
        lastOptionList.clear();
    }

    public boolean isBot(EntityPlayer player) {
        if (!isEnabled()) return false;
        if (bots.contains(player)) return true;

        if (checkBot(player)) {
            bots.add(player);
            return true;
        }

        return false;
    }

    public boolean checkBot(EntityPlayer player) {
        if (options.isEnabled("Tab")) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equals(player.getName())) {
                    return false;
                }
            }
            return true;
        }

        if (options.isEnabled("Hypixel")) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                return info.getGameProfile().getId().compareTo(player.getUniqueID()) != 0 ||
                        EnumChatFormatting.getTextWithoutFormattingCodes(player.getDisplayName().getUnformattedText()).startsWith("[NPC] ") ||
                        !player.getName().matches(VALID_USERNAME_REGEX);
            }
        }

        if (options.isEnabled("Simple")) {
            return player.getName().toLowerCase().contains("bot") ||
                    player.getName().toLowerCase().contains("npc");
        }

        return false;
    }
}