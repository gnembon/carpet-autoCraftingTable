package carpet_autocraftingtable.mixins;

import carpet_autocraftingtable.AutoCraftingTableSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import carpet_autocraftingtable.CraftingTableBlockEntity;

@Mixin(CraftingTableBlock.class)
public class CraftingTableBlockMixin extends Block implements BlockEntityProvider {
    protected CraftingTableBlockMixin(Settings block$Settings_1) {
        super(block$Settings_1);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return (AutoCraftingTableSettings.autoCraftingTable && state.isOf(Blocks.CRAFTING_TABLE)) ? new CraftingTableBlockEntity(pos, state) : null;
    }

    @Inject(method = "createScreenHandlerFactory", at = @At("HEAD"), cancellable = true)
    private void onCreateScreenHandler(BlockState state, World world, BlockPos pos, CallbackInfoReturnable<NamedScreenHandlerFactory> cir) {
        if (!AutoCraftingTableSettings.autoCraftingTable || !state.hasBlockEntity() || world.isClient) return;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CraftingTableBlockEntity) cir.setReturnValue((NamedScreenHandlerFactory) blockEntity);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return AutoCraftingTableSettings.autoCraftingTable && state.hasBlockEntity();
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (!AutoCraftingTableSettings.autoCraftingTable || !state.hasBlockEntity()) return 0;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CraftingTableBlockEntity craftingTableBlockEntity) {
            int filled = 0;
            for (ItemStack stack : craftingTableBlockEntity.inventory) {
                if (!stack.isEmpty()) filled++;
            }
            return (filled * 15) / 9;
        }
        return 0;
    }


    @Override
    public void onStateReplaced(BlockState oldState, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (oldState.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CraftingTableBlockEntity craftingTableBlockEntity) {
                ItemScatterer.spawn(world, pos, craftingTableBlockEntity.inventory);
                if (!craftingTableBlockEntity.output.isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), craftingTableBlockEntity.output);
                }
                world.updateNeighborsAlways(pos, this);
            }
            world.removeBlockEntity(pos);
            super.onStateReplaced(oldState, world, pos, newState, moved);
        }
    }

    @Override
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CraftingTableBlockEntity craftingTableBlockEntity) {
            ItemScatterer.spawn(world, pos, craftingTableBlockEntity.inventory);
            if (!craftingTableBlockEntity.output.isEmpty()) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), craftingTableBlockEntity.output);
            }
        }
    }
}
