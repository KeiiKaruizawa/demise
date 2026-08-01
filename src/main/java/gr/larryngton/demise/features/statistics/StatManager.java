package gr.larryngton.demise.features.statistics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.statistics.impl.*;
import gr.larryngton.demise.utils.InstanceAccess;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatManager implements InstanceAccess {
    public static File statFile = new File(Demise.INSTANCE.getMainDir(), "stats.json");
    public List<Stat> statList = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @SuppressWarnings("all")
    public StatManager() {
        if (!statFile.exists()) {
            try {
                statFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        register(
                new KillsStat(),
                new DeathsStat(),
                new PlaytimeStat(),
                new KillstreakStat(),
                new AvgCPSStat()
                // new NWordsStat() scary!
        );

        statList.forEach(stat -> Demise.INSTANCE.getEventManager().register(stat));
    }

    private void register(Stat... stats) {
        statList.addAll(Arrays.asList(stats));
    }

    public void onStop() {
        JsonObject object = new JsonObject();
        for (Stat stat : statList) {
            JsonObject statObject = new JsonObject();
            statObject.addProperty("value", stat.getStat());
            object.add(stat.getName(), statObject);
        }

        String jsonString = gson.toJson(object);

        try (FileWriter writer = new FileWriter(statFile)) {
            writer.write(jsonString);
            Demise.LOGGER.info("Saved stats");
        } catch (IOException e) {
            Demise.LOGGER.error("Failed to save stats", e);
        }
    }
}
