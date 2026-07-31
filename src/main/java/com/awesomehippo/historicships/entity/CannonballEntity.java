package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.NapoleonShipMod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    private static final float EXPLOSION_POWER = 11.0F;
    private static final int MAX_LIFE = 210;

    public CannonballEntity(EntityType<? extends CannonballEntity> type, Level level) {
        super(type, level);
    }

    public CannonballEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        super(NapoleonShipMod.CANNONBALL_ENTITY.get(), x, y, z, level, new ItemStack(Items.FIRE_CHARGE));
        if (owner != null) {
            this.setOwner(owner);
        }
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
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
        if (!this.level().isClientSide() && this.tickCount > MAX_LIFE) {
            this.detonate();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);

        // ignore hit on own ship
        if (hitResult.getEntity() instanceof NapoleonShipEntity) {
            return;
        }
        if (!this.level().isClientSide()) {
            this.detonate();
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            this.detonate();
        }
    }

    private void detonate() {
        if (this.isRemoved()) {
            return;
        }
        if (this.level() instanceof ServerLevel server) {
            server.explode(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_POWER, false, Level.ExplosionInteraction.TNT);
            server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 3.0F, 0.65F + this.random.nextFloat() * 0.12F);
        }
        this.discard();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.022;
    }
}
