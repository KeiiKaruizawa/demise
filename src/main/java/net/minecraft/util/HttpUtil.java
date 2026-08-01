package net.minecraft.util;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import gr.larryngton.demise.utils.api.HttpUtils;
import net.minecraft.server.MinecraftServer;
import okhttp3.*;
import okio.Buffer;
import okio.Okio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

public class HttpUtil {
    private static final Logger logger = LogManager.getLogger();

    public static void postMap(URL url, Map<String, Object> formData, boolean skipLoggingErrors) {
        FormBody.Builder formBuilder = new FormBody.Builder();

        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            if (entry.getValue() != null) {
                formBuilder.add(entry.getKey(), entry.getValue().toString());
            }
        }

        Request request = new Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .build();

        OkHttpClient okHttpClient = HttpUtils.CLIENT.newBuilder()
                .proxy(Optional.ofNullable(MinecraftServer.getServer()).map(MinecraftServer::getServerProxy).orElse(Proxy.NO_PROXY))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            response.body().source().readAll(Okio.blackhole());
        } catch (Exception exception) {
            if (!skipLoggingErrors) {
                logger.error("Could not post to {}", url, exception);
            }
        }
    }

    public static ListenableFuture<Object> downloadResourcePack(
            final File saveFile,
            final String packUrl,
            final Map<String, String> headers,
            final int maxSize,
            final IProgressUpdate progressUpdate,
            final Proxy proxy
    ) {
        ListenableFutureTask<Object> task = ListenableFutureTask.create(() -> {
            OkHttpClient client = HttpUtils.CLIENT.newBuilder().proxy(proxy).build();
            Request.Builder requestBuilder = new Request.Builder().url(packUrl);

            float headerProgress = 0f;
            float totalHeaders = headers.size();

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());

                if (progressUpdate != null) {
                    headerProgress++;
                    progressUpdate.setLoadingProgress((int) (headerProgress / totalHeaders * 100.0f));
                }
            }

            if (progressUpdate != null) {
                progressUpdate.resetProgressAndMessage("Downloading Resource Pack");
                progressUpdate.displayLoadingString("Sending Request...");
            }

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                ResponseBody body = response.body();
                long contentLength = body.contentLength();

                if (contentLength == 0) {
                    throw new IOException("Empty response body");
                }

                float downloaded = 0f;

                if (progressUpdate != null) {
                    progressUpdate.displayLoadingString(String.format("Downloading file (%.2f MB)...", contentLength / 1_000_000f));
                }

                if (saveFile.exists()) {
                    long existingSize = saveFile.length();
                    if (existingSize == contentLength) {
                        if (progressUpdate != null) {
                            progressUpdate.setDoneWorking();
                        }
                        return null;
                    }
                    saveFile.delete();
                } else {
                    File parent = saveFile.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }
                }

                try (var in = body.source();
                     var out = Okio.sink(saveFile);
                     var buffer = new Buffer();
                ) {
                    long bytesRead;

                    while ((bytesRead = in.read(buffer, 8192)) != -1) {
                        downloaded += bytesRead;

                        if (progressUpdate != null && contentLength > 0) {
                            progressUpdate.setLoadingProgress((int) (downloaded / contentLength * 100.0f));
                        }

                        if (maxSize > 0 && downloaded > maxSize) {
                            throw new IOException("Filesize exceeds max allowed: " + downloaded + " > " + maxSize);
                        }

                        if (Thread.interrupted()) {
                            throw new InterruptedException("Download interrupted");
                        }

                        out.write(buffer, bytesRead);
                    }

                    if (progressUpdate != null) {
                        progressUpdate.setDoneWorking();
                    }

                } catch (Throwable t) {
                    if (progressUpdate != null) {
                        progressUpdate.setDoneWorking();
                    }
                    throw t;
                }
            }

            return null;
        });

        HttpUtils.IO_WORKER.submit(task);
        return task;
    }

    public static int getSuitableLanPort() throws IOException {
        ServerSocket serversocket = null;
        int i = -1;

        try {
            serversocket = new ServerSocket(0);
            i = serversocket.getLocalPort();
        } finally {
            try {
                if (serversocket != null) {
                    serversocket.close();
                }
            } catch (IOException ignored) {
            }
        }

        return i;
    }

}
