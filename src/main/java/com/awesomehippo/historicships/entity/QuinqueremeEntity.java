package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.NapoleonShipMod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class QuinqueremeEntity extends OarShipEntity {
    public static final float MODEL_SCALE = 3.25F;
    public static final int MAX_PASSENGERS = 6;

    private static final OarShipStats STATS = new OarShipStats(
            MODEL_SCALE,
            5.2F, 0.06F, 40.0F, 5.8F, 0.20F, 74.0F, 22.0F, 56.0F,
            MAX_PASSENGERS,
            new float[][] {{22.0F, 0.0F}, {10.0F, -2.4F}, {10.0F, 2.4F}, {-2.0F, -2.4F}, {-2.0F, 2.4F}, {-20.0F, 0.0F}},
            0.26F, 5.2F,
            0.52F, 1.95F, 0.07F, 0.24F, 0.50F, 3.15F, 0.45F,
            0.38D, 0.16F, 0.09F, 0.045F,
            0.48D, 0.16F,
            0.022D, 0.00030D, 0.0026D, 0.988D,
            1.80D, 0.22D, 0.55F, 0.40F, 0.38F, 0.07D,
            () -> new ItemStack(NapoleonShipMod.QUINQUEREME_ITEM.get()));

    public QuinqueremeEntity(EntityType<? extends QuinqueremeEntity> type, Level level) {
        super(type, level);
    }

    public QuinqueremeEntity(Level level, double x, double y, double z) {
        this(NapoleonShipMod.QUINQUEREME_ENTITY.get(), level);
        this.placeAt(x, y, z);
    }

    @Override
    protected OarShipStats stats() {
        return STATS;
    }
}
