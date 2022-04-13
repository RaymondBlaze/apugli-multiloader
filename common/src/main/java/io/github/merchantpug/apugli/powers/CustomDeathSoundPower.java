package io.github.merchantpug.apugli.powers;

import io.github.apace100.origins.power.Power;
import io.github.apace100.origins.power.PowerType;
import io.github.apace100.origins.power.factory.PowerFactory;
import io.github.apace100.origins.util.SerializableData;
import io.github.apace100.origins.util.SerializableDataType;
import io.github.merchantpug.apugli.Apugli;
import io.github.merchantpug.apugli.util.ApugliDataTypes;
import io.github.merchantpug.apugli.util.SoundEventPitchVolume;
import io.github.merchantpug.apugli.util.SoundEventWeight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;

import java.util.ArrayList;
import java.util.List;

public class CustomDeathSoundPower extends Power {
    private final List<SoundEventWeight> sounds = new ArrayList<>();
    private final Boolean muted;
    private final float pitch;
    private final float volume;

    public static PowerFactory<?> getFactory() {
        return new PowerFactory<CustomDeathSoundPower>(
            Apugli.identifier("custom_death_sound"),
            new SerializableData()
                .add("muted", SerializableDataType.BOOLEAN, false)
                .add("sound", ApugliDataTypes.SOUND_EVENT_OPTIONAL_WEIGHT, null)
                .add("sounds", SerializableDataType.list(ApugliDataTypes.SOUND_EVENT_OPTIONAL_WEIGHT), null)
                .add("volume", SerializableDataType.FLOAT, 1F)
                .add("pitch", SerializableDataType.FLOAT, 1F),
            data ->
                    (type, player) -> {
                CustomDeathSoundPower power = new CustomDeathSoundPower(type, player, data.getBoolean("muted"), data.getFloat("volume"), data.getFloat("pitch"));
                if (data.isPresent("sound")) {
                    power.addSound(data.get("sound"));
                }
                if (data.isPresent("sounds")) {
                    ((List<SoundEventWeight>)data.get("sounds")).forEach(power::addSound);
                }
                return power;
            })
            .allowCondition();
    }

    public CustomDeathSoundPower(PowerType<?> type, PlayerEntity player, Boolean muted, float volume, float pitch){
        super(type, player);
        this.muted = muted;
        this.pitch = pitch;
        this.volume = volume;
    }

    public void addSound(SoundEventWeight sew) {
        this.sounds.add(sew);
    }

    public Boolean isMuted() {
        return muted;
    }

    public void playDeathSound(Entity entity) {
        if (this.muted) return;
        int totalWeight = 0;
        for (SoundEventWeight sew : sounds) {
            totalWeight += sew.weight;
        }

        int index = 0;
        for (double r = Math.random() * totalWeight; index < sounds.size() - 1; ++index) {
            r -= sounds.get(index).weight;
            if (r <= 0.0) break;
        }

        sounds.get(index).soundEventList.forEach(soundEventPitchVolume -> {
            float volume = Float.isNaN(soundEventPitchVolume.volume) ? this.volume : soundEventPitchVolume.volume;
            float pitch = Float.isNaN(soundEventPitchVolume.pitch) ? this.pitch : soundEventPitchVolume.pitch;
            entity.playSound(soundEventPitchVolume.soundEvent, volume, (entity.world.random.nextFloat() - entity.world.random.nextFloat()) * 0.2f + pitch);
        });
    }
}