package com.are;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Tall, thin, wrong. Inverted-cone head covered in eyes, spine spikes, arms that reach the ground.
 * Used by both the Watcher (black) and the Hollow (pale).
 */
public class HollowModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Are.id("hollow"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rArm;
    private final ModelPart rFore;
    private final ModelPart lArm;
    private final ModelPart lFore;
    private final ModelPart rLeg;
    private final ModelPart lLeg;

    public HollowModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rArm = root.getChild("right_arm");
        this.rFore = rArm.getChild("right_fore");
        this.lArm = root.getChild("left_arm");
        this.lFore = lArm.getChild("left_fore");
        this.rLeg = root.getChild("right_leg");
        this.lLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        // feet are at y = 24, the top of the head is at y = -20 (about 2.75 blocks tall)
        r.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7F, -14F, -2.5F, 14F, 8F, 5F)
                        .texOffs(40, 0).addBox(-5F, -6F, -2F, 10F, 3F, 4F)
                        .texOffs(70, 0).addBox(-3F, -3F, -1.5F, 6F, 3F, 3F),
                PartPose.offset(0F, -6F, 0F));

        r.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-3F, 0F, -1.5F, 6F, 14F, 3F)
                        .texOffs(20, 16).addBox(-1F, -2F, -1F, 2F, 2F, 2F)
                        .texOffs(30, 16).addBox(-0.5F, 1F, 1.5F, 1F, 2F, 2F)
                        .texOffs(30, 16).addBox(-0.5F, 5F, 1.5F, 1F, 2F, 2F)
                        .texOffs(30, 16).addBox(-0.5F, 9F, 1.5F, 1F, 2F, 2F),
                PartPose.offset(0F, -4F, 0F));

        PartDefinition ra = r.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 36).addBox(-1F, 0F, -1F, 2F, 10F, 2F),
                PartPose.offset(-4F, -2F, 0F));
        ra.addOrReplaceChild("right_fore", forearm(), PartPose.offset(0F, 10F, 0F));

        PartDefinition la = r.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 36).addBox(-1F, 0F, -1F, 2F, 10F, 2F),
                PartPose.offset(4F, -2F, 0F));
        la.addOrReplaceChild("left_fore", forearm(), PartPose.offset(0F, 10F, 0F));

        r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 36).addBox(-1F, 0F, -1F, 2F, 14F, 2F),
                PartPose.offset(-2.5F, 10F, 0F));
        r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 36).addBox(-1F, 0F, -1F, 2F, 14F, 2F),
                PartPose.offset(2.5F, 10F, 0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static CubeListBuilder forearm() {
        return CubeListBuilder.create()
                .texOffs(10, 36).addBox(-1F, 0F, -1F, 2F, 11F, 2F)
                .texOffs(20, 36).addBox(-1.5F, 11F, -1.5F, 3F, 2F, 3F)
                .texOffs(34, 36).addBox(-1.5F, 13F, -1.5F, 1F, 4F, 1F)
                .texOffs(34, 36).addBox(-0.5F, 13F, -0.5F, 1F, 4F, 1F)
                .texOffs(34, 36).addBox(0.5F, 13F, 0.5F, 1F, 4F, 1F);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float t = ageInTicks;

        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        head.zRot = 0.25F + Mth.sin(t * 0.21F) * 0.04F;           // permanent unsettling head tilt
        if (((int) t) % 47 < 3) {                                   // glitchy twitch
            head.zRot += (((int) t) % 2 == 0) ? 0.45F : -0.45F;
        }
        body.zRot = Mth.sin(t * 0.05F) * 0.04F;

        float swing = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
        rLeg.xRot = swing * 1.1F;
        lLeg.xRot = -swing * 1.1F;

        boolean aggro = entity instanceof Mob m && m.isAggressive();
        float sway = Mth.sin(t * 0.07F) * 0.05F;
        if (aggro) {
            rArm.xRot = -1.35F + sway;
            lArm.xRot = -1.35F - sway;
            rFore.xRot = -0.2F;
            lFore.xRot = -0.2F;
            rArm.zRot = 0.08F;
            lArm.zRot = -0.08F;
        } else {
            rArm.xRot = sway - swing * 0.5F;
            lArm.xRot = -sway + swing * 0.5F;
            rFore.xRot = -0.15F + sway;
            lFore.xRot = -0.15F - sway;
            rArm.zRot = 0.12F + sway;
            lArm.zRot = -0.12F - sway;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
