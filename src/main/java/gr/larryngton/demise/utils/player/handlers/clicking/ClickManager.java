package gr.larryngton.demise.utils.player.handlers.clicking;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.impl.legit.AutoClicker;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.InstanceAccess;
import lombok.Getter;
import net.minecraft.entity.EntityLivingBase;

@Getter
public class ClickManager implements InstanceAccess {
    ModeValue clickMode;
    final BoolValue ignoreItemUse;
    final SliderValue cps;
    final SliderValue maxSkipsInARow;
    BoolValue rayTrace;
    BoolValue failSwing;
    SliderValue swingRange;
    final BoolValue respectHitDelay;
    final SliderValue maxDelay;
    final BoolValue doubleClick;
    final SliderValue maxClickAmount;
    final SliderValue maxConcentrationDiff;
    final SliderValue consistency;
    final boolean isAutoClicker;
    final SliderValue perlinRandAdd;

    public ClickManager(Module module) {
        isAutoClicker = module instanceof AutoClicker;

        ignoreItemUse = new BoolValue("Ignore item use", true, module);

        if (!isAutoClicker) {
            clickMode = new ModeValue("Click mode", new String[]{"Legit", "Packet", "PlayerController"}, "Packet", module);
        }

        doubleClick = new BoolValue("Double click", false, module);
        maxClickAmount = new SliderValue("Max click amount", 6, 1, 10, 1, module, doubleClick::get);

        // the space in the cps string is here because changing option value types without renaming them kinda breaks my cfg system...
        cps = new SliderValue("CPS ", 12, 0, 40, 1, module);
        perlinRandAdd = new SliderValue("Perlin rand add", 1, 0, 5, 0.1F, module);

        maxSkipsInARow = new SliderValue("Max skips in a row", 2, 0, 5, 1, module);
        consistency = new SliderValue("Consistency", 0.5F, 0, 1, 0.01F, module);
        maxConcentrationDiff = new SliderValue("Max concentration diff", 2, 0, 20, 1, module);
        respectHitDelay = new BoolValue("Respect hit delay", false, module);
        maxDelay = new SliderValue("Max delay", 10, 1, 10, 1, module, respectHitDelay::get);

        if (!isAutoClicker) {
            rayTrace = new BoolValue("Raytrace", false, module);
            failSwing = new BoolValue("Fail swing", false, module);
            swingRange = new SliderValue("Swing range", 3.5F, 1, 8, 0.1F, module, failSwing::get);
        }
    }

    public void click(float attackRange, EntityLivingBase target) {
        ClickHandler.ClickMode mode = ClickHandler.ClickMode.valueOf(isAutoClicker ? "Legit" : clickMode.get());
        ClickHandler.initHandler(
                cps.getInt(),
                perlinRandAdd.get(),
                !isAutoClicker && rayTrace.get(),
                ignoreItemUse.get(),
                !isAutoClicker && failSwing.get(),
                respectHitDelay.get(),
                maxDelay.getInt(),
                attackRange,
                isAutoClicker ? 3 : swingRange.get(),
                doubleClick.get(),
                maxClickAmount.get(),
                maxSkipsInARow.getInt(),
                maxConcentrationDiff.getInt(),
                consistency.get(),
                mode,
                isAutoClicker ? mc.thePlayer : target
        );
    }
}