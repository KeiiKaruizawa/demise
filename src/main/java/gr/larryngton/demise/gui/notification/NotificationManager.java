package gr.larryngton.demise.gui.notification;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.animations.Animation;
import gr.larryngton.demise.utils.animations.Direction;
import gr.larryngton.demise.utils.render.RectUtils;
import gr.larryngton.demise.utils.render.RenderUtils;
import gr.larryngton.demise.utils.sound.SoundUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class NotificationManager implements InstanceAccess {
    private final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    @Setter
    private float toggleTime = 2;

    public void post(NotificationType type, String title, String description, ScaledResolution sr) {
        post(new Notification(type, title, description, sr));
    }

    public void post(NotificationType type, String title, String description, float time, ScaledResolution sr) {
        post(new Notification(type, title, description, time, sr));
    }

    public void post(NotificationType type, String title, ScaledResolution sr) {
        post(new Notification(type, title, title, sr));
    }

    private void post(Notification notification) {
        if (INSTANCE.getModuleManager().getModule(Interface.class).elements.isEnabled("Notifications")) {
            notifications.add(notification);
        }

        if (Demise.INSTANCE.getStartTimeLong() > 1000) {
            SoundUtil.notifSound(notification.getNotificationType());
        }
    }

    public void publish() {
        float offsetY = 0;

        for (Notification notification : getNotifications()) {
            Animation animation = notification.getAnimation();
            animation.setDirection(notification.getTimerUtil().hasTimeElapsed((long) notification.getTime()) ? Direction.BACKWARDS : Direction.FORWARDS);

            if (animation.finished(Direction.BACKWARDS)) {
                getNotifications().remove(notification);
            } else {
                Interface anInterface = Demise.INSTANCE.getModuleManager().getModule(Interface.class);
                double output = animation.getOutput();

                float width = (float) notification.getWidth();
                float height = (float) notification.getHeight();

                float watermarkOffset = getWatermarkOffset(anInterface);
                float x = (float) (10 * output);
                float y = 12 + watermarkOffset + offsetY;

                RectUtils.drawRound(x, y, width, height, 7, new Color(anInterface.bgColor(), true));

                Color acc = new Color(anInterface.color());
                Color barCol = new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), (int) (255 * output));

                RenderUtils.scissor(x - 2.5F, y - 2.5F, 3, height + 5);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                RectUtils.drawRound(x, y, 20, height, 7, barCol);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);

                Fonts.interSemiBold.get(17).drawString(notification.getTitle(), x + 4, y + 5, new Color(255, 255, 255, 255).getRGB());
                Fonts.interRegular.get(17).drawString(notification.getDescription(), x + 4, y + 15, new Color(170, 170, 170).getRGB());

                Demise.INSTANCE.getShaderManager().addToAllQueue(() -> RectUtils.drawShaderRound(x, y, width, height, 7, Color.black));
                Demise.INSTANCE.getShaderManager().addToBloomQueue(() -> {
                    RenderUtils.scissor(x - 2.5F, y - 2.5F, 3, height + 5);
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    RectUtils.drawRound(x, y, 20, height, 7, barCol);
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                });

                offsetY += (float) ((height + 7) * output);
            }
        }
    }

    private float getWatermarkOffset(Interface anInterface) {
        if (!anInterface.elements.isEnabled("Watermark"))
            return 0;

        return switch (anInterface.watermarkMode.get()) {
            case "Text" -> Fonts.urbanist.get(38).getHeight() + 8;
            case "Blue archive" -> 64;
            case "Old" -> mc.fontRendererObj.FONT_HEIGHT + 8;
            case "Miko" -> 100;
            case "Modern" -> Fonts.interSemiBold.get(17).getHeight() + 16;
            default -> 0;
        };
    }
}
