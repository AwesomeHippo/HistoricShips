package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.NapoleonShipMod;
import com.awesomehippo.historicships.network.SailPaintPacket;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.Nullable;

public class QuinqueremeEntity extends OarShipEntity {
    public static final float MODEL_SCALE = 3.25F;
    public static final int MAX_PASSENGERS = 6;
    public static final int CARGO_ROWS = 4;
    public static final int MAX_ANIMALS = 4;
    public static final int MAX_HULL = 55;
    public static final int TOWER_COOLDOWN = 28;
    private static final float U = MODEL_SCALE / 16.0F;
    private static final float TOWER_MODEL_X = -18.0F;
    private static final float TOWER_MUZZLE_Y = 13.6F;
    private static final float STONE_SPEED = 1.55F;
    private static final int VOLLEY = 3;

    private static final EntityDataAccessor<Integer> DATA_TOWER_UNTIL = SynchedEntityData.defineId(QuinqueremeEntity.class, EntityDataSerializers.INT);

    @Nullable
    private byte[] sailPaint;
    private int sailPaintVersion;

    private static final float[][] CARGO_XZ = {
        {6.0F, 0.0F},
        {-2.0F, -2.2F},
        {-2.0F, 2.2F},
        {-14.0F, 0.0F},
    };

    private static final float RAM_X0 = 56.2F;
    private static final ShipHull HULL = ShipHull.ofModel(U,
            -54.5F, 2.90F,
            -51.0F, 4.50F,
            -42.0F, 5.22F,
            40.0F, 5.22F,
            47.3F, 3.70F,
            53.2F, 2.25F,
            RAM_X0, 1.20F,
            66.8F, 0.55F);

    private static final OarShipStats STATS = new OarShipStats(
            MODEL_SCALE,
            5.22F, 0.04F, 40.0F, 5.85F, 0.12F, 74.0F, 22.0F, 56.0F,
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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TOWER_UNTIL, 0);
    }

    @Override
    protected int cargoRows() {
        return CARGO_ROWS;
    }

    @Override
    public int getMaxHull() {
        return MAX_HULL;
    }

    @Override
    protected ShipHull hullShape() {
        return HULL;
    }

    @Override
    protected float ramAlongMin() {
        return RAM_X0 * U;
    }

    @Override
    protected float ramDamage() {
        return 12.0F;
    }

    @Nullable
    public byte[] getSailPaint() {
        return this.sailPaint;
    }

    public int getSailPaintVersion() {
        return this.sailPaintVersion;
    }

    public void setSailPaintData(@Nullable byte[] pixels) {
        this.sailPaint = pixels;
        this.sailPaintVersion++;
    }

    public boolean canEditSail(Player player) {
        if (this.isRemoved() || this.isSinking() || !this.isOwner(player)) {
            return false;
        }
        double reach = this.halfLoa() + 8.0D;
        return player.distanceToSqr(this) <= reach * reach;
    }

    public void applySailPaint(byte[] pixels) {
        byte[] stored = pixels.length == 0 ? null : pixels.clone();
        this.setSailPaintData(stored);
        PacketDistributor.sendToPlayersTrackingEntity(this, new SailPaintPacket(this.getId(), stored == null ? new byte[0] : stored));
        this.level().playSound(null, this.getX(), this.getY() + 2.0, this.getZ(), SoundEvents.DYE_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    protected void writeDropStack(ItemStack stack) {
        if (this.sailPaint != null) {
            stack.set(NapoleonShipMod.SHIP_SAIL_PAINT.get(), new SailPaint.Data(this.sailPaint));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.sailPaint = SailPaint.fromInts(input.getIntArray("SailPaint").orElse(null));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.sailPaint != null) {
            output.putIntArray("SailPaint", SailPaint.toInts(this.sailPaint));
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(NapoleonShipMod.SAIL_BRUSH.get()) && this.canEditSail(player)) {
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

    public int getTowerCooldown() {
        return Math.max(0, this.entityData.get(DATA_TOWER_UNTIL) - (int) this.level().getGameTime());
    }

    public boolean tryFireTower() {
        if (this.getTowerCooldown() > 0) {
            return false;
        }
        this.entityData.set(DATA_TOWER_UNTIL, (int) this.level().getGameTime() + TOWER_COOLDOWN);
        return true;
    }

    public void serverFireTower(@Nullable LivingEntity shooter) {
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (this.getTowerCooldown() > 0) {
            return;
        }
        this.entityData.set(DATA_TOWER_UNTIL, (int) this.level().getGameTime() + TOWER_COOLDOWN);

        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double bowX = Mth.sin(yaw);
        double bowZ = -Mth.cos(yaw);
        double stbdX = -bowZ;
        double stbdZ = bowX;

        double mx = this.getX() + bowX * (TOWER_MODEL_X * U);
        double my = this.getY() + TOWER_MUZZLE_Y * U;
        double mz = this.getZ() + bowZ * (TOWER_MODEL_X * U);

        mx += bowX * 0.95;
        mz += bowZ * 0.95;

        Vec3 shipVel = this.getDeltaMovement();
        for (int i = 0; i < VOLLEY; i++) {
            double lat = (i - 1) * 0.22;
            double ox = mx + stbdX * lat + (this.random.nextDouble() - 0.5) * 0.04;
            double oy = my + this.random.nextDouble() * 0.08;
            double oz = mz + stbdZ * lat + (this.random.nextDouble() - 0.5) * 0.04;

            StoneBulletEntity stone = new StoneBulletEntity(server, ox, oy, oz, shooter);
            stone.setSourceShip(this);
            double spreadYaw = (i - 1) * 0.018 + (this.random.nextDouble() - 0.5) * 0.012;
            double dirX = bowX * Math.cos(spreadYaw) - bowZ * Math.sin(spreadYaw);
            double dirZ = bowX * Math.sin(spreadYaw) + bowZ * Math.cos(spreadYaw);
            double up = 0.78 + this.random.nextDouble() * 0.10;
            double spd = STONE_SPEED * (0.90 + this.random.nextDouble() * 0.08);
            stone.setDeltaMovement(dirX * spd + shipVel.x * 0.45, up + shipVel.y * 0.1, dirZ * spd + shipVel.z * 0.45);
            server.addFreshEntity(stone);
        }

        server.playSound(null, mx, my, mz, SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 1.15F, 0.7F + this.random.nextFloat() * 0.15F);
        server.playSound(null, mx, my, mz, SoundEvents.STONE_HIT, SoundSource.NEUTRAL, 0.95F, 0.85F + this.random.nextFloat() * 0.15F);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), mx + bowX * 0.4, my, mz + bowZ * 0.4, 16, 0.25, 0.15, 0.25, 0.06);
        server.sendParticles(ParticleTypes.CLOUD, mx + bowX * 0.3, my, mz + bowZ * 0.3, 8, 0.18, 0.10, 0.18, 0.04);
    }
}
