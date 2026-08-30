package com.awesomehippo.historicships.menu;

import com.awesomehippo.historicships.HistoricShips;
import com.awesomehippo.historicships.blockentity.ShipwrightWorkbenchBlockEntity;
import com.awesomehippo.historicships.recipe.ShipAssemblyRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ShipwrightMenu extends AbstractContainerMenu {

    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 212;

    public static final int MAT_X = 8;
    public static final int MAT_Y = 18;
    public static final int MAT_ROWS = 2;

    public static final int RECIPE_Y0 = 56;
    public static final int RECIPE_H = 20;
    public static final int PREVIEW_X = 8;

    public static final int BUTTON_X = 90;
    public static final int BUTTON_W = 42;
    public static final int BUTTON_H = 16;

    public static final int RESULT_X = 152;
    public static final int RESULT_Y = 76;

    public static final int INV_Y = 130;

    private final Container container;
    private final ContainerLevelAccess access;

    public ShipwrightMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(ShipAssemblyRecipe.TOTAL_SLOTS), ContainerLevelAccess.NULL);
    }

    public ShipwrightMenu(int containerId, Inventory playerInv, Container container) {
        this(containerId, playerInv, container, container instanceof ShipwrightWorkbenchBlockEntity be && be.getLevel() != null ? ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()) : ContainerLevelAccess.NULL);
    }

    public ShipwrightMenu(int containerId, Inventory playerInv, Container container, ContainerLevelAccess access) {
        super(HistoricShips.SHIPWRIGHT_MENU.get(), containerId);
        this.container = container;
        this.access = access;
        container.startOpen(playerInv.player);

        for (int row = 0; row < MAT_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(container, col + row * 9, MAT_X + col * 18, MAT_Y + row * 18));
            }
        }

        this.addSlot(new Slot(container, ShipAssemblyRecipe.RESULT_SLOT, RESULT_X, RESULT_Y) { @Override public boolean mayPlace(ItemStack stack) { return false; } });

        this.addStandardInventorySlots(playerInv, MAT_X, INV_Y);
    }

    public Container getContainer() {
        return this.container;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (player.level().isClientSide()) {
            return true;
        }
        ShipAssemblyRecipe recipe = ShipAssemblyRecipe.byIndex(buttonId);
        if (recipe == null) {
            return false;
        }
        if (!recipe.matches(this.container) || !recipe.canPlaceResult(this.container)) {
            return false;
        }

        recipe.consume(this.container);
        ItemStack crafted = recipe.resultStack();
        ItemStack existing = this.container.getItem(ShipAssemblyRecipe.RESULT_SLOT);
        if (existing.isEmpty()) {
            this.container.setItem(ShipAssemblyRecipe.RESULT_SLOT, crafted);
        } else {
            existing.grow(crafted.getCount());
            this.container.setItem(ShipAssemblyRecipe.RESULT_SLOT, existing);
        }
        this.container.setChanged();
        this.broadcastChanges();

        this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.15F));

        player.sendOverlayMessage(Component.translatable("gui.historicships.assembled", recipe.title));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int containerEnd = ShipAssemblyRecipe.TOTAL_SLOTS;
            if (index < containerEnd) {
                if (!this.moveItemStackTo(stack, containerEnd, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, ShipAssemblyRecipe.INPUT_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
