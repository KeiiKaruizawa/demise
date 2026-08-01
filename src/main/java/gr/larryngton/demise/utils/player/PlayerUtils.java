package gr.larryngton.demise.utils.player;

import com.google.common.base.Predicates;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.impl.misc.MouseOverEvent;
import gr.larryngton.demise.features.modules.impl.combat.KillAura;
import gr.larryngton.demise.utils.InstanceAccess;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler;
import gr.larryngton.demise.utils.player.handlers.rotation.RotationUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.*;
import net.optifine.reflect.Reflector;

import java.util.ArrayList;
import java.util.List;

import static gr.larryngton.demise.utils.player.handlers.rotation.RotationHandler.currentRotation;

@UtilityClass
public class PlayerUtils implements InstanceAccess {
    public boolean isBlockUnder(double height, boolean boundingBox) {
        if (boundingBox) {
            AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset(0, -height, 0);

            return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty();
        } else {
            for (int offset = 0; offset < height; offset++) {
                if (PlayerUtils.blockRelativeToPlayer(0, -offset, 0).isFullBlock()) {
                    return true;
                }
            }
        }
        return false;
    }

    public int findTool(BlockPos blockPos) {
        float bestSpeed = 1;
        int bestSlot = -1;

        IBlockState blockState = mc.theWorld.getBlockState(blockPos);

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            if (itemStack == null) {
                continue;
            }

            float speed = itemStack.getStrVsBlock(blockState.getBlock());

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public boolean overVoid() {
        return overVoid(mc.thePlayer.getPositionVector());
    }

    public boolean overVoid(Vec3 vec) {
        return overVoid(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    public boolean overVoid(double posX, double posY, double posZ) {
        for (int i = (int) posY; i > -1; i--) {
            if (!(mc.theWorld.getBlockState(new BlockPos(posX, i, posZ)).getBlock() instanceof BlockAir)) {
                return false;
            }
        }
        return true;
    }

    public boolean isInTeam(String str) {
        if (mc.thePlayer.getDisplayName() != null && str != null) {
            String targetName = str.replace("§r", "");
            String clientName = mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
            return targetName.startsWith("§" + clientName.charAt(1));
        }
        return false;
    }

    public boolean isInTeam(Entity entity) {
        if (mc.thePlayer.getDisplayName() != null && entity.getDisplayName() != null) {
            String targetName = entity.getDisplayName().getFormattedText().replace("§r", "");
            String clientName = mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
            return targetName.startsWith("§" + clientName.charAt(1));
        }
        return false;
    }

    public double getDistanceToEntityBox(Entity entity) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1);
        Vec3 pos = RotationUtils.getBestHitVec(entity);
        double xDist = Math.abs(pos.xCoord - eyes.xCoord);
        double yDist = Math.abs(pos.yCoord - eyes.yCoord);
        double zDist = Math.abs(pos.zCoord - eyes.zCoord);
        return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
    }

    public double getCustomDistanceToEntityBox(Vec3 eyes, Entity entity) {
        Vec3 pos = RotationUtils.getBestHitVec(entity);
        double xDist = Math.abs(pos.xCoord - eyes.xCoord);
        double yDist = Math.abs(pos.yCoord - eyes.yCoord);
        double zDist = Math.abs(pos.zCoord - eyes.zCoord);
        return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
    }

    public double getCustomDistanceToEntityBox(Vec3 eyes, Vec3 entityPos) {
        Vec3 pos = RotationUtils.getBestHitVecCustom(mc.thePlayer.getHitbox().offset(entityPos.xCoord - mc.thePlayer.posX, entityPos.yCoord - mc.thePlayer.posY, entityPos.zCoord - mc.thePlayer.posZ));
        double xDist = Math.abs(pos.xCoord - eyes.xCoord);
        double yDist = Math.abs(pos.yCoord - eyes.yCoord);
        double zDist = Math.abs(pos.zCoord - eyes.zCoord);
        return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
    }

    public boolean inLiquid() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava();
    }

    public Block getBlock(double x, double y, double z) {
        return mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public Block blockRelativeToPlayer(double offsetX, double offsetY, double offsetZ) {
        return getBlock(mc.thePlayer.posX + offsetX, mc.thePlayer.posY + offsetY, mc.thePlayer.posZ + offsetZ);
    }

    public EntityLivingBase getTarget(double distance) {
        return getTarget(distance, "Distance");
    }

    public EntityLivingBase getTarget(double distance, String targetPriority) {
        if (Demise.INSTANCE.getModuleManager().getModule(KillAura.class).isEnabled() && KillAura.target != null) {
            return KillAura.target;
        }

        EntityLivingBase target = null;
        double closestDistance = distance + 0.4;
        double leastHealth = Float.MAX_VALUE;
        double leastHurtTime = 10;
        double closestMOPDist = Double.MAX_VALUE;

        MovingObjectPosition cameraDirectionMOP = PlayerUtils.getMouseOver();

        for (Entity e : mc.theWorld.loadedEntityList) {
            if (!(e instanceof EntityLivingBase entity) || entity == mc.thePlayer) continue;
            if (mc.thePlayer.getPositionVector().squareDistanceTo(entity.getPositionVector()) > distance * distance) continue;
            if (entity instanceof EntityPlayer && Demise.INSTANCE.getFriendManager().isFriend((EntityPlayer) entity)) continue;

            double distanceToEntity = PlayerUtils.getDistanceToEntityBox(entity);

            if (distanceToEntity <= distance) {
                if (!Demise.INSTANCE.getClientSettings().isValidTarget(entity)) {
                    continue;
                }

                switch (targetPriority) {
                    case "None":
                        target = entity;
                        break;
                    case "Distance":
                        if (distanceToEntity < closestDistance) {
                            target = entity;
                            closestDistance = distanceToEntity;
                        }
                        break;
                    case "Health":
                        if (PlayerUtils.getActualHealth(entity) < leastHealth) {
                            target = entity;
                            leastHealth = PlayerUtils.getActualHealth(entity);
                        }
                        break;
                    case "HurtTime":
                        if (entity.hurtTime <= leastHurtTime) {
                            target = entity;
                            leastHurtTime = entity.hurtTime;
                        }
                        break;
                    case "RayTrace":
                        double dist = PlayerUtils.getCustomDistanceToEntityBox(cameraDirectionMOP.hitVec, entity);

                        if (dist < closestMOPDist) {
                            target = entity;
                            closestMOPDist = dist;
                        }
                        break;
                }
            }
        }

        return target;
    }

    public List<EntityLivingBase> getTargets(double distance) {
        List<EntityLivingBase> targets = new ArrayList<>();

        for (Entity e : mc.theWorld.loadedEntityList) {
            if (!(e instanceof EntityLivingBase entity) || entity == mc.thePlayer) continue;
            if (entity instanceof EntityPlayer && Demise.INSTANCE.getFriendManager().isFriend((EntityPlayer) entity))
                continue;

            double distanceToEntity = PlayerUtils.getDistanceToEntityBox(entity);

            if (distanceToEntity <= distance && Demise.INSTANCE.getClientSettings().isValidTarget(entity)) {
                targets.add(entity);
            }
        }

        return targets;
    }

    public Block getBlock(BlockPos blockPos) {
        return mc.theWorld.getBlockState(blockPos).getBlock();
    }

    public boolean isReplaceable(BlockPos blockPos) {
        return getBlock(blockPos).isReplaceable(mc.theWorld, blockPos);
    }

    private final String[] healthSubstrings = {"hp", "health", "lives", "❤"};

    public Float getActualHealth(EntityLivingBase entity) {
        Scoreboard scoreboard = entity.getEntityWorld().getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(2);

        if (objective == null) {
            return entity.getHealth();
        }

        Score score = scoreboard.getValueFromObjective(entity.getName(), objective);

        if (score == null || objective.getDisplayName() == null) {
            return entity.getHealth();
        }

        String displayName = objective.getDisplayName();

        boolean containsHealthSubstring = false;
        for (String substring : healthSubstrings) {
            if (displayName.contains(substring)) {
                containsHealthSubstring = true;
                break;
            }
        }

        if (!containsHealthSubstring) {
            return entity.getHealth();
        }

        int scoreboardHealth = score.getScorePoints();

        if (scoreboardHealth > 0) {
            return (float) scoreboardHealth;
        }

        return entity.getHealth();
    }

    public class PredictProcess {
        public Vec3 position;
        public float fallDistance;
        public boolean onGround;
        public boolean isCollidedHorizontally;
        public int tick;
        public boolean isInWater;

        public PredictProcess(Vec3 position, float fallDistance, boolean onGround, boolean isCollidedHorizontally) {
            this.position = position;
            this.fallDistance = fallDistance;
            this.onGround = onGround;
            this.isCollidedHorizontally = isCollidedHorizontally;
            this.isInWater = false;
        }

        public PredictProcess(Vec3 position, float fallDistance, boolean onGround, boolean isCollidedHorizontally, boolean inWater) {
            this.position = position;
            this.fallDistance = fallDistance;
            this.onGround = onGround;
            this.isCollidedHorizontally = isCollidedHorizontally;
            this.isInWater = inWater;
        }
    }

    public boolean insideBlock() {
        if (mc.thePlayer.ticksExisted < 5) {
            return false;
        }

        EntityPlayerSP player = mc.thePlayer;
        WorldClient world = mc.theWorld;
        AxisAlignedBB bb = player.getEntityBoundingBox();
        for (int x = MathHelper.floor_double(bb.minX); x < MathHelper.floor_double(bb.maxX) + 1; ++x) {
            for (int y = MathHelper.floor_double(bb.minY); y < MathHelper.floor_double(bb.maxY) + 1; ++y) {
                for (int z = MathHelper.floor_double(bb.minZ); z < MathHelper.floor_double(bb.maxZ) + 1; ++z) {
                    Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
                    AxisAlignedBB boundingBox;
                    if (block != null && !(block instanceof BlockAir) && (boundingBox = block.getCollisionBoundingBox(world, new BlockPos(x, y, z), world.getBlockState(new BlockPos(x, y, z)))) != null && player.getEntityBoundingBox().intersectsWith(boundingBox)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String getCurrServer() {
        String srv;

        if (!mc.isSingleplayer()) {
            if (mc.getCurrentServerData().serverIP.toLowerCase().contains("liquidproxy.net")) {
                srv = "liquidproxy.net";
            } else {
                srv = mc.getCurrentServerData().serverIP;
            }
        } else {
            srv = "Singleplayer";
        }

        return srv;
    }

    public boolean isHoldingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    public double getDistToTargetFromMouseOver(Vec3 eyes, Vec3 look, Entity target, AxisAlignedBB targetBB) {
        double blockReachDistance = 64;
        Vec3 vec32 = eyes.addVector(look.xCoord * blockReachDistance, look.yCoord * blockReachDistance, look.zCoord * blockReachDistance);

        Vec3 vec33 = null;

        MovingObjectPosition movingobjectposition = targetBB.calculateIntercept(eyes, vec32);

        if (targetBB.isVecInside(eyes)) {
            vec33 = movingobjectposition == null ? eyes : movingobjectposition.hitVec;
        } else if (movingobjectposition != null) {
            double d3 = eyes.distanceTo(movingobjectposition.hitVec);

            if (d3 < blockReachDistance) {
                boolean flag1 = false;

                if (Reflector.ForgeEntity_canRiderInteract.exists()) {
                    flag1 = Reflector.callBoolean(target, Reflector.ForgeEntity_canRiderInteract);
                }

                if (!(!flag1 && target == mc.thePlayer.ridingEntity)) {
                    vec33 = movingobjectposition.hitVec;
                }
            }
        }

        return vec33 == null ? Double.MAX_VALUE : eyes.distanceTo(vec33);
    }

    public MovingObjectPosition rayTraceBlock(float[] rot, double reach, float partialTicks) {
        Vec3 from = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 direction = mc.thePlayer.getLookCustom(rot[0], rot[1]);
        Vec3 to = from.addVector(direction.xCoord * reach, direction.yCoord * reach, direction.zCoord * reach);

        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(from, to, false, false, false);

        if (result == null) {
            return new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, to, EnumFacing.UP, new BlockPos(to));
        }

        return result;
    }

    public MovingObjectPosition rayTraceBlock(double reach, float partialTicks) {
        Vec3 from = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 direction = mc.thePlayer.getLookVec();
        Vec3 to = from.addVector(direction.xCoord * reach, direction.yCoord * reach, direction.zCoord * reach);

        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(from, to, false, false, false);

        if (result == null) {
            return new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, to, EnumFacing.UP, new BlockPos(to));
        }

        return result;
    }

    public Vec3 getPosFromAABB(AxisAlignedBB a) {
        return new Vec3((a.minX + a.maxX) / 2, a.minY, (a.minZ + a.maxZ) / 2);
    }

    public boolean isOnLiquid() {
        boolean onLiquid = false;
        AxisAlignedBB playerBB = mc.thePlayer.getEntityBoundingBox();
        WorldClient world = mc.theWorld;
        int y = (int) playerBB.offset(0.0, -0.01, 0.0).minY;
        for (int x = MathHelper.floor_double(playerBB.minX); x < MathHelper.floor_double(playerBB.maxX) + 1; ++x) {
            for (int z = MathHelper.floor_double(playerBB.minZ); z < MathHelper.floor_double(playerBB.maxZ) + 1; ++z) {
                Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
                if (block != null && !(block instanceof BlockAir)) {
                    if (!(block instanceof BlockLiquid)) {
                        return false;
                    }
                    onLiquid = true;
                }
            }
        }
        return onLiquid;
    }

    public PredictProcess simulatePlayer(int ticks) {
        return simulatePlayer(ticks, false);
    }

    public PredictProcess simulatePlayer(int ticks, boolean forceUnSneak) {
        List<PredictProcess> selfPrediction = new ArrayList<>();

        SimulatedPlayer simulatedSelf = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);
        simulatedSelf.rotationYaw = RotationHandler.currentRotation != null ? RotationHandler.currentRotation[0] : mc.thePlayer.rotationYaw;

        if (forceUnSneak) {
            simulatedSelf.movementInput.sneak = false;
        }

        for (int i = 0; i < ticks; i++) {
            simulatedSelf.tick();

            PlayerUtils.PredictProcess predictProcess = new PlayerUtils.PredictProcess(
                    simulatedSelf.getPos(),
                    simulatedSelf.fallDistance,
                    simulatedSelf.onGround,
                    simulatedSelf.isCollidedHorizontally
            );

            predictProcess.tick = i;

            selfPrediction.add(predictProcess);
        }

        return selfPrediction.get(selfPrediction.size() - 1);
    }

    public MovingObjectPosition blockRayTrace(BlockPos pos) {
        Vec3 from = mc.thePlayer.getPositionEyes(1);
        Vec3 direction = mc.thePlayer.getLookCustom(currentRotation[0], currentRotation[1]);
        Vec3 to = from.addVector(direction.xCoord * 4.5, direction.yCoord * 4.5, direction.zCoord * 4.5);

        MovingObjectPosition ray = PlayerUtils.getBlock(pos).collisionRayTrace(mc.theWorld, pos, from, to);
        return ray != null ? ray : new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, to, EnumFacing.UP, new BlockPos(to));
    }

    public MovingObjectPosition getMouseOver(EntityPlayer entity) {
        return getMouseOver(entity, entity.getPositionEyes(1), new float[]{entity.rotationYaw, entity.rotationPitch});
    }

    public MovingObjectPosition getMouseOver(EntityPlayer entity, Vec3 posEyes, float[] rot) {
        Entity pointedEntity = null;
        MovingObjectPosition objectMouseOver;
        double blockReachDistance = 4.5;

        double reach = 3.0D;
        objectMouseOver = entity.rayTraceCustom(blockReachDistance, rot[0], rot[1]);
        double distance = blockReachDistance;
        boolean flag = blockReachDistance > 3.0D;

        if (objectMouseOver != null) {
            distance = objectMouseOver.hitVec.distanceTo(posEyes);
        }

        final Vec3 vec31 = entity.getLook(1);
        final Vec3 vec32 = posEyes.addVector(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance);
        Vec3 vec33 = null;
        final float f = 1.0F;
        final List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
        double d2 = distance;

        for (final Entity entity1 : list) {
            final float f1 = entity1.getCollisionBorderSize();
            final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
            final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(posEyes, vec32);

            if (axisalignedbb.isVecInside(posEyes)) {
                if (d2 >= 0.0D) {
                    pointedEntity = entity1;
                    vec33 = movingobjectposition == null ? posEyes : movingobjectposition.hitVec;
                    d2 = 0.0D;
                }
            } else if (movingobjectposition != null) {
                double d3 = posEyes.distanceTo(movingobjectposition.hitVec);

                if (d3 < d2 || d2 == 0.0D) {
                    boolean flag1 = false;

                    if (Reflector.ForgeEntity_canRiderInteract.exists()) {
                        flag1 = Reflector.callBoolean(entity1, Reflector.ForgeEntity_canRiderInteract);
                    }

                    if (!flag1 && entity1 == entity.ridingEntity) {
                        if (d2 == 0.0D) {
                            pointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                        }
                    } else {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition.hitVec;
                        d2 = d3;
                    }
                }
            }
        }

        if (pointedEntity != null && flag && vec33 != null && posEyes.distanceTo(vec33) > reach) {
            pointedEntity = null;
            objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, null, new BlockPos(vec33));
        }

        if (pointedEntity != null && (d2 < distance || objectMouseOver == null)) {
            objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
        }

        return objectMouseOver;
    }

    public boolean blockNear(final int range) {
        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range; ++y) {
                for (int z = -range; z <= range; ++z) {
                    final Block block = blockRelativeToPlayer(x, y, z);

                    if (!(block instanceof BlockAir)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public float getItemCoolDown() {
        float cooldown = 4;

        if (mc.thePlayer.getHeldItem() != null) {
            Item item = mc.thePlayer.getHeldItem().getItem();

            if (item instanceof ItemSpade || item == Items.golden_axe || item == Items.diamond_axe || item == Items.wooden_hoe || item == Items.golden_hoe)
                cooldown = 20;

            if (item == Items.wooden_axe || item == Items.stone_axe)
                cooldown = 25;

            if (item instanceof ItemSword)
                cooldown = 12;

            if (item instanceof ItemPickaxe)
                cooldown = 17;

            if (item == Items.iron_axe)
                cooldown = 22;

            if (item == Items.stone_hoe)
                cooldown = 10;

            if (item == Items.iron_hoe)
                cooldown = 7;
        }

        cooldown *= Math.max(1, mc.timer.timerSpeed);

        return cooldown;
    }

    public MovingObjectPosition getMouseOver() {
        return getMouseOver(mc.thePlayer.getRotation());
    }

    public MovingObjectPosition getMouseOver(float[] rot) {
        Entity entity = mc.getRenderViewEntity();
        MovingObjectPosition objectMouseOver = new MovingObjectPosition(entity);
        Entity pointedEntity;

        if (entity != null && mc.theWorld != null) {
            pointedEntity = null;
            double blockReachDistance = mc.playerController.getBlockReachDistance();

            double reach = 3.0D;
            float expand = 0;

            MouseOverEvent mouseOverEvent = new MouseOverEvent(reach, expand);
            Demise.INSTANCE.getEventManager().call(mouseOverEvent);

            reach = Math.max(reach, mouseOverEvent.getRange());
            blockReachDistance = Math.max(blockReachDistance, mouseOverEvent.getRange() + 1.5);

            objectMouseOver = entity.rayTraceCustom(blockReachDistance, rot[0], rot[1]);
            double distance = blockReachDistance;
            final Vec3 vec3 = entity.getPositionEyes(1);
            boolean flag = false;

            expand = mouseOverEvent.getExpand();

            if (mouseOverEvent.getMovingObjectPosition() != null) {
                objectMouseOver = mouseOverEvent.getMovingObjectPosition();
                return objectMouseOver;
            }

            if (mc.playerController.extendedReach()) {
                blockReachDistance = 6.0D;
                distance = 6.0D;
            } else if (blockReachDistance > 3.0D) {
                flag = true;
            }

            if (objectMouseOver != null) {
                distance = objectMouseOver.hitVec.distanceTo(vec3);
            }

            final Vec3 vec31 = entity.getLookCustom(rot[0], rot[1]);
            final Vec3 vec32 = vec3.addVector(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance);
            Vec3 vec33 = null;
            final float f = 1.0F;
            final List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double d2 = distance;

            for (final Entity entity1 : list) {
                final float f1 = entity1.getCollisionBorderSize() + ((entity instanceof EntityPlayer && !entity.isInvisible()) ? expand : 0);
                final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        boolean flag1 = false;

                        if (Reflector.ForgeEntity_canRiderInteract.exists()) {
                            flag1 = Reflector.callBoolean(entity1, Reflector.ForgeEntity_canRiderInteract);
                        }

                        if (!flag1 && entity1 == entity.ridingEntity) {
                            if (d2 == 0.0D) {
                                pointedEntity = entity1;
                                vec33 = movingobjectposition.hitVec;
                            }
                        } else {
                            pointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && vec33 != null && vec3.distanceTo(vec33) > (ViaLoadingBase.getInstance().getTargetVersion().getVersion() <= 47 ? reach : reach - 0.1f)) {
                pointedEntity = null;
                objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, null, new BlockPos(vec33));
            }

            if (pointedEntity != null && (d2 < distance || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
            }
        }

        return objectMouseOver;
    }

    public int getPing(Entity entity) {
        if (entity instanceof EntityPlayer player) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
            return info == null ? 50 : info.getResponseTime();
        }

        return 50;
    }

    public boolean hittableBy(EntityLivingBase target) {
        double pred = getPing(target) / 50.0;
        Vec3 from = target.getPositionEyes(1).add(target.getMoveDeltaVector().multiply(pred));
        Vec3 direction = target.getLookVecFromVisualRot();
        double r = 3;
        Vec3 to = from.addVector(direction.xCoord * r, direction.yCoord * r, direction.zCoord * r);

        AxisAlignedBB predBB = mc.thePlayer.getHitbox().contract(0.01,0.01,0.01).offset(mc.thePlayer.getMoveDeltaVector().multiply(pred));
        MovingObjectPosition movingObjectPosition = predBB.calculateIntercept(from, to);

        return movingObjectPosition != null && movingObjectPosition.hitVec != null && predBB.expandXyz(0.06).isVecInside(movingObjectPosition.hitVec);
    }
}