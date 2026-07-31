package com.awesomehippo.historicships;

import com.awesomehippo.historicships.block.ShipwrightWorkbenchBlock;
import com.awesomehippo.historicships.blockentity.ShipwrightWorkbenchBlockEntity;
import com.awesomehippo.historicships.entity.CannonballEntity;
import com.awesomehippo.historicships.entity.DrakkarEntity;
import com.awesomehippo.historicships.entity.NapoleonShipEntity;
import com.awesomehippo.historicships.entity.QuinqueremeEntity;
import com.awesomehippo.historicships.item.DrakkarItem;
import com.awesomehippo.historicships.item.NapoleonShipItem;
import com.awesomehippo.historicships.item.QuinqueremeItem;
import com.awesomehippo.historicships.menu.ShipwrightMenu;
import com.awesomehippo.historicships.network.FireBowShellPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(NapoleonShipMod.MODID)
public class NapoleonShipMod {
    public static final String MODID = "historicships";

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<NapoleonShipEntity>> NAPOLEON_SHIP_ENTITY = ENTITIES.registerEntityType("napoleon_ship", NapoleonShipEntity::new, MobCategory.MISC, builder -> builder.sized(24.0F, 22.0F).clientTrackingRange(64).updateInterval(3).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<DrakkarEntity>> DRAKKAR_ENTITY = ENTITIES.registerEntityType("drakkar", DrakkarEntity::new, MobCategory.MISC, builder -> builder.sized(14.0F, 10.0F).clientTrackingRange(48).updateInterval(3).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<QuinqueremeEntity>> QUINQUEREME_ENTITY = ENTITIES.registerEntityType("quinquereme", QuinqueremeEntity::new, MobCategory.MISC, builder -> builder.sized(18.0F, 8.0F).clientTrackingRange(64).updateInterval(3).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<CannonballEntity>> CANNONBALL_ENTITY = ENTITIES.registerEntityType("cannonball", CannonballEntity::new, MobCategory.MISC, builder -> builder.sized(0.95F, 0.95F).clientTrackingRange(20).updateInterval(1).fireImmune());

    public static final DeferredBlock<ShipwrightWorkbenchBlock> SHIPWRIGHT_WORKBENCH = BLOCKS.registerBlock("shipwright_workbench", ShipwrightWorkbenchBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).ignitedByLava().noOcclusion());
    public static final DeferredItem<BlockItem> SHIPWRIGHT_WORKBENCH_ITEM = ITEMS.registerSimpleBlockItem(SHIPWRIGHT_WORKBENCH);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShipwrightWorkbenchBlockEntity>> SHIPWRIGHT_WORKBENCH_BE = BLOCK_ENTITIES.register("shipwright_workbench", () -> new BlockEntityType<>(ShipwrightWorkbenchBlockEntity::new, SHIPWRIGHT_WORKBENCH.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<ShipwrightMenu>> SHIPWRIGHT_MENU = MENUS.register("shipwright", () -> IMenuTypeExtension.create((id, inv, buf) -> new ShipwrightMenu(id, inv)));

    public static final DeferredItem<NapoleonShipItem> NAPOLEON_SHIP_ITEM = ITEMS.registerItem("napoleon_ship", NapoleonShipItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<DrakkarItem> DRAKKAR_ITEM = ITEMS.registerItem("drakkar", DrakkarItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<QuinqueremeItem> QUINQUEREME_ITEM = ITEMS.registerItem("quinquereme", QuinqueremeItem::new, props -> props.stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.historicships")).icon(() -> new ItemStack(NAPOLEON_SHIP_ITEM.get())).displayItems((params, output) -> {
        output.accept(SHIPWRIGHT_WORKBENCH_ITEM.get());
        output.accept(DRAKKAR_ITEM.get());
        output.accept(QUINQUEREME_ITEM.get());
        output.accept(NAPOLEON_SHIP_ITEM.get());
    }).build());

    public NapoleonShipMod(IEventBus modBus) {
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        CREATIVE_TABS.register(modBus);
        modBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(FireBowShellPacket.TYPE, FireBowShellPacket.STREAM_CODEC, FireBowShellPacket::handle);
    }
}
