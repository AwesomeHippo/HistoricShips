package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.NapoleonShipMod;

import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class StoredShipEntity extends Entity implements HasCustomInventoryScreen, ContainerEntity {
    public static final float CANNON_HULL_DAMAGE = 14.0F;
    public static final float STONE_HULL_DAMAGE = 10.0F;
    private static final float MAX_GENERIC_HIT = 10.0F;
    private static final float MAX_EXPLOSION_HULL = 8.0F;
    private static final float AXE_HULL_MUL = 1.75F;
    private static final int HURT_INVULN_TICKS = 10;
    public static final int SINK_DURATION = 100;

    private static final EntityDataAccessor<Integer> DATA_HULL = SynchedEntityData.defineId(StoredShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SINK_TICKS = SynchedEntityData.defineId(StoredShipEntity.class, EntityDataSerializers.INT);

    private NonNullList<ItemStack> itemStacks;
    private @Nullable ResourceKey<LootTable> lootTable;
    private long lootTableSeed;
    private int hull;
    private int hullInvuln;
    private @Nullable UUID ownerUuid;
    private int sinkTicks;
    private int sinkTicksPrev;
    private int sinkTicksSync;
    private @Nullable LivingEntity sinkingBreaker;
    private int outOfWaterTicks;

    protected StoredShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.itemStacks = NonNullList.withSize(this.cargoSlotCount(), ItemStack.EMPTY);
        this.hull = this.getMaxHull();
    }

    public void setOwner(@Nullable Player player) {
        this.ownerUuid = player != null ? player.getUUID() : null;
    }

    public boolean isOwner(Player player) {
        if (this.ownerUuid == null) {
            return true;
        }
        return this.ownerUuid.equals(player.getUUID());
    }

    protected abstract int cargoRows();

    protected abstract ItemStack createDropStack();

    public abstract int getMaxHull();

    protected abstract float halfLoa();

    protected abstract float halfBeam();

    protected abstract float hullHeight();

    protected abstract float cullHalfLoa();

    protected abstract float cullHalfBeam();

    protected abstract float cullHeight();

    protected abstract void controlShip();

    protected float hullBottomPad() {
        return 0.5F;
    }

    protected double swellRate() {
        return 0.024D;
    }

    protected double swellAmp() {
        return 0.00035D;
    }

    protected double buoyLift() {
        return 0.0028D;
    }

    protected double waterDrag() {
        return 0.989D;
    }

    public final int cargoSlotCount() {
        return this.cargoRows() * 9;
    }

    public int getHull() {
        return this.level().isClientSide() ? this.entityData.get(DATA_HULL) : this.hull;
    }

    public void setHull(int value) {
        this.hull = Math.min(this.getMaxHull(), Math.max(1, value));
        this.syncHull();
    }

    public int getHullPercent() {
        int max = Math.max(1, this.getMaxHull());
        return Math.min(100, Math.max(0, (this.getHull() * 100) / max));
    }

    // 0 pristine, 1 damaged (<=66%), 2 wrecked (<=33%)
    public int getDamageStage() {
        int pct = this.getHullPercent();
        if (pct > 66) {
            return 0;
        }
        if (pct > 33) {
            return 1;
        }
        return 2;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HULL, this.getMaxHull());
        builder.define(DATA_SINK_TICKS, 0);
    }

    private void syncHull() {
        this.entityData.set(DATA_HULL, this.hull);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.hullInvuln > 0) {
            this.hullInvuln--;
        }
        this.sinkTicksPrev = this.sinkTicksSync;
        this.sinkTicksSync = this.entityData.get(DATA_SINK_TICKS);
        if (this.isSinking()) {
            this.tickSinking();
        }
    }

    public boolean isSinking() {
        return this.level().isClientSide() ? this.entityData.get(DATA_SINK_TICKS) > 0 : this.sinkTicks > 0;
    }

    public float getSinkProgress(float partialTicks) {
        if (!this.isSinking()) {
            return 0.0F;
        }
        float ticks = this.level().isClientSide() ? Mth.lerp(partialTicks, this.sinkTicksPrev, this.sinkTicksSync) : this.sinkTicks;
        return Mth.clamp(ticks / (float) SINK_DURATION, 0.0F, 1.0F);
    }

    public float getSinkRollDir() {
        return this.getId() % 2 == 0 ? 1.0F : -1.0F;
    }

    protected void applySinkMotion() {
        Vec3 v = this.getDeltaMovement();
        double vy = Math.max(v.y - 0.008, -0.055);
        this.setDeltaMovement(v.x * 0.90, vy, v.z * 0.90);
    }

    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        return this.shipBox(pos, this.halfLoa(), this.halfBeam(), this.hullHeight());
    }

    public AABB makeCullBox() {
        return this.shipBox(this.position(), this.cullHalfLoa(), this.cullHalfBeam(), this.cullHeight());
    }

    private AABB shipBox(Vec3 pos, float halfLoa, float halfBeam, float height) {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        float bowX = Mth.sin(yaw);
        float bowZ = -Mth.cos(yaw);
        float stbdX = -bowZ;
        float stbdZ = bowX;

        double minX = pos.x;
        double maxX = pos.x;
        double minZ = pos.z;
        double maxZ = pos.z;
        for (int fl = -1; fl <= 1; fl += 2) {
            for (int s = -1; s <= 1; s += 2) {
                double x = pos.x + fl * halfLoa * bowX + s * halfBeam * stbdX;
                double z = pos.z + fl * halfLoa * bowZ + s * halfBeam * stbdZ;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        return new AABB(minX, pos.y - this.hullBottomPad(), minZ, maxX, pos.y + height, maxZ);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canSprint() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected void doWaterSplashEffect() {}

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        LivingEntity player = ShipAnimalCargo.firstPlayer(this);
        return player != null ? player : super.getControllingPassenger();
    }

    public boolean isConductor(Entity passenger) {
        return this.getControllingPassenger() == passenger;
    }

    protected void syncShipPosition() {
        if (this.isClientAuthoritative()) {
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        }
    }

    protected void tickMovement() {
        if (this.isSinking()) {
            this.applySinkMotion();
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else if (this.isLocalInstanceAuthoritative()) {
            this.updateWaterContact();
            this.applyBuoyancy();
            if (this.level().isClientSide()) {
                this.controlShip();
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
        this.setBoundingBox(this.makeBoundingBox());
        this.resolveHullCollisions();
    }

    private void updateWaterContact() {
        if (this.isInWater()) {
            this.outOfWaterTicks = 0;
        } else {
            this.outOfWaterTicks = Math.min(this.outOfWaterTicks + 1, 40);
        }
    }

    protected boolean isMarine() {
        return this.outOfWaterTicks < 6;
    }

    private void applyBuoyancy() {
        if (this.isMarine()) {
            Vec3 v = this.getDeltaMovement();
            double speed = Math.sqrt(v.x * v.x + v.z * v.z);
            double vy;
            if (this.tickCount < 30) {
                vy = v.y * 0.65D + 0.0025D;
                vy = Mth.clamp(vy, -0.012D, 0.01D);
            } else if (speed < 0.03D && Math.abs(v.y) < 0.02D) {
                vy = v.y * 0.5D;
                if (Math.abs(vy) < 0.002D) {
                    vy = 0.0D;
                }
            } else {
                double swell = Math.sin(this.tickCount * this.swellRate()) * this.swellAmp();
                vy = v.y * 0.90D + this.buoyLift() + swell;
                vy = Mth.clamp(vy, -0.014D, 0.012D);
            }
            double drag = this.waterDrag();
            if (speed < 0.02D) {
                drag = 0.92D;
            }
            this.setDeltaMovement(v.x * drag, vy, v.z * drag);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8D, 0.98D, 0.8D));
        }
    }

    private void resolveHullCollisions() {
        double searchR = this.halfLoa() + this.halfBeam() + 2.0;
        AABB search = new AABB(this.getX() - searchR, this.getY() - 0.75, this.getZ() - searchR, this.getX() + searchR, this.getY() + this.hullHeight() + 1.0, this.getZ() + searchR);

        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        float bowX = Mth.sin(yaw);
        float bowZ = -Mth.cos(yaw);
        float stbdX = -bowZ;
        float stbdZ = bowX;

        for (Entity other : this.level().getEntities(this, search, this::shouldHullCollideWith)) {
            this.pushEntityOutOfHull(other, bowX, bowZ, stbdX, stbdZ);
        }
    }

    private boolean shouldHullCollideWith(Entity other) {
        if (!other.isAlive() || other.isSpectator() || other.noPhysics) {
            return false;
        }
        if (other.getVehicle() == this || this.isPassengerOfSameVehicle(other)) {
            return false;
        }
        return other instanceof Player || other instanceof LivingEntity || other.isPushable();
    }

    private void pushEntityOutOfHull(Entity other, float bowX, float bowZ, float stbdX, float stbdZ) {
        AABB bb = other.getBoundingBox();

        double ox = other.getX() - this.getX();
        double oz = other.getZ() - this.getZ();
        double feet = bb.minY - this.getY();
        double head = bb.maxY - this.getY();

        if (head < 0.0 || feet > this.hullHeight()) {
            return;
        }

        // along = bow & across = beam
        double along = ox * bowX + oz * bowZ;
        double across = ox * stbdX + oz * stbdZ;

        float otherHalf = Math.max(other.getBbWidth() * 0.45F, 0.25F);
        double limAlong = this.halfLoa() + otherHalf;
        double limAcross = this.halfBeam() + otherHalf;

        if (Math.abs(along) >= limAlong || Math.abs(across) >= limAcross) {
            return;
        }

        double penAlong = limAlong - Math.abs(along);
        double penAcross = limAcross - Math.abs(across);

        double pushAlong = 0.0;
        double pushAcross = 0.0;

        if (penAcross <= penAlong + 0.05) {
            double sign = across >= 0.0 ? 1.0 : -1.0;
            if (Math.abs(across) < 1.0E-4) {
                sign = 1.0;
            }
            pushAcross = (limAcross + 0.03) * sign - across;
        } else {
            double sign = along >= 0.0 ? 1.0 : -1.0;
            if (Math.abs(along) < 1.0E-4) {
                sign = 1.0;
            }
            pushAlong = (limAlong + 0.03) * sign - along;
        }

        double pdx = bowX * pushAlong + stbdX * pushAcross;
        double pdz = bowZ * pushAlong + stbdZ * pushAcross;
        if (pdx * pdx + pdz * pdz < 1.0E-10) {
            return;
        }

        other.setPos(other.getX() + pdx, other.getY(), other.getZ() + pdz);
        Vec3 v = other.getDeltaMovement();

        double len = Math.sqrt(pdx * pdx + pdz * pdz);
        double nx = pdx / len;
        double nz = pdz / len;
        double into = v.x * nx + v.z * nz;
        if (into < 0.0) {
            other.setDeltaMovement(v.x - nx * into, v.y, v.z - nz * into);
        }
    }

    private void startSinking(ServerLevel level, @Nullable LivingEntity breaker) {
        if (this.isSinking()) {
            return;
        }
        this.sinkingBreaker = breaker;
        this.sinkTicks = 1;
        this.entityData.set(DATA_SINK_TICKS, 1);
        double x = this.getX();
        double y = this.getY() + 1.0;
        double z = this.getZ();
        level.playSound(null, x, y, z, SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.4F, 0.55F);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.2F, 0.7F);
        level.sendParticles(ParticleTypes.SPLASH, x, y + 0.2, z, 50, this.getBbWidth() * 0.3, 0.3, this.getBbWidth() * 0.3, 0.15);
    }

    private void tickSinking() {
        if (this.level().isClientSide()) {
            this.spawnSinkParticles();
            return;
        }
        this.sinkTicks++;
        this.entityData.set(DATA_SINK_TICKS, this.sinkTicks);
        if (this.sinkTicks % 25 == 0) {
            this.level().playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 0.9F, 0.45F + this.random.nextFloat() * 0.1F);
        }
        if (this.sinkTicks >= SINK_DURATION && this.level() instanceof ServerLevel server) {
            this.ejectPassengers();
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();
            server.playSound(null, x, y + 3.0, z, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.6F, 0.6F);
            server.sendParticles(ParticleTypes.BUBBLE, x, y + 1.0, z, 70, 1.4, 0.8, 1.4, 0.12);
            server.sendParticles(ParticleTypes.SPLASH, x, y + 3.2, z, 60, 1.2, 0.3, 1.2, 0.18);
            this.destroyShip(server, this.sinkingBreaker, false);
        }
    }

    private void spawnSinkParticles() {
        double spread = this.getBbWidth() * 0.35;
        int n = 1 + this.random.nextInt(2) + this.entityData.get(DATA_SINK_TICKS) / 40;
        for (int i = 0; i < n; i++) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * spread * 2.0;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * spread * 2.0;
            this.level().addParticle(ParticleTypes.BUBBLE, px, this.getY() + 0.3 + this.random.nextDouble() * 0.8, pz, 0.0, 0.05 + this.random.nextDouble() * 0.04, 0.0);
        }
        if (this.entityData.get(DATA_SINK_TICKS) < 40 && this.random.nextInt(3) == 0) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * spread * 2.0;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * spread * 2.0;
            this.level().addParticle(ParticleTypes.SPLASH, px, this.getY() + 1.1, pz, 0.0, 0.06, 0.0);
        }
    }

    public boolean damageHull(ServerLevel level, float amount, @Nullable Entity attacker) {
        if (this.isRemoved() || this.isInvulnerable() || this.isSinking() || amount <= 0.0F) {
            return false;
        }
        if (this.hull <= 0 || this.hull > this.getMaxHull()) {
            this.hull = this.getMaxHull();
        }
        boolean projectile = attacker instanceof CannonballEntity || attacker instanceof StoneBulletEntity;
        if (!projectile && this.hullInvuln > 0) {
            return false;
        }

        int dmg = Math.max(1, Math.round(amount));
        this.hull = Math.max(0, this.hull - dmg);
        if (!projectile) {
            this.hullInvuln = HURT_INVULN_TICKS;
        }
        this.syncHull();
        this.markHurt();
        this.gameEvent(GameEvent.ENTITY_DAMAGE, attacker);

        level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1.15F, 0.75F + this.random.nextFloat() * 0.2F);
        level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.NEUTRAL, 0.45F, 0.9F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()), this.getX(), this.getY() + 1.2, this.getZ(), 14, 0.8, 0.5, 0.8, 0.06);
        level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.4, this.getZ(), 6, 0.5, 0.3, 0.5, 0.02);

        if (this.hull <= 0) {
            LivingEntity breaker = null;
            if (attacker instanceof LivingEntity living) {
                breaker = living;
            } else if (attacker instanceof CannonballEntity ball && ball.getOwner() instanceof LivingEntity living) {
                breaker = living;
            } else if (attacker instanceof StoneBulletEntity stone && stone.getOwner() instanceof LivingEntity living) {
                breaker = living;
            }
            this.startSinking(level, breaker);
        }
        return true;
    }

    private void destroyShip(ServerLevel level, @Nullable LivingEntity breaker, boolean dropBoatItem) {
        DamageSource src = breaker != null ? this.damageSources().mobAttack(breaker) : this.damageSources().generic();
        this.chestVehicleDestroyed(src, level, this);
        this.clearChestVehicleContent();
        if (dropBoatItem) {
            this.spawnAtLocation(level, this.createDropStack());
        }
        this.kill(level);
    }

    public boolean packUp(ServerLevel level, Player player) {
        if (this.isRemoved() || this.isSinking() || !this.isOwner(player)) {
            return false;
        }
        this.ejectPassengers();
        DamageSource src = this.damageSources().playerAttack(player);
        this.chestVehicleDestroyed(src, level, this);
        this.clearChestVehicleContent();
        ItemStack stack = this.createDropStack();
        if (this.hull < this.getMaxHull()) {
            stack.set(NapoleonShipMod.SHIP_HULL.get(), this.hull);
        }
        if (!player.getInventory().add(stack) || !stack.isEmpty()) {
            this.spawnAtLocation(level, stack);
        }
        level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.0F, 0.9F + this.random.nextFloat() * 0.15F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()), this.getX(), this.getY() + 1.0, this.getZ(), 18, 0.7, 0.4, 0.7, 0.05);
        this.discard();
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hit) {
        if (player.isSecondaryUseActive() || this.isSinking() || !this.canAddPassenger(player)) {
            return this.openCargo(player);
        }
        if (!this.level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult openCargo(Player player) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        InteractionResult result = this.interactWithContainerVehicle(player);
        if (result.consumesAction() && player.level() instanceof ServerLevel server) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, player);
            PiglinAi.angerNearbyPiglins(server, player, true);
        }
        return result;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        player.openMenu(this);
        if (player.level() instanceof ServerLevel server) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, player);
            PiglinAi.angerNearbyPiglins(server, player, true);
        }
    }

    @Override
    public boolean canBeHitByProjectile() {
        return !this.isRemoved();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved() || this.isInvulnerable() || amount <= 0.0F) {
            return false;
        }

        Entity direct = source.getDirectEntity();
        Entity cause = source.getEntity();
        if (direct instanceof CannonballEntity || direct instanceof StoneBulletEntity
                || cause instanceof CannonballEntity || cause instanceof StoneBulletEntity) {
            return false;
        }
        if (cause instanceof Player player && this.isOwner(player) && this.isMeleePlayerHit(source, direct, cause)) {
            return this.packUp(level, player);
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return this.damageHull(level, Math.min(MAX_EXPLOSION_HULL, amount * 0.25F), direct != null ? direct : cause);
        }

        float hullDmg = Math.min(MAX_GENERIC_HIT, amount);
        if (cause instanceof Player player && player.getMainHandItem().is(ItemTags.AXES)) {
            hullDmg = Math.min(MAX_GENERIC_HIT * AXE_HULL_MUL, amount * AXE_HULL_MUL);
        }
        Entity attacker = direct != null ? direct : cause;
        return this.damageHull(level, hullDmg, attacker);
    }

    private boolean isMeleePlayerHit(DamageSource source, @Nullable Entity direct, @Nullable Entity cause) {
        if (!(cause instanceof Player)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        if (direct instanceof Projectile) {
            return false;
        }
        return direct == null || direct == cause;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide()) {
            int max = this.getMaxHull();
            if (this.hull <= 0 || this.hull > max) {
                this.hull = max;
            }
            this.syncHull();
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide() && reason.shouldDestroy()) {
            Containers.dropContents(this.level(), this, this);
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.readChestVehicleSaveData(input);
        this.hull = input.getIntOr("Hull", this.getMaxHull());
        this.hull = Math.min(this.getMaxHull(), Math.max(1, this.hull));
        this.ownerUuid = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.syncHull();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        this.addChestVehicleSaveData(output);
        output.putInt("Hull", this.hull);
        output.storeNullable("Owner", UUIDUtil.CODEC, this.ownerUuid);
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (this.lootTable != null && player.isSpectator()) {
            return null;
        }
        this.unpackChestVehicleLootTable(player);
        int rows = this.cargoRows();
        return switch (rows) {
            case 1 -> new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
            case 2 -> new ChestMenu(MenuType.GENERIC_9x2, containerId, inventory, this, 2);
            case 3 -> ChestMenu.threeRows(containerId, inventory, this);
            case 4 -> new ChestMenu(MenuType.GENERIC_9x4, containerId, inventory, this, 4);
            case 5 -> new ChestMenu(MenuType.GENERIC_9x5, containerId, inventory, this, 5);
            default -> ChestMenu.sixRows(containerId, inventory, this);
        };
    }

    @Override
    public void clearContent() {
        this.clearChestVehicleContent();
    }

    @Override
    public int getContainerSize() {
        return this.cargoSlotCount();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.getChestVehicleItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return this.removeChestVehicleItem(slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.removeChestVehicleItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.setChestVehicleItem(slot, stack);
    }

    @Override
    public SlotAccess getSlot(int slot) {
        return this.getChestVehicleSlot(slot);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return this.isChestVehicleStillValid(player);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(user.getLivingEntity()));
    }

    @Override
    public @Nullable ResourceKey<LootTable> getContainerLootTable() {
        return this.lootTable;
    }

    @Override
    public void setContainerLootTable(@Nullable ResourceKey<LootTable> lootTable) {
        this.lootTable = lootTable;
    }

    @Override
    public long getContainerLootTableSeed() {
        return this.lootTableSeed;
    }

    @Override
    public void setContainerLootTableSeed(long lootTableSeed) {
        this.lootTableSeed = lootTableSeed;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return this.itemStacks;
    }

    @Override
    public void clearItemStacks() {
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    }
}
