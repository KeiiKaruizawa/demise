package gr.larryngton.demise.utils.sound;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.visual.Interface;
import gr.larryngton.demise.gui.notification.NotificationType;
import gr.larryngton.demise.utils.InstanceAccess;
import lombok.experimental.UtilityClass;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

@UtilityClass
public class SoundUtil implements InstanceAccess {
    private int ticksExisted;
    private NotificationType prevNotificationType;

    public void notifSound(NotificationType type) {
        if (!Demise.INSTANCE.getClientSettings().notificationSounds.get()) {
            return;
        }

        boolean checkTicksExisted = mc.thePlayer != null;

        if (!checkTicksExisted || (prevNotificationType != type || ticksExisted != mc.thePlayer.ticksExisted)) {
            playSound("demise." + type.name().toLowerCase());
        }

        if (checkTicksExisted) {
            ticksExisted = mc.thePlayer.ticksExisted;
        }
        prevNotificationType = type;
    }

    public void playSound(String sound) {
        playSound(sound, 1, 1);
    }

    public void playSound(String sound, float volume, float pitch) {
        if (mc.theWorld != null) {
            mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, sound, volume, pitch, false);
        } else {
            mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(sound), 1));
        }
    }
}