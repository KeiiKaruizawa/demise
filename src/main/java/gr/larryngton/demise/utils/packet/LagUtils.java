package gr.larryngton.demise.utils.packet;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.WorldChangeEvent;
import gr.larryngton.demise.events.impl.packet.PacketEvent;
import gr.larryngton.demise.events.impl.packet.PacketReleaseEvent;
import gr.larryngton.demise.events.impl.player.MotionEvent;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.features.modules.impl.exploit.PingSpoof;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.math.TimerUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class LagUtils implements InstanceAccess {
    private static final long DEFAULT_TIMER_DELAY = 100L;
    public static final long BLINK_DELAY = 9999999L;

    public enum PacketType {
        // yes, I do follow the max line length
        REGULAR(new Class[]{C0FPacketConfirmTransaction.class, C00PacketKeepAlive.class, S1CPacketEntityMetadata.class}),
        VELOCITY(new Class[]{S12PacketEntityVelocity.class, S27PacketExplosion.class}),
        TELEPORTS(new Class[]{S08PacketPlayerPosLook.class, S39PacketPlayerAbilities.class, S09PacketHeldItemChange.class}),
        PLAYERS(new Class[]{S13PacketDestroyEntities.class, S14PacketEntity.class, S14PacketEntity.S16PacketEntityLook.class, S14PacketEntity.S15PacketEntityRelMove.class, S14PacketEntity.S17PacketEntityLookMove.class, S18PacketEntityTeleport.class, S20PacketEntityProperties.class, S19PacketEntityHeadLook.class}),
        ACTION(new Class[]{C02PacketUseEntity.class, C0DPacketCloseWindow.class, C0EPacketClickWindow.class, C0CPacketInput.class, C0BPacketEntityAction.class, C08PacketPlayerBlockPlacement.class, C07PacketPlayerDigging.class, C09PacketHeldItemChange.class, C13PacketPlayerAbilities.class, C15PacketClientSettings.class, C16PacketClientStatus.class, C17PacketCustomPayload.class, C18PacketSpectate.class, C19PacketResourcePackStatus.class, C0APacketAnimation.class}),
        MOVEMENT(new Class[]{C03PacketPlayer.class, C03PacketPlayer.C04PacketPlayerPosition.class, C03PacketPlayer.C05PacketPlayerLook.class, C03PacketPlayer.C06PacketPlayerPosLook.class});

        private final Class<?>[] packetClasses;
        @Getter
        @Setter
        private boolean enabled;

        PacketType(Class<?>[] packetClasses) {
            this.packetClasses = packetClasses;
        }

        public boolean containsPacket(Class<?> packetClass) {
            return Arrays.asList(packetClasses).contains(packetClass);
        }
    }

    public static final ConcurrentLinkedQueue<TimedPacket> packets = new ConcurrentLinkedQueue<>();
    private static final TimerUtil enabledTimer = new TimerUtil();

    private static boolean enabled;
    private static long delayAmount;
    private static boolean post;

    @EventTarget
    public void onPacketC(PacketEvent event) {
        if (event.getState() == PacketEvent.State.OUTGOING) {
            event.setCancelled(processPacket(event.getPacket(), event).isCancelled());
        }
    }

    @EventTarget
    public void onPacketS(PacketEvent event) {
        if (event.getState() == PacketEvent.State.INCOMING) {
            event.setCancelled(processPacket(event.getPacket(), event).isCancelled());
        }
    }

    private PacketEvent processPacket(Packet<?> packet, PacketEvent event) {
        if (!event.isCancelled() && enabled && shouldHandlePacket(packet)) {
            if ((packet instanceof S00PacketKeepAlive || packet instanceof C00PacketKeepAlive) && Demise.INSTANCE.getModuleManager().getModule(PingSpoof.class).isEnabled()) {
                return event;
            }

            event.setCancelled(true);
            packets.add(new TimedPacket(packet));
        }
        return event;
    }

    private boolean shouldHandlePacket(Packet<?> packet) {
        return Arrays.stream(PacketType.values()).anyMatch(type -> type.isEnabled() && type.containsPacket(packet.getClass()));
    }

    public static void dispatch() {
        if (!packets.isEmpty()) {
            boolean wasEnabled = enabled;
            enabled = false;
            packets.forEach(timedPacket -> {
                PacketReleaseEvent ev = new PacketReleaseEvent(timedPacket);
                Demise.INSTANCE.getEventManager().call(ev);

                if (!ev.isCancelled()) {
                    PacketUtils.queue(timedPacket.getPacket());
                }
            });
            enabled = wasEnabled;
            packets.clear();
        }
    }

    public static void disable() {
        enabled = false;
        enabledTimer.setTime(enabledTimer.getTime() - BLINK_DELAY);
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        disable();
        dispatch();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!post) {
            sendPackets();
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre() || !post) {
            return;
        }
        sendPackets();
    }

    private void sendPackets() {
        if (!(enabled = !enabledTimer.hasTimeElapsed(DEFAULT_TIMER_DELAY) && !(mc.currentScreen instanceof GuiDownloadTerrain))) {
            dispatch();
            return;
        }

        enabled = false;
        releaseTimedOutPackets();
        enabled = true;
    }

    private void releaseTimedOutPackets() {
        long now = System.currentTimeMillis();

        TimedPacket packet;
        while ((packet = packets.peek()) != null) {
            if (packet.getMillis() + delayAmount > now) break;

            PacketReleaseEvent ev = new PacketReleaseEvent(packet);
            Demise.INSTANCE.getEventManager().call(ev);

            if (!ev.isCancelled()) {
                PacketUtils.queue(packet.getPacket());
            }
            packets.poll();
        }
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players, boolean action) {
        spoof(amount, regular, velocity, teleports, players, action, false);
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players, boolean action, boolean movement) {
        spoof(amount, regular, velocity, teleports, players, action, movement, true);
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players, boolean action, boolean movement, boolean post) {
        enabledTimer.reset();
        PacketType.REGULAR.setEnabled(regular);
        PacketType.VELOCITY.setEnabled(velocity);
        PacketType.TELEPORTS.setEnabled(teleports);
        PacketType.PLAYERS.setEnabled(players);
        PacketType.ACTION.setEnabled(action);
        PacketType.MOVEMENT.setEnabled(movement);
        LagUtils.post = post;
        LagUtils.delayAmount = amount;
    }
}