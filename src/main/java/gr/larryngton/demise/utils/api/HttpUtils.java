package gr.larryngton.demise.utils.api;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gr.larryngton.demise.Demise;
import lombok.experimental.UtilityClass;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

@UtilityClass
public class HttpUtils {
    public static final Gson GSON = new Gson();

    public static final MediaType MEDIA_TYPE_STREAM = MediaType.parse("application/octet-stream");
    public static final MediaType MEDIA_TYPE_TEXT = MediaType.parse("application/octet-stream");

    public static final ExecutorService IO_WORKER = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() * 2),
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("IO Worker-%d").build()
    );

    public static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(10))
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(30))
            .writeTimeout(Duration.ofSeconds(30))
            .followRedirects(true)
            .followSslRedirects(true)
            .dispatcher(new Dispatcher(IO_WORKER))
            .addInterceptor(chain -> {
                Request request = chain.request();
                try {
                    Response response = chain.proceed(request);

                    if (response.isSuccessful()) {
                        return response;
                    } else {
                        // Response is not successful (code is not 2xx)
                        throw new HttpException(request.method(),
                                request.url(), response.code(), response.body().string());
                    }
                } catch (IOException e) {
                    // Failed to request
                    Demise.LOGGER.error("Failed to execute request {} {}", request.method(), request.url(), e);
                    throw e;
                }
            })
            .build();

    public static CompletableFuture<Response> send(@NotNull Consumer<Request.Builder> block) {
        Request.Builder builder = new Request.Builder();
        block.accept(builder);
        return enqueueFuture(CLIENT.newCall(builder.build()));
    }

    public static Response sendSync(@NotNull Consumer<Request.Builder> block) throws IOException {
        Request.Builder builder = new Request.Builder();
        block.accept(builder);
        return CLIENT.newCall(builder.build()).execute();
    }

    public static @NotNull CompletableFuture<Response> enqueueFuture(@NotNull Call call) {
        var callback = new CompletableFutureCallback();
        call.enqueue(callback);
        return callback.future;
    }

    private static final class CompletableFutureCallback implements Callback {
        public final @NotNull CompletableFuture<Response> future = new CompletableFuture<>();

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) {
            future.complete(response);
        }

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            future.completeExceptionally(e);
        }
    }

    public static <T> @NotNull Function<Response, T> bodyGson(@NotNull TypeToken<T> typeToken) {
        return bodyGson(typeToken.getType());
    }

    public static <T> @NotNull Function<Response, T> bodyGson(@NotNull Class<T> type) {
        return bodyGson((Type) type);
    }

    private static <T> @NotNull Function<Response, T> bodyGson(@NotNull Type type) {
        return response -> {
            try (Reader reader = response.body().charStream()) {
                return GSON.fromJson(reader, type);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        };
    }

    public static @NotNull Function<Response, String> bodyString() {
        return response -> {
            try {
                return response.body().string();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        };
    }

}
