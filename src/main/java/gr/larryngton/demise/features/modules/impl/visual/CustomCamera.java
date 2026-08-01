package gr.larryngton.demise.features.modules.impl.visual;

import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.UpdateEvent;
import gr.larryngton.demise.events.impl.render.CameraPositionEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.SliderValue;
import gr.larryngton.demise.utils.math.MathUtils;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "CustomCamera")
public class CustomCamera extends Module {
    private final SliderValue distance = new SliderValue("Distance", 1, 1, 20, 1, this);
    private final SliderValue currentPosBias = new SliderValue("Current pos bias", 0, 0, 1, 0.01F, this);

    private final List<Vec3> cameraPositionList = new ArrayList<>();
    private double lastY;

    @EventTarget
    public void onCameraPos(CameraPositionEvent e) {
        if (mc.gameSettings.thirdPersonView != 0) {
            Vec3 vec1 = cameraPositionList.get(0);
            Vec3 vec2 = cameraPositionList.get(1);
            Vec3 camVec = MathUtils.interpolateNoUpdateCheck(MathUtils.interpolate(vec1, vec2), new Vec3(e.getX(), e.getY(), e.getZ()), currentPosBias.get());
            lastY = camVec.yCoord = MathUtils.interpolateNoUpdateCheck(lastY, camVec.yCoord, 0.7);

            e.setX(camVec.xCoord);
            e.setY(camVec.yCoord);
            e.setZ(camVec.zCoord);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        cameraPositionList.add(mc.thePlayer.getPositionEyes(1));

        while (cameraPositionList.size() > distance.getInt() + 1) {
            cameraPositionList.remove(0);
        }
    }
}
