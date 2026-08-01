package gr.larryngton.demise.features.values.impl;

import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.values.Value;
import gr.larryngton.demise.utils.math.MathUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

public class DoubleSliderValue extends Value {
    @Setter
    private float valueStart;
    @Setter
    private float valueEnd;
    @Getter
    private final float min;
    @Getter
    private final float max;
    @Getter
    private final float increment;

    public DoubleSliderValue(String name, float valueStart, float valueEnd, float min, float max, float increment, Module module, Supplier<Boolean> visible) {
        super(name, module, visible, true);
        this.valueStart = valueStart;
        this.valueEnd = valueEnd;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public DoubleSliderValue(String name, float valueStart, float valueEnd, float min, float max, Module module, Supplier<Boolean> visible) {
        super(name, module, visible, true);
        this.valueStart = valueStart;
        this.valueEnd = valueEnd;
        this.min = min;
        this.max = max;
        this.increment = 1;
    }

    public DoubleSliderValue(String name, float valueStart, float valueEnd, float min, float max, float increment, Module module) {
        super(name, module, () -> true, false);
        this.valueStart = valueStart;
        this.valueEnd = valueEnd;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public DoubleSliderValue(String name, float valueStart, float valueEnd, float min, float max, Module module) {
        super(name, module, () -> true, false);
        this.valueStart = valueStart;
        this.valueEnd = valueEnd;
        this.min = min;
        this.max = max;
        this.increment = 1;
    }

    public float[] get() {
        return new float[]{valueStart, valueEnd};
    }

    public float getStart() {
        return valueStart;
    }

    public float getEnd() {
        return valueEnd;
    }

    public float random() {
        return MathUtils.randomizeFloat(valueStart, valueEnd);
    }
}
