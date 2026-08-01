package gr.larryngton.demise.utils.packet;

import gr.larryngton.demise.utils.api.HttpUtils;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class UserUtils {
    private static final Map<String, CompletableFuture<String>> uuidCache = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<String>> usernameCache = new ConcurrentHashMap<>();

    /**
     * Check if token is valid
     * <p>
     * Exam
     * 7a7c4193280a4060971f1e73be3d9bdb
     * 89371141db4f4ec485d68d1f63d01eec
     */
    public static boolean isValidTokenOffline(String token) {
        return token.length() >= 32;
    }

    public static CompletableFuture<String> getUsername(String uuid) throws IOException {
        return uuidCache.computeIfAbsent(uuid, key ->
                HttpUtils.send(it -> it.url("https://api.minecraftservices.com/minecraft/profile/lookup/" + key))
                        .thenApply(HttpUtils.bodyGson(Profile.class))
                        .thenApply(Profile::name)
        );
    }

    /**
     * Get UUID of username
     */
    public static CompletableFuture<String> getUUID(String username) {
        return usernameCache.computeIfAbsent(username, key ->
                HttpUtils.send(it -> it.url("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + key))
                        .thenApply(HttpUtils.bodyGson(Profile.class))
                        .thenApply(Profile::id)
        );
    }

    private record Profile(String id, String name) {
    }
}