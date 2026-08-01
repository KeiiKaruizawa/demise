package gr.larryngton.demise.utils.render.video;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.render.shader.impl.MenuBackground;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

public class VideoComponent implements InstanceAccess {
    private static final String DEFAULT_VIDEO = "background.mp4";
    private static File currentVideoFile;

    public static void ensureVideoExists() {
        currentVideoFile = new File(mc.mcDataDir, DEFAULT_VIDEO);
        if (!currentVideoFile.exists()) {
            unpackFile(currentVideoFile);
        }
    }

    public static void startVideoPlayback() {
        try {
            if (currentVideoFile != null && currentVideoFile.exists()) {
                VideoPlayer.init(currentVideoFile);
            }
        } catch (Exception e) {
            Demise.LOGGER.error("Failed to start video playback", e);
        }
    }

    @SneakyThrows
    private static void unpackFile(File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.copy(Objects.requireNonNull(
                    MenuBackground.class.getClassLoader().getResourceAsStream("assets/minecraft/demise/img/YaeMiko.mp4")), fos);
        }
    }
}
