package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.player.PlayerUtils;
import gr.larryngton.demise.utils.player.handlers.clicking.ClickHandler;
import gr.larryngton.demise.utils.player.trackers.PingTracker;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import net.minecraft.util.Vec3;

@ModuleInfo(name = "BlockHit", description = "Automatically blockhits in order to reduce incoming dmg.")
public class BlockHit extends Module {
    private final SliderValue blockRange = new SliderValue("Block range", 2, 0.1f, 8, 0.1f, this);
    private final BoolValue onPatternSkip = new BoolValue("On click pattern skip", false, this);
    private final BoolValue furtherSkip = new BoolValue("Further skip", false, this, onPatternSkip::get);
    private final BoolValue switchBlock = new BoolValue("Switch block", false, this, () -> !onPatternSkip.get());

    private boolean isBlocking;
    private EntityLivingBase target;
    private final TimerUtil lastHurtTimer = new TimerUtil();

    @EventTarget
    public void onDisable() {
        target = null;
    }

    @EventTarget
    public void onGame(GameEvent e) {
        target = PlayerUtils.getTarget(blockRange.get() + 3);

        if (shouldBlock()) {
            boolean extraCheck = !switchBlock.get() || mc.thePlayer.ticksExisted % 2 == 0;

            if (onPatternSkip.get() && ClickHandler.isInitialized()) {
                Integer currentClick = ClickHandler.getClickPattern().get(0);
                Integer nextClick = ClickHandler.getClickPattern().get(1);

                if (!furtherSkip.get()) {
                    nextClick = 0;
                }

                if (currentClick == null || nextClick == null) {
                    extraCheck = true;
                } else {
                    extraCheck = currentClick == 0;

                    if (currentClick == 0 && nextClick == 1) {
                        extraCheck = false;
                    }
                }
            }

            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), extraCheck);
            isBlocking = extraCheck;
        } else if (isBlocking) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            isBlocking = false;
        }

        if (mc.thePlayer.hurtTime != 0) {
            lastHurtTimer.reset();
        }
    }

    private boolean shouldBlock() {
        if (target == null) return false;

        Vec3 selfPrediction = mc.thePlayer.getPositionVector().add(mc.thePlayer.getMoveDeltaVector());
        Vec3 targetPrediction = target.getPositionVector().add(target.getMoveDeltaVector());

        boolean rangeCheck = PlayerUtils.getCustomDistanceToEntityBox(selfPrediction.add(0, mc.thePlayer.getEyeHeight(), 0), targetPrediction) <= 3 &&
                PlayerUtils.getCustomDistanceToEntityBox(target.getPositionEyes(1), mc.thePlayer) <= 3;

        return PlayerUtils.getDistanceToEntityBox(target) <= blockRange.get() &&
                mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword &&
                rangeCheck &&
                !lastHurtTimer.hasTimeElapsed(750) &&
                PlayerUtils.hittableBy(target) &&
                mc.thePlayer.hurtTime <= Demise.INSTANCE.getPingTracker().getTickPing() + 1;
    }
}