package carpet_autocraftingtable.mixins;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.Nullable;

@Mixin(CraftingInventory.class)
public interface CraftingInventoryMixin
{
    @Accessor("stacks")
    void setInventory(DefaultedList<ItemStack> inventory);

    @Accessor("handler")
    void setHandler(@Nullable ScreenHandler handler);
}
