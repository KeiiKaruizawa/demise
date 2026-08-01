package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventPriority;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.misc.AFKHandler;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@ModuleInfo(name = "AFKGui", description = "Renders a lock screen-looking gui when afk.", legit = true)
public class AFKGui extends Module {
    private final TimerUtil transitionTimer = new TimerUtil();
    private int bgAlpha;
    private final TimerUtil textTimer = new TimerUtil();
    private final TimerUtil textAlphaTimer = new TimerUtil();
    private int textAlpha;
    private long cachedAFKTicks;

    @EventTarget
    public void onEnable() {
        Demise.INSTANCE.getModuleManager().getModule(AFKHandler.class).setEnabled(true);
    }

    @EventTarget
    // need high event priority so the overlay gets rendered above (almost) everything
    @EventPriority(100)
    public void onRender2D(Render2DEvent e) {
        AFKHandler afkHandler = Demise.INSTANCE.getModuleManager().getModule(AFKHandler.class);

        if (afkHandler.isAFK()) {
            if (transitionTimer.hasTimeElapsed(10)) {
                bgAlpha = Math.min(bgAlpha + 1, 255);
                transitionTimer.reset();
            }

            if (textTimer.hasTimeElapsed(4000)) {
                if (textAlphaTimer.hasTimeElapsed(10)) {
                    textAlpha = Math.min(textAlpha + 1, 150);
                    textAlphaTimer.reset();
                }
            }
        } else {
            bgAlpha = Math.max(bgAlpha - 1, 0);
            textAlpha = Math.max(textAlpha - 1, 0);
            textTimer.reset();
        }

        if (afkHandler.afkTicks / 20 > 0) {
            cachedAFKTicks = afkHandler.afkTicks;
        }

        ScaledResolution sr = new ScaledResolution(mc);

        if (bgAlpha > 0) {
            RenderUtils.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, bgAlpha).getRGB());
        }

        if (textAlpha > 0) {
            Fonts.urbanist.get(87).drawCenteredString(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), sr.getScaledWidth() / 2f, 80, new Color(255, 255, 255, textAlpha).getRGB());
            Fonts.interRegular.get(18).drawCenteredString("AFK for " + (cachedAFKTicks / 20) + " seconds", sr.getScaledWidth() / 2f, 132, new Color(200, 200, 200, textAlpha).getRGB());
        }
    }
}
