package com.github.merchantpug.apugli.power;

import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import the.great.migration.merchantpug.apugli.Apugli;

public class EnergySwirlPower extends Power {
    private final ResourceLocation textureLocation;
    private final String textureUrl;
    private final float size;
    private final float speed;

    public static PowerFactory<?> getFactory() {
        return new PowerFactory<EnergySwirlPower>(Apugli.identifier("energy_swirl"),
                new SerializableData()
                        .add("texture_location", SerializableDataTypes.IDENTIFIER, null)
                        .add("texture_url", SerializableDataTypes.STRING, null)
                        .add("size", SerializableDataTypes.FLOAT, 1.0F)
                        .add("speed", SerializableDataTypes.FLOAT, 0.01F),
                data ->
                        (type, entity) ->
                                new EnergySwirlPower(type, entity, data.getId("texture_location"), data.getString("texture_url"), data.getFloat("size"), data.getFloat("speed")))
                .allowCondition();
    }

    public EnergySwirlPower(PowerType<?> type, LivingEntity entity, ResourceLocation textureLocation, String textureUrl, float size, float speed) {
        super(type, entity);
        if(textureLocation == null && textureUrl == null) {
            Apugli.LOGGER.warn("EnergySwirlPower '" + this.getType().getIdentifier() + "' does not have a valid `texture_location` or `texture_url` field. This power will not render.");
        }
        this.textureLocation = textureLocation;
        this.textureUrl = textureUrl;
        this.size = size;
        this.speed = speed;
    }

    @Nullable public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    @Nullable public String getTextureUrl() {
        return textureUrl;
    }

    public ResourceLocation getUrlTextureIdentifier() {
        return new ResourceLocation(Apugli.MODID, "energyswirlpower/" + this.getType().getIdentifier().getNamespace() + "/" + this.getType().getIdentifier().getPath());
    }

    public float getSize() {
        return size;
    }

    public float getSpeed() {
        return speed;
    }
}
