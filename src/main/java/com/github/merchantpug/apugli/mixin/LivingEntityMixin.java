package com.github.merchantpug.apugli.mixin;

import com.github.merchantpug.apugli.access.ItemStackAccess;
import com.github.merchantpug.apugli.access.LivingEntityAccess;
import com.github.merchantpug.apugli.power.*;
import com.github.merchantpug.apugli.util.HitsOnTargetUtil;
import io.github.apace100.apoli.component.PowerHolderComponent;
import com.github.merchantpug.apugli.util.ItemStackFoodComponentUtil;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements LivingEntityAccess {

    @Shadow public abstract boolean isFallFlying();

    @Shadow protected abstract boolean isOnSoulSpeedBlock();

    @Shadow public abstract boolean isDead();

    @Shadow protected abstract boolean tryUseTotem(DamageSource source);

    @Shadow protected abstract void initDataTracker();

    @Shadow public abstract SoundEvent getEatSound(ItemStack stack);

    @Shadow public abstract boolean isAlive();

    public LivingEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique private final HashMap<Entity, Integer> hitsHashmap = new HashMap<>();
    @Unique private int hitsResetCounter;

    @Redirect(method = "handleStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V", ordinal = 1))
    private void playHurtSoundsStatus(LivingEntity instance, SoundEvent soundEvent, float v, float p) {
        List<CustomHurtSoundPower> powers = PowerHolderComponent.getPowers(instance, CustomHurtSoundPower.class);
        if (!powers.isEmpty()) {
            if (powers.stream().anyMatch(CustomHurtSoundPower::isMuted)) return;
            powers.forEach(power -> power.playHurtSound(instance));
            return;
        }
        instance.playSound(soundEvent, v, p);
    }

    @Redirect(method = "handleStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V", ordinal = 2))
    private void playDeathSoundsStatus(LivingEntity instance, SoundEvent soundEvent, float v, float p) {
        List<CustomDeathSoundPower> powers = PowerHolderComponent.getPowers(instance, CustomDeathSoundPower.class);
        if (!powers.isEmpty()) {
            if (powers.stream().anyMatch(CustomDeathSoundPower::isMuted)) return;
            powers.forEach(power -> power.playDeathSound(instance));
            return;
        }
        instance.playSound(soundEvent, v, p);
    }

    @Redirect(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"))
    private void playDeathSounds(LivingEntity instance, SoundEvent soundEvent, float v, float p) {
        List<CustomDeathSoundPower> powers = PowerHolderComponent.getPowers(instance, CustomDeathSoundPower.class);
        if (!powers.isEmpty()) {
            if (powers.stream().anyMatch(CustomDeathSoundPower::isMuted)) return;
            powers.forEach(power -> power.playDeathSound(instance));
            return;
        }
        instance.playSound(soundEvent, v, p);
    }

    @Inject(method = "playHurtSound", at = @At("HEAD"), cancellable = true)
    private void playHurtSounds(DamageSource source, CallbackInfo ci) {
        List<CustomHurtSoundPower> powers = PowerHolderComponent.getPowers(this, CustomHurtSoundPower.class);
        if (powers.isEmpty()) return;
        if (powers.stream().anyMatch(CustomHurtSoundPower::isMuted)) ci.cancel();
        powers.forEach(power -> power.playHurtSound(this));
        ci.cancel();
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void runDamageFunctions(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || source.getAttacker() == null) return;
        if ((LivingEntity)(Object)this instanceof TameableEntity tameable) {
            PowerHolderComponent.withPowers(tameable.getOwner(), ActionWhenTameHitPower.class, p -> true, p -> p.whenHit(source.getAttacker(), source, amount));
        }
        if (source.getAttacker() instanceof TameableEntity tameable) {
            PowerHolderComponent.withPowers(tameable.getOwner(), ActionOnTameHitPower.class, p -> true, p -> p.onHit(this, source, amount));
        }
        if (this.isDead() && !this.tryUseTotem(source)) {
            hitsHashmap.clear();
            HitsOnTargetUtil.sendPacket((LivingEntity)(Object)this, null, HitsOnTargetUtil.PacketType.CLEAR, 0);
            return;
        }
        addToHits(source.getAttacker(), 1);
        HitsOnTargetUtil.sendPacket((LivingEntity)(Object)this, (LivingEntity)source.getAttacker(), HitsOnTargetUtil.PacketType.ADD, getHits().get(source.getAttacker()) + 1);
        hitsResetCounter = 0;
    }

    @Unique private boolean hasModifiedDamage;

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float modifyDamageBasedOnEnchantment(float originalValue, DamageSource source, float amount) {
        float[] additionalValue = {0.0F};
        LivingEntity thisAsLiving = (LivingEntity)(Object)this;

        if (source.getAttacker() != null && source.getAttacker() instanceof LivingEntity && !source.isProjectile()) {
            List<ModifyEnchantmentDamageDealtPower> damageDealtPowers = PowerHolderComponent.getPowers(source.getAttacker(), ModifyEnchantmentDamageDealtPower.class).stream().filter(p -> p.doesApply(source, amount, thisAsLiving)).collect(Collectors.toList());

            damageDealtPowers.forEach(power -> additionalValue[0] += power.baseValue);

            for (ModifyEnchantmentDamageDealtPower power : damageDealtPowers) {
                for (int i = 0; i < EnchantmentHelper.getLevel(power.enchantment, ((LivingEntity)source.getAttacker()).getEquippedStack(EquipmentSlot.MAINHAND)); i++) {
                    additionalValue[0] = PowerHolderComponent.modify(source.getAttacker(), ModifyEnchantmentDamageDealtPower.class,
                            additionalValue[0], enchantmentDamageTakenPower -> true, p -> p.executeActions(thisAsLiving));
                }
            }
        }

        List<ModifyEnchantmentDamageTakenPower> damageTakenPowers = PowerHolderComponent.getPowers(this, ModifyEnchantmentDamageTakenPower.class).stream().filter(p -> p.doesApply(source, amount)).collect(Collectors.toList());

        damageTakenPowers.forEach(power -> additionalValue[0] += power.baseValue);

        if (source.getAttacker() != null && source.getAttacker() instanceof LivingEntity) {
            for (ModifyEnchantmentDamageTakenPower power : damageTakenPowers) {
                for (int i = 0; i < EnchantmentHelper.getLevel(power.enchantment, ((LivingEntity)source.getAttacker()).getEquippedStack(EquipmentSlot.MAINHAND)); i++) {
                    additionalValue[0] = PowerHolderComponent.modify(this, ModifyEnchantmentDamageTakenPower.class,
                            additionalValue[0], enchantmentDamageTakenPower -> true, p -> p.executeActions(source.getAttacker()));
                }
            }
        }

        hasModifiedDamage = originalValue +  additionalValue[0] != originalValue;

        return originalValue + additionalValue[0];
    }

    @Inject(method = "eatFood", at = @At("HEAD"), cancellable = true)
    private void eatFood(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (((ItemStackAccess)(Object)stack).isItemStackFood()) {
            world.playSound(null, this.getX(), this.getY(), this.getZ(), this.getEatSound(stack), SoundCategory.NEUTRAL, 1.0f, 1.0f + (world.random.nextFloat() - world.random.nextFloat()) * 0.4f);
            ItemStackFoodComponentUtil.applyFoodEffects(stack, world, (LivingEntity)(Object)this);
            if (!((LivingEntity)(Object)this instanceof PlayerEntity) || !((PlayerEntity)(Object)this).getAbilities().creativeMode) {
                stack.decrement(1);
            }
            this.emitGameEvent(GameEvent.EAT);

            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSleeping()Z"), cancellable = true)
    private void preventHitIfDamageIsZero(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(hasModifiedDamage && amount == 0.0F) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldDisplaySoulSpeedEffects", at = @At("HEAD"), cancellable = true)
    private void shouldDisplaySoulSpeedEffects(CallbackInfoReturnable<Boolean> cir) {
        if (PowerHolderComponent.hasPower(this, ModifySoulSpeedPower.class)) {
            int soulSpeedValue = (int)PowerHolderComponent.modify(this, ModifySoulSpeedPower.class, EnchantmentHelper.getEquipmentLevel(Enchantments.SOUL_SPEED, (LivingEntity)(Object)this));
            cir.setReturnValue(this.age % 5 == 0 && this.getVelocity().x != 0.0D && this.getVelocity().z != 0.0D && !this.isSpectator() && soulSpeedValue > 0 && ((LivingEntityAccessor)this).invokeIsOnSoulSpeedBlock());
        }
    }

    @Inject(method = "isOnSoulSpeedBlock", at = @At("HEAD"), cancellable = true)
    private void isOnSoulSpeedBlock(CallbackInfoReturnable<Boolean> cir) {
        PowerHolderComponent.getPowers(this, ModifySoulSpeedPower.class).forEach(power -> {
            if (power.blockCondition != null) {
                cir.setReturnValue(power.blockCondition.test(new CachedBlockPosition(this.world, this.getVelocityAffectingPos(), true)));
            }
        });
    }

    @ModifyVariable(method = "addSoulSpeedBoostIfNeeded", at = @At("STORE"), ordinal = 0)
    private int replaceLevelOfSoulSpeed(int i) {
        return i = (int)PowerHolderComponent.modify(this, ModifySoulSpeedPower.class, i);
    }

    @Inject(method = "getVelocityMultiplier", at = @At("HEAD"), cancellable = true)
    private void getVelocityMultiplier(CallbackInfoReturnable<Float> cir) {
        if (PowerHolderComponent.hasPower(this, ModifySoulSpeedPower.class)) {
            int soulSpeedValue = (int)PowerHolderComponent.modify(this, ModifySoulSpeedPower.class, EnchantmentHelper.getEquipmentLevel(Enchantments.SOUL_SPEED, (LivingEntity)(Object)this));
            if (soulSpeedValue <= 0 || !this.isOnSoulSpeedBlock()) {
                cir.setReturnValue(super.getVelocityMultiplier());
            } else {
                cir.setReturnValue(1.0F);
            }
        }
    }

    @Inject(method = "addSoulSpeedBoostIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;damage(ILnet/minecraft/entity/LivingEntity;Ljava/util/function/Consumer;)V"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void itemStack(CallbackInfo ci, int i) {
        int baseValue = (int)PowerHolderComponent.modify(this, ModifySoulSpeedPower.class, 0);
        if (PowerHolderComponent.hasPower(this, ModifySoulSpeedPower.class) && i == baseValue) {
            ci.cancel();
        }
    }

    @Inject(method = "isUndead", at = @At("HEAD"), cancellable = true)
    private void invertInstantEffects(CallbackInfoReturnable<Boolean> cir) {
        if (PowerHolderComponent.hasPower(this, InvertInstantEffectsPower.class)) {
            cir.setReturnValue(true);
        }
    }

    @Unique private int apugli_framesOnGround;

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (!this.isAlive()) return;
        if (!world.isClient && hitsHashmap.size() > 0) {
            if (hitsResetCounter == 901) {
                hitsHashmap.clear();
                HitsOnTargetUtil.sendPacket((LivingEntity)(Object)this, null, HitsOnTargetUtil.PacketType.CLEAR, 0);
                hitsResetCounter++;
            } else if (hitsResetCounter < 901) {
                hitsResetCounter++;
            }
        }
        if (PowerHolderComponent.hasPower(this, HoverPower.class)) {
            this.setVelocity(this.getVelocity().multiply(1.0, 0.0, 1.0));
            this.fallDistance = 0.0F;
        }
        PowerHolderComponent.KEY.get(this).getPowers(EdibleItemPower.class, true).forEach(EdibleItemPower::tempTick);
        if (PowerHolderComponent.hasPower(this, BunnyHopPower.class) && !this.world.isClient) {
            BunnyHopPower bunnyHopPower = PowerHolderComponent.getPowers(this, BunnyHopPower.class).get(0);
            if (apugli_framesOnGround > 4) {
                bunnyHopPower.setValue(0);
                PowerHolderComponent.syncPower(this, bunnyHopPower.getType());
            }
            if (this.onGround || this.isTouchingWater() || this.isInLava() || this.hasVehicle() || this.isFallFlying()) {
                if (apugli_framesOnGround <= 4) {
                    apugli_framesOnGround += 1;
                }
            } else {
                this.apugli_framesOnGround = 0;
            }
        }
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void travel(Vec3d movementInput, CallbackInfo ci) {
        if (this.isDead()) return;
        if (PowerHolderComponent.hasPower(this, BunnyHopPower.class)) {
            BunnyHopPower bunnyHopPower = PowerHolderComponent.getPowers(this, BunnyHopPower.class).get(0);
            if (!this.world.isClient) {
                if (this.apugli_framesOnGround <= 4) {
                    if (this.apugli_framesOnGround == 0) {
                        if (this.age % bunnyHopPower.tickRate == 0) {
                            if (bunnyHopPower.getValue() < bunnyHopPower.getMax()) {
                                bunnyHopPower.increment();
                                PowerHolderComponent.syncPower(this, bunnyHopPower.getType());
                            }
                        }
                    }
                }
            }
            this.updateVelocity((float)bunnyHopPower.increasePerTick * bunnyHopPower.getValue(), movementInput);
        }
    }

    @Override
    public HashMap<Entity, Integer> getHits() {
        return this.hitsHashmap;
    }

    @Override
    public void addToHits(Entity entity, int value) {
        this.hitsHashmap.put(entity, hitsHashmap.containsKey(entity) ? hitsHashmap.get(entity) + value: value);
    }

    @Override
    public void setHits(Entity entity, int value) {
        this.hitsHashmap.put(entity, value);
    }
}