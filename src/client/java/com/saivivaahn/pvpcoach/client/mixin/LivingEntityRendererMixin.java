package com.saivivaahn.pvpcoach.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a bright emissive red fill to entities that PvP Coach marks as the current target. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Shadow protected EntityModel<?> model;

    @Shadow protected abstract ResourceLocation getTextureLocation(LivingEntityRenderState state);

    @Inject(
            method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V")
    )
    private void pvpcoach$renderRedTarget(
            LivingEntityRenderState state,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        // PvPCoachClient sets glowing only on the entity currently under the crosshair.
        if (!state.appearsGlowing || state.isInvisibleToPlayer) return;

        ResourceLocation texture = getTextureLocation(state);
        VertexConsumer redBuffer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(texture));
        model.renderToBuffer(
                poseStack,
                redBuffer,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                0xFFFF2020
        );
    }
}
