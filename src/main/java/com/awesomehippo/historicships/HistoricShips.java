package com.awesomehippo.historicships;

import com.awesomehippo.historicships.block.ShipwrightWorkbenchBlock;
import com.awesomehippo.historicships.blockentity.ShipwrightWorkbenchBlockEntity;
import com.awesomehippo.historicships.entity.CannonballEntity;
import com.awesomehippo.historicships.entity.DrakkarEntity;
import com.awesomehippo.historicships.entity.NapoleonShipEntity;
import com.awesomehippo.historicships.entity.QuinqueremeEntity;
import com.awesomehippo.historicships.entity.SailPaint;
import com.awesomehippo.historicships.entity.StoneBulletEntity;
import com.awesomehippo.historicships.entity.StoredShipEntity;
import com.awesomehippo.historicships.item.DrakkarItem;
import com.awesomehippo.historicships.item.NapoleonShipItem;
import com.awesomehippo.historicships.item.QuinqueremeItem;
import com.awesomehippo.historicships.menu.NapoleonShipMenu;
import com.awesomehippo.historicships.menu.ShipwrightMenu;
import com.awesomehippo.historicships.network.FireBowShellPacket;
import com.awesomehippo.historicships.network.FireTowerStonePacket;
import com.awesomehippo.historicships.network.RamHitPacket;
import com.awesomehippo.historicships.network.RamKnockPacket;
import com.awesomehippo.historicships.network.SailPaintPacket;
import com.awesomehippo.historicships.network.ToggleSailsPacket;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(HistoricShips.MODID)
public class HistoricShips {
    public static final String MODID = "historicships";

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SHIP_HULL = DATA_COMPONENTS.registerComponentType("hull", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SailPaint.Data>> SHIP_SAIL_PAINT = DATA_COMPONENTS.registerComponentType("sail_paint", builder -> builder.persistent(SailPaint.CODEC).networkSynchronized(SailPaint.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SailPaint.Data>> SHIP_FRONT_SAIL_PAINT = DATA_COMPONENTS.registerComponentType("front_sail_paint", builder -> builder.persistent(SailPaint.CODEC).networkSynchronized(SailPaint.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SHIP_SAIL_STRIPE = DATA_COMPONENTS.registerComponentType("sail_stripe", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<EntityType<?>, EntityType<NapoleonShipEntity>> NAPOLEON_SHIP_ENTITY = ENTITIES.registerEntityType("napoleon_ship", NapoleonShipEntity::new, MobCategory.MISC, builder -> builder.sized(24.0F, 22.0F).clientTrackingRange(16).updateInterval(3).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<DrakkarEntity>> DRAKKAR_ENTITY = ENTITIES.registerEntityType("drakkar", DrakkarEntity::new, MobCategory.MISC, builder -> builder.sized(14.0F, 10.0F).clientTrackingRange(10).updateInterval(3).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<QuinqueremeEntity>> QUINQUEREME_ENTITY = ENTITIES.registerEntityType("quinquereme", QuinqueremeEntity::new, MobCategory.MISC, builder -> builder.sized(18.0F, 8.0F).clientTrackingRange(12).updateInterval(3).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<CannonballEntity>> CANNONBALL_ENTITY = ENTITIES.registerEntityType("cannonball", CannonballEntity::new, MobCategory.MISC, builder -> builder.sized(0.95F, 0.95F).clientTrackingRange(8).updateInterval(1).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<StoneBulletEntity>> STONE_BULLET_ENTITY = ENTITIES.registerEntityType("stone_bullet", StoneBulletEntity::new, MobCategory.MISC, builder -> builder.sized(0.72F, 0.72F).clientTrackingRange(6).updateInterval(1).fireImmune());

    public static final DeferredBlock<ShipwrightWorkbenchBlock> SHIPWRIGHT_WORKBENCH = BLOCKS.registerBlock("shipwright_workbench", ShipwrightWorkbenchBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).ignitedByLava().noOcclusion());
    public static final DeferredItem<BlockItem> SHIPWRIGHT_WORKBENCH_ITEM = ITEMS.registerSimpleBlockItem(SHIPWRIGHT_WORKBENCH);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShipwrightWorkbenchBlockEntity>> SHIPWRIGHT_WORKBENCH_BE = BLOCK_ENTITIES.register("shipwright_workbench", () -> new BlockEntityType<>(ShipwrightWorkbenchBlockEntity::new, SHIPWRIGHT_WORKBENCH.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<ShipwrightMenu>> SHIPWRIGHT_MENU = MENUS.register("shipwright", () -> IMenuTypeExtension.create((id, inv, buf) -> new ShipwrightMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<NapoleonShipMenu>> NAPOLEON_MENU = MENUS.register("napoleon_ship", () -> IMenuTypeExtension.create((id, inv, buf) -> {
        if (buf.readableBytes() > 0) {
            Entity entity = inv.player.level().getEntity(buf.readVarInt());
            int tab = buf.readableBytes() > 0 ? buf.readVarInt() : NapoleonShipMenu.TAB_CARGO;
            if (entity instanceof NapoleonShipEntity ship) {
                return new NapoleonShipMenu(id, inv, ship, tab);
            }
        }
        return new NapoleonShipMenu(id, inv);
    }));

    public static final DeferredItem<NapoleonShipItem> NAPOLEON_SHIP_ITEM = ITEMS.registerItem("napoleon_ship", NapoleonShipItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<DrakkarItem> DRAKKAR_ITEM = ITEMS.registerItem("drakkar", DrakkarItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<QuinqueremeItem> QUINQUEREME_ITEM = ITEMS.registerItem("quinquereme", QuinqueremeItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<Item> SAIL_BRUSH = ITEMS.registerItem("sail_brush", Item::new, props -> props.stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.historicships")).icon(HistoricShips::tabIcon).displayItems((params, output) -> {
        output.accept(SHIPWRIGHT_WORKBENCH_ITEM.get());
        if (ShipsConfig.DRAKKAR.get()) {
            output.accept(DRAKKAR_ITEM.get());
        }
        if (ShipsConfig.QUINQUEREME.get()) {
            output.accept(QUINQUEREME_ITEM.get());
            output.accept(SAIL_BRUSH.get());
        }
        if (ShipsConfig.NAPOLEON_SHIP.get()) {
            output.accept(NAPOLEON_SHIP_ITEM.get());
        }
    }).build());

    public HistoricShips(IEventBus modBus, ModContainer container) {
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        CREATIVE_TABS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        modBus.addListener(this::registerPayloads);
        ShipsConfig.register(container);
        NeoForge.EVENT_BUS.addListener(this::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onStartTracking);
    }

    private void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof QuinqueremeEntity ship && event.getEntity() instanceof ServerPlayer player) {
            if (ship.getSailPaint() != null) {
                PacketDistributor.sendToPlayer(player, new SailPaintPacket(ship.getId(), SailPaintPacket.MAIN, ship.getSailPaint()));
            }
            if (ship.getFrontSailPaint() != null) {
                PacketDistributor.sendToPlayer(player, new SailPaintPacket(ship.getId(), SailPaintPacket.FRONT, ship.getFrontSailPaint()));
            }
        }
    }

    private void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (repairAsPassenger(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (repairAsPassenger(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean repairAsPassenger(Player player, InteractionHand hand) {
        return player.getVehicle() instanceof StoredShipEntity ship && ship.tryRepair(player, hand);
    }

    private static ItemStack tabIcon() {
        if (ShipsConfig.NAPOLEON_SHIP.get()) {
            return new ItemStack(NAPOLEON_SHIP_ITEM.get());
        }
        if (ShipsConfig.QUINQUEREME.get()) {
            return new ItemStack(QUINQUEREME_ITEM.get());
        }
        if (ShipsConfig.DRAKKAR.get()) {
            return new ItemStack(DRAKKAR_ITEM.get());
        }
        return new ItemStack(SHIPWRIGHT_WORKBENCH_ITEM.get());
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(FireBowShellPacket.TYPE, FireBowShellPacket.STREAM_CODEC, FireBowShellPacket::handle);
        registrar.playToServer(FireTowerStonePacket.TYPE, FireTowerStonePacket.STREAM_CODEC, FireTowerStonePacket::handle);
        registrar.playToServer(ToggleSailsPacket.TYPE, ToggleSailsPacket.STREAM_CODEC, ToggleSailsPacket::handle);
        registrar.playToServer(RamHitPacket.TYPE, RamHitPacket.STREAM_CODEC, RamHitPacket::handle);
        registrar.playToClient(RamKnockPacket.TYPE, RamKnockPacket.STREAM_CODEC, RamKnockPacket::handle);
        registrar.playBidirectional(SailPaintPacket.TYPE, SailPaintPacket.STREAM_CODEC, SailPaintPacket::handleServer, SailPaintPacket::handleClient);
    }
}
