package com.awesomehippo.historicships.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public final class ShipProjectileHits {
    private ShipProjectileHits() {}

    public static @Nullable StoredShipEntity resolveShip(@Nullable Entity hit) {
        Entity cur = hit;
        while (cur != null) {
            if (cur instanceof StoredShipEntity ship) {
                return ship;
            }
            cur = cur.getVehicle();
        }
        return null;
    }

    public static boolean isFriendlyFire(@Nullable Entity owner, Entity hit) {
        if (owner == null) {
            return false;
        }
        if (hit == owner) {
            return true;
        }
        StoredShipEntity targetShip = resolveShip(hit);
        if (targetShip != null) {
            return owner.getRootVehicle() == targetShip;
        }
        return hit.isPassengerOfSameVehicle(owner);
    }

    public static boolean isFromSourceShip(@Nullable Entity sourceShip, Entity entity) {
        if (sourceShip == null) {
            return false;
        }
        if (entity == sourceShip) {
            return true;
        }
        StoredShipEntity hitShip = resolveShip(entity);
        if (hitShip != null && hitShip == sourceShip) {
            return true;
        }
        return entity.isPassengerOfSameVehicle(sourceShip) || sourceShip.isPassengerOfSameVehicle(entity);
    }

    public static @Nullable StoredShipEntity findOverlappingShip(Projectile projectile, @Nullable Entity sourceShip, double inflate) {
        if (!(projectile.level() instanceof ServerLevel server) || projectile.isRemoved()) {
            return null;
        }
        AABB search = projectile.getBoundingBox().inflate(inflate);
        StoredShipEntity best = null;
        double bestDist = Double.MAX_VALUE;
        Vec3 at = projectile.position();
        for (Entity entity : server.getEntities(projectile, search)) {
            StoredShipEntity ship = resolveShip(entity);
            if (ship == null) {
                continue;
            }
            if (isFromSourceShip(sourceShip, ship) || isFriendlyFire(projectile.getOwner(), ship)) {
                continue;
            }
            double dist = ship.getBoundingBox().distanceToSqr(at);
            if (dist < bestDist) {
                bestDist = dist;
                best = ship;
            }
        }
        return best;
    }

    public static void splashShips(ServerLevel server, Projectile projectile, @Nullable Entity sourceShip, Vec3 at, double radius, float baseDamage) {
        AABB area = new AABB(at.x - radius, at.y - radius, at.z - radius, at.x + radius, at.y + radius, at.z + radius);
        for (Entity entity : server.getEntities(projectile, area)) {
            if (!(entity instanceof StoredShipEntity ship)) {
                continue;
            }
            if (isFromSourceShip(sourceShip, ship) || isFriendlyFire(projectile.getOwner(), ship)) {
                continue;
            }
            double dist = Math.sqrt(ship.getBoundingBox().distanceToSqr(at));
            if (dist > radius) {
                continue;
            }
            float falloff = 1.0F - (float) (dist / radius);
            float dmg = baseDamage * (0.40F + 0.60F * falloff);
            ship.damageHull(server, dmg, projectile);
        }
    }
}
