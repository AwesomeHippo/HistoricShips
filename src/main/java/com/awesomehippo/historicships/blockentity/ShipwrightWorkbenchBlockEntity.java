package com.awesomehippo.historicships.blockentity;

import com.awesomehippo.historicships.HistoricShips;
import com.awesomehippo.historicships.menu.ShipwrightMenu;
import com.awesomehippo.historicships.recipe.ShipAssemblyRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ShipwrightWorkbenchBlockEntity extends BaseContainerBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.historicships.shipwright");
    private NonNullList<ItemStack> items = NonNullList.withSize(ShipAssemblyRecipe.TOTAL_SLOTS, ItemStack.EMPTY);

    public ShipwrightWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(HistoricShips.SHIPWRIGHT_WORKBENCH_BE.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return ShipAssemblyRecipe.TOTAL_SLOTS;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ShipwrightMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != ShipAssemblyRecipe.RESULT_SLOT;
    }
}
