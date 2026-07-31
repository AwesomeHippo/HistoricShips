package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.NapoleonShipMod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public class NapoleonShipEntity extends Entity {

    public static final float MODEL_SCALE = 3.55F;
    private static final float U = MODEL_SCALE / 16.0F;
    private static final float HALF_BEAM = 11.0F * U + 0.08F;
    private static final float HALF_LOA = 56.5F * U;
    private static final float HULL_HEIGHT = 13.0F * U + 0.28F;
    private static final float CULL_HALF_LOA = 94.0F * U;
    private static final float CULL_HALF_BEAM = 20.0F * U;
    private static final float CULL_HEIGHT = 98.0F * U;
    private static final float FUNNEL_AFT_X = -16.0F;
    private static final float FUNNEL_FWD_X = 11.0F;
    private static final float FUNNEL_MOUTH_Y = 26.3F;
    public static final int BROADSIDE_COOLDOWN = 36;
    public static final int MAX_PASSENGERS = 6;
    private static final float[][] SEAT_XZ = {
        {37.5F, 0.0F},
        {34.0F, -3.4F},
        {34.0F, 3.4F},
        {8.0F, -4.0F},
        {8.0F, 4.0F},
        {-34.0F, 0.0F},
    };
    private float deltaRotation;
    private float steerSmoothed;
    private float helmAngle;
    private float helmAngleO;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private int broadsideCooldown;
    private boolean boosting;
    private float sailFill;
    private boolean sailsFurled;
    private float sailDeploy = 1.0F;
    private int sailToggleCooldown;
    private static final int SAIL_TOGGLE_COOLDOWN_TICKS = 40;
    private static final float CRUISE_SAILS = 0.55F;
    private static final float CRUISE_FURLED = 0.40F;
    private static final float BOOST_SAILS = 2.05F;
    private static final float BOOST_FURLED = 1.20F;
    private static final float LAND_SPEED = 0.08F;
    private static final float THRUST_BLEND = 0.22F;
    private static final float THRUST_BLEND_BOOST = 0.48F;
    private float smoothedMaxSpeed = CRUISE_SAILS;
    private int outOfWaterTicks;
    private static final float TURN_RATE = 2.55F;
    private static final float STEER_SMOOTH = 0.40F;
    private static final float BOW_SHELL_SPEED = 4.65F;

    public NapoleonShipEntity(EntityType<? extends NapoleonShipEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public NapoleonShipEntity(Level level, double x, double y, double z) {
        this(NapoleonShipMod.NAPOLEON_SHIP_ENTITY.get(), level);
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
                double x = pos.x + fl * CULL_HALF_LOA * bowX + s * CULL_HALF_BEAM * stbdX;
                double z = pos.z + fl * CULL_HALF_LOA * bowZ + s * CULL_HALF_BEAM * stbdZ;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }

        return new AABB(minX, pos.y - 0.6, minZ, maxX, pos.y + CULL_HEIGHT, maxZ);
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
        return this.getPassengers().size() < MAX_PASSENGERS;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        final float modelDeckY = 13.0F;

        final float waterlinePad = 0.55F;
        float seatY = modelDeckY * U + waterlinePad;

        int index = this.getPassengers().indexOf(passenger);
        if (index < 0) {
            index = this.getPassengers().size();
        }
        if (index >= SEAT_XZ.length) {
            index = SEAT_XZ.length - 1;
        }

        float modelX = SEAT_XZ[index][0];
        float modelZ = SEAT_XZ[index][1];

        // (forward is negated)
        float localForward = -(modelX * U);
        float localRight = modelZ * U;
        return new Vec3(localRight, seatY, localForward).yRot(-this.getYRot() * ((float) Math.PI / 180.0F));
    }

    public boolean isConductor(Entity passenger) {
        return this.getControllingPassenger() == passenger;
    }

    @Override
    public void tick() {
        super.tick();
        this.tickLerp();
        this.helmAngleO = this.helmAngle;

        if (this.isLocalInstanceAuthoritative()) {
            this.updateWaterContact();
            this.applyBuoyancy();
            if (this.level().isClientSide()) {
                this.controlBoat();
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.broadsideCooldown > 0) {
            this.broadsideCooldown--;
        }

        this.setBoundingBox(this.makeBoundingBox());

        this.resolveHullCollisions();

        if (this.level().isClientSide()) {
            this.updateSailFillState();
            this.updateSailDeployState();
            if (this.sailToggleCooldown > 0) {
                this.sailToggleCooldown--;
            }
            this.tickSteamPlume();
        }
    }

    @Override
    protected void doWaterSplashEffect() {}

    private void updateSailFillState() {
        double speed = this.getDeltaMovement().horizontalDistance();

        float target = Mth.clamp((float) (speed / 0.45D), 0.0F, 1.0F);
        if (this.boosting) {
            target = Mth.clamp(target + 0.22F, 0.0F, 1.0F);
        }

        float ease = target < this.sailFill ? 0.028F : 0.14F;
        this.sailFill += (target - this.sailFill) * ease;

        if (this.sailFill < 0.0005F) {
            this.sailFill = 0.0F;
        }
    }

    public float getSailFill() {
        return this.sailFill;
    }

    public boolean toggleSails() {
        if (!this.canToggleSails()) {
            return this.sailsFurled;
        }
        this.sailsFurled = !this.sailsFurled;

        this.sailToggleCooldown = SAIL_TOGGLE_COOLDOWN_TICKS;
        return this.sailsFurled;
    }

    public boolean canToggleSails() {
        return this.sailToggleCooldown <= 0;
    }

    public boolean areSailsFurled() {
        return this.sailsFurled;
    }

    public float getSailDeploy() {
        return this.sailDeploy;
    }

    private void updateSailDeployState() {
        float target = this.sailsFurled ? 0.0F : 1.0F;

        float ease = this.sailsFurled ? 0.028F : 0.022F;
        this.sailDeploy += (target - this.sailDeploy) * ease;
        if (Math.abs(this.sailDeploy - target) < 0.002F) {
            this.sailDeploy = target;
        }
    }

    private Vec3 modelPointToWorld(float modelX, float modelY, float modelZ) {
        float localForward = -(modelX * U);
        float localRight = modelZ * U;
        float localY = modelY * U + 0.18F;
        return new Vec3(localRight, localY, localForward).yRot(-this.getYRot() * Mth.DEG_TO_RAD).add(this.position());
    }

    private void tickSteamPlume() {
        double speed = this.getDeltaMovement().horizontalDistance();
        boolean underWay = speed > 0.02;
        boolean boost = this.boosting;
        int puffsPerStack;
        if (boost) {
            puffsPerStack = 4 + this.random.nextInt(2);
        } else if (underWay) {
            puffsPerStack = 2 + this.random.nextInt(2);
        } else {
            puffsPerStack = this.random.nextInt(3) == 0 ? 1 : 0;
        }
        if (puffsPerStack <= 0) {
            return;
        }

        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double bowX = Mth.sin(yaw);
        double bowZ = -Mth.cos(yaw);
        double aftX = -bowX;
        double aftZ = -bowZ;

        // funnel smoke drifts behind the ship heading
        float[] funnelXs = {FUNNEL_AFT_X, FUNNEL_FWD_X};
        for (float fx : funnelXs) {
            Vec3 mouth = this.modelPointToWorld(fx, FUNNEL_MOUTH_Y, 0.0F);
            for (int i = 0; i < puffsPerStack; i++) {

                double drift = boost ? 0.10 + this.random.nextDouble() * 0.10
                                : underWay ? 0.06 + this.random.nextDouble() * 0.08
                                        : 0.025 + this.random.nextDouble() * 0.035;
                double rise = boost ? 0.06 + this.random.nextDouble() * 0.05
                                : underWay ? 0.045 + this.random.nextDouble() * 0.035
                                        : 0.028 + this.random.nextDouble() * 0.022;
                double jx = (this.random.nextDouble() - 0.5) * (boost ? 0.28 : 0.15);
                double jz = (this.random.nextDouble() - 0.5) * (boost ? 0.28 : 0.15);
                this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, mouth.x + jx * 0.45, mouth.y, mouth.z + jz * 0.45, aftX * drift + jx * 0.02, rise, aftZ * drift + jz * 0.02);
                if ((underWay || boost) && this.random.nextBoolean()) {
                    this.level().addParticle(ParticleTypes.SMOKE, mouth.x, mouth.y + 0.12, mouth.z, aftX * drift * 1.2, rise * 0.9, aftZ * drift * 1.2);
                }
                if (boost && this.random.nextInt(3) == 0) {
                    this.level().addParticle(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y + 0.05, mouth.z, aftX * drift * 0.9, rise * 1.1, aftZ * drift * 0.9);
                }
            }
        }
    }

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
                if (vy > 0.01D) {
                    vy = 0.01D;
                }
                if (vy < -0.012D) {
                    vy = -0.012D;
                }
            } else if (speed < 0.03D && Math.abs(v.y) < 0.02D) {

                vy = v.y * 0.5D;
                if (Math.abs(vy) < 0.002D) {
                    vy = 0.0D;
                }
            } else {
                double swellAmp = 0.00035D;
                double swell = Math.sin(this.tickCount * 0.024D) * swellAmp;
                vy = v.y * 0.90D + 0.0028D + swell;
                if (vy > 0.012D) {
                    vy = 0.012D;
                }
                if (vy < -0.014D) {
                    vy = -0.014D;
                }
            }

            double drag = 0.989D;
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

        double searchR = HALF_LOA + HALF_BEAM + 2.0;
        AABB search = new AABB(this.getX() - searchR, this.getY() - 0.75, this.getZ() - searchR, this.getX() + searchR, this.getY() + HULL_HEIGHT + 1.0, this.getZ() + searchR);

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

        if (head < 0.0 || feet > HULL_HEIGHT) {
            return;
        }

        // along = bow & across = beam
        double along = ox * bowX + oz * bowZ;
        double across = ox * stbdX + oz * stbdZ;

        float otherHalf = Math.max(other.getBbWidth() * 0.45F, 0.25F);
        double limAlong = HALF_LOA + otherHalf;
        double limAcross = HALF_BEAM + otherHalf;

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

    private void controlBoat() {
        this.boosting = false;
        if (!this.isVehicle()) {

            this.helmAngle += (0.0F - this.helmAngle) * 0.2F;
            this.steerSmoothed += (0.0F - this.steerSmoothed) * 0.25F;
            return;
        }
        Entity controller = this.getFirstPassenger();
        if (!(controller instanceof Player player)) {
            this.helmAngle += (0.0F - this.helmAngle) * 0.2F;
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

        this.boosting = OarShipEntity.wantsSprint(player) && forwardKey > 0.05F;
        this.tickSmoothedMaxSpeed(this.rawWaterMaxSpeed());

        this.steerSmoothed += (inputLeft - this.steerSmoothed) * STEER_SMOOTH;
        if (Math.abs(this.steerSmoothed) < 0.01F) {
            this.steerSmoothed = 0.0F;
        }

        double horiz = this.getDeltaMovement().horizontalDistance();
        float turnScale = 1.0F - (float) Mth.clamp(horiz / 1.50D, 0.0D, 0.28D);
        this.deltaRotation = -this.steerSmoothed * TURN_RATE * turnScale;
        this.setYRot(this.getYRot() + this.deltaRotation);

        float targetHelm = Mth.clamp(this.steerSmoothed, -1.0F, 1.0F) * 1.0F;
        this.helmAngle += (targetHelm - this.helmAngle) * 0.30F;

        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        double bowX = -Mth.sin(yawRad);
        double bowZ = Mth.cos(yawRad);

        if (thrustDir != 0.0F) {
            float max = this.smoothedMaxSpeed;
            if (forwardKey < 0.0F) {
                max *= 0.55F;
            }
            float blend = this.boosting ? THRUST_BLEND_BOOST * 0.40F : THRUST_BLEND * 0.38F;
            blend *= 0.85F + 0.25F * this.sailDeploy;
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
            double nx = v.x + (bowX * speed - v.x) * 0.06D;
            double nz = v.z + (bowZ * speed - v.z) * 0.06D;
            this.setDeltaMovement(nx, v.y, nz);
        }
    }

    public boolean isBoosting() {
        return this.boosting;
    }

    private float rawWaterMaxSpeed() {
        float d = Mth.clamp(this.sailDeploy, 0.0F, 1.0F);
        float cruise = Mth.lerp(d, CRUISE_FURLED, CRUISE_SAILS);
        float boost = Mth.lerp(d, BOOST_FURLED, BOOST_SAILS);
        return this.boosting ? boost : cruise;
    }

    private void tickSmoothedMaxSpeed(float waterTarget) {
        float target = this.isMarine() ? waterTarget : LAND_SPEED;
        float ease = target < this.smoothedMaxSpeed ? 0.07F : 0.11F;
        this.smoothedMaxSpeed += (target - this.smoothedMaxSpeed) * ease;
    }

    public boolean tryFireAll() {
        if (this.broadsideCooldown > 0) {
            return false;
        }
        this.broadsideCooldown = BROADSIDE_COOLDOWN;

        if (this.level().isClientSide()) {
            float yaw = this.getYRot() * Mth.DEG_TO_RAD;

            double bowX = Mth.sin(yaw);
            double bowZ = -Mth.cos(yaw);
            double stbdX = -bowZ;
            double stbdZ = bowX;
            double beam = HALF_BEAM;
            double stationStep = 5.0 * U;

            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.9F, 1.15F + this.random.nextFloat() * 0.15F, false);
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.55F, 0.85F + this.random.nextFloat() * 0.1F, false);
            for (int s = -1; s <= 1; s += 2) {
                for (int g = -4; g <= 4; g++) {
                    for (int deck = 0; deck < 2; deck++) {
                        double ox = this.getX() + stbdX * beam * s + bowX * g * stationStep + (this.random.nextDouble() - 0.5) * 0.35;
                        double oy = this.getY() + (0.95 + deck * 0.55) * (MODEL_SCALE / 2.75F) + this.random.nextDouble() * 0.2;
                        double oz = this.getZ() + stbdZ * beam * s + bowZ * g * stationStep + (this.random.nextDouble() - 0.5) * 0.35;

                        double out = 0.22 + this.random.nextDouble() * 0.14;
                        double rise = 0.04 + this.random.nextDouble() * 0.06;

                        for (int p = 0; p < 4; p++) {
                            this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, ox, oy, oz, stbdX * s * out, rise, stbdZ * s * out);
                        }
                        for (int p = 0; p < 3; p++) {
                            this.level().addParticle(ParticleTypes.LARGE_SMOKE, ox + stbdX * s * 0.15, oy + 0.05, oz + stbdZ * s * 0.15, stbdX * s * (out * 1.25), rise * 0.8, stbdZ * s * (out * 1.25));
                        }
                        this.level().addParticle(ParticleTypes.SMOKE, ox, oy, oz, stbdX * s * 0.28, 0.03, stbdZ * s * 0.28);

                        if (this.random.nextBoolean()) {
                            this.level().addParticle(ParticleTypes.FLAME, ox + stbdX * s * 0.2, oy, oz + stbdZ * s * 0.2, stbdX * s * 0.08, 0.02, stbdZ * s * 0.08);
                        }
                    }
                }
            }

            fireBowChasersFx(bowX, bowZ, stbdX, stbdZ);
        }
        return true;
    }

    private void fireBowChasersFx(double bowX, double bowZ, double stbdX, double stbdZ) {
        double bowDist = 60.5F * U;
        double gunY = this.getY() + 6.4F * U + 0.2;
        double boomX = this.getX() + bowX * bowDist;
        double boomZ = this.getZ() + bowZ * bowDist;

        this.level().playLocalSound(boomX, gunY, boomZ, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.55F, 1.15F + this.random.nextFloat() * 0.15F, false);

        double[] lateral = {-2.9F * U, 2.9F * U};
        for (double lat : lateral) {

            double ox = this.getX() + bowX * (bowDist + 0.9) + stbdX * lat;
            double oz = this.getZ() + bowZ * (bowDist + 0.9) + stbdZ * lat;
            for (int i = 0; i < 8; i++) {
                double speed = 0.18 + this.random.nextDouble() * 0.2;
                this.level().addParticle(ParticleTypes.SMOKE, ox + (this.random.nextDouble() - 0.5) * 0.1, gunY + this.random.nextDouble() * 0.15, oz + (this.random.nextDouble() - 0.5) * 0.1, bowX * speed, 0.02, bowZ * speed);
            }
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(ParticleTypes.FLAME, ox + bowX * 0.15, gunY, oz + bowZ * 0.15, bowX * 0.08, 0.01, bowZ * 0.08);
            }
        }
    }

    public void serverFireBowShells(@Nullable LivingEntity shooter) {
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }

        if (this.broadsideCooldown > 0) {
            return;
        }
        this.broadsideCooldown = BROADSIDE_COOLDOWN;

        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double bowX = Mth.sin(yaw);
        double bowZ = -Mth.cos(yaw);
        double stbdX = -bowZ;
        double stbdZ = bowX;
        double bowDist = 60.5F * U + 2.6;
        double gunY = this.getY() + 6.4F * U + 0.35;
        double[] lateral = {-2.9F * U, 2.9F * U};

        Vec3 shipVel = this.getDeltaMovement();

        for (double lat : lateral) {
            double ox = this.getX() + bowX * bowDist + stbdX * lat;
            double oz = this.getZ() + bowZ * bowDist + stbdZ * lat;
            CannonballEntity shell = new CannonballEntity(server, ox, gunY, oz, shooter);

            double up = 0.06 + this.random.nextDouble() * 0.03;
            shell.setDeltaMovement(bowX * BOW_SHELL_SPEED + shipVel.x * 0.85, up + shipVel.y * 0.15, bowZ * BOW_SHELL_SPEED + shipVel.z * 0.85);
            server.addFreshEntity(shell);
        }

        server.playSound(null, this.getX(), gunY, this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.15F, 0.75F + this.random.nextFloat() * 0.15F);
    }

    public int getBroadsideCooldown() {
        return this.broadsideCooldown;
    }

    public float getHelmAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.helmAngleO, this.helmAngle);
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
        this.spawnAtLocation(level, new ItemStack(NapoleonShipMod.NAPOLEON_SHIP_ITEM.get()));
        this.kill(level);
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
