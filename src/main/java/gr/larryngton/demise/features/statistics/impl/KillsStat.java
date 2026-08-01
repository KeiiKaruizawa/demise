package gr.larryngton.demise.features.statistics.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.player.KillEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.statistics.Stat;
import gr.larryngton.demise.utils.math.TimerUtil;
import net.minecraft.entity.player.EntityPlayer;

import java.text.DecimalFormat;

public class KillsStat extends Stat {
    private int sessionKills;
    private final long totalKills = loadStat();
    private EntityPlayer target;
    private final TimerUtil attackTimer = new TimerUtil();
    private final DecimalFormat decimalFormat = new DecimalFormat("###,###");

    @Override
    public String getName() {
        return "Kills";
    }

    @EventTarget
    public void onAttackEvent(AttackEvent e) {
        if (e.getTargetEntity() instanceof EntityPlayer player) {
            target = player;
            attackTimer.reset();
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (target != null) {
            if (!mc.theWorld.playerEntities.contains(target)) {
                if (mc.thePlayer.ticksExisted > 20 && !attackTimer.hasTimeElapsed(5000)) {
                    Demise.INSTANCE.getEventManager().call(new KillEvent(target));
                    sessionKills++;
                }
                target = null;
            }
        }
    }

    @Override
    public String getStatFormatted(boolean session) {
        return decimalFormat.format(session ? sessionKills : totalKills + sessionKills).replace(",", ".");
    }

    @Override
    public String getStat() {
        return String.valueOf(totalKills + sessionKills);
    }
}
