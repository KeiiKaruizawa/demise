package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.render.shader.impl.MenuBackground;
import gr.larryngton.demise.utils.render.video.VideoPlayer;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.bytedeco.javacv.FrameGrabber;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

@ModuleInfo(name = "ADHD", description = "Waiter! Waiter! More stimulation and dopamine please!")
public class ADHD extends Module {

    private static final String[] VIDEO_NAMES = {"SubwaySurfers", "Slime", "Butter"};
    private static final String[] POSITION_NAMES = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};

    public final ModeValue video = new ModeValue("Video", VIDEO_NAMES, "SubwaySurfers", this);
    private final ModeValue position = new ModeValue("Position", POSITION_NAMES, "Bottom Right", this);
    private final SliderValue width = new SliderValue("Width", 100, 100, 800, this);
    private final SliderValue height = new SliderValue("Height", 170, 60, 450, this);
    private final SliderValue padding = new SliderValue("Padding", 10, 0, 100, this);

    private String loadedVideo;

    @Override
    public void onEnable() {
        loadVideo(video.get());
    }

    @Override
    public void onDisable() {
        try {
            VideoPlayer.stop();
        } catch (Exception ignored) {
        }
        loadedVideo = null;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!video.get().equals(loadedVideo)) {
            loadVideo(video.get());
        }

        int screenWidth = event.scaledResolution().getScaledWidth();
        int screenHeight = event.scaledResolution().getScaledHeight();

        int w = (int) width.get();
        int h = (int) height.get();
        int pad = (int) padding.get();

        int left;
        int top;

        switch (position.get()) {
            case "Top Left" -> {
                left = pad;
                top = pad;
            }
            case "Top Right" -> {
                left = screenWidth - w - pad;
                top = pad;
            }
            case "Bottom Left" -> {
                left = pad;
                top = screenHeight - h - pad;
            }
            default -> {
                left = screenWidth - w - pad;
                top = screenHeight - h - pad;
            }
        }

        try {
            VideoPlayer.render(left, top, left + w, top + h);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    private void loadVideo(String name) {
        try {
            VideoPlayer.stop();
        } catch (Exception ignored) {
        }

        String fileName = name.toLowerCase().replace(" ", "") + ".mp4";
        File file = new File(mc.mcDataDir, "demise/brainrot/" + fileName);

        if (!file.exists()) {
            unpackFile(file, "assets/minecraft/demise/img/brainrot/" + fileName);
        }

        try {
            VideoPlayer.init(file);
            loadedVideo = name;
        } catch (Exception e) {
            Demise.LOGGER.error("Failed to start ADHD video playback", e);
        }
    }

    @SneakyThrows
    private void unpackFile(File file, String resourcePath) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.copy(Objects.requireNonNull(
                    MenuBackground.class.getClassLoader().getResourceAsStream(resourcePath)), fos);
        }
    }

    // I coded this shit at like 3 in the morning and haven't slept in over a day fuck my life and im so fucking tired
    // I should be asleep and this is what im doing its okay though
}