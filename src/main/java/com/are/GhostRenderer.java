package com.are;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class GhostRenderer<T extends Mob> extends MobRenderer<T, HollowModel<T>> {
    private final ResourceLocation texture;

    public GhostRenderer(EntityRendererProvider.Context ctx, String name) {
        super(ctx, new HollowModel<T>(ctx.bakeLayer(HollowModel.LAYER)), 0.0F);
        this.texture = Are.id("textures/entity/" + name + ".png");
        this.addLayer(new GlowLayer<T, HollowModel<T>>(this, RenderType.eyes(Are.id("textures/entity/" + name + "_eyes.png"))));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
