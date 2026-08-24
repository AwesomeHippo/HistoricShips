package com.awesomehippo.historicships.item;

import com.awesomehippo.historicships.ShipsConfig;
import com.awesomehippo.historicships.entity.QuinqueremeEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class QuinqueremeItem extends HistoricShipItem {
    public QuinqueremeItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Entity createShip(Level level, double x, double y, double z) {
        return new QuinqueremeEntity(level, x, y, z);
    }

    @Override
    public int getMaxHull() {
        return QuinqueremeEntity.MAX_HULL;
    }

    @Override
    public boolean isEnabled() {
        return ShipsConfig.QUINQUEREME.get();
    }
}
