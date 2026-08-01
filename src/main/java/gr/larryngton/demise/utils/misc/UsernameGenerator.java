package gr.larryngton.demise.utils.misc;

import gr.larryngton.demise.utils.InstanceAccess;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UsernameGenerator implements InstanceAccess {
    public static List<String> retrieve() {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL("https://raw.githubusercontent.com/jeanphorn/wordlist/master/usernames.txt").openConnection();
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");

            try (InputStream stream = getStream(connection);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .toList();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static String generate() {
        List<String> usernames = retrieve();
        if (usernames.isEmpty()) return null;

        List<String> filtered = usernames.stream().filter(u -> u.length() >= 3 && u.length() <= 6).toList();

        if (filtered.size() < 2) return null;

        String prefix = filtered.get(rand.nextInt(filtered.size()));
        String suffix = filtered.get(rand.nextInt(filtered.size()));

        if (Math.random() > 0.02) {
            suffix += rand.nextBoolean() ? "" : rand.nextInt(rand.nextBoolean() ? 500 : 100);

            if (Math.random() < 0.45) {
                if (rand.nextBoolean()) {
                    prefix = StringUtils.capitalizeWords(prefix);
                }

                if (rand.nextBoolean()) {
                    suffix = StringUtils.capitalizeWords(suffix);
                }
            }
        } else {
            prefix = "xX" + prefix;
            suffix += "Xx";
        }

        return prefix + suffix;
    }

    private static InputStream getStream(HttpsURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        return responseCode >= 200 && responseCode < 400 ? connection.getInputStream() : connection.getErrorStream();
    }
}
