package com.awesomehippo.historicships.item;

import com.awesomehippo.historicships.ShipsConfig;
import com.awesomehippo.historicships.entity.NapoleonShipEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class NapoleonShipItem extends HistoricShipItem {
    public NapoleonShipItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Entity createShip(Level level, double x, double y, double z) {
        return new NapoleonShipEntity(level, x, y, z);
    }

    @Override
    public int getMaxHull() {
        return NapoleonShipEntity.MAX_HULL;
    }

    @Override
    public boolean isEnabled() {
        return ShipsConfig.NAPOLEON_SHIP.get();
    }
}
