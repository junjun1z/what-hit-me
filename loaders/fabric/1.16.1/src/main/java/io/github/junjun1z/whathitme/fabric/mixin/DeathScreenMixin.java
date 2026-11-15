package io.github.junjun1z.whathitme.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.junjun1z.whathitme.fabric.WhatHitMeFabricClient;
import net.minecraft.client.gui.screens.DeathScreen;

/** The death screen panel (no Fabric API screen events on this version): opened in init, drawn after the screen. */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {

    @Inject(method = "init", at = @At("HEAD"))
    private void whathitme$init(CallbackInfo ci) {
        WhatHitMeFabricClient.onDeathScreenInit((DeathScreen) (Object) this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void whathitme$render(PoseStack pose, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        WhatHitMeFabricClient.onDeathScreenRender((DeathScreen) (Object) this, pose, mouseX, mouseY);
    }
}
