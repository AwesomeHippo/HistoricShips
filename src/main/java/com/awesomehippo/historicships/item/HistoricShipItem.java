package com.awesomehippo.historicships.item;

import com.awesomehippo.historicships.NapoleonShipMod;
import com.awesomehippo.historicships.entity.StoredShipEntity;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public abstract class HistoricShipItem extends Item {
    public HistoricShipItem(Properties properties) {
        super(properties);
    }

    protected abstract Entity createShip(Level level, double x, double y, double z);

    public abstract int getMaxHull();

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }

        Vec3 look = player.getViewVector(1.0F);
        List<Entity> entities = level.getEntities(player, player.getBoundingBox().expandTowards(look.scale(5.0D)).inflate(1.0D), EntitySelector.NO_SPECTATORS.and(Entity::isPickable));
        if (!entities.isEmpty()) {
            Vec3 eye = player.getEyePosition();
            for (Entity entity : entities) {
                AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (box.contains(eye)) {
                    return InteractionResult.PASS;
                }
            }
        }

        if (hit.getType() == HitResult.Type.BLOCK) {
            Entity boat = createShip(level, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
            boat.setYRot(player.getYRot());
            if (!level.noCollision(boat, boat.getBoundingBox())) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                if (boat instanceof StoredShipEntity ship) {
                    ship.setOwner(player);
                    Integer hull = stack.get(NapoleonShipMod.SHIP_HULL.get());
                    if (hull != null) {
                        ship.setHull(hull);
                    }
                }
                level.addFreshEntity(boat);
                level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
                stack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
