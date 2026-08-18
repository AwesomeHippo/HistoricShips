package com.awesomehippo.historicships.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public final class ShipAnimalCargo {
    private static final float MAX_ANIMAL_WIDTH = 2.2F;

    private ShipAnimalCargo() {}

    public static boolean isCargoAnimal(Entity entity) {
        return entity instanceof Animal && entity.getBbWidth() <= MAX_ANIMAL_WIDTH && entity.isAlive() && !entity.isSpectator();
    }

    public static int countPlayers(Entity ship) {
        int n = 0;
        for (Entity e : ship.getPassengers()) {
            if (e instanceof Player) {
                n++;
            }
        }
        return n;
    }

    public static int countAnimals(Entity ship) {
        int n = 0;
        for (Entity e : ship.getPassengers()) {
            if (isCargoAnimal(e)) {
                n++;
            }
        }
        return n;
    }

    public static int playerIndex(Entity ship, Entity passenger) {
        int i = 0;
        for (Entity e : ship.getPassengers()) {
            if (e instanceof Player) {
                if (e == passenger) {
                    return i;
                }
                i++;
            }
        }
        return i;
    }

    public static int animalIndex(Entity ship, Entity passenger) {
        int i = 0;
        for (Entity e : ship.getPassengers()) {
            if (isCargoAnimal(e)) {
                if (e == passenger) {
                    return i;
                }
                i++;
            }
        }
        return i;
    }

    public static @Nullable LivingEntity firstPlayer(Entity ship) {
        for (Entity e : ship.getPassengers()) {
            if (e instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    public static Vec3 seatOffset(float yawDeg, float modelX, float modelZ, float seatY, float u) {
        // (forward is negated)
        float localForward = -(modelX * u);
        float localRight = modelZ * u;
        return new Vec3(localRight, seatY, localForward).yRot(-yawDeg * ((float) Math.PI / 180.0F));
    }

    public static boolean tryBoardLeashed(Player player, Entity ship, int maxAnimals) {
        if (player.level().isClientSide() || !(player.level() instanceof ServerLevel server)) {
            return false;
        }
        if (countAnimals(ship) >= maxAnimals) {
            return false;
        }
        List<Leashable> leashed = Leashable.leashableLeashedTo(player);
        if (leashed.isEmpty()) {
            return false;
        }
        boolean any = false;
        for (Leashable leashable : leashed) {
            if (!(leashable instanceof Entity entity) || !isCargoAnimal(entity)) {
                continue;
            }
            if (countAnimals(ship) >= maxAnimals) {
                break;
            }
            leashable.removeLeash();
            if (entity.startRiding(ship, true, true)) {
                any = true;
            } else if (leashable.canHaveALeashAttachedTo(player)) {
                leashable.setLeashedTo(player, true);
            }
        }
        if (any) {
            server.playSound(null, ship.getX(), ship.getY(), ship.getZ(), SoundEvents.LEAD_UNTIED, SoundSource.NEUTRAL, 0.7F, 1.0F);
        }
        return any;
    }

    public static boolean tryUnloadAnimals(Player player, Entity ship) {
        if (player.level().isClientSide() || !(player.level() instanceof ServerLevel server)) {
            return false;
        }
        List<Entity> animals = new ArrayList<>();
        for (Entity e : ship.getPassengers()) {
            if (isCargoAnimal(e)) {
                animals.add(e);
            }
        }
        if (animals.isEmpty()) {
            return false;
        }
        float yaw = ship.getYRot() * Mth.DEG_TO_RAD;
        double sideX = -Mth.cos(yaw);
        double sideZ = -Mth.sin(yaw);
        int i = 0;
        for (Entity animal : animals) {
            animal.stopRiding();
            double ox = sideX * (1.6 + (i % 2) * 0.7);
            double oz = sideZ * (1.6 + (i % 2) * 0.7);
            double along = ((i / 2) - 0.5) * 0.9;
            double bowX = Mth.sin(yaw);
            double bowZ = -Mth.cos(yaw);
            animal.setPos(ship.getX() + ox + bowX * along, ship.getY() + 0.35, ship.getZ() + oz + bowZ * along);
            if (animal instanceof Leashable leashable && leashable.canHaveALeashAttachedTo(player)) {
                leashable.setLeashedTo(player, true);
            }
            i++;
        }
        server.playSound(null, ship.getX(), ship.getY(), ship.getZ(), SoundEvents.LEAD_TIED, SoundSource.NEUTRAL, 0.7F, 1.0F);
        return true;
    }
}
