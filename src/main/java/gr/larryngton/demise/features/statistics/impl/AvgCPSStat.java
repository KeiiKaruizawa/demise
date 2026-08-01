package gr.larryngton.demise.features.statistics.impl;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.statistics.Stat;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.trackers.CPSTracker;

import java.util.List;

public class AvgCPSStat extends Stat {
    private int clicks;
    private long sampledClicks;
    private int avgSessionCPS;
    private long samples;
    private final long totalSamples;
    private final long totalSampledClicks;
    private final TimerUtil attackTimer = new TimerUtil();

    public AvgCPSStat() {
        String loadStr = loadStatStr();
        int index = -1;

        StringBuilder builder = new StringBuilder();
        List<Character> ch = loadStr.chars().mapToObj(c -> (char) c).toList();
        for (Character character : ch) {
            if (character == '$') {
                index = ch.indexOf(character) + 1;
                break;
            }

            builder.append(character);
        }
        totalSampledClicks = Long.parseLong(builder.toString());

        if (index != -1) {
            StringBuilder builder1 = new StringBuilder();
            List<Character> ch1 = loadStr.substring(index).chars().mapToObj(c -> (char) c).toList();
            for (Character character : ch1) {
                builder1.append(character);
            }
            totalSamples = Long.parseLong(builder1.toString());
        } else {
            totalSamples = 0;
        }
    }

    @Override
    public String getName() {
        return "Average CPS";
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.ticksExisted % 20 == 0) {
            if (!attackTimer.hasTimeElapsed(1000) && clicks > 0) {
                samples++;

                sampledClicks += clicks;
                avgSessionCPS = Math.toIntExact(sampledClicks / samples);
            }

            clicks = 0;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        attackTimer.reset();
    }

    @EventTarget
    public void onLClick(CPSTracker.LClickEvent e) {
        clicks++;
    }

    @Override
    public String getStatFormatted(boolean session) {
        if (session) {
            long totalSampledClicks = this.totalSampledClicks + sampledClicks;
            long totalSamples = this.totalSamples + samples;

            long avgCPS = totalSamples == 0 ? 0 : (long) Math.ceil((double) (totalSampledClicks / totalSamples));
            return avgCPS + " CPS";
        }

        return avgSessionCPS + " CPS";
    }

    @Override
    public String getStat() {
        long totalSampledClicks = this.totalSampledClicks + sampledClicks;
        long totalSamples = this.totalSamples + samples;
        return totalSampledClicks + "$" + totalSamples;
    }
}
