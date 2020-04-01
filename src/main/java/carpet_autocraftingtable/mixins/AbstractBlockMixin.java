package carpet_autocraftingtable.mixins;

import carpet_autocraftingtable.AutoCraftingTableSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.CraftingTableBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin
{

    public boolean hasBlockEntity() {
        if ((Object) this instanceof CraftingTableBlock) return AutoCraftingTableSettings.autoCraftingTable;
        return this instanceof BlockEntityProvider;
    }
}
