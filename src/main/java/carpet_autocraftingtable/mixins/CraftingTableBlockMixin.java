package carpet_autocraftingtable.mixins;

import carpet.CarpetSettings;
import carpet_autocraftingtable.AutoCraftingTableSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
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

    /*@Override is final
    public boolean hasBlockEntity() {
        return AutoCraftingTableSettings.autoCraftingTable;
    }*/

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
    {
        return AutoCraftingTableSettings.autoCraftingTable ? (state.isOf(Blocks.CRAFTING_TABLE) ? new CraftingTableBlockEntity(pos, state) : null) : null;
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onActivateActionResult(BlockState blockState_1, World world_1, BlockPos blockPos_1, PlayerEntity playerEntity_1, Hand hand_1, BlockHitResult blockHitResult_1, CallbackInfoReturnable<ActionResult> cir)
    {
        if (!AutoCraftingTableSettings.autoCraftingTable) return;
        if (!blockState_1.hasBlockEntity()) return;
        if (!world_1.isClient) {
            BlockEntity blockEntity = world_1.getBlockEntity(blockPos_1);
            if (blockEntity instanceof CraftingTableBlockEntity) {
                playerEntity_1.openHandledScreen((NamedScreenHandlerFactory) blockEntity);
            }
        }
        playerEntity_1.incrementStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        cir.setReturnValue(ActionResult.SUCCESS);
        cir.cancel();
    }

    @Override
    public boolean hasComparatorOutput(BlockState blockState) {
        return AutoCraftingTableSettings.autoCraftingTable && blockState.hasBlockEntity();
    }

    @Override
    public int getComparatorOutput(BlockState blockState, World world, BlockPos pos) {
        if (!AutoCraftingTableSettings.autoCraftingTable) return 0;
        if (!blockState.hasBlockEntity()) return 0;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CraftingTableBlockEntity) {
            CraftingTableBlockEntity craftingTableBlockEntity = (CraftingTableBlockEntity) blockEntity;
            int filled = 0;
            for (ItemStack stack : craftingTableBlockEntity.inventory) {
                if (!stack.isEmpty()) filled++;
            }
            return (filled * 15) / 9;
        }
        return 0;
    }


    @Override
    public void onStateReplaced(BlockState state1, World world, BlockPos pos, BlockState state2, boolean boolean_1) {
        if (state1.getBlock() != state2.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CraftingTableBlockEntity) {
                CraftingTableBlockEntity craftingTableBlockEntity = ((CraftingTableBlockEntity)blockEntity);
                ItemScatterer.spawn(world, pos, craftingTableBlockEntity.inventory);
                if (!craftingTableBlockEntity.output.isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), craftingTableBlockEntity.output);
                }
                world.updateNeighborsAlways(pos, this);
            }
            world.removeBlockEntity(pos);

            super.onStateReplaced(state1, world, pos, state2, boolean_1);
        }
    }
}
