package com.awesomehippo.historicships.item;

import com.awesomehippo.historicships.NapoleonShipMod;
import com.awesomehippo.historicships.ShipsConfig;
import com.awesomehippo.historicships.entity.DrakkarEntity;
import com.awesomehippo.historicships.entity.DrakkarSailStripe;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DrakkarItem extends HistoricShipItem {
    public DrakkarItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Entity createShip(Level level, double x, double y, double z) {
        return new DrakkarEntity(level, x, y, z);
    }

    @Override
    public int getMaxHull() {
        return DrakkarEntity.MAX_HULL;
    }

    @Override
    public boolean isEnabled() {
        return ShipsConfig.DRAKKAR.get();
    }

    @Override
    protected void applyPlaceData(ItemStack stack, Entity boat) {
        if (boat instanceof DrakkarEntity drakkar) {
            Integer stripe = stack.get(NapoleonShipMod.SHIP_SAIL_STRIPE.get());
            if (stripe != null) {
                drakkar.setSailStripe(DrakkarSailStripe.byId(stripe.byteValue()));
            }
        }
    }
}
