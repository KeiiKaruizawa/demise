package gr.larryngton.demise.features.modules;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModuleCategory {
    General("General"),
    Combat("Combat"),
    Legit("Legit"),
    Movement("Movement"),
    Player("Player"),
    Misc("Misc"),
    Exploit("Exploit"),
    Visual("Visual");

    private final String name;
}