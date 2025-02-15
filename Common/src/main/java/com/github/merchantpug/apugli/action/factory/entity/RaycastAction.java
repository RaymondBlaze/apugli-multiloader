package com.github.merchantpug.apugli.action.factory.entity;

import com.github.merchantpug.apugli.action.factory.IActionFactory;
import com.github.merchantpug.apugli.platform.Services;
import com.github.merchantpug.apugli.util.RaycastUtil;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;

import java.util.List;

public class RaycastAction implements IActionFactory<Entity> {
    
    @Override
    public SerializableData getSerializableData() {
        return new SerializableData()
            .add("distance", SerializableDataTypes.DOUBLE, null)
            .add("pierce", SerializableDataTypes.BOOLEAN, false)
            .add("particle", SerializableDataTypes.PARTICLE_EFFECT_OR_TYPE, null)
            .add("spacing", SerializableDataTypes.DOUBLE, 0.5)
            .add("block_action", Services.ACTION.blockDataType(), null)
            .add("block_condition", Services.CONDITION.blockDataType(), null)
            .add("bientity_action", Services.ACTION.biEntityDataType(), null)
            .add("bientity_condition", Services.CONDITION.biEntityDataType(), null)
            .add("target_action", Services.ACTION.entityDataType(), null)
            .add("target_condition", Services.CONDITION.entityDataType(), null)
            .add("self_action", Services.ACTION.entityDataType(), null);
    }
    
    @Override
    public void execute(SerializableData.Instance data, Entity entity) {
        //Block Hit
        double blockDistance = data.isPresent("distance") ?
            data.getDouble("distance") :
            Services.PLATFORM.getReachDistance(entity);
        BlockHitResult blockHitResult = RaycastUtil.raycastBlock(entity, blockDistance);
        HitResult.Type blockHitResultType = blockHitResult.getType();
        //Entity Hit
        double entityDistance = data.isPresent("distance") ?
            data.getDouble("distance") :
            Services.PLATFORM.getAttackRange(entity);
        EntityHitResult entityHitResult = RaycastUtil.raycastEntity(blockHitResult, entity, entityDistance);
        HitResult.Type entityHitResultType = entityHitResult != null ? entityHitResult.getType() : null;

        double squaredParticleDistance = entityHitResult != null && !data.getBoolean("pierce") ? entityHitResult.getLocation().distanceToSqr(entity.getEyePosition()) : entityDistance * entityDistance;
        createParticlesAtHitPos(data, entity, Math.sqrt(squaredParticleDistance));
        //Execute Actions
        if(data.getBoolean("pierce")) {
            List<EntityHitResult> list = RaycastUtil.raycastEntities(entity, (traceEntity) -> !traceEntity.isSpectator() && traceEntity.isPickable(), entityDistance);
            handlePierce(data, entity, list);
            return;
        }
        if(blockHitResultType == HitResult.Type.BLOCK) {
            onHitBlock(data, entity, blockHitResult);
        }
        if(entityHitResultType == HitResult.Type.ENTITY) {
            onHitEntity(data, entity, entityHitResult, false);
        }
    }
    
    protected void createParticlesAtHitPos(SerializableData.Instance data, Entity entity, double entityReach) {
        if(!data.isPresent("particle") || entity.level.isClientSide()) return;
        ParticleOptions particleEffect = data.get("particle");
        
        for(double d = data.getDouble("spacing"); d < entityReach; d += data.getDouble("spacing")) {
            ((ServerLevel)entity.level).sendParticles(particleEffect, entity.getEyePosition().x() + d * entity.getViewVector(0).x(), entity.getEyePosition().y() + d * entity.getViewVector(0).y(), entity.getEyePosition().z() + d * entity.getViewVector(0).z(), 1, 0, 0, 0, 0);
        }
    }
    
    protected void handlePierce(SerializableData.Instance data, Entity entity, List<EntityHitResult> list) {
        if(list.isEmpty()) return;
        list.forEach(targetEntity -> onHitEntity(data, entity, targetEntity, true));
        executeSelfAction(data, entity);
    }

    protected void executeSelfAction(SerializableData.Instance data, Entity entity) {
        if(!data.isPresent("self_action") || !entity.isAlive()) return;
        Services.ACTION.executeEntity(data,"self_action", entity);
    }

    protected void onHitBlock(SerializableData.Instance data, Entity entity, BlockHitResult result) {
        if(!data.isPresent("block_action") && !Services.CONDITION.checkBlock(data, "block_condition", entity.level, result.getBlockPos())) return;
        Services.ACTION.executeBlock(data,"block_action", entity.level, result.getBlockPos(), result.getDirection());
        executeSelfAction(data, entity);
    }
    
    protected void onHitEntity(SerializableData.Instance data, Entity actor, EntityHitResult result, boolean calledThroughPierce) {
        boolean hasTargetAction = data.isPresent("target_action");
        boolean hasBiEntityAction = data.isPresent("bientity_action");
        if(!hasTargetAction && !hasBiEntityAction) return;
        if(Services.CONDITION.checkEntity(data, "target_condition", actor)) return;
        Entity target = result.getEntity();
        if(!Services.CONDITION.checkBiEntity(data, "bientity_condition", actor, target)) return;
        if(hasTargetAction) {
            Services.ACTION.executeEntity(data, "target_action", actor);
        }
        if(hasBiEntityAction) {
            Services.ACTION.executeBiEntity(data, "bientity_action", actor, target);
        }
        if(calledThroughPierce) return;
        executeSelfAction(data, actor);
    }
    
}
