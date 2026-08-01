package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.CancellableEvent;
import gr.larryngton.demise.events.impl.Event;
import gr.larryngton.demise.events.impl.render.PostRender2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.settings.GameSettings;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "FastFPS", description = "Updates fps faster.")
public class FastFPS extends Module {
    private final SliderValue updateTime = new SliderValue("Update time", 500, 50, 1000, 25, this);

    private long lastTime = System.currentTimeMillis();
    private final List<Double> frameTimes = new ArrayList<>();
    private final TimerUtil updateTimer = new TimerUtil();
    private final TimerUtil updateTimer1Sec = new TimerUtil();

    @EventTarget
    public void onDisable() {
        frameTimes.clear();
    }

    @EventTarget
    public void onPostRender2D(PostRender2DEvent e) {
        frameTimes.add((double) (System.currentTimeMillis() - lastTime));
        lastTime = System.currentTimeMillis();
    }

    @EventTarget
    public void onPostTick(PostTickEvent e) {
        frameTimes.clear();
    }

    @EventTarget
    public void onFrameUpdate(FrameUpdateEvent e) {
        if (!frameTimes.isEmpty()) {
            e.setCancelled(true);

            if (updateTimer.hasTimeElapsed(updateTime.get())) {
                double avgFrameTime = 0;
                for (Double d : frameTimes) {
                    avgFrameTime += d;
                }
                avgFrameTime /= frameTimes.size();

                Minecraft.setDebugFPS((int) (1000 / avgFrameTime));
                mc.debug = String.format("%d fps (%d chunk update%s) T: %s%s%s%s%s", Minecraft.getDebugFPS(), RenderChunk.renderChunksUpdated, RenderChunk.renderChunksUpdated != 1 ? "s" : "", (float) mc.gameSettings.limitFramerate == GameSettings.Options.FRAMERATE_LIMIT.getValueMax() ? "inf" : Integer.valueOf(mc.gameSettings.limitFramerate), mc.gameSettings.enableVsync ? " vsync" : "", mc.gameSettings.fancyGraphics ? "" : " fast", mc.gameSettings.clouds == 0 ? "" : (mc.gameSettings.clouds == 1 ? " fast-clouds" : " fancy-clouds"), OpenGlHelper.useVbo() ? " vbo" : "");
                updateTimer.reset();
            }

            if (updateTimer1Sec.hasTimeElapsed(1000)) {
                RenderChunk.renderChunksUpdated = 0;
                updateTimer1Sec.reset();
            }
        }
    }

    // will only be used here anyway
    public static class FrameUpdateEvent extends CancellableEvent {
    }

    public static class PostTickEvent implements Event {
    }
}
