package com.awesomehippo.historicships.client;

import com.awesomehippo.historicships.HistoricShips;
import com.awesomehippo.historicships.client.model.DrakkarModel;
import com.awesomehippo.historicships.client.model.NapoleonShipModel;
import com.awesomehippo.historicships.client.model.QuinqueremeModel;
import com.awesomehippo.historicships.client.model.QuinqueremePaintModel;
import com.awesomehippo.historicships.client.renderer.DrakkarRenderer;
import com.awesomehippo.historicships.client.renderer.NapoleonShipRenderer;
import com.awesomehippo.historicships.client.renderer.QuinqueremeRenderer;
import com.awesomehippo.historicships.client.renderer.SailPaintTextures;
import com.awesomehippo.historicships.client.screen.NapoleonShipScreen;
import com.awesomehippo.historicships.client.screen.SailPaintScreen;
import com.awesomehippo.historicships.client.screen.ShipwrightScreen;
import com.awesomehippo.historicships.entity.DrakkarEntity;
import com.awesomehippo.historicships.entity.DrakkarSailStripe;
import com.awesomehippo.historicships.entity.NapoleonShipEntity;
import com.awesomehippo.historicships.entity.OarShipEntity;
import com.awesomehippo.historicships.entity.QuinqueremeEntity;
import com.awesomehippo.historicships.entity.ShipAnimalCargo;
import com.awesomehippo.historicships.entity.StoredShipEntity;
import com.awesomehippo.historicships.item.HistoricShipItem;
import com.awesomehippo.historicships.network.FireBowShellPacket;
import com.awesomehippo.historicships.network.FireTowerStonePacket;
import com.awesomehippo.historicships.network.RamHitPacket;
import com.awesomehippo.historicships.network.ToggleSailsPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(value = HistoricShips.MODID, dist = Dist.CLIENT)
public class HistoricShipsClient {

    private static final float CONDUCTOR_FOV = 28.0F;

    private static final float OAR_CONDUCTOR_FOV = 32.0F;

    private static final float PASSENGER_FOV = 14.0F;
    private static final float OAR_PASSENGER_FOV = 16.0F;

    private static final float FOV_SMOOTH = 0.18F;

    private static final float SPEED_SMOOTH = 0.10F;

    private static final int SPEED_LABEL_INTERVAL_TICKS = 15;

    private static final float SPEED_DISPLAY_QUANT = 1.0F;

    private float rideFovBonus;

    private float smoothedBps;

    private float displayedBps = -1.0F;
    private String displayedSpeed = "0 b/s";
    private int speedVehicleId = -1;

    private int lastSpeedLabelTick = -999;
    private boolean frontHeld;
    private int engineHintTick;

    public HistoricShipsClient(IEventBus modBus) {
        modBus.addListener(this::registerLayers);
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::registerScreens);
        modBus.addListener(HistoricShipsKeys::register);

        RamHitPacket.clientSend = packet -> ClientPacketDistributor.sendToServer(packet);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(ShipLook::tick);
        NeoForge.EVENT_BUS.addListener(ShipLook::scroll);
        NeoForge.EVENT_BUS.addListener(ShipLook::cameraFov);
        NeoForge.EVENT_BUS.addListener(ShipLook::cameraAngles);
        NeoForge.EVENT_BUS.addListener(ShipLook::cameraDistance);
        NeoForge.EVENT_BUS.addListener(BowTrajectoryPreview::render);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);
        NeoForge.EVENT_BUS.addListener(this::onComputeFov);
        NeoForge.EVENT_BUS.addListener(this::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(this::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(this::onSailBrush);
    }

    private void onSailBrush(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide() || !(event.getTarget() instanceof QuinqueremeEntity ship)) {
            return;
        }
        if (event.getItemStack().is(HistoricShips.SAIL_BRUSH.get()) && ship.canEditSail(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            SailPaintScreen.open(ship);
        }
    }

    private void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof QuinqueremeEntity ship) {
            SailPaintTextures.release(ship.getId());
        }
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SailPaintTextures.clear();
    }

    private void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof HistoricShipItem) {
            Integer hull = stack.get(HistoricShips.SHIP_HULL.get());
            if (hull != null) {
                event.getToolTip().add(Component.translatable("item.historicships.hull", hull));
            }
            Integer stripe = stack.get(HistoricShips.SHIP_SAIL_STRIPE.get());
            if (stripe != null) {
                event.getToolTip().add(Component.translatable("item.historicships.sail_stripe", Component.translatable("color.minecraft." + DrakkarSailStripe.byId(stripe.byteValue()).dye().getName())));
            }
        }
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(HistoricShips.SHIPWRIGHT_MENU.get(), ShipwrightScreen::new);
        event.register(HistoricShips.NAPOLEON_MENU.get(), NapoleonShipScreen::new);
    }

    private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NapoleonShipModel.LAYER_LOCATION, NapoleonShipModel::createBodyLayer);
        event.registerLayerDefinition(DrakkarModel.LAYER_LOCATION, DrakkarModel::createBodyLayer);
        event.registerLayerDefinition(QuinqueremeModel.LAYER_LOCATION, QuinqueremeModel::createBodyLayer);
        event.registerLayerDefinition(QuinqueremePaintModel.LAYER_LOCATION, QuinqueremePaintModel::createBodyLayer);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HistoricShips.NAPOLEON_SHIP_ENTITY.get(), NapoleonShipRenderer::new);
        event.registerEntityRenderer(HistoricShips.DRAKKAR_ENTITY.get(), DrakkarRenderer::new);
        event.registerEntityRenderer(HistoricShips.QUINQUEREME_ENTITY.get(), QuinqueremeRenderer::new);

        event.registerEntityRenderer(HistoricShips.CANNONBALL_ENTITY.get(), ctx -> new ThrownItemRenderer<>(ctx, 4.6F, true));
        event.registerEntityRenderer(HistoricShips.STONE_BULLET_ENTITY.get(), ctx -> new ThrownItemRenderer<>(ctx, 3.4F, true));
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            this.frontHeld = false;
            return;
        }
        LocalPlayer player = mc.player;
        if (player.getVehicle() instanceof NapoleonShipEntity napoleon && napoleon.isConductor(player)) {
            this.hintEngine(player, napoleon);
        }
        if (mc.screen != null) {
            this.frontHeld = false;
            return;
        }
        if (player.getVehicle() instanceof NapoleonShipEntity napoleon) {
            if (!napoleon.isConductor(player)) {
                this.frontHeld = false;
                return;
            }
            boolean down = HistoricShipsKeys.FIRE_FRONT.isDown();
            HistoricShipsKeys.FIRE_FRONT.consumeClick();
            if (down) {
                this.frontHeld = true;
            } else if (this.frontHeld) {
                this.frontHeld = false;
                if (napoleon.tryFireAll()) {
                    ClientPacketDistributor.sendToServer(new FireBowShellPacket(napoleon.getId(), FireBowShellPacket.FRONT));
                }
            }
            if (HistoricShipsKeys.FIRE_LEFT.consumeClick()) {
                if (napoleon.tryFireAll()) {
                    ClientPacketDistributor.sendToServer(new FireBowShellPacket(napoleon.getId(), FireBowShellPacket.LEFT));
                }
            }
            if (HistoricShipsKeys.FIRE_RIGHT.consumeClick()) {
                if (napoleon.tryFireAll()) {
                    ClientPacketDistributor.sendToServer(new FireBowShellPacket(napoleon.getId(), FireBowShellPacket.RIGHT));
                }
            }
            if (HistoricShipsKeys.TOGGLE_SAILS.consumeClick()) {
                ClientPacketDistributor.sendToServer(new ToggleSailsPacket(napoleon.getId()));
            }
            return;
        }
        if (player.getVehicle() instanceof QuinqueremeEntity quin) {
            if (!quin.isConductor(player)) {
                this.frontHeld = false;
                return;
            }
            boolean down = HistoricShipsKeys.FIRE_FRONT.isDown();
            HistoricShipsKeys.FIRE_FRONT.consumeClick();
            if (down) {
                this.frontHeld = true;
            } else if (this.frontHeld) {
                this.frontHeld = false;
                if (quin.tryFireTower()) {
                    ClientPacketDistributor.sendToServer(new FireTowerStonePacket(quin.getId()));
                }
            }
            return;
        }
        this.frontHeld = false;
    }

    private void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        float target = 0.0F;
        if (player != null && mc.options.getCameraType().isFirstPerson() && player.getVehicle() != null) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof NapoleonShipEntity ship) {
                target = ship.isConductor(player) ? CONDUCTOR_FOV : PASSENGER_FOV;
            } else if (vehicle instanceof DrakkarEntity ship) {
                target = ship.isConductor(player) ? OAR_CONDUCTOR_FOV : OAR_PASSENGER_FOV;
            } else if (vehicle instanceof QuinqueremeEntity ship) {
                target = ship.isConductor(player) ? OAR_CONDUCTOR_FOV : OAR_PASSENGER_FOV;
            }
        }

        this.rideFovBonus += (target - this.rideFovBonus) * FOV_SMOOTH;
        if (this.rideFovBonus < 0.05F) {
            this.rideFovBonus = 0.0F;
            return;
        }
        event.setFOV(event.getFOV() + this.rideFovBonus);
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return;
        }

        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();

        if (vehicle instanceof NapoleonShipEntity ship) {
            if (ship.isConductor(player)) {
                drawConductorPanel(g, font, sw, ship);
            } else {
                drawPassengerPanel(g, font, sw, ship, formatSpeedLine(ship, ship.isBoosting()), ShipAnimalCargo.countPlayers(ship), NapoleonShipEntity.MAX_PASSENGERS, ship.getAnimalCount(), NapoleonShipEntity.MAX_ANIMALS);
            }
            return;
        }
        if (vehicle instanceof DrakkarEntity ship) {
            if (ship.isConductor(player)) {
                drawOarShipPanel(g, font, sw, tr("entity.historicships.drakkar"), ship, ShipAnimalCargo.countPlayers(ship), DrakkarEntity.MAX_PASSENGERS, ship.getAnimalCount(), DrakkarEntity.MAX_ANIMALS);
            } else {
                drawPassengerPanel(g, font, sw, ship, formatSpeedLine(ship, oarSpeeding(ship)), ShipAnimalCargo.countPlayers(ship), DrakkarEntity.MAX_PASSENGERS, ship.getAnimalCount(), DrakkarEntity.MAX_ANIMALS);
            }
            return;
        }
        if (vehicle instanceof QuinqueremeEntity ship) {
            if (ship.isConductor(player)) {
                drawQuinqueremePanel(g, font, sw, ship);
            } else {
                drawPassengerPanel(g, font, sw, ship, formatSpeedLine(ship, oarSpeeding(ship)), ShipAnimalCargo.countPlayers(ship), QuinqueremeEntity.MAX_PASSENGERS, ship.getAnimalCount(), QuinqueremeEntity.MAX_ANIMALS);
            }
        }
    }

    // smooth speed label updates so the hud doesn't flicker
    private String formatSpeed(Entity vehicle) {
        float instant = (float) (vehicle.getDeltaMovement().horizontalDistance() * 20.0D);
        int tick = vehicle.tickCount;
        if (vehicle.getId() != this.speedVehicleId) {
            this.speedVehicleId = vehicle.getId();
            this.smoothedBps = instant;
            this.displayedBps = quantizeSpeed(instant);
            this.displayedSpeed = formatSpeedLabel(this.displayedBps);
            this.lastSpeedLabelTick = tick;
            return this.displayedSpeed;
        }

        this.smoothedBps += (instant - this.smoothedBps) * SPEED_SMOOTH;
        if (this.smoothedBps < 0.20F && instant < 0.20F) {
            this.smoothedBps = 0.0F;
        }

        float quantized = quantizeSpeed(this.smoothedBps);
        boolean stopped = quantized <= 0.0F && this.displayedBps > 0.0F;
        boolean started = quantized > 0.0F && this.displayedBps <= 0.0F && this.smoothedBps > 0.5F;
        boolean intervalElapsed = tick - this.lastSpeedLabelTick >= SPEED_LABEL_INTERVAL_TICKS;

        boolean bigJump = Math.abs(quantized - this.displayedBps) >= 3.0F;

        if (stopped || started || (intervalElapsed && quantized != this.displayedBps) || bigJump) {
            this.displayedBps = quantized;
            this.displayedSpeed = formatSpeedLabel(quantized);
            this.lastSpeedLabelTick = tick;
        }
        return this.displayedSpeed;
    }

    private static float quantizeSpeed(float bps) {
        if (bps < 0.35F) {
            return 0.0F;
        }
        return Math.round(bps / SPEED_DISPLAY_QUANT) * SPEED_DISPLAY_QUANT;
    }

    private static String formatSpeedLabel(float bps) {
        return String.format(java.util.Locale.ROOT, "%.0f b/s", bps);
    }

    private String formatSpeedLine(Entity vehicle, boolean speeding) {
        return tr("gui.historicships.hud.speed_mode", formatSpeed(vehicle), tr(speeding ? "gui.historicships.hud.fast" : "gui.historicships.hud.normal"));
    }

    private static boolean oarSpeeding(OarShipEntity ship) {
        return ship.isHardRowing() || ship.getHardAmount(0.0F) > 0.5F;
    }

    private void drawOarShipPanel(GuiGraphicsExtractor g, Font font, int sw, String title, StoredShipEntity ship, int crew, int maxCrew, int animals, int maxAnimals) {
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        boolean speeding = ship instanceof OarShipEntity oar && oarSpeeding(oar);
        rows.add(new String[] {tr("gui.historicships.hud.speed"), formatSpeedLine(ship, speeding)});
        rows.add(new String[] {tr("gui.historicships.hud.hull"), formatHull(ship)});
        rows.add(new String[] {tr("gui.historicships.hud.crew"), crew + "/" + maxCrew});
        if (maxAnimals > 0) {
            rows.add(new String[] {tr("gui.historicships.hud.animals"), animals + "/" + maxAnimals});
        }
        rows.add(new String[] {tr("gui.historicships.hud.cargo"), cargoKeys()});
        rows.add(new String[] {tr("gui.historicships.hud.look"), keyName(HistoricShipsKeys.LOOK_AROUND)});
        drawPanel(g, font, sw, panelTitle(ship, title), rows.toArray(new String[0][]));
    }

    private void drawQuinqueremePanel(GuiGraphicsExtractor g, Font font, int sw, QuinqueremeEntity ship) {
        String kFire = HistoricShipsKeys.FIRE_FRONT.getTranslatedKeyMessage().getString();
        String tower = ship.getTowerCooldown() > 0
                ? tr("gui.historicships.hud.weapon_wait", kFire, ship.getTowerCooldown() / 20 + 1)
                : tr("gui.historicships.hud.weapon_ready", kFire);
        drawPanel(g, font, sw, panelTitle(ship, tr("entity.historicships.quinquereme")), new String[][] {
            {tr("gui.historicships.hud.speed"), formatSpeedLine(ship, oarSpeeding(ship))},
            {tr("gui.historicships.hud.hull"), formatHull(ship)},
            {tr("gui.historicships.hud.crew"), ShipAnimalCargo.countPlayers(ship) + "/" + QuinqueremeEntity.MAX_PASSENGERS},
            {tr("gui.historicships.hud.animals"), ship.getAnimalCount() + "/" + QuinqueremeEntity.MAX_ANIMALS},
            {tr("gui.historicships.hud.tower"), tower},
            {tr("gui.historicships.hud.cargo"), cargoKeys()},
            {tr("gui.historicships.hud.look"), keyName(HistoricShipsKeys.LOOK_AROUND)}
        });
    }

    private void drawConductorPanel(GuiGraphicsExtractor g, Font font, int sw, NapoleonShipEntity ship) {
        String kFire = HistoricShipsKeys.FIRE_FRONT.getTranslatedKeyMessage().getString();
        String kLeft = HistoricShipsKeys.FIRE_LEFT.getTranslatedKeyMessage().getString();
        String kRight = HistoricShipsKeys.FIRE_RIGHT.getTranslatedKeyMessage().getString();
        String kGuns = kFire + " / " + kLeft + " / " + kRight;
        String kSail = HistoricShipsKeys.TOGGLE_SAILS.getTranslatedKeyMessage().getString();

        String guns = ship.getBroadsideCooldown() > 0
                ? tr("gui.historicships.hud.weapon_wait", kGuns, ship.getBroadsideCooldown() / 20 + 1)
                : tr("gui.historicships.hud.weapon_ready", kGuns);

        String sails = tr(ship.areSailsFurled() ? "gui.historicships.hud.sails_furled" : "gui.historicships.hud.sails_open", kSail);
        int crew = ShipAnimalCargo.countPlayers(ship);
        drawPanel(g, font, sw, panelTitle(ship, tr("entity.historicships.napoleon_ship")), new String[][] {
            {tr("gui.historicships.hud.speed"), formatSpeedLine(ship, ship.isBoosting())},
            {tr("gui.historicships.hud.hull"), formatHull(ship)},
            {tr("gui.historicships.hud.crew"), crew + "/" + NapoleonShipEntity.MAX_PASSENGERS},
            {tr("gui.historicships.hud.animals"), ship.getAnimalCount() + "/" + NapoleonShipEntity.MAX_ANIMALS},
            {tr("gui.historicships.hud.guns"), guns},
            {tr("gui.historicships.hud.sails"), sails},
            {tr("gui.historicships.hud.cargo"), cargoKeys()},
            {tr("gui.historicships.hud.look"), keyName(HistoricShipsKeys.LOOK_AROUND)}
        });
    }

    private static String formatHull(StoredShipEntity ship) {
        return ship.getHullPercent() + "%";
    }

    private void hintEngine(LocalPlayer player, NapoleonShipEntity ship) {
        if (ship.canSteamBoost() || this.engineHintTick++ % 20 != 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) {
            return;
        }
        String inv = keyName(mc.options.keyInventory);
        if (ship.getWaterLevel() <= 0) {
            player.sendOverlayMessage(Component.translatable("gui.historicships.hud.need_water", inv));
            return;
        }
        player.sendOverlayMessage(Component.translatable("gui.historicships.hud.need_coal", inv));
    }

    private static String panelTitle(StoredShipEntity ship, String title) {
        return ship.isSinking() ? tr("gui.historicships.hud.sinking", title) : title;
    }

    private static String cargoKeys() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return tr("gui.historicships.hud.shift_use");
        }
        return tr("gui.historicships.hud.cargo_keys", tr("gui.historicships.hud.shift_use"), keyName(mc.options.keyInventory));
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String keyName(net.minecraft.client.KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    private void drawPassengerPanel(GuiGraphicsExtractor g, Font font, int sw, StoredShipEntity ship, String speed, int crew, int maxCrew, int animals, int maxAnimals) {
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[] {tr("gui.historicships.hud.speed"), speed});
        rows.add(new String[] {tr("gui.historicships.hud.hull"), formatHull(ship)});
        rows.add(new String[] {tr("gui.historicships.hud.crew"), crew + "/" + maxCrew});
        if (maxAnimals > 0) {
            rows.add(new String[] {tr("gui.historicships.hud.animals"), animals + "/" + maxAnimals});
        }
        rows.add(new String[] {tr("gui.historicships.hud.look"), keyName(HistoricShipsKeys.LOOK_AROUND)});
        drawPanel(g, font, sw, panelTitle(ship, tr("gui.historicships.hud.passenger")), rows.toArray(new String[0][]));
    }

    private void drawPanel(GuiGraphicsExtractor g, Font font, int sw, String title, String[][] rows) {
        final int padX = 6;
        final int padY = 4;
        final int lineH = font.lineHeight + 1;
        final int gap = 8;

        int maxLabel = font.width(title);
        int maxValue = 0;
        for (String[] row : rows) {
            maxLabel = Math.max(maxLabel, font.width(row[0]));
            maxValue = Math.max(maxValue, font.width(row[1]));
        }
        final int boxW = Math.min(152, Math.max(108, padX * 2 + maxLabel + gap + maxValue));
        final int boxH = padY * 2 + lineH * (1 + rows.length) + 1;
        final int boxX = sw - boxW - 5;
        final int boxY = 5;

        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, ARGB.color(140, 0, 0, 0));

        int y = boxY + padY;
        g.text(font, title, boxX + padX, y, 0xFFFFFFFF, true);
        y += lineH + 1;

        int valueRight = boxX + boxW - padX;
        for (String[] row : rows) {
            g.text(font, row[0], boxX + padX, y, 0xFFA0A0A0, true);
            int vw = font.width(row[1]);
            g.text(font, row[1], valueRight - vw, y, 0xFFE0E0E0, true);
            y += lineH;
        }
    }
}
