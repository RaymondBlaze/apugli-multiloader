package com.github.merchantpug.apugli.platform;

import com.github.merchantpug.apugli.Apugli;
import com.github.merchantpug.apugli.platform.services.IPowerHelper;
import com.github.merchantpug.apugli.power.factory.SpecialPowerFactory;
import com.github.merchantpug.apugli.power.factory.SimplePowerFactory;
import com.github.merchantpug.apugli.registry.ApugliRegisters;
import com.github.merchantpug.apugli.registry.services.RegistryObject;
import com.google.auto.service.AutoService;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.power.CooldownPower;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerTypeReference;
import io.github.apace100.apoli.power.VariableIntPower;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableDataType;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedList;
import java.util.List;
import java.util.OptionalInt;

@SuppressWarnings({"rawtypes", "unchecked"})
@AutoService(IPowerHelper.class)
public class FabricPowerHelper implements IPowerHelper<PowerTypeReference> {
    
    @Override
    public PowerFactory<?> unwrapSimpleFactory(PowerFactory<?> factory) {
        return factory;
    }
    
    @Override
    public <F extends SimplePowerFactory<?>> RegistryObject<F> registerFactory(F factory) {
        return ApugliRegisters.POWERS.register(factory.getSerializerId().getPath(), ()  -> factory);
    }
    
    @Override
    public <F extends SpecialPowerFactory<?>> RegistryObject<F> registerFactory(String name, Class<F> factoryClass) {
        F factory = Services.load(factoryClass);
        return (RegistryObject<F>)(Object)ApugliRegisters.POWERS.register(name, () -> (PowerFactory<?>) factory);
    }
    
    @Override
    public <P extends Power> List<P> getPowers(LivingEntity entity, SimplePowerFactory<P> factory) {
        return PowerHolderComponent.getPowers(entity, factory.getPowerClass());
    }
    
    @Override
    public <P> List<P> getPowers(LivingEntity entity, SpecialPowerFactory<P> factory) {
        List<Power> powers = PowerHolderComponent.KEY.get(entity).getPowers();
        Class<P> cls = factory.getPowerClass();
        List<P> list = new LinkedList<>();
        for(Power power : powers) {
            if(cls.isAssignableFrom(power.getClass()) && power.isActive()) {
                list.add((P)power);
            }
        }
        return list;
    }
    
    @Override
    public <P extends Power> boolean hasPower(LivingEntity entity, SimplePowerFactory<P> factory) {
        return PowerHolderComponent.hasPower(entity, factory.getPowerClass());
    }
    
    @Override
    public <P> boolean hasPower(LivingEntity entity, SpecialPowerFactory<P> factory) {
        List<Power> powers = PowerHolderComponent.KEY.get(entity).getPowers();
        Class<P> cls = factory.getPowerClass();
        List<P> list = new LinkedList<>();
        for(Power power : powers) {
            if(cls.isAssignableFrom(power.getClass()) && power.isActive()) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public SerializableDataType<PowerTypeReference> getPowerTypeDataType() {
        return ApoliDataTypes.POWER_TYPE;
    }
    
    @Override
    public OptionalInt getResource(LivingEntity entity, PowerTypeReference powerType) {
        Power power = powerType.get(entity);
        if(power instanceof VariableIntPower vip) {
            return OptionalInt.of(vip.getValue());
        } else if(power instanceof CooldownPower cdp) {
            return OptionalInt.of(cdp.getRemainingTicks());
        }
        Apugli.LOG.warn("Failed to get resource for power [{}], because it doesn't hold any resource!", powerType.getIdentifier());
        return OptionalInt.empty();
    }
    
    @Override
    public OptionalInt setResource(LivingEntity entity, PowerTypeReference powerType, int value) {
        Power power = powerType.get(entity);
        if(power instanceof VariableIntPower vip) {
            int result = vip.setValue(value);
            PowerHolderComponent.syncPower(entity, powerType);
            return OptionalInt.of(result);
        } else if(power instanceof CooldownPower cdp) {
            cdp.setCooldown(value);
            PowerHolderComponent.syncPower(entity, powerType);
            return OptionalInt.of(cdp.getRemainingTicks());
        }
        Apugli.LOG.warn("Failed to set resource for power [{}], because it doesn't hold any resource!", powerType.getIdentifier());
        return OptionalInt.empty();
    }
    
}
