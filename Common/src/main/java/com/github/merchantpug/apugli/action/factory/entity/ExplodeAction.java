package com.github.merchantpug.apugli.action.factory.entity;

import com.github.merchantpug.apugli.action.factory.IActionFactory;
import com.github.merchantpug.apugli.platform.Services;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ExplodeAction implements IActionFactory<Entity> {
    
    @Override
    public SerializableData getSerializableData() {
        return new SerializableData()
            .add("power", SerializableDataTypes.FLOAT)
            .add("destruction_type", SerializableDataType.enumValue(Explosion.BlockInteraction.class), Explosion.BlockInteraction.BREAK)
            .add("damage_self", SerializableDataTypes.BOOLEAN, true)
            .add("indestructible", Services.CONDITION.blockDataType(), null)
            .add("destructible", Services.CONDITION.blockDataType(), null)
            .add("create_fire", SerializableDataTypes.BOOLEAN, false)
            .add("use_charged", SerializableDataTypes.BOOLEAN, false)
            .add("charged_modifier", Services.PLATFORM.getModifierDataType(), null)
            .add("charged_modifiers", Services.PLATFORM.getModifiersDataType(), null)
            .add("spawn_effect_cloud", SerializableDataTypes.BOOLEAN, false);
    }
    
    @Override
    public void execute(SerializableData.Instance data, Entity entity) {
        if(entity.level.isClientSide) return;
        float power = data.getFloat("power");
        if(data.getBoolean("use_charged")) {
            power = applyChargedModifiers(data, entity, power);
        }
        summonExplosion(data, entity, power);
        spawnEffectCloud(data, entity);
    }
    
    protected float applyChargedModifiers(SerializableData.Instance data, Entity entity, float power) {
        if(!(entity instanceof LivingEntity living)) return power;
        List<?> chargedModifiers = new ArrayList<>();
        if(data.isPresent("charged_modifier")) {
            chargedModifiers.add(data.get("charged_modifier"));
        }
        if(data.isPresent("charged_modifiers")) {
            chargedModifiers.addAll(data.get("charged_modifiers"));
        }
        if(chargedModifiers.isEmpty()) return power;
        boolean tmoCharged = Services.PLATFORM.isModLoaded("toomanyorigins") && ((LivingEntity) entity).hasEffect(Registry.MOB_EFFECT.get(new ResourceLocation("toomanyorigins", "charged")));
        boolean cursedCharged = Services.PLATFORM.isModLoaded("cursedorigins") && ((LivingEntity) entity).hasEffect(Registry.MOB_EFFECT.get(new ResourceLocation("cursedorigins", "charged")));
        if(tmoCharged || cursedCharged) {
            ((LivingEntity)entity).removeEffect(Registry.MOB_EFFECT.get(new ResourceLocation("toomanyorigins", "charged")));
            ((LivingEntity)entity).removeEffect(Registry.MOB_EFFECT.get(new ResourceLocation("cursedorigins", "charged")));
            return (float) Services.PLATFORM.applyModifiers(living, chargedModifiers, data.getFloat("power"));
        }
        return power;
    }
    
    protected void summonExplosion(SerializableData.Instance data, Entity entity, float power) {
        boolean damageSelf = data.getBoolean("damage_self");
        boolean createFire = data.getBoolean("create_fire");
        Explosion.BlockInteraction destructionType = data.get("destruction_type");
        ExplosionDamageCalculator calculator = null;
        if(data.isPresent("destructible")) {
            calculator = createBlockConditionedExplosionDamageCalculator(data, "indestructible", entity.level, false);
        } else if(data.isPresent("indestructible")) {
            calculator = createBlockConditionedExplosionDamageCalculator(data, "destructible", entity.level, true);
        }
        if(calculator != null) {
            entity.level.explode(damageSelf ? null : entity,
                entity instanceof LivingEntity living ?
                    DamageSource.explosion(living) :
                    DamageSource.explosion((LivingEntity) null),
                calculator,
                entity.getX(), entity.getY(), entity.getZ(),
                power, createFire, destructionType
            );
        } else {
            entity.level.explode(damageSelf ? null : entity,
                entity.getX(), entity.getY(), entity.getZ(),
                power, createFire, destructionType
            );
        }
    }
    
    private ExplosionDamageCalculator createBlockConditionedExplosionDamageCalculator(SerializableData.Instance data, String fieldName, Level levelIn, boolean indestructible) {
        return new ExplosionDamageCalculator() {
            @Override
            public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState blockState, FluidState fluidState) {
                Optional<Float> def = super.getBlockExplosionResistance(explosion, level, pos, blockState, fluidState);
                Optional<Float> ovr = Services.CONDITION.checkBlock(data, fieldName, levelIn, pos) == indestructible
                    ? Optional.of(Blocks.WATER.getExplosionResistance())
                    : Optional.empty();
                return ovr.isPresent() ? def.isPresent() ? def.get() > ovr.get() ? def : ovr : ovr : def;
            }
        };
    }
    
    protected void spawnEffectCloud(SerializableData.Instance data, Entity entity) {
        if(!(entity instanceof LivingEntity)) return;
        Collection<MobEffectInstance> collection = ((LivingEntity)entity).getActiveEffects();
        if(!collection.isEmpty() && data.getBoolean("spawn_effect_cloud")) {
            AreaEffectCloud areaEffectCloudEntity = new AreaEffectCloud(entity.level, entity.getX(), entity.getY(), entity.getZ());
            areaEffectCloudEntity.setRadius(2.5F);
            areaEffectCloudEntity.setRadiusOnUse(-0.5F);
            areaEffectCloudEntity.setWaitTime(10);
            areaEffectCloudEntity.setDuration(areaEffectCloudEntity.getDuration() / 2);
            areaEffectCloudEntity.setRadiusPerTick(-areaEffectCloudEntity.getRadius() / (float)areaEffectCloudEntity.getDuration());

            for(MobEffectInstance statusEffectInstance : collection) {
                areaEffectCloudEntity.addEffect(new MobEffectInstance(statusEffectInstance));
            }
            entity.level.addFreshEntity(areaEffectCloudEntity);
        }
    }
    
}
