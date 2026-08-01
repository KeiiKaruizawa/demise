package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.player.trackers.LagTracker;

import java.awt.*;

@ModuleInfo(name = "LagDetector")
public class LagDetector extends Module {
    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (Demise.INSTANCE.getLagTracker().isLagging()) {
            Fonts.interRegular.get(18).drawCenteredStringWithShadow(LagTracker.timer.getTime() + "ms", e.scaledResolution().getScaledWidth() / 2F, 75, Color.RED.getRGB());
        }
    }
}