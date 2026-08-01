package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.anticheat.impl.Check;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.combat.AntiBot;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.MultiBoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import net.minecraft.entity.player.EntityPlayer;

import java.util.*;
import java.util.stream.Collectors;

@ModuleInfo(name = "AntiCheat", description = "Detects cheaters in your game.", legit = true)
public class AntiCheat extends Module {
    public final MultiBoolValue checks = new MultiBoolValue("Checks", Arrays.asList(
            new BoolValue("Motion A", false),
            new BoolValue("Motion B", false),
            new BoolValue("Motion C", false),
            new BoolValue("Motion D", false),
            new BoolValue("Invalid swing", false),
            new BoolValue("NoSlow A", false),
            new BoolValue("NoSlow B", false),
            new BoolValue("AutoClicker A", false),
            new BoolValue("AutoClicker B", false),
            new BoolValue("FakeLag", false)
    ), this);

    public final SliderValue alertCoolDown = new SliderValue("Alert Cooldown", 1000, 0, 2000, 1, this);
    public final BoolValue alertInChat = new BoolValue("Alert in chat", false, this);

    private final Set<EntityPlayer> cheaters = new HashSet<>();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        Demise.INSTANCE.getAntiCheat().incrementTick();
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            Demise.INSTANCE.getAntiCheat().onPacket(e.getPacket());
        }
    }

    @Override
    public void onDisable() {
        cheaters.clear();
    }

    public void mark(EntityPlayer ent) {
        cheaters.add(ent);
    }

    public boolean isCheater(EntityPlayer ent) {
        for (EntityPlayer player : cheaters) {
            if (!ent.getName().equals(player.getName())) continue;
            return true;
        }
        return false;
    }
}
