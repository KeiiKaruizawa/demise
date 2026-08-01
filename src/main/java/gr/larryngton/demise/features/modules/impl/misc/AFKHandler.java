package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.player.MoveUtils;
import org.lwjglx.opengl.Display;

@ModuleInfo(name = "AFKHandler", description = "Limits fps and disables shaders when you are AFK.")
public class AFKHandler extends Module {
    private final SliderValue limiterFPS = new SliderValue("Frame limit", 20, 1, 60, 1, this);

    // long instead of an integer because someone might afk for more than 2,147,483,647 ticks
    public long afkTicks;
    private int lastLimitFramerate;
    private boolean updatedCachedRate;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        afkTicks++;

        if (MoveUtils.isMoving() || ((mc.mouseHelper.deltaX != 0 || mc.mouseHelper.deltaY != 0) && mc.currentScreen == null)) {
            afkTicks = 0;
        }
    }

    @EventTarget
    public void onGame(GameEvent e) {
        if (isAFK()) {
            if (!updatedCachedRate) {
                lastLimitFramerate = mc.gameSettings.limitFramerate;
                updatedCachedRate = true;
            }

            mc.gameSettings.limitFramerate = limiterFPS.getInt();
        } else if (updatedCachedRate) {
            mc.gameSettings.limitFramerate = lastLimitFramerate;
            updatedCachedRate = false;
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        afkTicks = 0;
    }

    public boolean isAFK() {
        return afkTicks > 600 || !Display.isActive();
    }
}
