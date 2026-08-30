package com.awesomehippo.historicships.entity;

import com.awesomehippo.historicships.NapoleonShipMod;
import com.awesomehippo.historicships.menu.NapoleonEngineMenu;
import com.awesomehippo.historicships.network.FireBowShellPacket;

import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public class NapoleonShipEntity extends StoredShipEntity {

    public static final float MODEL_SCALE = 3.55F;
    public static final int CARGO_ROWS = 6;
    public static final int MAX_ANIMALS = 4;
    public static final int MAX_HULL = 80;
    private static final float U = MODEL_SCALE / 16.0F;
    private static final float HALF_BEAM = 11.40F * U;
    private static final float HALF_LOA = 60.5F * U;
    private static final float HULL_HEIGHT = 16.0F * U;
    private static final ShipHull HULL = ShipHull.ofModel(U,
            -55.8F, 7.10F,
            -54.5F, 8.55F,
            -52.0F, 10.40F,
            -19.5F, 10.40F,
            -18.5F, 11.48F,
            18.5F, 11.48F,
            19.5F, 10.40F,
            41.5F, 10.40F,
            42.0F, 7.55F,
            56.0F, 7.55F,
            57.6F, 6.25F,
            60.5F, 5.10F);
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
    private static final float[][] CARGO_XZ = {
        {8.0F, 0.0F},
        {0.0F, -3.5F},
        {0.0F, 3.5F},
        {-12.0F, 0.0F},
    };
    private float deltaRotation;
    private float steerSmoothed;
    private float helmAngle;
    private float helmAngleO;
    private boolean boosting;
    private float sailFill;
    private boolean sailsFurled;
    private float sailDeploy = 1.0F;
    private int sailToggleCooldown;
    private static final int SAIL_TOGGLE_COOLDOWN_TICKS = 40;
    private static final float CRUISE_SAILS = 0.40F;
    private static final float CRUISE_FURLED = 0.28F;
    private static final float BOOST_SAILS = 1.30F;
    private static final float BOOST_FURLED = 0.80F;
    private static final float LAND_SPEED = 0.08F;
    private static final float THRUST_BLEND = 0.22F;
    private static final float THRUST_BLEND_BOOST = 0.48F;
    private float smoothedMaxSpeed = CRUISE_SAILS;
    private static final float TURN_RATE = 2.15F;
    private static final float STEER_SMOOTH = 0.40F;
    private static final float BOW_SHELL_SPEED = 4.65F;

    public static final int MAX_WATER = 4000;
    public static final int WATER_PER_BUCKET = 1000;
    public static final int MIN_BOOST_PRESSURE = 22;
    private static final EntityDataAccessor<Integer> DATA_WATER = SynchedEntityData.defineId(NapoleonShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PRESSURE = SynchedEntityData.defineId(NapoleonShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BROADSIDE_UNTIL = SynchedEntityData.defineId(NapoleonShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SAILS_FURLED = SynchedEntityData.defineId(NapoleonShipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BOOSTING = SynchedEntityData.defineId(NapoleonShipEntity.class, EntityDataSerializers.BOOLEAN);

    private final SimpleContainer engineItems = new SimpleContainer(NapoleonEngineMenu.ENGINE_SLOTS) {
        @Override
        public boolean stillValid(Player player) {
            return NapoleonShipEntity.this.isAlive() && player.distanceToSqr(NapoleonShipEntity.this) < 4096.0D;
        }
    };
    private int waterLevel = MAX_WATER / 2;
    private int litTime;
    private int litDuration;
    private int pressure;
    private final ContainerData engineData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case NapoleonEngineMenu.DATA_WATER -> NapoleonShipEntity.this.waterLevel;
                case NapoleonEngineMenu.DATA_MAX_WATER -> MAX_WATER;
                case NapoleonEngineMenu.DATA_LIT -> NapoleonShipEntity.this.litTime;
                case NapoleonEngineMenu.DATA_LIT_TOTAL -> Math.max(1, NapoleonShipEntity.this.litDuration);
                case NapoleonEngineMenu.DATA_PRESSURE -> NapoleonShipEntity.this.pressure;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case NapoleonEngineMenu.DATA_WATER -> NapoleonShipEntity.this.waterLevel = value;
                case NapoleonEngineMenu.DATA_LIT -> NapoleonShipEntity.this.litTime = value;
                case NapoleonEngineMenu.DATA_LIT_TOTAL -> NapoleonShipEntity.this.litDuration = value;
                case NapoleonEngineMenu.DATA_PRESSURE -> NapoleonShipEntity.this.pressure = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return NapoleonEngineMenu.DATA_COUNT;
        }
    };

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WATER, MAX_WATER / 2);
        builder.define(DATA_PRESSURE, 0);
        builder.define(DATA_BROADSIDE_UNTIL, 0);
        builder.define(DATA_SAILS_FURLED, false);
        builder.define(DATA_BOOSTING, false);
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
    protected float halfLoa() {
        return HALF_LOA;
    }

    @Override
    protected float halfBeam() {
        return HALF_BEAM;
    }

    @Override
    protected float hullHeight() {
        return HULL_HEIGHT;
    }

    @Override
    protected float cullHalfLoa() {
        return CULL_HALF_LOA;
    }

    @Override
    protected float cullHalfBeam() {
        return CULL_HALF_BEAM;
    }

    @Override
    protected float cullHeight() {
        return CULL_HEIGHT;
    }

    @Override
    protected float hullBottomPad() {
        return 0.6F;
    }

    @Override
    protected int cargoRows() {
        return CARGO_ROWS;
    }

    @Override
    protected ItemStack createDropStack() {
        return new ItemStack(NapoleonShipMod.NAPOLEON_SHIP_ITEM.get());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hit) {
        ItemStack stack = player.getItemInHand(hand);
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
        final float modelDeckY = 13.0F;
        final float waterlinePad = 0.55F;
        float seatY = modelDeckY * U + waterlinePad;

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
            seatY -= 0.15F;
        } else {
            int index = ShipAnimalCargo.playerIndex(this, passenger);
            if (index < 0) {
                index = 0;
            }
            if (index >= SEAT_XZ.length) {
                index = SEAT_XZ.length - 1;
            }
            modelX = SEAT_XZ[index][0];
            modelZ = SEAT_XZ[index][1];
        }
        return ShipAnimalCargo.seatOffset(this.getYRot(), modelX, modelZ, seatY, U);
    }

    public int getAnimalCount() {
        return ShipAnimalCargo.countAnimals(this);
    }

    @Override
    public void tick() {
        super.tick();
        this.syncShipPosition();
        this.helmAngleO = this.helmAngle;

        if (!this.level().isClientSide()) {
            this.tickEngine();
            boolean boost = this.serverHardSteam();
            this.boosting = boost;
            this.entityData.set(DATA_BOOSTING, boost);
        }

        this.tickMovement();
        if (this.sailToggleCooldown > 0) {
            this.sailToggleCooldown--;
        }

        if (this.level().isClientSide()) {
            this.updateSailFillState();
            this.updateSailDeployState();
            this.tickSteamPlume();
        }
    }

    private void updateSailFillState() {
        double speed = Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
        float target = Mth.clamp((float) (speed / 0.45D), 0.0F, 1.0F);
        if (this.isBoosting()) {
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
        if (this.level().isClientSide() || this.sailToggleCooldown > 0) {
            return this.sailsFurled;
        }
        this.sailsFurled = !this.sailsFurled;
        this.entityData.set(DATA_SAILS_FURLED, this.sailsFurled);
        this.sailToggleCooldown = SAIL_TOGGLE_COOLDOWN_TICKS;
        this.level().playSound(null, this.getX(), this.getY() + 4.0, this.getZ(), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.9F, this.sailsFurled ? 0.8F : 1.05F);
        return this.sailsFurled;
    }

    public boolean areSailsFurled() {
        return this.level().isClientSide() ? this.entityData.get(DATA_SAILS_FURLED) : this.sailsFurled;
    }

    public float getSailDeploy() {
        return this.sailDeploy;
    }

    private void updateSailDeployState() {
        boolean furled = this.areSailsFurled();
        float target = furled ? 0.0F : 1.0F;

        float ease = furled ? 0.028F : 0.022F;
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
        double speed = Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
        boolean underWay = speed > 0.02;
        boolean boost = this.isBoosting();
        float steam = this.getPressure() / 100.0F;
        if (steam < 0.05F && !boost) {
            if (this.random.nextInt(8) != 0) {
                return;
            }
        }
        int puffsPerStack;
        if (boost) {
            puffsPerStack = 4 + this.random.nextInt(2);
        } else if (steam > 0.35F) {
            puffsPerStack = 2 + this.random.nextInt(2);
        } else if (underWay || steam > 0.1F) {
            puffsPerStack = 1 + this.random.nextInt(2);
        } else {
            puffsPerStack = this.random.nextInt(4) == 0 ? 1 : 0;
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

    @Override
    protected void controlShip() {
        this.boosting = false;
        if (!this.isVehicle()) {

            this.helmAngle += (0.0F - this.helmAngle) * 0.2F;
            this.steerSmoothed += (0.0F - this.steerSmoothed) * 0.25F;
            return;
        }
        Entity controller = this.getControllingPassenger();
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

        boolean wantsBoost = OarShipEntity.wantsSprint(player) && forwardKey > 0.05F;
        this.boosting = wantsBoost && this.canSteamBoost();
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
        if (this.level().isClientSide() && !this.isLocalInstanceAuthoritative()) {
            return this.entityData.get(DATA_BOOSTING);
        }
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
        if (this.getBroadsideCooldown() > 0) {
            return false;
        }
        this.entityData.set(DATA_BROADSIDE_UNTIL, (int) this.level().getGameTime() + BROADSIDE_COOLDOWN);
        return true;
    }

    public void serverFireBowShells(@Nullable LivingEntity shooter, int mode) {
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }

        if (this.getBroadsideCooldown() > 0) {
            return;
        }
        this.entityData.set(DATA_BROADSIDE_UNTIL, (int) this.level().getGameTime() + BROADSIDE_COOLDOWN);

        boolean fireLeft = mode == FireBowShellPacket.LEFT;
        boolean fireRight = mode == FireBowShellPacket.RIGHT;
        boolean fireFront = !fireLeft && !fireRight;

        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double bowX = Mth.sin(yaw);
        double bowZ = -Mth.cos(yaw);
        double stbdX = -bowZ;
        double stbdZ = bowX;
        double bowDist = 60.5F * U + 2.6;
        double gunY = this.getY() + 6.4F * U + 0.35;
        double[] lateral = {-2.9F * U, 2.9F * U};
        Vec3 shipVel = this.getDeltaMovement();

        if (fireFront) {
            for (double lat : lateral) {
                double ox = this.getX() + bowX * bowDist + stbdX * lat;
                double oz = this.getZ() + bowZ * bowDist + stbdZ * lat;
                CannonballEntity shell = new CannonballEntity(server, ox, gunY, oz, shooter, CannonballEntity.FRONT_EXPLOSION);
                shell.setSourceShip(this);
                double up = 0.06 + this.random.nextDouble() * 0.03;
                shell.setDeltaMovement(bowX * BOW_SHELL_SPEED + shipVel.x * 0.85, up + shipVel.y * 0.15, bowZ * BOW_SHELL_SPEED + shipVel.z * 0.85);
                server.addFreshEntity(shell);
            }
        }

        if (fireLeft || fireRight) {
            double beam = HALF_BEAM + 0.9;
            double stationStep = 5.0 * U;
            int s = fireLeft ? -1 : 1;
            for (int g = -4; g <= 4; g++) {
                for (int deck = 0; deck < 2; deck++) {
                    double ox = this.getX() + stbdX * beam * s + bowX * g * stationStep;
                    double oy = this.getY() + (0.95 + deck * 0.55) * (MODEL_SCALE / 2.75F);
                    double oz = this.getZ() + stbdZ * beam * s + bowZ * g * stationStep;
                    CannonballEntity shell = new CannonballEntity(server, ox, oy, oz, shooter, CannonballEntity.SIDE_EXPLOSION);
                    shell.setSourceShip(this);
                    double up = 0.06 + this.random.nextDouble() * 0.03;
                    shell.setDeltaMovement(stbdX * s * BOW_SHELL_SPEED + shipVel.x * 0.85, up + shipVel.y * 0.15, stbdZ * s * BOW_SHELL_SPEED + shipVel.z * 0.85);
                    server.addFreshEntity(shell);
                }
            }
        }

        server.playSound(null, this.getX(), gunY, this.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.NEUTRAL, 1.15F, 0.72F + this.random.nextFloat() * 0.08F);
        this.serverGunSmoke(server, bowX, bowZ, stbdX, stbdZ, gunY, bowDist, lateral, mode);
    }

    private void serverGunSmoke(ServerLevel server, double bowX, double bowZ, double stbdX, double stbdZ, double gunY, double bowDist, double[] lateral, int mode) {
        boolean fireLeft = mode == FireBowShellPacket.LEFT;
        boolean fireRight = mode == FireBowShellPacket.RIGHT;
        boolean fireFront = !fireLeft && !fireRight;
        if (fireLeft || fireRight) {
            double beam = HALF_BEAM;
            double stationStep = 5.0 * U;
            int s = fireLeft ? -1 : 1;
            for (int g = -4; g <= 4; g++) {
                for (int deck = 0; deck < 2; deck++) {
                    double ox = this.getX() + stbdX * beam * s + bowX * g * stationStep;
                    double oy = this.getY() + (0.95 + deck * 0.55) * (MODEL_SCALE / 2.75F);
                    double oz = this.getZ() + stbdZ * beam * s + bowZ * g * stationStep;
                    server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, ox, oy, oz, 4, 0.12, 0.08, 0.12, 0.04);
                    server.sendParticles(ParticleTypes.SMOKE, ox, oy, oz, 2, 0.08, 0.05, 0.08, 0.03);
                }
            }
        }
        if (fireFront) {
            for (double lat : lateral) {
                double ox = this.getX() + bowX * (bowDist + 0.9) + stbdX * lat;
                double oz = this.getZ() + bowZ * (bowDist + 0.9) + stbdZ * lat;
                server.sendParticles(ParticleTypes.SMOKE, ox, gunY, oz, 6, 0.10, 0.08, 0.10, 0.04);
                server.sendParticles(ParticleTypes.FLAME, ox, gunY, oz, 2, 0.04, 0.04, 0.04, 0.01);
            }
        }
    }

    public int getBroadsideCooldown() {
        return Math.max(0, this.entityData.get(DATA_BROADSIDE_UNTIL) - (int) this.level().getGameTime());
    }

    public float getHelmAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.helmAngleO, this.helmAngle);
    }

    public SimpleContainer getEngineContainer() {
        return this.engineItems;
    }

    public ContainerData getEngineData() {
        return this.engineData;
    }

    public static boolean isEngineFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.COAL_BLOCK);
    }

    public static int engineBurnTime(ItemStack stack) {
        if (stack.is(Items.COAL_BLOCK)) {
            return 16000;
        }
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return 1600;
        }
        return 0;
    }

    public int getWaterLevel() {
        return this.level().isClientSide() ? this.entityData.get(DATA_WATER) : this.waterLevel;
    }

    public int getPressure() {
        return this.level().isClientSide() ? this.entityData.get(DATA_PRESSURE) : this.pressure;
    }

    public boolean canSteamBoost() {
        return this.getPressure() >= MIN_BOOST_PRESSURE && this.getWaterLevel() > 0;
    }

    public void openEngineMenu(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.historicships.engine");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new NapoleonEngineMenu(id, inv, NapoleonShipEntity.this);
            }
        }, buf -> buf.writeVarInt(NapoleonShipEntity.this.getId()));
    }

    private void syncEngineData() {
        this.entityData.set(DATA_WATER, this.waterLevel);
        this.entityData.set(DATA_PRESSURE, this.pressure);
    }

    private void tickEngine() {
        this.tryAcceptWaterBucket();
        boolean wasLit = this.litTime > 0;

        boolean hardSteam = this.serverHardSteam();
        if (this.litTime > 0) {
            this.litTime--;
            if (this.waterLevel > 0) {
                if (this.tickCount % 6 == 0) {
                    this.waterLevel = Math.max(0, this.waterLevel - 1);
                }
                if (this.tickCount % 3 == 0) {
                    this.pressure = Math.min(100, this.pressure + 1);
                }
                if (hardSteam && this.tickCount % 2 == 0) {
                    this.waterLevel = Math.max(0, this.waterLevel - 1);
                }
            } else {
                if (this.tickCount % 4 == 0) {
                    this.pressure = Math.max(0, this.pressure - 2);
                }
            }
        } else {
            this.tryLightFuel();
            if (this.tickCount % 8 == 0) {
                this.pressure = Math.max(0, this.pressure - 1);
            }
        }

        if (hardSteam && this.pressure > 0 && this.tickCount % 5 == 0) {
            this.pressure = Math.max(0, this.pressure - 1);
        }

        if (wasLit != (this.litTime > 0) || this.tickCount % 5 == 0) {
            this.syncEngineData();
        }
    }

    private boolean serverHardSteam() {
        if (!(this.getControllingPassenger() instanceof Player player)) {
            return false;
        }
        return player.isSprinting() && this.waterLevel > 0 && this.pressure >= MIN_BOOST_PRESSURE;
    }

    private void tryAcceptWaterBucket() {
        ItemStack stack = this.engineItems.getItem(NapoleonEngineMenu.WATER_SLOT);
        if (!stack.is(Items.WATER_BUCKET) || this.waterLevel >= MAX_WATER) {
            return;
        }
        this.waterLevel = Math.min(MAX_WATER, this.waterLevel + WATER_PER_BUCKET);
        this.engineItems.setItem(NapoleonEngineMenu.WATER_SLOT, new ItemStack(Items.BUCKET));
        this.engineItems.setChanged();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.0F);
    }

    private void tryLightFuel() {
        if (this.waterLevel <= 0) {
            return;
        }
        ItemStack fuel = this.engineItems.getItem(NapoleonEngineMenu.FUEL_SLOT);
        int burn = engineBurnTime(fuel);
        if (burn <= 0) {
            return;
        }
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            this.engineItems.setItem(NapoleonEngineMenu.FUEL_SLOT, ItemStack.EMPTY);
        } else {
            this.engineItems.setItem(NapoleonEngineMenu.FUEL_SLOT, fuel);
        }
        this.litTime = burn;
        this.litDuration = burn;
        this.engineItems.setChanged();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.35F, 0.8F);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide() && reason.shouldDestroy()) {
            Containers.dropContents(this.level(), this, this.engineItems);
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        NonNullList<ItemStack> items = NonNullList.withSize(NapoleonEngineMenu.ENGINE_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty("EngineItems"), items);
        for (int i = 0; i < items.size(); i++) {
            this.engineItems.setItem(i, items.get(i));
        }
        this.waterLevel = input.getIntOr("BoilerWater", MAX_WATER / 2);
        this.litTime = input.getIntOr("LitTime", 0);
        this.litDuration = input.getIntOr("LitDuration", 0);
        this.pressure = input.getIntOr("Pressure", 0);
        this.syncEngineData();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        NonNullList<ItemStack> items = NonNullList.withSize(NapoleonEngineMenu.ENGINE_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < NapoleonEngineMenu.ENGINE_SLOTS; i++) {
            items.set(i, this.engineItems.getItem(i));
        }
        ContainerHelper.saveAllItems(output.child("EngineItems"), items);
        output.putInt("BoilerWater", this.waterLevel);
        output.putInt("LitTime", this.litTime);
        output.putInt("LitDuration", this.litDuration);
        output.putInt("Pressure", this.pressure);
    }
}
