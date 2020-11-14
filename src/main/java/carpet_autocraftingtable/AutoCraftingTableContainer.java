package carpet_autocraftingtable;

import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class AutoCraftingTableContainer extends CraftingScreenHandler {
    private final CraftingTableBlockEntity blockEntity;
    private final PlayerEntity player;

    AutoCraftingTableContainer(int id, PlayerInventory playerInventory, CraftingTableBlockEntity blockEntity) {
        super(id, playerInventory);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        slots.clear();
        this.addSlot(new OutputSlot(this.blockEntity, this.player));

        for(int y = 0; y < 3; ++y) {
            for(int x = 0; x < 3; ++x) {
                this.addSlot(new Slot(this.blockEntity, x + y * 3 + 1, 30 + x * 18, 17 + y * 18));
            }
        }

        for(int y = 0; y < 3; ++y) {
            for(int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
            }
        }

        for(int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 142));
        }
    }

    @Override
    public void onContentChanged(Inventory inv) {
        if (this.player instanceof ServerPlayerEntity) {
            ServerPlayNetworkHandler netHandler = ((ServerPlayerEntity) this.player).networkHandler;
            netHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(this.syncId, 0, this.blockEntity.getStack(0)));
        }
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int slot) {
        if (slot == 0) {
            ItemStack before = this.blockEntity.getStack(0).copy();
            ItemStack current = before.copy();
            if (!this.insertItem(current, 10, 46, true)) {
                return ItemStack.EMPTY;
            }
            this.blockEntity.removeStack(0, before.getCount() - current.getCount());

            if(player instanceof ServerPlayerEntity && blockEntity.getLastRecipe() != null)
            {
                // this sets recipe in container
                if (!blockEntity.shouldCraftRecipe(player.world, (ServerPlayerEntity) player, blockEntity.getLastRecipe()))
                {
                    return ItemStack.EMPTY;
                }
            }
            slots.get(0).onStackChanged(current, before); // calls onCrafted if different
            return this.blockEntity.getStack(0);
        }
        return super.transferSlot(player, slot);
    }

    public void close(PlayerEntity player) {
        PlayerInventory playerInventory = player.getInventory();
        if (!playerInventory.getCursorStack().isEmpty()) {
            player.dropItem(playerInventory.getCursorStack(), false);
            playerInventory.setCursorStack(ItemStack.EMPTY);
        }
        this.blockEntity.onContainerClose(this);
    }

    private class OutputSlot extends Slot {
        private PlayerEntity player;
        OutputSlot(Inventory inv, PlayerEntity player) {
            super(inv, 0, 124, 35);
            this.player = player;
        }

        @Override
        public boolean canInsert(ItemStack itemStack_1) {
            return false;
        }

        @Override
        protected void onTake(int amount) {
            AutoCraftingTableContainer.this.blockEntity.removeStack(0, amount);
        }

        @Override
        protected void onCrafted(ItemStack stack, int amount)
        {
            super.onCrafted(stack);
            // from CraftingResultsSlot onCrafted
            if (amount > 0) {
                stack.onCraft(this.player.world, this.player, amount);
            }

            if (this.inventory instanceof RecipeUnlocker) {
                ((RecipeUnlocker)this.inventory).unlockLastRecipe(this.player);
            }
        }

        @Override
        public ItemStack onTakeItem(PlayerEntity player, ItemStack stack)
        {
            onCrafted(stack, stack.getCount());
            return super.onTakeItem(player, stack);
        }
    }
}
