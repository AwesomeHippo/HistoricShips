package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.HistoricShips;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class DrakkarEntity extends OarShipEntity {
    public static final float MODEL_SCALE = 2.40F;
    public static final int MAX_PASSENGERS = 4;
    public static final int CARGO_ROWS = 3;
    public static final int MAX_ANIMALS = 2;
    public static final int MAX_HULL = 40;
    private static final float U = MODEL_SCALE / 16.0F;
    private static final ShipHull HULL = ShipHull.ofModel(U,
            -36.4F, 2.90F,
            -30.5F, 6.55F,
            30.5F, 6.55F,
            37.2F, 2.80F);

    private static final float[][] CARGO_XZ = {
        {-22.0F, -2.6F},
        {-22.0F, 2.6F},
    };

    private static final EntityDataAccessor<Byte> DATA_SAIL_STRIPE = SynchedEntityData.defineId(DrakkarEntity.class, EntityDataSerializers.BYTE);

    private static final OarShipStats STATS = new OarShipStats(
            MODEL_SCALE,
            6.55F, 0.03F, 37.2F, 8.55F, 0.10F, 42.0F, 22.0F, 44.0F,
            MAX_PASSENGERS,
            new float[][] {{18.0F, 0.0F}, {8.0F, -2.8F}, {8.0F, 2.8F}, {-16.0F, 0.0F}},
            0.28F, 5.3F,
            0.58F, 1.95F, 0.28F, 0.55F, 5.20F, 0.58F,
            0.42D, 0.18F, 0.10F, 0.05F,
            0.52D, 0.18F,
            0.024D, 0.00035D, 0.0028D, 0.989D,
            2.40D, 0.10D, 0.60F, 0.42F, 0.38F, 0.08D,
            () -> new ItemStack(HistoricShips.DRAKKAR_ITEM.get()));

    public DrakkarEntity(EntityType<? extends DrakkarEntity> type, Level level) {
        super(type, level);
    }

    public DrakkarEntity(Level level, double x, double y, double z) {
        this(HistoricShips.DRAKKAR_ENTITY.get(), level);
        this.placeAt(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SAIL_STRIPE, DrakkarSailStripe.RED.id());
    }

    @Override
    protected OarShipStats stats() {
        return STATS;
    }

    @Override
    protected ShipHull hullShape() {
        return HULL;
    }

    @Override
    protected int cargoRows() {
        return CARGO_ROWS;
    }

    @Override
    public int getMaxHull() {
        return MAX_HULL;
    }

    public DrakkarSailStripe getSailStripe() {
        return DrakkarSailStripe.byId(this.entityData.get(DATA_SAIL_STRIPE));
    }

    public void setSailStripe(DrakkarSailStripe stripe) {
        this.entityData.set(DATA_SAIL_STRIPE, stripe.id());
    }

    @Override
    protected void writeDropStack(ItemStack stack) {
        DrakkarSailStripe stripe = this.getSailStripe();
        if (stripe != DrakkarSailStripe.RED) {
            stack.set(HistoricShips.SHIP_SAIL_STRIPE.get(), (int) stripe.id());
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hit) {
        ItemStack stack = player.getItemInHand(hand);
        DyeColor dye = stack.get(DataComponents.DYE);
        DrakkarSailStripe stripe = DrakkarSailStripe.fromDye(dye);
        if (stripe != null && this.getSailStripe() != stripe) {
            if (!this.level().isClientSide()) {
                this.setSailStripe(stripe);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.LEAD)) {
            if (player.isSecondaryUseActive()) {
                if (ShipAnimalCargo.tryUnloadAnimals(player, this)) {
                    return InteractionResult.SUCCESS;
                }
            } else if (ShipAnimalCargo.tryBoardLeashed(player, this, MAX_ANIMALS)) {
                return InteractionResult.SUCCESS;
            }
        }
        return super.interact(player, hand, hit);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (passenger instanceof Player) {
            return ShipAnimalCargo.countPlayers(this) < MAX_PASSENGERS;
        }
        if (ShipAnimalCargo.isCargoAnimal(passenger)) {
            return ShipAnimalCargo.countAnimals(this) < MAX_ANIMALS;
        }
        return false;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        float seatY = this.stats().modelDeckY * this.stats().u + this.stats().seatYPad;
        float modelX;
        float modelZ;
        if (ShipAnimalCargo.isCargoAnimal(passenger)) {
            int index = ShipAnimalCargo.animalIndex(this, passenger);
            if (index < 0) {
                index = 0;
            }
            if (index >= CARGO_XZ.length) {
                index = CARGO_XZ.length - 1;
            }
            modelX = CARGO_XZ[index][0];
            modelZ = CARGO_XZ[index][1];
            seatY -= 0.10F;
        } else {
            int index = ShipAnimalCargo.playerIndex(this, passenger);
            float[][] seats = this.stats().seatXz;
            if (index < 0) {
                index = 0;
            }
            if (index >= seats.length) {
                index = seats.length - 1;
            }
            modelX = seats[index][0];
            modelZ = seats[index][1];
        }
        return ShipAnimalCargo.seatOffset(this.getYRot(), modelX, modelZ, seatY, this.stats().u);
    }

    public int getAnimalCount() {
        return ShipAnimalCargo.countAnimals(this);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setSailStripe(DrakkarSailStripe.byId(input.getByteOr("SailStripe", DrakkarSailStripe.RED.id())));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("SailStripe", this.getSailStripe().id());
    }
}
