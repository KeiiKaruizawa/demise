package gr.larryngton.demise.gui.notification;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.misc.Test;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.animations.Animation;
import gr.larryngton.demise.utils.animations.Translate;
import gr.larryngton.demise.utils.animations.impl.EaseOutSine;
import gr.larryngton.demise.utils.math.TimerUtil;
import lombok.Getter;
import net.minecraft.client.gui.ScaledResolution;

@Getter
public class Notification implements InstanceAccess {
    private final NotificationType notificationType;
    private final String title, description;
    private final float time;
    private final TimerUtil timerUtil;
    private final Animation animation;
    private final Translate translate;

    public Notification(NotificationType type, String title, String description, ScaledResolution sr) {
        this(type, title, description, Demise.INSTANCE.getNotificationManager().getToggleTime(), sr);
    }

    public Notification(NotificationType type, String title, String description, float time, ScaledResolution sr) {
        this.title = title;
        this.description = description;
        this.time = (long) (time * 1000);
        timerUtil = new TimerUtil();
        this.notificationType = type;
        this.animation = new EaseOutSine(250, 1);
        this.translate = new Translate(sr.getScaledWidth() - this.getWidth(), sr.getScaledHeight() - getHeight());
    }

    public double getWidth() {
        return Math.max(Fonts.interSemiBold.get(17).getStringWidth(getTitle()), Fonts.interRegular.get(17).getStringWidth(getDescription())) + 9;
    }

    public double getHeight() {
        return 24.5;
    }
}