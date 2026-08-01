package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.gui.mainmenu.GuiMainMenu;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

import java.util.function.Supplier;

@ModuleInfo(name = "Radio", description = "Plays the radio!")
public class Radio extends Module {
    private static final String[] RADIO_NAMES = new String[]{
            "Lofi", "NightCore"
    };

    private final ModeValue radio = new ModeValue("Radio", RADIO_NAMES, "Lofi", this);
    private final SliderValue volume = new SliderValue("Volume", 1f, 0f, 1f, 0.01f, this);

    private ISound currentSound;
    private String currentStation;

    public ISound getCurrentSound() {
        return currentSound;
    }

    @Override
    public void onEnable() {
        if (mc.theWorld != null) {
            play();
        }
    }

    @Override
    public void onDisable() {
        stop();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.currentScreen instanceof GuiMainMenu) {
            stop();
            currentStation = null;
            return;
        }

        if (mc.theWorld == null) {
            return;
        }

        boolean stationChanged = !radio.get().equals(currentStation);
        boolean soundDied = currentSound != null && !mc.getSoundHandler().isSoundPlaying(currentSound);

        if (stationChanged || soundDied) {
            play();
        }
    }

    private void play() {
        stop();

        currentStation = radio.get();

        try {
            currentSound = new LoopingSound(new ResourceLocation(currentStation.toLowerCase()), volume::get);
            mc.getSoundHandler().playSound(currentSound);
        } catch (Exception ex) {
            currentSound = null;
        }
    }

    private void stop() {
        if (currentSound != null) {
            try {
                mc.getSoundHandler().stopSound(currentSound);
            } catch (Exception ignored) {
            }
            currentSound = null;
        }
    }

    private static class LoopingSound extends MovingSound {
        private final Supplier<Float> volumeSupplier;

        LoopingSound(ResourceLocation location, Supplier<Float> volumeSupplier) {
            super(location);
            this.volumeSupplier = volumeSupplier;
            this.attenuationType = ISound.AttenuationType.NONE;
            this.repeat = true;
        }

        @Override
        public float getVolume() {
            return volumeSupplier.get();
        }

        @Override
        public void update() {
        }
    }
}