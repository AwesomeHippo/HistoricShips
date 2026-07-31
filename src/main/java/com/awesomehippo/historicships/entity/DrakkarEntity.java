package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.NapoleonShipMod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DrakkarEntity extends OarShipEntity {
    public static final float MODEL_SCALE = 2.40F;
    public static final int MAX_PASSENGERS = 4;

    private static final OarShipStats STATS = new OarShipStats(
            MODEL_SCALE,
            6.5F, 0.06F, 34.0F, 8.0F, 0.20F, 42.0F, 22.0F, 44.0F,
            MAX_PASSENGERS,
            new float[][] {{18.0F, 0.0F}, {8.0F, -2.8F}, {8.0F, 2.8F}, {-16.0F, 0.0F}},
            0.28F, 5.3F,
            0.58F, 2.35F, 0.08F, 0.28F, 0.55F, 5.20F, 0.58F,
            0.42D, 0.18F, 0.10F, 0.05F,
            0.52D, 0.18F,
            0.024D, 0.00035D, 0.0028D, 0.989D,
            2.40D, 0.10D, 0.60F, 0.42F, 0.38F, 0.08D,
            () -> new ItemStack(NapoleonShipMod.DRAKKAR_ITEM.get()));

    public DrakkarEntity(EntityType<? extends DrakkarEntity> type, Level level) {
        super(type, level);
    }

    public DrakkarEntity(Level level, double x, double y, double z) {
        this(NapoleonShipMod.DRAKKAR_ENTITY.get(), level);
        this.placeAt(x, y, z);
    }

    @Override
    protected OarShipStats stats() {
        return STATS;
    }
}
