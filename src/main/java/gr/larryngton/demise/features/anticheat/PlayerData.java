package gr.larryngton.demise.features.anticheat;

import java.util.List;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.anticheat.impl.Check;
import gr.larryngton.demise.features.modules.impl.misc.AntiCheat;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.player.PlayerUtils;
import lombok.Getter;
import net.minecraft.block.BlockAir;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.Vec3;

@Getter
public class PlayerData implements InstanceAccess {
    private final EntityOtherPlayerMP player;
    private final List<Check> checks;
    private AntiCheat ac = Demise.INSTANCE.getModuleManager().getModule(AntiCheat.class);
    private int serverPosX;
    private int serverPosY;
    private int serverPosZ;
    private int ticksSinceLastTeleport;
    private int ticksSinceLastVelocity;
    private double x;
    private double y;
    private double z;
    private double lastX;
    private double lastY;
    private double lastZ;
    private double deltaX;
    private double deltaY;
    private double deltaZ;
    private double lastDeltaX;
    private double lastDeltaY;
    private double lastDeltaZ;
    private double speedXZ;
    private double lastSpeedXZ;
    private Vec3 groundPosition;
    private Vec3 lastGroundPosition;
    private double yaw;
    private double pitch;
    private boolean onGround;
    private boolean lastOnGround;
    private boolean isRoughlyGround;
    private boolean lastIsRoughlyGround;
    private boolean nearBlock;
    private boolean lastNearBlock;
    private long lastS14;
    private long timeSinceLastS14;

    public PlayerData(EntityOtherPlayerMP player) {
        this.player = player;
        serverPosX = player.serverPosX;
        serverPosY = player.serverPosY;
        serverPosZ = player.serverPosZ;
        checks = CheckManager.loadChecks(this);
    }

    public void onPacket(Packet<?> packet) {
        if (packet instanceof S14PacketEntity.S15PacketEntityRelMove || packet instanceof S14PacketEntity.S17PacketEntityLookMove) {
            S14PacketEntity s14 = (S14PacketEntity) packet;
            if (s14.getEntityId() == player.getEntityId()) {
                serverPosX += s14.getX();
                serverPosY += s14.getY();
                serverPosZ += s14.getZ();
                updatePosition();

                if (packet instanceof S14PacketEntity.S17PacketEntityLookMove) {
                    yaw = s14.func_149066_f();
                    pitch = s14.func_149063_g();
                }

                lastDeltaX = deltaX;
                lastDeltaY = deltaY;
                lastDeltaZ = deltaZ;

                if (lastX != 0) {
                    deltaX = x - lastX;
                }

                if (lastY != 0) {
                    deltaY = y - lastY;
                }

                if (lastZ != 0) {
                    deltaZ = z - lastZ;
                }

                lastSpeedXZ = speedXZ;
                speedXZ = Math.hypot(deltaX, deltaZ);

                lastOnGround = onGround;
                onGround = checkOffsets(0.5, -0.43);

                lastIsRoughlyGround = isRoughlyGround;
                isRoughlyGround = checkOffsets(0.5, -0.99);

                lastNearBlock = nearBlock;
                nearBlock = checkOffsets(1, -0.5);

                if (onGround) {
                    lastGroundPosition = groundPosition;
                    groundPosition = getPosition();
                }
            }
        }

        if (packet instanceof S18PacketEntityTeleport s18) {
            if (s18.getEntityId() == player.getEntityId()) {
                serverPosX = s18.getX();
                serverPosY = s18.getY();
                serverPosZ = s18.getZ();
                updatePosition();
                ticksSinceLastTeleport = 0;
            }
        }

        if (packet instanceof S12PacketEntityVelocity s12 && s12.getEntityID() == player.getEntityId()) {
            ticksSinceLastVelocity = 0;
        }

        timeSinceLastS14 = System.currentTimeMillis() - lastS14;
        if (speedXZ < 5 && mc.thePlayer.ticksExisted > 40 && !Demise.INSTANCE.getLagTracker().isLagging()) {
            if (ac == null) {
                ac = Demise.INSTANCE.getModuleManager().getModule(AntiCheat.class);
            }

            checks.forEach((check) -> {
                if (ac.checks.isEnabled(check.getName())) {
                    check.onPacket(packet);
                }
            });
        }

        if (packet instanceof S14PacketEntity s14 && s14.getEntityId() == player.getEntityId()) {
            lastS14 = System.currentTimeMillis();
        }
    }

    private boolean checkOffsets(double xzOffset, double yOffset) {
        return hasBlock(-xzOffset, yOffset, -xzOffset)
                || hasBlock(xzOffset, yOffset, -xzOffset)
                || hasBlock(xzOffset, yOffset, xzOffset)
                || hasBlock(-xzOffset, yOffset, xzOffset);
    }

    private boolean hasBlock(double x1, double y1, double z1) {
        return !(PlayerUtils.getBlock(x + x1, y + y1, z + z1) instanceof BlockAir);
    }

    private void updatePosition() {
        lastX = x;
        lastY = y;
        lastZ = z;
        x = (double) serverPosX / (double) 32.0F;
        y = (double) serverPosY / (double) 32.0F;
        z = (double) serverPosZ / (double) 32.0F;
    }

    public Vec3 getPosition() {
        return new Vec3(x, y, z);
    }

    public void incrementTick() {
        ticksSinceLastVelocity++;
        ticksSinceLastTeleport++;
    }
}
