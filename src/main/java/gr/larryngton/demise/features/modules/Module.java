package gr.larryngton.demise.features.modules;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.modules.impl.client.ClientSettings;
import gr.larryngton.demise.features.values.Value;
import gr.larryngton.demise.gui.notification.NotificationType;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.animations.Translate;
import gr.larryngton.demise.utils.animations.impl.EaseInOutQuad;
import gr.larryngton.demise.utils.packet.PacketUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class Module implements InstanceAccess {
    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    @Setter
    private int keyBind;
    @Getter
    private String tag = "";
    @Getter
    private final List<Value> values = new ArrayList<>();
    @Getter
    @Setter
    private boolean hidden;
    private boolean state;
    @Getter
    @Setter
    private boolean expanded;
    @Getter
    private final EaseInOutQuad animation = new EaseInOutQuad(175, 1);
    @Getter
    private final Translate translate = new Translate(0.0, 0.0);
    @Getter
    private final boolean isLegit;

    protected Module() {
        if (this.getClass() != ClientSettings.class) {
            ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);
            Objects.requireNonNull(moduleInfo, "ModuleInfo annotation is missing on " + getClass().getName());
            this.name = moduleInfo.name();
            this.description = moduleInfo.description();
            this.keyBind = moduleInfo.key();
            this.isLegit = moduleInfo.legit();
        } else {
            this.name = "ClientSettings";
            this.description = "";
            this.keyBind = 0;
            this.isLegit = true;
        }
    }

    public void setTag(String tag) {
        if (Demise.INSTANCE.getClientSettings().funy.get() && !Objects.equals(tag, "")) {
            this.tag = "§7 chatgpt pro billionaire bypass india vs pakistan";
            return;
        }

        if (tag == null || tag.isEmpty()) {
            this.tag = "";
            return;
        }

        this.tag = " §7" + tag;
    }

    public boolean isEnabled() {
        return state;
    }

    public boolean isDisabled() {
        return !state;
    }

    public <M extends Module> boolean isEnabled(Class<M> module) {
        Module mod = Demise.INSTANCE.getModuleManager().getModule(module);
        return mod != null && mod.isEnabled();
    }

    public <M extends Module> boolean isDisabled(Class<M> module) {
        return !isEnabled(module);
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }

    public void setEnabled(boolean enabled) {
        if (this.state != enabled) {
            this.state = enabled;
            if (enabled) {
                enable();
            } else {
                disable();
            }
        }
    }

    private void enable() {
        Demise.INSTANCE.getEventManager().register(this);
        try {
            onEnable();
            if (!getName().equals("ClickGUI")) {
                Demise.INSTANCE.getNotificationManager().post(NotificationType.ENABLE, "Enabled", "Module " + getName(), new ScaledResolution(mc));
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void disable() {
        Demise.INSTANCE.getEventManager().unregister(this);
        try {
            onDisable();
            if (!getName().equals("ClickGUI")) {
                Demise.INSTANCE.getNotificationManager().post(NotificationType.DISABLE, "Disabled", "Module " + getName(), new ScaledResolution(mc));
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) {
        if (mc.thePlayer != null) {
            e.printStackTrace();
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public <M extends Module> M getModule(Class<M> clazz) {
        return Demise.INSTANCE.getModuleManager().getModule(clazz);
    }

    public void addValues(Value... settings) {
        values.addAll(Arrays.asList(settings));
    }

    public void addValue(Value value) {
        addValues(value);
    }

    public void sendPacket(Packet packet) {
        PacketUtils.sendPacket(packet);
    }

    public void sendPacketNoEvent(Packet packet) {
        PacketUtils.sendPacketNoEvent(packet);
    }

    public Value getValue(String valueName) {
        return values.stream()
                .filter(value -> value.getName().equalsIgnoreCase(valueName))
                .findFirst()
                .orElse(null);
    }
}