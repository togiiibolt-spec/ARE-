package com.are;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.world.entity.Entity;

/** Eyes that glow in the dark (rendered full-bright). */
public class GlowLayer<T extends Entity, M extends EntityModel<T>> extends EyesLayer<T, M> {
    private final RenderType type;

    public GlowLayer(RenderLayerParent<T, M> parent, RenderType type) {
        super(parent);
        this.type = type;
    }

    @Override
    public RenderType renderType() {
        return type;
    }
}
