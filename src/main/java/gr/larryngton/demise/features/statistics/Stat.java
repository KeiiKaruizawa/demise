package gr.larryngton.demise.features.statistics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gr.larryngton.demise.utils.InstanceAccess;

import java.io.FileReader;
import java.io.IOException;

public abstract class Stat implements InstanceAccess {
    public abstract String getName();

    public abstract String getStatFormatted(boolean session);

    public abstract String getStat();

    public long loadStat() {
        return Long.parseLong(loadStatStr());
    }

    public String loadStatStr() {
        try (FileReader reader = new FileReader(StatManager.statFile)) {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(reader);

            if (!element.isJsonNull()) {
                JsonObject jsonObject = element.getAsJsonObject();

                if (!jsonObject.isJsonNull()) {
                    if (jsonObject.has(getName())) {
                        JsonObject statObject = jsonObject.get(getName()).getAsJsonObject();

                        if (statObject.has("value")) {
                            return statObject.get("value").getAsString();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "0";
    }
}
