package gr.larryngton.demise.features.config.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gr.larryngton.demise.Demise;
import gr.larryngton.demise.features.config.Config;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleCategory;
import gr.larryngton.demise.features.values.Value;
import gr.larryngton.demise.features.values.impl.*;
import gr.larryngton.demise.utils.render.ColorUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class ModuleConfig extends Config {
    public ModuleConfig(String name) {
        super(name);
    }

    @Override
    public void loadConfig(JsonObject object) {
        for (Module module : Demise.INSTANCE.getModuleManager().getModules()) {
            if (object.has(module.getName())) {
                loadModule(object, module);
            }
        }

        if (object.has("ClientSettings")) {
            loadModule(object, Demise.INSTANCE.getClientSettings());
        }

        if (!object.has("HiddenDefaultsApplied")) {
            List<Module> visual = Demise.INSTANCE.getModuleManager().getModulesByCategory().get(ModuleCategory.Visual);
            List<Module> legit = Demise.INSTANCE.getModuleManager().getModulesByCategory().get(ModuleCategory.Legit);
            if (visual != null) visual.forEach(module -> module.setHidden(true));
            if (legit != null) legit.forEach(module -> module.setHidden(true));
        }
    }

    @Override
    public JsonObject saveConfig() {
        JsonObject object = new JsonObject();
        for (Module module : Demise.INSTANCE.getModuleManager().getModules()) {
            object.add(module.getName(), saveModule(module));
        }

        object.add("ClientSettings", saveModule(Demise.INSTANCE.getClientSettings()));
        object.addProperty("HiddenDefaultsApplied", true);
        return object;
    }

    private void loadModule(JsonObject object, Module module) {
        JsonObject moduleObject = object.get(module.getName()).getAsJsonObject();

        if (moduleObject.has("State")) {
            module.setEnabled(moduleObject.get("State").getAsBoolean());
        }

        if (moduleObject.has("Key")) {
            module.setKeyBind(moduleObject.get("Key").getAsInt());
        }
        if (moduleObject.has("Hidden")) {
            module.setHidden(moduleObject.get("Hidden").getAsBoolean());
        }

        if (moduleObject.has("Values")) {
            JsonObject valuesObject = moduleObject.get("Values").getAsJsonObject();

            for (Value value : module.getValues()) {
                if (valuesObject.has(value.getName())) {
                    JsonElement theValue = valuesObject.get(value.getName());
                    if (value instanceof SliderValue sliderValue) {
                        sliderValue.setValue(theValue.getAsNumber().floatValue());
                    }
                    if (value instanceof BoolValue boolValue) {
                        boolValue.set(theValue.getAsBoolean());
                    }
                    if (value instanceof ModeValue modeValue) {
                        modeValue.set(theValue.getAsString());
                    }
                    if (value instanceof MultiBoolValue multiBoolValue) {
                        if (!theValue.getAsString().isEmpty()) {
                            String[] strings = theValue.getAsString().split(", ");
                            multiBoolValue.getToggled().forEach(option -> option.set(false));

                            for (BoolValue boolValue : multiBoolValue.getValues()) {
                                boolValue.set(Arrays.stream(strings).anyMatch(boolValue.getName()::equalsIgnoreCase));
                            }
                        }
                    }
                    if (value instanceof ColorValue colorValue) {
                        JsonObject colorValues = theValue.getAsJsonObject();
                        colorValue.set(ColorUtils.applyOpacity(new Color(colorValues.get("RGB").getAsInt()), colorValues.get("Alpha").getAsFloat()));
                    }
                    if (value instanceof TextValue textValue) {
                        textValue.setText(theValue.getAsString());
                    }
                    if (value instanceof DoubleSliderValue doubleSliderValue) {
                        JsonObject range = theValue.getAsJsonObject();
                        doubleSliderValue.setValueStart(range.get("Start").getAsFloat());
                        doubleSliderValue.setValueEnd(range.get("End").getAsFloat());
                    }
                }
            }
        }
    }

    private JsonObject saveModule(Module module) {
        JsonObject moduleObject = new JsonObject();

        moduleObject.addProperty("State", module.isEnabled());
        moduleObject.addProperty("Key", module.getKeyBind());
        moduleObject.addProperty("Hidden", module.isHidden());

        JsonObject valuesObject = new JsonObject();

        for (Value value : module.getValues()) {
            if (value instanceof SliderValue sliderValue) {
                valuesObject.addProperty(value.getName(), sliderValue.get());
            }
            if (value instanceof BoolValue boolValue) {
                valuesObject.addProperty(value.getName(), boolValue.get());
            }
            if (value instanceof ModeValue modeValue) {
                valuesObject.addProperty(value.getName(), modeValue.get());
            }
            if (value instanceof MultiBoolValue multiBoolValue) {
                valuesObject.addProperty(value.getName(), multiBoolValue.isEnabled());
            }
            if (value instanceof ColorValue colorValue) {
                JsonObject colorValues = new JsonObject();
                colorValues.addProperty("RGB", Color.HSBtoRGB(colorValue.getHue(), colorValue.getSaturation(), colorValue.getBrightness()));
                colorValues.addProperty("Alpha", colorValue.getAlpha());
                valuesObject.add(colorValue.getName(), colorValues);
            }
            if (value instanceof TextValue textValue) {
                valuesObject.addProperty(value.getName(), textValue.getText());
            }
            if (value instanceof DoubleSliderValue doubleSliderValue) {
                JsonObject range = new JsonObject();
                range.addProperty("Start", doubleSliderValue.getStart());
                range.addProperty("End", doubleSliderValue.getEnd());
                valuesObject.add(value.getName(), range);
            }
        }

        moduleObject.add("Values", valuesObject);

        return moduleObject;
    }
}