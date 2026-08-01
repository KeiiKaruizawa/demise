package gr.larryngton.demise.features.statistics.impl;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.SendMessageEvent;
import gr.larryngton.demise.features.statistics.Stat;

import java.text.DecimalFormat;

public class NWordsStat extends Stat {
    private long sessionNWords;
    private final long totalNWords = loadStat();
    private final DecimalFormat decimalFormat = new DecimalFormat("###,###");
    private final String[] nWords = new String[]{"", "", "", "", ""};

    @Override
    public String getName() {
        return "N words";
    }

    @EventTarget
    public void onSendMessage(SendMessageEvent e) {
        for (String msg : e.getMessage().split(" ")) {
            for (String hi : nWords) {
                if (msg.toLowerCase().contains(hi)) {
                    sessionNWords++;
                    break;
                }
            }
        }
    }

    @Override
    public String getStatFormatted(boolean session) {
        return decimalFormat.format(session ? sessionNWords : totalNWords + sessionNWords).replace(",", ".");
    }

    @Override
    public String getStat() {
        return String.valueOf(totalNWords + sessionNWords);
    }
}
