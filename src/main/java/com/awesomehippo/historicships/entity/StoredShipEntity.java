package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.HistoricShips;
import com.awesomehippo.historicships.network.RamHitPacket;

import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public abstract class StoredShipEntity extends Entity implements HasCustomInventoryScreen, Container, MenuProvider {
    public static final float CANNON_HULL_DAMAGE = 14.0F;
    public static final float STONE_HULL_DAMAGE = 10.0F;
    private static final float MAX_GENERIC_HIT = 10.0F;
    private static final float MAX_EXPLOSION_HULL = 8.0F;
    private static final float AXE_HULL_MUL = 1.75F;
    private static final int HURT_INVULN_TICKS = 10;
    private static final int RAM_COOLDOWN_TICKS = 28;
    private static final float RAM_MIN_CLOSE = 0.08F;
    private static final int REPAIR_PLANKS = 30;
    private static final float REPAIR_HULL_FRAC = 0.15F;
    public static final int SINK_DURATION = 100;

    private static final EntityDataAccessor<Integer> DATA_HULL = SynchedEntityData.defineId(StoredShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SINK_TICKS = SynchedEntityData.defineId(StoredShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_OWNER = SynchedEntityData.defineId(StoredShipEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);

    private NonNullList<ItemStack> itemStacks;
    private int hull;
    private int hullInvuln;
    private int ramCooldown;
    private double ramVelX;
    private double ramVelZ;
    private @Nullable UUID ownerUuid;
    private int sinkTicks;
    private int sinkTicksPrev;
    private int sinkTicksSync;
    private @Nullable LivingEntity sinkingBreaker;
    private int outOfWaterTicks;
    private @Nullable ShipHull defaultHull;

    protected StoredShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.itemStacks = NonNullList.withSize(this.cargoSlotCount(), ItemStack.EMPTY);
        this.hull = this.getMaxHull();
    }

    public void setOwner(@Nullable Player player) {
        this.ownerUuid = player != null ? player.getUUID() : null;
        this.syncOwner();
    }

    public boolean isOwner(Player player) {
        UUID owner = this.ownerId();
        if (owner == null) {
            return true;
        }
        return owner.equals(player.getUUID());
    }

    private @Nullable UUID ownerId() {
        if (this.level().isClientSide()) {
            return this.entityData.get(DATA_OWNER).map(EntityReference::getUUID).orElse(null);
        }
        return this.ownerUuid;
    }

    private void syncOwner() {
        this.entityData.set(DATA_OWNER, this.ownerUuid == null ? Optional.empty() : Optional.of(EntityReference.of(this.ownerUuid)));
    }

    protected abstract int cargoRows();

    protected abstract ItemStack createDropStack();

    protected void writeDropStack(ItemStack stack) {}

    public abstract int getMaxHull();

    protected abstract float halfLoa();

    protected float bowReach() {
        return this.hullShape().bow();
    }

    protected float sternReach() {
        return this.hullShape().stern();
    }

    protected ShipHull hullShape() {
        if (this.defaultHull == null) {
            this.defaultHull = ShipHull.rect(this.halfLoa(), this.halfBeam());
        }
        return this.defaultHull;
    }

    protected float ramAlongMin() {
        return this.bowReach();
    }

    protected float ramDamage() {
        return 0.0F;
    }

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

    public void setHullPercent(int pct) {
        int max = this.getMaxHull();
        this.setHull(Math.max(1, Math.round(max * Mth.clamp(pct, 1, 100) / 100.0F)));
    }

    public int getHullPercent() {
        int max = Math.max(1, this.getMaxHull());
        return Math.min(100, Math.max(0, (this.getHull() * 100) / max));
    }

    // 0 pristine, 1 damaged (<=66%), 2 wrecked (<=33%)
    //TODO: maybe add more stages
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
        builder.define(DATA_OWNER, Optional.empty());
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
        if (this.ramCooldown > 0) {
            this.ramCooldown--;
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
        ShipHull hull = this.hullShape();
        return this.shipBox(pos, hull.bow(), hull.stern(), hull.maxHalf(), this.hullHeight());
    }

    public AABB makeCullBox() {
        float cull = this.cullHalfLoa();
        return this.shipBox(this.position(), cull, cull, this.cullHalfBeam(), this.cullHeight());
    }

    private AABB shipBox(Vec3 pos, float bow, float stern, float halfBeam, float height) {
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
            float along = fl < 0 ? stern : bow;
            for (int s = -1; s <= 1; s += 2) {
                double x = pos.x + fl * along * bowX + s * halfBeam * stbdX;
                double z = pos.z + fl * along * bowZ + s * halfBeam * stbdZ;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        return new AABB(minX, pos.y - this.hullBottomPad(), minZ, maxX, pos.y + height, maxZ);
    }

    public boolean hullContains(Vec3 at, double radius) {
        return this.hullContains(at.x, at.y, at.z, radius);
    }

    public boolean hullContains(double x, double y, double z, double radius) {
        double localY = y - this.getY();
        if (localY < -this.hullBottomPad() - radius || localY > this.hullHeight() + radius) {
            return false;
        }
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        float bowX = Mth.sin(yaw);
        float bowZ = -Mth.cos(yaw);
        float stbdX = -bowZ;
        float stbdZ = bowX;
        double ox = x - this.getX();
        double oz = z - this.getZ();
        return this.hullShape().contains(ox * bowX + oz * bowZ, ox * stbdX + oz * stbdZ, radius);
    }

    public double hullDistance(Vec3 at) {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        float bowX = Mth.sin(yaw);
        float bowZ = -Mth.cos(yaw);
        float stbdX = -bowZ;
        float stbdZ = bowX;
        double ox = at.x - this.getX();
        double oz = at.z - this.getZ();
        double horiz = this.hullShape().distance(ox * bowX + oz * bowZ, ox * stbdX + oz * stbdZ);
        double localY = at.y - this.getY();
        double vy = 0.0;
        if (localY < -this.hullBottomPad()) {
            vy = -this.hullBottomPad() - localY;
        } else if (localY > this.hullHeight()) {
            vy = localY - this.hullHeight();
        }
        return Math.sqrt(horiz * horiz + vy * vy);
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

    public Vec3 gunAimDirection(@Nullable LivingEntity shooter, float maxYaw, float maxUp, float minUp) {
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        float relYaw = 0.0F;
        float pitch;
        if (shooter instanceof Player player) {
            relYaw = Mth.clamp(Mth.wrapDegrees(player.getYRot() + 180.0F - this.getYRot()), -maxYaw, maxYaw);
            pitch = Mth.clamp(player.getXRot(), -maxUp, -minUp);
        } else {
            pitch = minUp > 0.0F ? -minUp : 0.0F;
        }
        float aimYaw = yawRad + relYaw * Mth.DEG_TO_RAD;
        float pitchRad = pitch * Mth.DEG_TO_RAD;
        double horiz = Mth.cos(pitchRad);
        return new Vec3(Mth.sin(aimYaw) * horiz, -Mth.sin(pitchRad), -Mth.cos(aimYaw) * horiz);
    }

    protected void syncShipPosition() {
        if (this.isClientAuthoritative()) {
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        }
    }

    protected void tickMovement() {
        this.trackRamVelocity();
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
        if (!this.level().isClientSide() && this.serverHasMoveInput() && this.random.nextInt(40) == 0) {
            this.playSound(SoundEvents.WOOD_STEP, 0.8F, 0.55F + this.random.nextFloat() * 0.2F);
        }
    }

    private boolean serverHasMoveInput() {
        return this.getControllingPassenger() instanceof ServerPlayer player && (player.getLastClientInput().forward() || player.getLastClientInput().backward());
    }

    private void trackRamVelocity() {
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        if (dx * dx + dz * dz > 1.0E-6) {
            this.ramVelX = dx;
            this.ramVelZ = dz;
            return;
        }
        Vec3 v = this.getDeltaMovement();
        if (v.horizontalDistanceSqr() > 1.0E-6) {
            this.ramVelX = v.x;
            this.ramVelZ = v.z;
            return;
        }
        this.ramVelX *= 0.82;
        this.ramVelZ *= 0.82;
        if (this.ramVelX * this.ramVelX + this.ramVelZ * this.ramVelZ < 1.0E-6) {
            this.ramVelX = 0.0;
            this.ramVelZ = 0.0;
        }
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
        ShipHull hull = this.hullShape();
        double searchR = Math.max(hull.bow(), hull.stern()) + hull.maxHalf() + 2.0;
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
        if (other instanceof StoredShipEntity) {
            return true;
        }
        return other instanceof Player || other instanceof LivingEntity || other.isPushable();
    }

    private void pushEntityOutOfHull(Entity other, float bowX, float bowZ, float stbdX, float stbdZ) {
        AABB bb = other.getBoundingBox();
        double feet = bb.minY - this.getY();
        double head = bb.maxY - this.getY();
        if (head < 0.0 || feet > this.hullHeight()) {
            return;
        }

        if (other instanceof StoredShipEntity ship) {
            this.tryRamHit(ship, bowX, bowZ);
            if (this.getId() > ship.getId()) {
                return;
            }
            this.separateShips(ship);
            return;
        }

        double ox = other.getX() - this.getX();
        double oz = other.getZ() - this.getZ();
        // along = bow & across = beam
        double along = ox * bowX + oz * bowZ;
        double across = ox * stbdX + oz * stbdZ;
        float otherHalf = Math.max(other.getBbWidth() * 0.45F, 0.25F);
        ShipHull hull = this.hullShape();
        double minX = -hull.stern() - otherHalf;
        double maxX = hull.bow() + otherHalf;
        if (along < minX || along > maxX) {
            return;
        }
        double limAcross = hull.halfAt((float) along) + otherHalf;
        if (Math.abs(across) >= limAcross) {
            return;
        }

        double penBow = maxX - along;
        double penStern = along - minX;
        double penAlong = Math.min(penBow, penStern);
        double penAcross = limAcross - Math.abs(across);
        double pushAlong = 0.0;
        double pushAcross = 0.0;
        if (penAcross <= penAlong + 0.05) {
            double sign = across >= 0.0 ? 1.0 : -1.0;
            if (Math.abs(across) < 1.0E-4) {
                sign = 1.0;
            }
            pushAcross = (limAcross + 0.03) * sign - across;
        } else if (penBow <= penStern) {
            pushAlong = penBow + 0.03;
        } else {
            pushAlong = -(penStern + 0.03);
        }

        double pdx = bowX * pushAlong + stbdX * pushAcross;
        double pdz = bowZ * pushAlong + stbdZ * pushAcross;
        if (pdx * pdx + pdz * pdz < 1.0E-10) {
            return;
        }
        double len = Math.sqrt(pdx * pdx + pdz * pdz);
        other.setPos(other.getX() + pdx, other.getY(), other.getZ() + pdz);
        cancelVelocityInto(other, -(pdx / len), -(pdz / len));
    }

    private void separateShips(StoredShipEntity other) {
        if (this.trySeparateAt(other, this.getX(), this.getZ(), other.getX(), other.getZ())) {
            return;
        }
        double adx = this.getX() - this.xo;
        double adz = this.getZ() - this.zo;
        double bdx = other.getX() - other.xo;
        double bdz = other.getZ() - other.zo;
        double speed = Math.sqrt(adx * adx + adz * adz) + Math.sqrt(bdx * bdx + bdz * bdz);
        int steps = Mth.clamp((int) (speed / 0.45) + 1, 1, 6);
        if (steps <= 1) {
            return;
        }
        for (int i = steps - 1; i >= 1; i--) {
            double t = i / (double) steps;
            double ax = this.xo + adx * t;
            double az = this.zo + adz * t;
            double bx = other.xo + bdx * t;
            double bz = other.zo + bdz * t;
            double[] probe = new double[3];
            if (this.hullPush(other, ax, az, bx, bz, probe)) {
                this.setPos(ax, this.getY(), az);
                other.setPos(bx, other.getY(), bz);
                this.trySeparateAt(other, ax, az, bx, bz);
                return;
            }
        }
    }

    private boolean trySeparateAt(StoredShipEntity other, double ax, double az, double bx, double bz) {
        double[] hit = new double[3];
        if (!this.hullPush(other, ax, az, bx, bz, hit)) {
            return false;
        }
        double nx = hit[0];
        double nz = hit[1];
        double depth = hit[2] + 0.03;
        if (depth * depth < 1.0E-10) {
            return true;
        }
        double share = depth * 0.5;
        this.setPos(this.getX() - nx * share, this.getY(), this.getZ() - nz * share);
        other.setPos(other.getX() + nx * share, other.getY(), other.getZ() + nz * share);
        cancelVelocityInto(this, nx, nz);
        cancelVelocityInto(other, -nx, -nz);
        return true;
    }

    private boolean hullPush(StoredShipEntity other, double ax, double az, double bx, double bz, double[] hit) {
        float aYaw = this.getYRot() * Mth.DEG_TO_RAD;
        float aBowX = Mth.sin(aYaw);
        float aBowZ = -Mth.cos(aYaw);
        float bYaw = other.getYRot() * Mth.DEG_TO_RAD;
        float bBowX = Mth.sin(bYaw);
        float bBowZ = -Mth.cos(bYaw);
        hit[0] = 0.0;
        hit[1] = 0.0;
        hit[2] = Double.POSITIVE_INFINITY;
        return ShipHull.overlapPush(this.hullShape(), ax, az, aBowX, aBowZ, -aBowZ, aBowX, other.hullShape(), bx, bz, bBowX, bBowZ, -bBowZ, bBowX, hit);
    }

    private static void cancelVelocityInto(Entity entity, double nx, double nz) {
        Vec3 v = entity.getDeltaMovement();
        double into = v.x * nx + v.z * nz;
        if (into > 0.0) {
            entity.setDeltaMovement(v.x - nx * into, v.y, v.z - nz * into);
        }
    }

    private void tryRamHit(StoredShipEntity target, float bowX, float bowZ) {
        float base = this.ramDamage();
        if (base <= 0.0F || this.ramCooldown > 0 || this.isSinking() || target.isSinking()) {
            return;
        }
        float ram0 = this.ramAlongMin();
        float ram1 = this.bowReach();
        if (ram0 >= ram1 - 0.05F) {
            return;
        }
        double closing = (this.ramVelX - target.ramVelX) * bowX + (this.ramVelZ - target.ramVelZ) * bowZ;
        if (closing < RAM_MIN_CLOSE) {
            return;
        }
        if (!this.ramTouches(target, bowX, bowZ)) {
            return;
        }
        if (this.level().isClientSide()) {
            if (this.isLocalInstanceAuthoritative()) {
                RamHitPacket.send(this.getId(), target.getId(), (float) closing);
                this.ramCooldown = RAM_COOLDOWN_TICKS;
            }
            return;
        }
        if (this.isLocalInstanceAuthoritative() && this.level() instanceof ServerLevel server) {
            this.applyRamHit(server, target, closing, bowX, bowZ);
        }
    }

    private boolean ramTouches(StoredShipEntity target, float bowX, float bowZ) {
        float ram0 = this.ramAlongMin();
        float ram1 = this.bowReach();
        double hy = this.getY() + 0.35;
        for (int n = 0; n < 2; n++) {
            double ox = n == 0 ? this.getX() : this.xo;
            double oz = n == 0 ? this.getZ() : this.zo;
            for (int i = 0; i <= 4; i++) {
                float a = ram0 + (ram1 - ram0) * (i / 4.0F);
                if (target.hullContains(ox + bowX * a, hy, oz + bowZ * a, 0.40)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void serverRamHit(ServerLevel server, StoredShipEntity target, float closing) {
        if (this.ramDamage() <= 0.0F || this.ramCooldown > 0 || this.isSinking() || target.isSinking() || target == this) {
            return;
        }
        double maxDist = this.bowReach() + Math.max(target.bowReach(), target.sternReach()) + 4.0;
        if (this.distanceTo(target) > maxDist) {
            return;
        }
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        float bowX = Mth.sin(yaw);
        float bowZ = -Mth.cos(yaw);
        double along = (target.getX() - this.getX()) * bowX + (target.getZ() - this.getZ()) * bowZ;
        if (along < this.ramAlongMin() * 0.35) {
            return;
        }
        this.applyRamHit(server, target, closing, bowX, bowZ);
    }

    private void applyRamHit(ServerLevel server, StoredShipEntity target, double closing, float bowX, float bowZ) {
        float base = this.ramDamage();
        if (base <= 0.0F || this.ramCooldown > 0) {
            return;
        }
        closing = Mth.clamp(closing, RAM_MIN_CLOSE, 2.0);
        Vec3 ov = target.getDeltaMovement();
        float dmg = Mth.clamp(base * (float) (closing / 0.50D), base * 0.55F, base * 1.85F);
        if (target.damageHull(server, dmg, this)) {
            this.ramCooldown = RAM_COOLDOWN_TICKS;
            this.ramImpact(bowX, bowZ, closing);
            target.setDeltaMovement(ov.add(bowX * closing * 0.28, 0.04, bowZ * closing * 0.28));
        }
    }

    private void ramImpact(float bowX, float bowZ, double closing) {
        double hx = this.getX() + bowX * this.bowReach();
        double hy = this.getY() + 0.45;
        double hz = this.getZ() + bowZ * this.bowReach();
        BlockParticleOption wood = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState());
        BlockParticleOption copper = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COPPER_BLOCK.defaultBlockState());
        if (this.level() instanceof ServerLevel server) {
            float loud = (float) Mth.clamp(0.85 + closing * 0.5, 0.9, 1.3);
            server.playSound(null, hx, hy, hz, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.NEUTRAL, 1.15F * loud, 0.48F);
            server.playSound(null, hx, hy, hz, SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.2F, 0.48F + this.random.nextFloat() * 0.08F);
            server.playSound(null, hx, hy, hz, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.0F, 0.62F);
            int shards = 16 + (int) (Mth.clamp(closing, 0.08, 1.2) * 18);
            for (int i = 0; i < shards; i++) {
                double vx = bowX * (0.10 + this.random.nextDouble() * 0.26) + (this.random.nextDouble() - 0.5) * 0.16;
                double vy = 0.06 + this.random.nextDouble() * 0.22;
                double vz = bowZ * (0.10 + this.random.nextDouble() * 0.26) + (this.random.nextDouble() - 0.5) * 0.16;
                server.sendParticles(wood, hx, hy, hz, 0, vx, vy, vz, 1.0);
            }
            for (int i = 0; i < 8; i++) {
                server.sendParticles(copper, hx, hy, hz, 0, (this.random.nextDouble() - 0.5) * 0.14, 0.04 + this.random.nextDouble() * 0.10, (this.random.nextDouble() - 0.5) * 0.14, 1.0);
            }
            server.sendParticles(ParticleTypes.SPLASH, hx, hy, hz, 28, 0.45, 0.22, 0.45, 0.16);
            server.sendParticles(ParticleTypes.BUBBLE, hx, hy - 0.12, hz, 14, 0.32, 0.12, 0.32, 0.06);
            server.sendParticles(ParticleTypes.CLOUD, hx + bowX * 0.2, hy + 0.12, hz + bowZ * 0.2, 8, 0.22, 0.10, 0.22, 0.02);
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
        level.playSound(null, x, y, z, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.2F, 0.7F);
        float w = this.getBbWidth();
        level.sendParticles(ParticleTypes.SPLASH, x, y + 0.15, z, 80, w * 0.35, 0.25, w * 0.35, 0.22);
        level.sendParticles(ParticleTypes.BUBBLE, x, y - 0.2, z, 40, w * 0.28, 0.35, w * 0.28, 0.08);
        level.sendParticles(ParticleTypes.BUBBLE_POP, x, y + 0.35, z, 18, w * 0.3, 0.15, w * 0.3, 0.02);
    }

    private void tickSinking() {
        if (this.level().isClientSide()) {
            this.spawnSinkParticles();
            return;
        }
        this.sinkTicks++;
        this.entityData.set(DATA_SINK_TICKS, this.sinkTicks);
        if (this.sinkTicks % 12 == 0) {
            this.level().playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.45F, 0.75F + this.random.nextFloat() * 0.2F);
        }
        if (this.sinkTicks >= SINK_DURATION && this.level() instanceof ServerLevel server) {
            this.ejectPassengers();
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();
            server.playSound(null, x, y + 3.0, z, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.6F, 0.55F);
            server.playSound(null, x, y + 1.0, z, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.1F, 0.8F);
            server.sendParticles(ParticleTypes.BUBBLE, x, y + 0.4, z, 90, 1.6, 0.9, 1.6, 0.14);
            server.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, x, y + 0.2, z, 40, 1.2, 0.4, 1.2, 0.08);
            server.sendParticles(ParticleTypes.SPLASH, x, y + 2.4, z, 90, 1.4, 0.35, 1.4, 0.22);
            server.sendParticles(ParticleTypes.BUBBLE_POP, x, y + 2.2, z, 30, 1.1, 0.25, 1.1, 0.04);
            this.destroyShip(server, this.sinkingBreaker, false);
        }
    }

    private void spawnSinkParticles() {
        float t = this.entityData.get(DATA_SINK_TICKS) / (float) SINK_DURATION;
        double spread = this.getBbWidth() * (0.32 + t * 0.28);
        double waterY = this.getY() + 0.35 + (1.0F - t) * 0.9;
        int bubbles = 3 + this.random.nextInt(3) + (int) (t * 5);
        for (int i = 0; i < bubbles; i++) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * spread * 2.0;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * spread * 2.0;
            this.level().addParticle(ParticleTypes.BUBBLE, px, this.getY() + 0.15 + this.random.nextDouble() * (0.5 + t * 0.6), pz, 0.0, 0.06 + this.random.nextDouble() * 0.07, 0.0);
            if (this.random.nextBoolean()) {
                this.level().addParticle(ParticleTypes.BUBBLE_COLUMN_UP, px, this.getY() + 0.1, pz, 0.0, 0.10 + this.random.nextDouble() * 0.06, 0.0);
            }
        }
        int splashes = 2 + this.random.nextInt(2) + (int) (t * 3);
        for (int i = 0; i < splashes; i++) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * spread * 2.0;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * spread * 2.0;
            this.level().addParticle(ParticleTypes.SPLASH, px, waterY, pz, (this.random.nextDouble() - 0.5) * 0.12, 0.08 + this.random.nextDouble() * 0.06, (this.random.nextDouble() - 0.5) * 0.12);
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.BUBBLE_POP, px, waterY + 0.05, pz, 0.0, 0.02, 0.0);
            }
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

        level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.NEUTRAL, 1.15F, 0.75F + this.random.nextFloat() * 0.2F);
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
        Containers.dropContents(level, this, this);
        this.clearContent();
        if (dropBoatItem) {
            ItemStack stack = this.createDropStack();
            this.writeDropStack(stack);
            this.spawnAtLocation(level, stack);
        }
        this.kill(level);
    }

    public boolean packUp(ServerLevel level, Player player) {
        if (this.isRemoved() || this.isSinking() || !this.isOwner(player)) {
            return false;
        }
        this.ejectPassengers();
        Containers.dropContents(level, this, this);
        this.clearContent();
        ItemStack stack = this.createDropStack();
        int pct = this.getHullPercent();
        if (pct < 100) {
            stack.set(HistoricShips.SHIP_HULL.get(), pct);
        }
        this.writeDropStack(stack);
        if (!player.getInventory().add(stack) || !stack.isEmpty()) {
            this.spawnAtLocation(level, stack);
        }
        level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.0F, 0.9F + this.random.nextFloat() * 0.15F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()), this.getX(), this.getY() + 1.0, this.getZ(), 18, 0.7, 0.4, 0.7, 0.05);
        this.discard();
        return true;
    }

    public boolean tryRepair(Player player, InteractionHand hand) {
        if (this.isRemoved() || this.isSinking() || player.isSecondaryUseActive()) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ItemTags.PLANKS) || this.getHull() >= this.getMaxHull()) {
            return false;
        }
        if (!player.getAbilities().instabuild && stack.getCount() < REPAIR_PLANKS) {
            if (this.level() instanceof ServerLevel) {
                player.sendOverlayMessage(Component.translatable("item.historicships.repair_need", REPAIR_PLANKS));
            }
            return true;
        }
        if (this.level() instanceof ServerLevel server) {
            int heal = Math.max(1, Math.round(this.getMaxHull() * REPAIR_HULL_FRAC));
            this.setHull(this.getHull() + heal);
            if (!player.getAbilities().instabuild) {
                stack.shrink(REPAIR_PLANKS);
            }
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();
            server.playSound(null, x, y, z, SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.0F, 0.85F + this.random.nextFloat() * 0.2F);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()), x, y, z, 8, 0.25, 0.2, 0.25, 0.04);
            player.sendOverlayMessage(Component.translatable("item.historicships.hull", this.getHullPercent()));
        }
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hit) {
        if (this.isSinking()) {
            return this.openCargo(player);
        }
        if (this.tryRepair(player, hand)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() || !this.canAddPassenger(player)) {
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
        this.openInventory(player);
        if (player.level() instanceof ServerLevel server) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, player);
            PiglinAi.angerNearbyPiglins(server, player, true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        this.openInventory(player);
        if (player.level() instanceof ServerLevel server) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, player);
            PiglinAi.angerNearbyPiglins(server, player, true);
        }
    }

    protected void openInventory(Player player) {
        player.openMenu(this);
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
            this.syncOwner();
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
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.itemStacks);
        this.hull = input.getIntOr("Hull", this.getMaxHull());
        this.hull = Math.min(this.getMaxHull(), Math.max(1, this.hull));
        this.ownerUuid = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.syncHull();
        this.syncOwner();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        ContainerHelper.saveAllItems(output, this.itemStacks);
        output.putInt("Hull", this.hull);
        output.storeNullable("Owner", UUIDUtil.CODEC, this.ownerUuid);
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
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
        this.itemStacks.clear();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.itemStacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getContainerSize() {
        return this.cargoSlotCount();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.itemStacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return ContainerHelper.removeItem(this.itemStacks, slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.itemStacks.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.itemStacks.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.itemStacks.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        if (slot < 0 || slot >= this.getContainerSize()) {
            return null;
        }
        return SlotAccess.forListElement(this.itemStacks, slot);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.isWithinEntityInteractionRange(this.getBoundingBox(), 4.0);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(user.getLivingEntity()));
    }

}
