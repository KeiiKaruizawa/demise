package gr.larryngton.demise.gui.widget.impl;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.misc.Debug;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.gui.widget.Widget;
import gr.larryngton.demise.utils.player.handlers.clicking.ClickHandler;
import gr.larryngton.demise.utils.player.handlers.clicking.ScaffoldClickHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.predictions.PredictionEngine;

public class DebugWidget extends Widget {
    private final Debug debug = Demise.INSTANCE.getModuleManager().getModule(Debug.class);

    public DebugWidget() {
        super("Debug");
    }

    @Override
    public void render() {
        height = 0;

        if (debug.clickHandler.get()) {
            StringBuilder str = new StringBuilder();

            for (Integer i : ClickHandler.getClickPattern()) {
                str.append(i);
            }

            Fonts.interRegular.get(18).drawStringWithShadow(str.toString(), renderX - Fonts.interRegular.get(18).getStringWidth(str.toString()) + width, renderY + height, -1);
            height += Fonts.interRegular.get(18).getHeight() + 2;
        }

        if (debug.clickHandler.get()) {
            StringBuilder str = new StringBuilder();

            for (Integer i : ScaffoldClickHandler.getClickPattern()) {
                str.append(i);
            }

            Fonts.interRegular.get(18).drawStringWithShadow(str.toString(), renderX - Fonts.interRegular.get(18).getStringWidth(str.toString()) + width, renderY + height, -1);
            height += Fonts.interRegular.get(18).getHeight() + 2;
        }

        if (debug.simulatedReactionTime.get() && !PredictionEngine.lastReset) {
            double averageMotion = 0;
            for (Double motion : PredictionEngine.previousTargetMotions) {
                averageMotion += motion;
            }
            averageMotion /= PredictionEngine.previousTargetMotions.size();

            String str = "Current reaction time: " + PredictionEngine.currentReactionTime + " | Average motion: " + averageMotion;

            Fonts.interRegular.get(18).drawStringWithShadow(str, renderX - Fonts.interRegular.get(18).getStringWidth(str) + width, renderY + height, -1);
            height += Fonts.interRegular.get(18).getHeight() + 2;
        }

        if (debug.attackRange.get()) {
            String str = "Attack range: " + debug.range;
            Fonts.interRegular.get(18).drawStringWithShadow(str, renderX - Fonts.interRegular.get(18).getStringWidth(str) + width, renderY + height, -1);
            height += Fonts.interRegular.get(18).getHeight() + 2;
        }
    }

    @Override
    public boolean shouldRender() {
        return debug.isEnabled();
    }
}
