package gr.larryngton.demise.features.statistics.impl;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.statistics.Stat;

import java.text.DecimalFormat;

public class DeathsStat extends Stat {
    private int sessionDeaths;
    private final long totalDeaths = loadStat();
    private final DecimalFormat decimalFormat = new DecimalFormat("###,###");
    private boolean died;

    @Override
    public String getName() {
        return "Deaths";
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.getHealth() <= 0) {
            if (!died) {
                sessionDeaths++;
                died = true;
            }
        } else {
            died = false;
        }
    }

    @Override
    public String getStatFormatted(boolean session) {
        return decimalFormat.format(session ? sessionDeaths : totalDeaths + sessionDeaths).replace(",", ".");
    }

    @Override
    public String getStat() {
        return String.valueOf(totalDeaths + sessionDeaths);
    }
}
