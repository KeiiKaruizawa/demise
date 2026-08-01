package gr.larryngton.demise.features.modules.impl.legit;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.player.SprintUpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.DoubleSliderValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.TimerUtil;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.player.MoveUtils;
import gr.larryngton.demise.utils.player.PlayerUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;

@ModuleInfo(name = "SprintReset", description = "Fixes sprint de-syncs upon attacking.")
public class SprintReset extends Module {
    private final ModeValue mode = new ModeValue("Reset mode", new String[]{"Silent", "WTap", "Sneak"}, "Silent", this);
    private final DoubleSliderValue reSprintDelay = new DoubleSliderValue("Re-sprint delay", 0, 0, 0, 9, 1, this);
    private final BoolValue notWhenHurt = new BoolValue("Not when hurt", false, this);
    private final SliderValue minTargetTicksFromGround = new SliderValue("Min target ticks from ground", 5, 0, 12, 1, this);

    private final TimerUtil attackTimer = new TimerUtil();
    private EntityLivingBase target;
    private final TimerUtil reSprintTimer = new TimerUtil();
    private boolean resyncNeeded;
    private int nextSprintTime;
    private int selfHurtTimeOnAttack;

    @EventTarget
    public void onDisable() {
        target = null;
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (e.getTargetEntity() == target) {
            attackTimer.reset();
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        setTag(mode.get());

        if (e.isPre()) {
            target = PlayerUtils.getTarget(4.5);

            if (target != null &&
                    target.hurtTime == 10 &&
                    !attackTimer.hasTimeElapsed(200 + mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime()) &&
                    e.isSprinting() &&
                    mc.thePlayer.serverSprintState) {
                selfHurtTimeOnAttack = mc.thePlayer.hurtTime;
                resyncNeeded = true;
                nextSprintTime = (int) reSprintDelay.random();
                reSprintTimer.reset();
            }
        }
    }

    @EventTarget
    public void onSprintUpdate(SprintUpdateEvent e) {
        if (e.getState() == SprintUpdateEvent.State.PRE && mode.is("Silent")) {
            if (shouldReset()) {
                e.setCancelled(true);
                mc.thePlayer.setSprinting(false);
//                ChatUtils.sendMessageClient("reset in " + nextSprintTime + " ticks");
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (!mode.is("Silent") && shouldReset()) {
            switch (mode.get()) {
                case "WTap":
                    e.setForward(0);
                    e.setStrafe(0);
                    break;
                case "Sneak":
                    e.setSneaking(true);
                    break;
            }
        }
    }

    private boolean shouldReset() {
        if (resyncNeeded && reSprintTimer.hasTimeElapsed(nextSprintTime * 50L)) {
            resyncNeeded = false;

            boolean hurtTime = selfHurtTimeOnAttack != 0 && mc.thePlayer.hurtTime <= 2;
            boolean crit = mc.thePlayer.fallDistance > 0.0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && !mc.thePlayer.isPotionActive(Potion.blindness) && mc.thePlayer.ridingEntity == null;
            boolean strictHurtTime = mc.thePlayer.hurtTime != 0 && notWhenHurt.get();
            boolean targetAir = mc.thePlayer.hurtTime != 0 && MoveUtils.getTicksForGround(target) < minTargetTicksFromGround.getInt();

            if (hurtTime || crit || strictHurtTime || targetAir) {
                String str = "prevented reset";

                if (hurtTime) {
                    str += ", hurtTime";
                }

                if (strictHurtTime) {
                    str += ", strict hurtTime";
                }

                if (crit) {
                    str += ", crit";
                }

                if (targetAir) {
                    str += ", target air";
                }

                ChatUtils.sendMessageClient(str);
                return false;
            }

            return true;
        }

        return false;
    }
}