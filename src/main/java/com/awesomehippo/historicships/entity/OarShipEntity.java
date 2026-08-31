package com.awesomehippo.historicships.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class OarShipEntity extends StoredShipEntity {
    private static final EntityDataAccessor<Float> DATA_ROW_PHASE = SynchedEntityData.defineId(OarShipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROW_INTENSITY = SynchedEntityData.defineId(OarShipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROW_HARD = SynchedEntityData.defineId(OarShipEntity.class, EntityDataSerializers.FLOAT);

    private float deltaRotation;
    private float steerSmoothed;
    private boolean hardRowing;
    private float rowPhase;
    private float rowPhaseO;
    private float rowIntensity;
    private float rowIntensityO;
    private float hardAmount;
    private float hardAmountO;
    private float sailFill;
    private float smoothedMaxSpeed;
    private double animLastX;
    private double animLastZ;
    private boolean animPosInit;

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ROW_PHASE, 0.0F);
        builder.define(DATA_ROW_INTENSITY, 0.0F);
        builder.define(DATA_ROW_HARD, 0.0F);
    }

    @Override
    protected float halfLoa() {
        return this.stats().halfLoa;
    }

    @Override
    protected float halfBeam() {
        return this.stats().halfBeam;
    }

    @Override
    protected float hullHeight() {
        return this.stats().hullHeight;
    }

    @Override
    protected float cullHalfLoa() {
        return this.stats().cullHalfLoa;
    }

    @Override
    protected float cullHalfBeam() {
        return this.stats().cullHalfBeam;
    }

    @Override
    protected float cullHeight() {
        return this.stats().cullHeight;
    }

    @Override
    protected double swellRate() {
        return this.stats().swellRate;
    }

    @Override
    protected double swellAmp() {
        return this.stats().swellAmp;
    }

    @Override
    protected double buoyLift() {
        return this.stats().buoyLift;
    }

    @Override
    protected double waterDrag() {
        return this.stats().waterDrag;
    }

    @Override
    protected ItemStack createDropStack() {
        return this.stats().dropStack.get();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && this.getPassengers().size() < this.stats().maxPassengers;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        float seatY = this.stats().modelDeckY * this.stats().u + this.stats().seatYPad;
        int index = ShipAnimalCargo.playerIndex(this, passenger);
        if (index < 0) {
            index = 0;
        }
        if (index >= this.stats().seatXz.length) {
            index = this.stats().seatXz.length - 1;
        }
        float modelX = this.stats().seatXz[index][0];
        float modelZ = this.stats().seatXz[index][1];
        return ShipAnimalCargo.seatOffset(this.getYRot(), modelX, modelZ, seatY, this.stats().u);
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

        double animSpeed = 0.0D;
        if (this.animPosInit) {
            animSpeed = Math.hypot(this.getX() - this.animLastX, this.getZ() - this.animLastZ);
        }
        this.animLastX = this.getX();
        this.animLastZ = this.getZ();
        this.animPosInit = true;

        this.rowPhaseO = this.rowPhase;
        this.rowIntensityO = this.rowIntensity;
        this.hardAmountO = this.hardAmount;
        if (!this.level().isClientSide()) {
            this.tickRowing(animSpeed);
        } else {
            this.syncRowingFromData();
            this.updateSailFillState(animSpeed);
        }

        this.tickMovement();
    }

    private void tickRowing(double speed) {
        float target = 0.0F;
        if (this.isVehicle() && this.isInWater() && speed > 0.008D) {
            target = Mth.clamp((float) (speed / this.stats().rowSpeedDiv), 0.0F, 1.0F);
        }
        float ease = target < this.rowIntensity ? 0.10F : this.stats().rowEaseUp;
        this.rowIntensity += (target - this.rowIntensity) * ease;
        if (this.rowIntensity < 0.002F) {
            this.rowIntensity = 0.0F;
        }

        float hardTarget = (this.serverHardRowing() && this.rowIntensity > 0.05F) ? 1.0F : 0.0F;
        this.hardAmount += (hardTarget - this.hardAmount) * 0.14F;
        if (this.hardAmount < 0.002F) {
            this.hardAmount = 0.0F;
        }

        float step = (this.stats().rowStepBase + this.stats().rowStepHard * this.hardAmount) * this.rowIntensity;
        this.rowPhase = (this.rowPhase + step) % Mth.TWO_PI;
        if (this.isInWater() && this.serverMovingForward() && this.tickCount % 16 == 0) {
            this.playSound(SoundEvents.BOAT_PADDLE_WATER, 1.0F, 0.75F + this.random.nextFloat() * 0.2F);
        }

        this.entityData.set(DATA_ROW_PHASE, this.rowPhase);
        this.entityData.set(DATA_ROW_INTENSITY, this.rowIntensity);
        this.entityData.set(DATA_ROW_HARD, this.hardAmount);
    }

    private boolean serverHardRowing() {
        if (this.getControllingPassenger() instanceof ServerPlayer player) {
            Input input = player.getLastClientInput();
            return input.sprint() && input.forward();
        }
        return false;
    }

    private boolean serverMovingForward() {
        return this.getControllingPassenger() instanceof ServerPlayer player && player.getLastClientInput().forward();
    }

    private void syncRowingFromData() {
        this.rowIntensity += (this.entityData.get(DATA_ROW_INTENSITY) - this.rowIntensity) * 0.35F;
        this.hardAmount += (this.entityData.get(DATA_ROW_HARD) - this.hardAmount) * 0.35F;

        float step = (this.stats().rowStepBase + this.stats().rowStepHard * this.hardAmount) * this.rowIntensity;
        this.rowPhase += step;

        float synced = this.entityData.get(DATA_ROW_PHASE);
        while (synced < this.rowPhase - Mth.PI) {
            synced += Mth.TWO_PI;
        }
        while (synced > this.rowPhase + Mth.PI) {
            synced -= Mth.TWO_PI;
        }
        float err = synced - this.rowPhase;
        if (Math.abs(err) > 1.0F) {
            this.rowPhase = synced;
        } else {
            this.rowPhase += err * 0.2F;
        }
    }

    private void updateSailFillState(double speed) {
        float target = Mth.clamp((float) (speed / this.stats().sailSpeedDiv), 0.0F, 1.0F);
        if (this.hardAmount > 0.5F) {
            target = Mth.clamp(target + this.stats().sailHardBonus, 0.0F, 1.0F);
        }
        float ease = target < this.sailFill ? 0.06F : 0.12F;
        this.sailFill += (target - this.sailFill) * ease;
        if (this.sailFill < 0.002F) {
            this.sailFill = 0.0F;
        }
    }

    @Override
    protected void controlShip() {
        this.hardRowing = false;
        if (!this.isVehicle()) {
            this.steerSmoothed += (0.0F - this.steerSmoothed) * 0.25F;
            return;
        }
        Entity controller = this.getControllingPassenger();
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

        if (thrustDir != 0.0F && this.isMarine()) {
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
        float target = (this.isMarine() ? waterTarget : 0.0F) * this.hullSpeedMult();
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

}
