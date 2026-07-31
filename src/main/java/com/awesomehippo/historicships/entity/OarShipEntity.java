package com.awesomehippo.historicships.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public abstract class OarShipEntity extends Entity {
    private float deltaRotation;
    private float steerSmoothed;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private boolean hardRowing;
    private float rowPhase;
    private float rowPhaseO;
    private float rowIntensity;
    private float rowIntensityO;
    private float hardAmount;
    private float hardAmountO;
    private float sailFill;
    private float smoothedMaxSpeed;
    private int outOfWaterTicks;

    protected abstract OarShipStats stats();

    protected OarShipEntity(EntityType<? extends OarShipEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.smoothedMaxSpeed = this.stats().cruise;
    }

    protected void placeAt(double x, double y, double z) {
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
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
                double x = pos.x + fl * this.stats().cullHalfLoa * bowX + s * this.stats().cullHalfBeam * stbdX;
                double z = pos.z + fl * this.stats().cullHalfLoa * bowZ + s * this.stats().cullHalfBeam * stbdZ;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        return new AABB(minX, pos.y - 0.5, minZ, maxX, pos.y + this.stats().cullHeight, maxZ);
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
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hit) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide()) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.stats().maxPassengers;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        float seatY = this.stats().modelDeckY * this.stats().u + this.stats().seatYPad;
        int index = this.getPassengers().indexOf(passenger);
        if (index < 0) {
            index = this.getPassengers().size();
        }
        if (index >= this.stats().seatXz.length) {
            index = this.stats().seatXz.length - 1;
        }
        float modelX = this.stats().seatXz[index][0];
        float modelZ = this.stats().seatXz[index][1];
        // (forward is negated)
        float localForward = -(modelX * this.stats().u);
        float localRight = modelZ * this.stats().u;
        return new Vec3(localRight, seatY, localForward).yRot(-this.getYRot() * ((float) Math.PI / 180.0F));
    }

    public boolean isConductor(Entity passenger) {
        return this.getControllingPassenger() == passenger;
    }

    public boolean isHardRowing() {
        return this.hardRowing;
    }

    public float getRowPhase(float partialTicks) {
        return Mth.lerp(partialTicks, this.rowPhaseO, this.rowPhase);
    }

    public float getRowIntensity(float partialTicks) {
        return Mth.lerp(partialTicks, this.rowIntensityO, this.rowIntensity);
    }

    public float getHardAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.hardAmountO, this.hardAmount);
    }

    public float getSailFill() {
        return this.sailFill;
    }

    @Override
    public void tick() {
        super.tick();
        this.tickLerp();
        this.rowPhaseO = this.rowPhase;
        this.rowIntensityO = this.rowIntensity;
        this.hardAmountO = this.hardAmount;

        if (this.isLocalInstanceAuthoritative()) {
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

        if (this.level().isClientSide()) {
            this.tickRowAnimation();
            this.updateSailFillState();
        }
    }

    private void tickRowAnimation() {
        double speed = this.getDeltaMovement().horizontalDistance();
        float target = 0.0F;
        if (this.isVehicle() && this.isInWater() && speed > 0.008D) {
            target = Mth.clamp((float) (speed / this.stats().rowSpeedDiv), 0.0F, 1.0F);
        }
        float ease = target < this.rowIntensity ? 0.10F : this.stats().rowEaseUp;
        this.rowIntensity += (target - this.rowIntensity) * ease;
        if (this.rowIntensity < 0.002F) {
            this.rowIntensity = 0.0F;
        }

        float hardTarget = (this.hardRowing && this.rowIntensity > 0.05F) ? 1.0F : 0.0F;
        this.hardAmount += (hardTarget - this.hardAmount) * 0.14F;
        if (this.hardAmount < 0.002F) {
            this.hardAmount = 0.0F;
        }

        float step = (this.stats().rowStepBase + this.stats().rowStepHard * this.hardAmount) * this.rowIntensity;
        this.rowPhase += step;
    }

    private void updateSailFillState() {
        double speed = this.getDeltaMovement().horizontalDistance();
        float target = Mth.clamp((float) (speed / this.stats().sailSpeedDiv), 0.0F, 1.0F);
        if (this.hardRowing) {
            target = Mth.clamp(target + this.stats().sailHardBonus, 0.0F, 1.0F);
        }
        float ease = target < this.sailFill ? 0.06F : 0.12F;
        this.sailFill += (target - this.sailFill) * ease;
        if (this.sailFill < 0.002F) {
            this.sailFill = 0.0F;
        }
    }

    @Override
    protected void doWaterSplashEffect() {}

    private void updateWaterContact() {
        if (this.isInWater()) {
            this.outOfWaterTicks = 0;
        } else {
            this.outOfWaterTicks = Math.min(this.outOfWaterTicks + 1, 40);
        }
    }

    private boolean isMarine() {
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
                double swell = Math.sin(this.tickCount * this.stats().swellRate) * this.stats().swellAmp;
                vy = v.y * 0.90D + this.stats().buoyLift + swell;
                vy = Mth.clamp(vy, -0.014D, 0.012D);
            }
            double drag = this.stats().waterDrag;
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
        double searchR = this.stats().halfLoa + this.stats().halfBeam + 2.0;
        AABB search = new AABB(this.getX() - searchR, this.getY() - 0.75, this.getZ() - searchR, this.getX() + searchR, this.getY() + this.stats().hullHeight + 1.0, this.getZ() + searchR);
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        float bowX = Mth.sin(yaw);
        float bowZ = -Mth.cos(yaw);
        float stbdX = -bowZ;
        float stbdZ = bowX;
        for (Entity other : this.level().getEntities(this, search, this::shouldHullCollideWith)) {
            pushEntityOutOfHull(other, bowX, bowZ, stbdX, stbdZ);
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
        if (head < 0.0 || feet > this.stats().hullHeight) {
            return;
        }
        // along = bow & across = beam
        double along = ox * bowX + oz * bowZ;
        double across = ox * stbdX + oz * stbdZ;
        float otherHalf = Math.max(other.getBbWidth() * 0.45F, 0.25F);
        double limAlong = this.stats().halfLoa + otherHalf;
        double limAcross = this.stats().halfBeam + otherHalf;
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

    private void controlShip() {
        this.hardRowing = false;
        if (!this.isVehicle()) {
            this.steerSmoothed += (0.0F - this.steerSmoothed) * 0.25F;
            return;
        }
        Entity controller = this.getFirstPassenger();
        if (!(controller instanceof Player player)) {
            this.steerSmoothed += (0.0F - this.steerSmoothed) * 0.25F;
            return;
        }

        float inputLeft = player.xxa;
        float forwardKey = player.zza;
        if (player.level().isClientSide()) {
            float[] axes = com.awesomehippo.historicships.client.ClientInput.moveAxes();
            inputLeft = axes[0];
            forwardKey = axes[1];
        }
        float thrustDir = -forwardKey;

        this.hardRowing = wantsSprint(player) && forwardKey > 0.05F;
        this.tickSmoothedMaxSpeed(this.hardRowing ? this.stats().hardRow : this.stats().cruise);

        this.steerSmoothed += (inputLeft - this.steerSmoothed) * this.stats().steerSmooth;
        if (Math.abs(this.steerSmoothed) < 0.01F) {
            this.steerSmoothed = 0.0F;
        }

        double horiz = this.getDeltaMovement().horizontalDistance();
        float turnScale = 1.0F - (float) Mth.clamp(horiz / this.stats().turnHorizDiv, 0.0D, this.stats().turnHorizCap);
        this.deltaRotation = -this.steerSmoothed * this.stats().turnRate * turnScale;
        this.setYRot(this.getYRot() + this.deltaRotation);

        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        double bowX = -Mth.sin(yawRad);
        double bowZ = Mth.cos(yawRad);

        if (thrustDir != 0.0F) {
            float max = this.smoothedMaxSpeed;
            if (forwardKey < 0.0F) {
                max *= this.stats().reverseMaxMul;
            }
            float blend = this.hardRowing ? this.stats().thrustBlendHard * this.stats().hardBlendMul : this.stats().thrustBlend * this.stats().softBlendMul;
            if (forwardKey < 0.0F) {
                blend *= 0.55F;
            }
            double dirX = bowX * Math.signum(thrustDir);
            double dirZ = bowZ * Math.signum(thrustDir);
            Vec3 v = this.getDeltaMovement();
            double nx = v.x + (dirX * max - v.x) * blend;
            double nz = v.z + (dirZ * max - v.z) * blend;
            this.setDeltaMovement(nx, v.y, nz);
        } else if (Math.abs(this.deltaRotation) > 0.01F && horiz > 0.03D) {
            Vec3 v = this.getDeltaMovement();
            double speed = horiz;
            double nx = v.x + (bowX * speed - v.x) * this.stats().coastAlign;
            double nz = v.z + (bowZ * speed - v.z) * this.stats().coastAlign;
            this.setDeltaMovement(nx, v.y, nz);
        }
    }

    private void tickSmoothedMaxSpeed(float waterTarget) {
        float target = this.isMarine() ? waterTarget : this.stats().landSpeed;
        float ease = target < this.smoothedMaxSpeed ? 0.07F : 0.11F;
        this.smoothedMaxSpeed += (target - this.smoothedMaxSpeed) * ease;
    }

    public static boolean wantsSprint(Player player) {
        if (player.isSprinting()) {
            return true;
        }
        if (player.level().isClientSide()) {
            return com.awesomehippo.historicships.client.ClientInput.sprintHeld();
        }
        return false;
    }

    private void tickLerp() {
        if (this.isClientAuthoritative()) {
            this.lerpSteps = 0;
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
        }
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.getXRot());
            this.lerpSteps--;
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity e = this.getFirstPassenger();
        return e instanceof LivingEntity living ? living : super.getControllingPassenger();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isInvulnerable()) {
            return false;
        }
        this.spawnAtLocation(level, this.stats().dropStack.get());
        this.kill(level);
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
