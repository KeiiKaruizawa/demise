package gr.larryngton.demise.features.statistics.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.statistics.Stat;

public class PlaytimeStat extends Stat {
    private long sessionPlaytime;
    private final long totalPlaytime = loadStat();

    @Override
    public String getName() {
        return "Playtime";
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        sessionPlaytime = System.currentTimeMillis() - Demise.INSTANCE.getStartTimeLong();
    }

    public static String longToTime(long elapsed) {
        int elapsedSec = Math.toIntExact(elapsed / 1000);
        String days = elapsedSec > 86400 ? elapsedSec / 86400 + "d " : "";
        elapsedSec = !days.isEmpty() ? elapsedSec % 86400 : elapsedSec;
        String hours = elapsedSec > 3600 ? elapsedSec / 3600 + "h " : "";
        elapsedSec = !hours.isEmpty() ? elapsedSec % 3600 : elapsedSec;
        String minutes = elapsedSec > 60 ? elapsedSec / 60 + "m " : "";
        elapsedSec = !minutes.isEmpty() ? elapsedSec % 60 : elapsedSec;
        String seconds = elapsedSec > 0 ? elapsedSec + "s" : "";
        return days + hours + minutes + seconds;
    }

    @Override
    public String getStatFormatted(boolean session) {
        return longToTime(session ? sessionPlaytime : totalPlaytime + sessionPlaytime);
    }

    @Override
    public String getStat() {
        return String.valueOf(totalPlaytime + sessionPlaytime);
    }
}
