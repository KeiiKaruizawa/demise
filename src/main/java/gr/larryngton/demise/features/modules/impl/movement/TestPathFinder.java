package gr.larryngton.demise.features.modules.impl.movement;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.MoveInputEvent;
import gr.larryngton.demise.events.impl.render.Render2DEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.modules.impl.misc.PathFinderModule;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.gui.font.Fonts;
import gr.larryngton.demise.utils.misc.ChatUtils;
import gr.larryngton.demise.utils.player.SimulatedPlayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInput.Action;
import net.minecraft.util.Vec3;

import java.util.*;

@ModuleInfo(name = "TestPathFinder", description = "Test module for a better pathfinder.")
public class TestPathFinder extends Module {
    private final SliderValue maxIterationsPerTick = new SliderValue("Max iterations per tick", 500, 1, 1000, 1, this);
    private final SliderValue maxInputsPerUpdate = new SliderValue("Max inputs per update", 5, 1, 100, 1, this);

    public TestPathFinder() {
        maxIterationsPerTick.setDescription("Should be low to resync the player's position more since player prediction isn't 1:1 to vanilla mc.");
    }

    private List<Node> nodeList = new ArrayList<>();
    private int iterations;

    @Override
    public void onEnable() {
        iterations = 0;
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            setEnabled(false);
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        Fonts.interRegular.get(18).drawCenteredStringWithShadow(String.valueOf(iterations), sr.getScaledWidth() / 2F, sr.getScaledHeight() / 2F + 20, -1);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (nodeList.isEmpty()) {
            nodeList = findPath(getModule(PathFinderModule.class).targetPos, maxIterationsPerTick.getInt());

            if (!nodeList.isEmpty()) {
                ChatUtils.sendMessageClient(nodeList.size());
            }
        }

        if (!nodeList.isEmpty()) {
            Node next = nodeList.get(0);
            nodeList.remove(0);

            e.setMovementInput(next.actionUsed.getInput());
//            e.setCancelled(true);
//            mc.thePlayer.setPosition(next.pos.xCoord, next.pos.yCoord, next.pos.zCoord);
        }
    }

    private List<Node> findPath(Vec3 target, int maxScanSize) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Map<String, Integer> visited = new HashMap<>();

        boolean climbMode = target.yCoord > mc.thePlayer.posY + 0.5;

        SimulatedPlayer startSim = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);
        Node start = new Node(
                startSim,
                null,
                null,
                0,
                heuristic(new Node(startSim, null, null, 0, 0), target, climbMode)
        );

        open.add(start);

        Node best = start;
        float yaw = mc.thePlayer.rotationYaw;

        for (int i = 0; i <= maxScanSize && !open.isEmpty(); i++) {
            iterations++;

            Node current = open.poll();

            if (isBetter(current, best, target, climbMode)) {
                best = current;
            }

            if (isGoal(current, target, climbMode)) {
                return reconstructPath(current);
            }

            String key = hash(current);
            if (visited.containsKey(key) && visited.get(key) <= current.gCost)
                continue;

            visited.put(key, current.gCost);

//            Action anyColliding = Action.NONE;
            for (Action action : MovementInput.Action.values()) {
                iterations++;

                MovementInput input = action.getInput();

                SimulatedPlayer sim = current.player.copy();
                sim.movementInput = input;
                sim.rotationYaw = yaw;

//                SimulatedPlayer tempSim = current.player.copy();
//                tempSim.movementInput = input;
//                tempSim.rotationYaw = yaw;
//                tempSim.tick();
//
//                if (action.getInput().jump) {
//                    if (action.getInput().moveForward != anyColliding.getInput().moveForward || action.getInput().moveStrafe != anyColliding.getInput().moveStrafe) {
//                        if (tempSim.posY < sim.posY && !tempSim.onGround && sim.onGround) {
//                            continue;
//                        }
//                    }
//                }
//
//                if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, sim.getEntityBoundingBox().offset(sim.motionX / 3.0D, -1.0D, sim.motionZ / 3.0D)).isEmpty()) {
//                    action.getInput().setJump(true);
//                }

                sim.tick();

//                if (sim.isCollidedHorizontally) {
//                    anyColliding = action;
//                }

                Node next = new Node(
                        sim,
                        action,
                        current,
                        current.gCost + 1,
                        heuristic(new Node(sim, action, current, current.gCost + 1, 0), target, climbMode)
                );

                if (next.fallDistance > 4F)
                    continue;

                open.add(next);
            }
        }

        return reconstructPath(best);
    }

    private boolean isBetter(Node a, Node b, Vec3 target, boolean climbMode) {
        return pathScore(a, target, climbMode) < pathScore(b, target, climbMode);
    }

    private double pathScore(Node node, Vec3 target, boolean climbMode) {
        double dx = node.pos.xCoord - target.xCoord;
        double dy = node.pos.yCoord - target.yCoord;
        double dz = node.pos.zCoord - target.zCoord;

        double horizontalSq = dx * dx + dz * dz;
        double score = horizontalSq + (dy * dy * 1.5);

        if (climbMode) {
            double belowTarget = Math.max(0.0, target.yCoord - node.pos.yCoord);

            score += belowTarget * 100.0;

            if (belowTarget < 0.75) {
                score += horizontalSq * 0.75;
            }
        } else {
            if (isUnreachableLike(node, target)) {
                score += BLOCKED_PENALTY;
                if (Math.abs(dy) > BIG_Y_DIFF) {
                    score += BIG_Y_PENALTY + Math.abs(dy) * 4.0;
                }
            }
        }

        return score;
    }

    private boolean isUnreachableLike(Node node, Vec3 target) {
        double yDiff = Math.abs(node.pos.yCoord - target.yCoord);
        return yDiff > BIG_Y_DIFF || isObstructed(node.pos, target);
    }

    boolean isGoal(Node n, Vec3 target, boolean climbMode) {
        if (!n.onGround) {
            return false;
        }

        double dx = n.pos.xCoord - target.xCoord;
        double dy = n.pos.yCoord - target.yCoord;
        double dz = n.pos.zCoord - target.zCoord;

        double horizontalDistSq = dx * dx + dz * dz;

        if (climbMode) {
            return horizontalDistSq < 0.35 * 0.35 && Math.abs(dy) < 0.4;
        }

        return n.pos.distanceTo(target) < 0.2 && n.pos.yCoord >= target.yCoord && !isUnreachableLike(n, target);
    }

    private double heuristic(Node node, Vec3 target, boolean climbMode) {
        double dx = node.pos.xCoord - target.xCoord;
        double dy = node.pos.yCoord - target.yCoord;
        double dz = node.pos.zCoord - target.zCoord;

        double dist = dx * dx + dy * dy + dz * dz;

        if (climbMode) {
            double belowTarget = Math.max(0.0, target.yCoord - node.pos.yCoord);

            dist += belowTarget * 25.0;

            if (belowTarget < 1.0) {
                dist += (dx * dx + dz * dz) * 0.5;
            }
        } else {
            if (isUnreachableLike(node, target)) {
                dist += BLOCKED_PENALTY;

                if (Math.abs(dy) > BIG_Y_DIFF) {
                    dist += Math.abs(dy) * 4.0;
                }
            } else {
                dist += Math.abs(dy) * 1.5;
            }
        }

        return dist;
    }

    private static final double BIG_Y_DIFF = 1.5;
    private static final double BLOCKED_PENALTY = 25.0;
    private static final double BIG_Y_PENALTY = 12.0;

    private boolean isObstructed(Vec3 from, Vec3 to) {
        Vec3 eye = from.addVector(0.0, 1.62, 0.0);
        return mc.theWorld.rayTraceBlocks(eye, to, false, true, false) != null;
    }

    private List<Node> reconstructPath(Node node) {
        List<Node> actions = new ArrayList<>();

        while (node.parent != null && node.actionUsed != null) {
            iterations++;
            actions.add(node);
            node = node.parent;
        }

        while (actions.size() > maxInputsPerUpdate.getInt()) {
            actions.remove(0);
        }

        Collections.reverse(actions);
        return actions;
    }

    private String hash(Node n) {
        return String.format(
                "%.2f|%.2f|%.2f|%b",
                Math.round(n.pos.xCoord * 10) / 10.0,
                Math.round(n.pos.yCoord * 10) / 10.0,
                Math.round(n.pos.zCoord * 10) / 10.0,
                n.onGround
        );
    }

    static class Node {
        SimulatedPlayer player;
        Vec3 pos;
        boolean onGround;
        float fallDistance;
        Action actionUsed;

        Node parent;
        int gCost;
        double hCost;
        double fCost;

        Node(SimulatedPlayer player, Action actionUsed, Node parent, int gCost, double hCost) {
            this.player = player;
            this.pos = player.getPos();
            this.onGround = player.onGround;
            this.fallDistance = player.fallDistance;
            this.actionUsed = actionUsed;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }
    }
}