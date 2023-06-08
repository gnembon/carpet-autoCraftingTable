package carpet_autocraftingtable;

import carpet_autocraftingtable.mixins.CraftingInventoryMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

import static net.minecraft.util.math.Direction.DOWN;

public class CraftingTableBlockEntity extends LockableContainerBlockEntity implements SidedInventory, RecipeUnlocker, RecipeInputProvider {
    public static final BlockEntityType<CraftingTableBlockEntity> TYPE = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        "carpet:crafting_table",
        BlockEntityType.Builder.create(CraftingTableBlockEntity::new, Blocks.CRAFTING_TABLE).build(null)
    );
    private static final int[] OUTPUT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] INPUT_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    public DefaultedList<ItemStack> inventory;
    public ItemStack output = ItemStack.EMPTY;
    private Recipe<?> lastRecipe;
    private final List<AutoCraftingTableContainer> openContainers = new ArrayList<>();

    public CraftingTableBlockEntity(BlockPos pos, BlockState state) {  //this(BlockEntityType.BARREL);
        super(TYPE, pos, state);
        this.inventory = DefaultedList.ofSize(9, ItemStack.EMPTY);
        ((CraftingInventoryMixin) craftingInventory).setInventory(this.inventory);
    }

    private final CraftingInventory craftingInventory = new CraftingInventory(null, 3, 3);

    public static void init() { } // registers BE type

    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        Inventories.writeNbt(tag, inventory);
        tag.put("Output", output.writeNbt(new NbtCompound()));
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        Inventories.readNbt(tag, inventory);
        this.output = ItemStack.fromNbt(tag.getCompound("Output"));
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.crafting");
    }

    @Override
    protected ScreenHandler createScreenHandler(int id, PlayerInventory playerInventory) {
        AutoCraftingTableContainer container = new AutoCraftingTableContainer(id, playerInventory, this);
        this.openContainers.add(container);
        return container;
    }

    @Override
    public int[] getAvailableSlots(Direction dir) {
        return (dir == DOWN && (!output.isEmpty() || getCurrentRecipe().isPresent())) ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return slot > 0 && getStack(slot).isEmpty();
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot != 0 || !output.isEmpty() || getCurrentRecipe().isPresent();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot != 0 && slot <= size();
    }

    @Override
    public int size() {
        return 10;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) return false;
        }
        return output.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot > 0) return this.inventory.get(slot - 1);
        if (!output.isEmpty()) return output;
        Optional<CraftingRecipe> recipe = getCurrentRecipe();
        return recipe.map(craftingRecipe -> craftingRecipe.craft(craftingInventory, Objects.requireNonNull(getWorld()).getRegistryManager())).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot == 0) {
            if (output.isEmpty()) output = craft();
            return output.split(amount);
        }
        return Inventories.splitStack(this.inventory, slot - 1, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot == 0) {
            ItemStack output = this.output;
            this.output = ItemStack.EMPTY;
            return output;
        }
        return Inventories.removeStack(this.inventory, slot - 1);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot == 0) {
            output = stack;
            return;
        }
        inventory.set(slot - 1, stack);
        markDirty();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        for (AutoCraftingTableContainer c : openContainers) c.onContentChanged(this);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return player.getBlockPos().getSquaredDistance(this.pos) <= 64.0D;
    }

    @Override
    public void provideRecipeInputs(RecipeMatcher finder) {
        for (ItemStack stack : this.inventory) finder.addInput(stack);
    }

    @Override
    public void setLastRecipe(Recipe<?> recipe) {
        lastRecipe = recipe;
    }

    @Override
    public Recipe<?> getLastRecipe() {
        return lastRecipe;
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    private Optional<CraftingRecipe> getCurrentRecipe() {
        // No need to find recipes if the inventory is empty. Cannot craft anything.
        if (this.getWorld() == null || this.isEmpty()) return Optional.empty();

        CraftingRecipe lastRecipe = (CraftingRecipe) getLastRecipe();
        RecipeManager manager = this.getWorld().getRecipeManager();

        if (lastRecipe != null) {
            Map<Identifier, CraftingRecipe> allRecipes = manager.getAllOfType(RecipeType.CRAFTING);
            CraftingRecipe mapRecipe = null;
            for (CraftingRecipe recipe : allRecipes.values()) {
                if (recipe.equals(lastRecipe)) {
                    mapRecipe = recipe;
                    break;
                }
            }
            if (mapRecipe != null && mapRecipe.matches(craftingInventory, getWorld())) {
                return Optional.of(lastRecipe);
            }
        }
        Optional<CraftingRecipe> recipe = manager.getFirstMatch(RecipeType.CRAFTING, craftingInventory, getWorld());
        recipe.ifPresent(this::setLastRecipe);
        return recipe;
    }

    private ItemStack craft() {
        if (this.getWorld() == null) return ItemStack.EMPTY;
        Optional<CraftingRecipe> optionalRecipe = getCurrentRecipe();
        if (optionalRecipe.isEmpty()) return ItemStack.EMPTY;
        CraftingRecipe recipe = optionalRecipe.get();
        ItemStack result = recipe.craft(craftingInventory, getWorld().getRegistryManager());
        DefaultedList<ItemStack> remaining = getWorld().getRecipeManager().getRemainingStacks(RecipeType.CRAFTING, craftingInventory, getWorld());
        for (int i = 0; i < 9; i++) {
            ItemStack current = inventory.get(i);
            ItemStack remainingStack = remaining.get(i);
            if (!current.isEmpty()) current.decrement(1);
            if (!remainingStack.isEmpty()) {
                if (current.isEmpty()) {
                    inventory.set(i, remainingStack);
                } else if (ItemStack.canCombine(current, remainingStack)) {
                    current.increment(remainingStack.getCount());
                } else {
                    ItemScatterer.spawn(getWorld(), pos.getX(), pos.getY(), pos.getZ(), remainingStack);
                }
            }
        }
        markDirty();
        return result;
    }

    public void onContainerClose(AutoCraftingTableContainer container) {
        this.openContainers.remove(container);
    }

    public boolean matches(Recipe<? super CraftingInventory> recipe) {
        return this.getWorld() != null && recipe.matches(this.craftingInventory, this.getWorld());
    }
}
