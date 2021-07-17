package com.tom.cpm.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import com.mojang.blaze3d.matrix.MatrixStack;

import com.tom.cpm.client.ClientProxy;
import com.tom.cpm.shared.config.Player;
import com.tom.cpm.shared.model.TextureSheetType;

@Mixin(value = PlayerRenderer.class, priority = 900)
public abstract class PlayerRendererMixin extends LivingRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> {

	public PlayerRendererMixin(EntityRendererManager rendererManager,
			PlayerModel<AbstractClientPlayerEntity> entityModelIn, float shadowSizeIn) {
		super(rendererManager, entityModelIn, shadowSizeIn);
	}

	@Inject(
			at = @At("RETURN"),
			method = {
					"getEntityTexture(Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;)Lnet/minecraft/util/ResourceLocation;"
			},
			cancellable = true)
	public void onGetEntityTexture(AbstractClientPlayerEntity entity, CallbackInfoReturnable<ResourceLocation> cbi) {
		ClientProxy.mc.getPlayerRenderManager().bindSkin(getEntityModel(), cbi, TextureSheetType.SKIN);
	}

	@Redirect(at =
			@At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;"
							+ "getLocationSkin()Lnet/minecraft/util/ResourceLocation;"
					),
			method = "renderItem("
					+ "Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;"
					+ "ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;"
					+ "Lnet/minecraft/client/renderer/model/ModelRenderer;Lnet/minecraft/client/renderer/model/ModelRenderer;)V"
			)
	public ResourceLocation getSkinTex(AbstractClientPlayerEntity player) {
		return getEntityTexture(player);
	}

	@Inject(at = @At("HEAD"), method = "renderRightArm(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;)V")
	public void onRenderRightArmPre(MatrixStack matrices, IRenderTypeBuffer vertexConsumers, int light, AbstractClientPlayerEntity player, CallbackInfo cbi) {
		ClientProxy.INSTANCE.renderHand(vertexConsumers);
	}

	@Inject(at = @At("HEAD"), method = "renderLeftArm(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;)V")
	public void onRenderLeftArmPre(MatrixStack matrices, IRenderTypeBuffer vertexConsumers, int light, AbstractClientPlayerEntity player, CallbackInfo cbi) {
		ClientProxy.INSTANCE.renderHand(vertexConsumers);
	}

	@Inject(at = @At("RETURN"), method = "renderRightArm(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;)V")
	public void onRenderRightArmPost(MatrixStack matrices, IRenderTypeBuffer vertexConsumers, int light, AbstractClientPlayerEntity player, CallbackInfo cbi) {
		ClientProxy.INSTANCE.unbind(getEntityModel());
	}

	@Inject(at = @At("RETURN"), method = "renderLeftArm(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;)V")
	public void onRenderLeftArmPost(MatrixStack matrices, IRenderTypeBuffer vertexConsumers, int light, AbstractClientPlayerEntity player, CallbackInfo cbi) {
		ClientProxy.INSTANCE.unbind(getEntityModel());
	}

	@Inject(at = @At("HEAD"), method = "renderName(Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;Lnet/minecraft/util/text/ITextComponent;Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V", cancellable = true)
	public void onRenderName(AbstractClientPlayerEntity entityIn, ITextComponent displayNameIn, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, CallbackInfo cbi) {
		if(!Player.isEnableNames())cbi.cancel();
	}

	@Redirect(at =
			@At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/RenderType;getEntitySolid("
							+ "Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
					),
			method = "renderItem("
					+ "Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;"
					+ "ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;"
					+ "Lnet/minecraft/client/renderer/model/ModelRenderer;Lnet/minecraft/client/renderer/model/ModelRenderer;)V",
					require = 0
			)
	public RenderType getArmLayer(ResourceLocation loc, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, AbstractClientPlayerEntity playerIn, ModelRenderer rendererArmIn, ModelRenderer rendererArmwearIn) {
		return ClientProxy.mc.getPlayerRenderManager().isBound(getEntityModel()) ? RenderType.getEntityTranslucent(getEntityTexture(playerIn)) : RenderType.getEntitySolid(getEntityTexture(playerIn));
	}
}
