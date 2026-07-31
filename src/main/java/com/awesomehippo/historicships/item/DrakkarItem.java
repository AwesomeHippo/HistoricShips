package com.awesomehippo.historicships.item;

import com.awesomehippo.historicships.entity.DrakkarEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class DrakkarItem extends HistoricShipItem {
    public DrakkarItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Entity createShip(Level level, double x, double y, double z) {
        return new DrakkarEntity(level, x, y, z);
    }
}
