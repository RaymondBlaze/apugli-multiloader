package com.github.merchantpug.apugli.power;

import com.github.merchantpug.apugli.power.factory.SimplePowerFactory;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class CustomHurtSoundPower extends CustomSoundPower {
    
    public CustomHurtSoundPower(PowerType<?> type, LivingEntity living, SerializableData.Instance data) {
        super(type, living, data);
    }
    
    @Override
    protected void playSound(Entity entity, SoundEvent soundEvent, float volume, float pitch) {
        RandomSource random = entity instanceof LivingEntity living
            ? living.getRandom()
            : entity.level.random;
        entity.level.playSound(null,
            entity.getX(), entity.getY(), entity.getZ(),
            soundEvent, entity.getSoundSource(),
            volume, (random.nextFloat() - random.nextFloat()) * 0.2F + pitch
        );
    }
    
    public static class Factory extends SimplePowerFactory<CustomHurtSoundPower> {
        
        public Factory() {
            super("custom_hurt_sound",
                CustomSoundPower.getSerializableData(),
                data -> (type, entity) -> new CustomHurtSoundPower(type, entity, data)
            );
            allowCondition();
        }
        
        @Override
        public Class<CustomHurtSoundPower> getPowerClass() {
            return CustomHurtSoundPower.class;
        }
        
    }
    
}