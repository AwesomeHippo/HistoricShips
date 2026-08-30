package com.awesomehippo.historicships.menu;

import com.awesomehippo.historicships.HistoricShips;
import com.awesomehippo.historicships.entity.NapoleonShipEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class NapoleonEngineMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 166;
    public static final int FUEL_SLOT = 0;
    public static final int WATER_SLOT = 1;
    public static final int ENGINE_SLOTS = 2;

    public static final int DATA_WATER = 0;
    public static final int DATA_MAX_WATER = 1;
    public static final int DATA_LIT = 2;
    public static final int DATA_LIT_TOTAL = 3;
    public static final int DATA_PRESSURE = 4;
    public static final int DATA_COUNT = 5;

    private final Container engine;
    private final ContainerData data;
    private final NapoleonShipEntity ship;

    public NapoleonEngineMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(ENGINE_SLOTS), new SimpleContainerData(DATA_COUNT), null);
    }

    public NapoleonEngineMenu(int containerId, Inventory playerInv, NapoleonShipEntity ship) {
        this(containerId, playerInv, ship.getEngineContainer(), ship.getEngineData(), ship);
    }

    public NapoleonEngineMenu(int containerId, Inventory playerInv, Container engine, ContainerData data, NapoleonShipEntity ship) {
        super(HistoricShips.ENGINE_MENU.get(), containerId);
        checkContainerSize(engine, ENGINE_SLOTS);
        checkContainerDataCount(data, DATA_COUNT);
        this.engine = engine;
        this.data = data;
        this.ship = ship;
        engine.startOpen(playerInv.player);

        this.addSlot(new Slot(engine, FUEL_SLOT, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return NapoleonShipEntity.isEngineFuel(stack);
            }
        });
        this.addSlot(new Slot(engine, WATER_SLOT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addStandardInventorySlots(playerInv, 8, 84);
        this.addDataSlots(data);
    }

    public int getWater() {
        return this.data.get(DATA_WATER);
    }

    public int getMaxWater() {
        return Math.max(1, this.data.get(DATA_MAX_WATER));
    }

    public int getLitTime() {
        return this.data.get(DATA_LIT);
    }

    public int getLitTotal() {
        return Math.max(1, this.data.get(DATA_LIT_TOTAL));
    }

    public int getPressure() {
        return this.data.get(DATA_PRESSURE);
    }

    public boolean isLit() {
        return this.getLitTime() > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.ship != null) {
            return this.ship.isAlive() && player.distanceToSqr(this.ship) < 64.0 * 64.0;
        }
        return this.engine.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.engine.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < ENGINE_SLOTS) {
                if (!this.moveItemStackTo(stack, ENGINE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(Items.WATER_BUCKET)) {
                if (!this.moveItemStackTo(stack, WATER_SLOT, WATER_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (NapoleonShipEntity.isEngineFuel(stack)) {
                if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < ENGINE_SLOTS + 27) {
                if (!this.moveItemStackTo(stack, ENGINE_SLOTS + 27, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, ENGINE_SLOTS, ENGINE_SLOTS + 27, false)) {
                return ItemStack.EMPTY;
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
