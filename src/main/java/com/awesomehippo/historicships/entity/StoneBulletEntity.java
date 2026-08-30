package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.HistoricShips;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public class StoneBulletEntity extends ThrowableItemProjectile {
    private static final float DAMAGE = 9.0F;
    private static final int MAX_LIFE = 120;
    private static final float PICK_RADIUS = 1.5F;
    private static final double SPLASH_RADIUS = 4.0;
    private static final double OVERLAP_INFLATE = 1.25;

    private @Nullable Entity sourceShip;

    public StoneBulletEntity(EntityType<? extends StoneBulletEntity> type, Level level) {
        super(type, level);
    }

    public StoneBulletEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        super(HistoricShips.STONE_BULLET_ENTITY.get(), x, y, z, level, new ItemStack(Items.COBBLESTONE));
        if (owner != null) {
            this.setOwner(owner);
        }
    }

    public void setSourceShip(@Nullable Entity ship) {
        this.sourceShip = ship;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    public float getPickRadius() {
        return PICK_RADIUS;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
            if (this.random.nextBoolean()) {
                this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
        if (!this.level().isClientSide() && !this.isRemoved()) {
            StoredShipEntity ship = ShipProjectileHits.findOverlappingShip(this, this.sourceShip, OVERLAP_INFLATE);
            if (ship != null && this.level() instanceof ServerLevel server) {
                ship.damageHull(server, StoredShipEntity.STONE_HULL_DAMAGE, this);
                this.shatter(this.position(), false);
                return;
            }
            if (this.tickCount > MAX_LIFE) {
                this.shatter(null, true);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)) {
            return false;
        }
        if (ShipProjectileHits.isFromSourceShip(this.sourceShip, entity)) {
            return false;
        }
        if (ShipProjectileHits.isFriendlyFire(this.getOwner(), entity)) {
            return false;
        }
        if (entity instanceof StoredShipEntity ship && !ship.hullContains(this.position(), Math.max(this.getBbWidth() * 0.5, 0.2))) {
            return false;
        }
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity hit = hitResult.getEntity();
        if (ShipProjectileHits.isFromSourceShip(this.sourceShip, hit) || ShipProjectileHits.isFriendlyFire(this.getOwner(), hit)) {
            return;
        }
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel server) {
            StoredShipEntity ship = ShipProjectileHits.resolveShip(hit);
            if (ship != null) {
                if (ShipProjectileHits.isFromSourceShip(this.sourceShip, ship)) {
                    return;
                }
                ship.damageHull(server, StoredShipEntity.STONE_HULL_DAMAGE, this);
                this.shatter(hitResult.getLocation(), false);
                return;
            }
            hit.hurtServer(server, this.damageSources().thrown(this, this.getOwner()), DAMAGE);
            Vec3 v = this.getDeltaMovement().normalize().scale(0.55);
            hit.push(v.x, 0.22, v.z);
            this.shatter(hitResult.getLocation(), true);
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide() && hitResult.getType() != HitResult.Type.ENTITY) {
            this.shatter(hitResult.getLocation(), true);
        }
    }

    private void shatter(@Nullable Vec3 at, boolean splashHull) {
        if (this.isRemoved()) {
            return;
        }
        if (this.level() instanceof ServerLevel server) {
            double x = at != null ? at.x : this.getX();
            double y = at != null ? at.y : this.getY();
            double z = at != null ? at.z : this.getZ();
            if (splashHull) {
                ShipProjectileHits.splashShips(server, this, this.sourceShip, new Vec3(x, y, z), SPLASH_RADIUS, StoredShipEntity.STONE_HULL_DAMAGE);
            }
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), x, y, z, 18, 0.22, 0.18, 0.22, 0.08);
            server.sendParticles(ParticleTypes.CLOUD, x, y, z, 6, 0.15, 0.1, 0.15, 0.02);
            server.playSound(null, x, y, z, SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 1.1F, 0.75F + this.random.nextFloat() * 0.2F);
        }
        this.discard();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.036;
    }
}
