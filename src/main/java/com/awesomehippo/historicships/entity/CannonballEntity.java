package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.NapoleonShipMod;

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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import org.jetbrains.annotations.Nullable;

public class CannonballEntity extends ThrowableItemProjectile {
    private static final float EXPLOSION_POWER = 2.25F;
    private static final float LIVING_DAMAGE = 12.0F;
    private static final int MAX_LIFE = 210;
    private static final double SPLASH_RADIUS = 6.0;
    private static final float PICK_RADIUS = 1.75F;
    private static final double OVERLAP_INFLATE = 1.5;

    private @Nullable Entity sourceShip;

    public CannonballEntity(EntityType<? extends CannonballEntity> type, Level level) {
        super(type, level);
    }

    public CannonballEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        super(NapoleonShipMod.CANNONBALL_ENTITY.get(), x, y, z, level, new ItemStack(Items.FIRE_CHARGE));
        if (owner != null) {
            this.setOwner(owner);
        }
    }

    public void setSourceShip(@Nullable Entity ship) {
        this.sourceShip = ship;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    public float getPickRadius() {
        return PICK_RADIUS;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
        if (!this.level().isClientSide() && !this.isRemoved()) {
            StoredShipEntity ship = ShipProjectileHits.findOverlappingShip(this, this.sourceShip, OVERLAP_INFLATE);
            if (ship != null && this.level() instanceof ServerLevel server) {
                this.applyHullHit(server, ship, StoredShipEntity.CANNON_HULL_DAMAGE);
                this.discard();
                return;
            }
            if (this.tickCount > MAX_LIFE) {
                this.detonate();
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
                this.applyHullHit(server, ship, StoredShipEntity.CANNON_HULL_DAMAGE);
                this.discard();
                return;
            }
            if (hit instanceof LivingEntity living) {
                living.hurtServer(server, this.damageSources().thrown(this, this.getOwner()), LIVING_DAMAGE);
            }
            this.detonate();
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide() && hitResult.getType() != HitResult.Type.ENTITY) {
            this.detonate();
        }
    }

    private void detonate() {
        if (this.isRemoved()) {
            return;
        }
        if (this.level() instanceof ServerLevel server) {
            ShipProjectileHits.splashShips(server, this, this.sourceShip, this.position(), SPLASH_RADIUS, StoredShipEntity.CANNON_HULL_DAMAGE);
            server.explode(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_POWER, false, Level.ExplosionInteraction.TNT);
            server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.1F, 0.7F + this.random.nextFloat() * 0.1F);
        }
        this.discard();
    }

    private void applyHullHit(ServerLevel server, StoredShipEntity ship, float damage) {
        if (!ship.damageHull(server, damage, this)) {
            return;
        }
        server.playSound(null, ship.getX(), ship.getY() + 1.0, ship.getZ(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.NEUTRAL, 1.05F, 0.7F + this.random.nextFloat() * 0.1F);
        server.sendParticles(ParticleTypes.EXPLOSION, ship.getX(), ship.getY() + 1.2, ship.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        server.sendParticles(ParticleTypes.SMOKE, ship.getX(), ship.getY() + 1.5, ship.getZ(), 8, 0.6, 0.4, 0.6, 0.02);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.022;
    }
}
