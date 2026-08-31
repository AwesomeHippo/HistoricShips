package com.awesomehippo.historicships.entity;

import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public final class OarShipStats {
    public final float halfBeam;
    public final float halfLoa;
    public final float hullHeight;
    public final float cullHalfLoa;
    public final float cullHalfBeam;
    public final float cullHeight;
    public final int maxPassengers;
    public final float[][] seatXz;
    public final float seatYPad;
    public final float modelDeckY;
    public final float u;
    public final float cruise;
    public final float hardRow;
    public final float thrustBlend;
    public final float thrustBlendHard;
    public final float turnRate;
    public final float steerSmooth;
    public final double rowSpeedDiv;
    public final float rowEaseUp;
    public final float rowStepBase;
    public final float rowStepHard;
    public final double sailSpeedDiv;
    public final float sailHardBonus;
    public final double swellRate;
    public final double swellAmp;
    public final double buoyLift;
    public final double waterDrag;
    public final double turnHorizDiv;
    public final double turnHorizCap;
    public final float reverseMaxMul;
    public final float hardBlendMul;
    public final float softBlendMul;
    public final double coastAlign;
    public final Supplier<ItemStack> dropStack;

    public OarShipStats(float modelScale, float halfBeamModel, float halfBeamPad, float halfLoaModel, float hullHeightModel, float hullPad, float cullHalfLoaModel, float cullHalfBeamModel, float cullHeightModel, int maxPassengers, float[][] seatXz, float seatYPad, float modelDeckY, float cruise, float hardRow, float thrustBlend, float thrustBlendHard, float turnRate, float steerSmooth, double rowSpeedDiv, float rowEaseUp, float rowStepBase, float rowStepHard, double sailSpeedDiv, float sailHardBonus, double swellRate, double swellAmp, double buoyLift, double waterDrag, double turnHorizDiv, double turnHorizCap, float reverseMaxMul, float hardBlendMul, float softBlendMul, double coastAlign, Supplier<ItemStack> dropStack) {
        float u = modelScale / 16.0F;
        this.u = u;
        this.halfBeam = halfBeamModel * u + halfBeamPad;
        this.halfLoa = halfLoaModel * u;
        this.hullHeight = hullHeightModel * u + hullPad;
        this.cullHalfLoa = cullHalfLoaModel * u;
        this.cullHalfBeam = cullHalfBeamModel * u;
        this.cullHeight = cullHeightModel * u;
        this.maxPassengers = maxPassengers;
        this.seatXz = seatXz;
        this.seatYPad = seatYPad;
        this.modelDeckY = modelDeckY;
        this.cruise = cruise;
        this.hardRow = hardRow;
        this.thrustBlend = thrustBlend;
        this.thrustBlendHard = thrustBlendHard;
        this.turnRate = turnRate;
        this.steerSmooth = steerSmooth;
        this.rowSpeedDiv = rowSpeedDiv;
        this.rowEaseUp = rowEaseUp;
        this.rowStepBase = rowStepBase;
        this.rowStepHard = rowStepHard;
        this.sailSpeedDiv = sailSpeedDiv;
        this.sailHardBonus = sailHardBonus;
        this.swellRate = swellRate;
        this.swellAmp = swellAmp;
        this.buoyLift = buoyLift;
        this.waterDrag = waterDrag;
        this.turnHorizDiv = turnHorizDiv;
        this.turnHorizCap = turnHorizCap;
        this.reverseMaxMul = reverseMaxMul;
        this.hardBlendMul = hardBlendMul;
        this.softBlendMul = softBlendMul;
        this.coastAlign = coastAlign;
        this.dropStack = dropStack;
    }
}
