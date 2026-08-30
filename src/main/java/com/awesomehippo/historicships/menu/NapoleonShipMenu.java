package com.awesomehippo.historicships.menu;

import com.awesomehippo.historicships.HistoricShips;
import com.awesomehippo.historicships.entity.NapoleonShipEntity;

import net.minecraft.server.level.ServerPlayer;
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

public class NapoleonShipMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 176;
    public static final int CARGO_HEIGHT = 222;
    public static final int ENGINE_HEIGHT = 166;
    public static final int CARGO_ROWS = NapoleonShipEntity.CARGO_ROWS;
    public static final int CARGO_SLOTS = CARGO_ROWS * 9;
    public static final int FUEL_SLOT = 0;
    public static final int WATER_SLOT = 1;
    public static final int ENGINE_SLOTS = 2;
    public static final int TAB_CARGO = 0;
    public static final int TAB_ENGINE = 1;

    public static final int DATA_WATER = 0;
    public static final int DATA_MAX_WATER = 1;
    public static final int DATA_LIT = 2;
    public static final int DATA_LIT_TOTAL = 3;
    public static final int DATA_PRESSURE = 4;
    public static final int DATA_COUNT = 5;

    private static final int CARGO_PLAYER_Y = 18 + CARGO_ROWS * 18 + 13;
    private static final int ENGINE_PLAYER_Y = 84;

    private final Container cargo;
    private final Container engine;
    private final ContainerData data;
    private final NapoleonShipEntity ship;
    private int tab = TAB_CARGO;

    public NapoleonShipMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(CARGO_SLOTS), new SimpleContainer(ENGINE_SLOTS), new SimpleContainerData(DATA_COUNT), null, TAB_CARGO);
    }

    public NapoleonShipMenu(int containerId, Inventory playerInv, NapoleonShipEntity ship) {
        this(containerId, playerInv, ship, TAB_CARGO);
    }

    public NapoleonShipMenu(int containerId, Inventory playerInv, NapoleonShipEntity ship, int tab) {
        this(containerId, playerInv, ship, ship.getEngineContainer(), ship.getEngineData(), ship, tab);
    }

    public NapoleonShipMenu(int containerId, Inventory playerInv, Container cargo, Container engine, ContainerData data, NapoleonShipEntity ship, int tab) {
        super(HistoricShips.NAPOLEON_MENU.get(), containerId);
        checkContainerSize(cargo, CARGO_SLOTS);
        checkContainerSize(engine, ENGINE_SLOTS);
        checkContainerDataCount(data, DATA_COUNT);
        this.cargo = cargo;
        this.engine = engine;
        this.data = data;
        this.ship = ship;
        this.tab = tab == TAB_ENGINE ? TAB_ENGINE : TAB_CARGO;
        cargo.startOpen(playerInv.player);
        engine.startOpen(playerInv.player);

        for (int y = 0; y < CARGO_ROWS; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new TabSlot(cargo, x + y * 9, 8 + x * 18, 18 + y * 18, TAB_CARGO));
            }
        }
        this.addSlot(new TabSlot(engine, FUEL_SLOT, 56, 53, TAB_ENGINE) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return NapoleonShipEntity.isEngineFuel(stack);
            }
        });
        this.addSlot(new TabSlot(engine, WATER_SLOT, 56, 17, TAB_ENGINE) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addStandardInventorySlots(playerInv, 8, this.tab == TAB_ENGINE ? ENGINE_PLAYER_Y : CARGO_PLAYER_Y);
        this.addDataSlots(data);
    }

    public int getTab() {
        return this.tab;
    }

    public int guiHeight() {
        return this.tab == TAB_ENGINE ? ENGINE_HEIGHT : CARGO_HEIGHT;
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
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != TAB_CARGO && buttonId != TAB_ENGINE) {
            return false;
        }
        if (buttonId == this.tab) {
            return true;
        }
        if (this.ship != null && player instanceof ServerPlayer) {
            this.ship.openShipMenu(player, buttonId);
            return true;
        }
        this.tab = buttonId;
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.ship != null) {
            return this.ship.isAlive() && player.distanceToSqr(this.ship) < 64.0 * 64.0;
        }
        return this.cargo.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.cargo.stopOpen(player);
        this.engine.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int engineStart = CARGO_SLOTS;
            int playerStart = CARGO_SLOTS + ENGINE_SLOTS;
            int playerInvEnd = playerStart + 27;
            if (index < CARGO_SLOTS) {
                if (!this.moveItemStackTo(stack, playerStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < playerStart) {
                if (!this.moveItemStackTo(stack, playerStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.tab == TAB_ENGINE) {
                if (stack.is(Items.WATER_BUCKET)) {
                    if (!this.moveItemStackTo(stack, engineStart + WATER_SLOT, engineStart + WATER_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (NapoleonShipEntity.isEngineFuel(stack)) {
                    if (!this.moveItemStackTo(stack, engineStart + FUEL_SLOT, engineStart + FUEL_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < playerInvEnd) {
                    if (!this.moveItemStackTo(stack, playerInvEnd, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, playerStart, playerInvEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, CARGO_SLOTS, false)) {
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

    private class TabSlot extends Slot {
        private final int tab;

        TabSlot(Container container, int index, int x, int y, int tab) {
            super(container, index, x, y);
            this.tab = tab;
        }

        @Override
        public boolean isActive() {
            return NapoleonShipMenu.this.tab == this.tab;
        }
    }
}
