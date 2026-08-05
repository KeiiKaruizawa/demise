package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.AttackEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.ModeValue;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

@ModuleInfo(name = "HitSounds", description = "Renders a sound effect when attacking someone", legit = true)
public class HitSounds extends Module {
    private static final String[] HITSOUND_NAMES = new String[]{
            "Neverlose", "Pop", "Glass", "Fatality", "Skeet", "Primordial"
    };

    private final ModeValue hitsound = new ModeValue("Hit Sounds", HITSOUND_NAMES, "Neverlose", this);

    private ISound currentSound;

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (currentSound != null && mc.getSoundHandler().isSoundPlaying(currentSound)) {
            return;
        }

        currentSound = PositionedSoundRecord.create(new ResourceLocation(hitsound.get().toLowerCase()));
        mc.getSoundHandler().playSound(currentSound);
    }
}