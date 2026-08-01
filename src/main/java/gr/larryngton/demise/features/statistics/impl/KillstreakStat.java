package gr.larryngton.demise.features.statistics.impl;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.KillEvent;
import gr.larryngton.demise.features.statistics.Stat;
import gr.larryngton.demise.utils.math.TimerUtil;

public class KillstreakStat extends Stat {
    private int sessionBest;
    private final long totalBest = loadStat();
    private final TimerUtil killTimer = new TimerUtil();
    private int kills;

    @Override
    public String getName() {
        return "Best killstreak";
    }

    @EventTarget
    public void onKill(KillEvent e) {
        kills++;

        int sessionTempBest;
        if (!killTimer.hasTimeElapsed(2500)) {
            sessionTempBest = kills + 1;
        } else {
            sessionTempBest = kills = 0;
        }

        sessionBest = Math.max(sessionBest, sessionTempBest);
        killTimer.reset();
    }

    @Override
    public String getStatFormatted(boolean session) {
        return (session ? sessionBest : Math.max(sessionBest, totalBest)) + " kills";
    }

    @Override
    public String getStat() {
        return String.valueOf(Math.max(sessionBest, totalBest));
    }
}
