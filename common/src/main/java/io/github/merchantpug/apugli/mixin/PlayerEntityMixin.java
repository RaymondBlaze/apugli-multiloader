package io.github.merchantpug.apugli.mixin;

import io.github.apace100.origins.component.OriginComponent;
import io.github.merchantpug.apugli.powers.ActionOnEquipPower;
import io.github.merchantpug.apugli.powers.AerialAffinityPower;
import io.github.merchantpug.apugli.powers.CustomDeathSoundPower;
import io.github.merchantpug.apugli.powers.CustomHurtSoundPower;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Redirect(method = {"Lnet/minecraft/entity/player/PlayerEntity;getDigSpeed(Lnet/minecraft/block/BlockState;)F","Lnet/minecraft/entity/player/PlayerEntity;getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F"}, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;onGround:Z", opcode = Opcodes.GETFIELD))
    private boolean hasAirAffinity(PlayerEntity instance) {
        return OriginComponent.hasPower(instance, AerialAffinityPower.class) || instance.isOnGround();
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void modifyHurtSound(CallbackInfoReturnable<SoundEvent> cir) {
        List<CustomHurtSoundPower> powers = OriginComponent.getPowers(this, CustomHurtSoundPower.class);
        if (powers.isEmpty()) return;
        if (powers.stream().anyMatch(CustomHurtSoundPower::isMuted)) cir.cancel();
        powers.forEach(power -> power.playHurtSound(this));
        cir.cancel();
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void modifyDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
        List<CustomDeathSoundPower> powers = OriginComponent.getPowers((LivingEntity)(Object)this, CustomDeathSoundPower.class);
        if (powers.isEmpty()) return;
        if (powers.stream().anyMatch(CustomDeathSoundPower::isMuted)) cir.cancel();
        powers.forEach(power -> power.playDeathSound((LivingEntity)(Object)this));
        cir.cancel();
    }

    @Inject(method = "equipStack", at = @At(value = "TAIL"))
    public void equipStack(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if (slot.getType() != EquipmentSlot.Type.ARMOR && !slot.equals(EquipmentSlot.OFFHAND)) return;

        OriginComponent.getPowers((PlayerEntity)(Object)this, ActionOnEquipPower.class).forEach(power -> power.fireAction(slot, stack));
    }
}