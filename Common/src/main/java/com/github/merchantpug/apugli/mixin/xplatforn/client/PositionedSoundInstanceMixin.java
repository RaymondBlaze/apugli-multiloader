package com.github.merchantpug.apugli.mixin.xplatforn.client;

import com.github.merchantpug.apugli.access.AbstractSoundInstanceAccess;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleSoundInstance.class)
public class PositionedSoundInstanceMixin {
    @Inject(method = "<init>(Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFLnet/minecraft/util/math/random/Random;ZILnet/minecraft/client/sound/SoundInstance$AttenuationType;DDD)V", at = @At(value = "TAIL"))
    private void captureSoundEvent(SoundEvent sound, SoundSource category, float volume, float pitch, RandomSource random, boolean repeat, int repeatDelay, SoundInstance.Attenuation attenuationType, double x, double y, double z, CallbackInfo ci) {
        ((AbstractSoundInstanceAccess)this).setSoundEvent(sound);
    }
}
