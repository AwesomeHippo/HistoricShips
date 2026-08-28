package com.awesomehippo.historicships.client.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.util.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public final class SailPaintRenderType {
    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("historicships", "pipeline/sail_paint"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withSampler("Sampler1")
            .withCull(true)
            .build();

    private static final Function<Identifier, RenderType> BY_TEXTURE = Util.memoize(texture -> {
        RenderSetup state = RenderSetup.builder(PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup();
        return RenderType.create("historicships_sail_paint", state);
    });

    private SailPaintRenderType() {}

    public static RenderType of(Identifier texture) {
        return BY_TEXTURE.apply(texture);
    }
}
