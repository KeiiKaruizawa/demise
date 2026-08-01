package gr.larryngton.demise.utils.world.pathfinding;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Vec3;

import java.util.ArrayList;

@Getter
@Setter
public class PathHub {
    private Vec3 loc;
    private PathHub parentPathHub;
    private ArrayList<Vec3> pathway;
    private double sqDist;
    private double currentCost;
    private double maxCost;

    public PathHub(Vec3 loc, PathHub parentPathHub, ArrayList<Vec3> pathway, double sqDist, double currentCost, double maxCost) {
        this.loc = loc;
        this.parentPathHub = parentPathHub;
        this.pathway = pathway;
        this.sqDist = sqDist;
        this.currentCost = currentCost;
        this.maxCost = maxCost;
    }
}
