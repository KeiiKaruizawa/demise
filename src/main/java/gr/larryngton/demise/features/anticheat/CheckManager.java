package gr.larryngton.demise.features.anticheat;

import gr.larryngton.demise.features.anticheat.impl.Check;
import gr.larryngton.demise.features.anticheat.impl.combat.FakeLag;
import gr.larryngton.demise.features.anticheat.impl.combat.InvalidSwing;
import gr.larryngton.demise.features.anticheat.impl.combat.autoclicker.AutoClickerA;
import gr.larryngton.demise.features.anticheat.impl.combat.autoclicker.AutoClickerB;
import gr.larryngton.demise.features.anticheat.impl.movement.motion.MotionA;
import gr.larryngton.demise.features.anticheat.impl.movement.motion.MotionB;
import gr.larryngton.demise.features.anticheat.impl.movement.motion.MotionC;
import gr.larryngton.demise.features.anticheat.impl.movement.motion.MotionD;
import gr.larryngton.demise.features.anticheat.impl.movement.noslow.NoSlowA;
import gr.larryngton.demise.features.anticheat.impl.movement.noslow.NoSlowB;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CheckManager {
    public static final List<Class<?>> checks = new ArrayList<>();

    public CheckManager() {
        addChecks(
                MotionA.class,
                MotionB.class,
                MotionC.class,
                MotionD.class,
                InvalidSwing.class,
                NoSlowA.class,
                NoSlowB.class,
                AutoClickerA.class,
                AutoClickerB.class,
                FakeLag.class
        );
    }

    public void addChecks(Class<?>... checks) {
        CheckManager.checks.addAll(Arrays.asList(checks));
    }

    private static final List<Constructor<?>> constructors = new ArrayList<>();

    public static List<Check> loadChecks(PlayerData data) {
        List<Check> checkList = new ArrayList<>();

        for (Constructor<?> constructor : constructors) {
            try {
                checkList.add((Check) constructor.newInstance(data));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        return checkList;
    }

    public static void setup() {
        for (Class<?> clazz : checks) {
            try {
                constructors.add(clazz.getConstructor(PlayerData.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
