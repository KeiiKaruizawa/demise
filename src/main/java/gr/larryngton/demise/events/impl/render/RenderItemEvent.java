package gr.larryngton.demise.events.impl.render;

import gr.larryngton.demise.events.impl.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;

@Getter
@Setter
@AllArgsConstructor
public final class RenderItemEvent extends CancellableEvent {
    private EnumAction enumAction;
    private boolean useItem;
    private float progress, swingProgress;
    private ItemStack itemToRender;
}
