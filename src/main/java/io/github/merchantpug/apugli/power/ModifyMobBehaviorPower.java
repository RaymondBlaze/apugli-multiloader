package io.github.merchantpug.apugli.power;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.merchantpug.apugli.Apugli;
import io.github.merchantpug.apugli.behavior.MobBehavior;
import io.github.merchantpug.apugli.mixin.ServerWorldAccessor;
import io.github.merchantpug.apugli.util.ApugliDataTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.TypeFilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class ModifyMobBehaviorPower extends Power {
    private final Predicate<Pair<Entity, Entity>> bientityCondition;

    private final MobBehavior mobBehavior;
    public final List<MobEntity> modifiableEntities = new ArrayList<>();
    public final List<MobEntity> modifiedEntities = new ArrayList<>();

    public static PowerFactory<?> getFactory() {
        return new PowerFactory<ModifyMobBehaviorPower>(Apugli.identifier("modify_mob_behavior"),
                new SerializableData()
                        .add("bientity_condition", ApoliDataTypes.BIENTITY_CONDITION, null)
                        .add("behavior", ApugliDataTypes.MOB_BEHAVIOR),
                data ->
                        (type, player) -> new ModifyMobBehaviorPower(type, player, data.get("bientity_condition"), data.get("behavior")))
                .allowCondition();
    }

    public ModifyMobBehaviorPower(PowerType<?> type, LivingEntity entity, Predicate<Pair<Entity, Entity>> bientityCondition, MobBehavior mobBehavior) {
        super(type, entity);
        this.bientityCondition = bientityCondition;
        this.mobBehavior = mobBehavior;
        this.addMobPredicate(pair -> doesApply(pair.getLeft(), pair.getRight()));
        this.addPowerHolderPredicate(livingEntity -> this.getType().isActive(livingEntity));
        this.setTicking(true);
    }

    public boolean doesApply(LivingEntity powerHolder, LivingEntity mob) {
        return this.bientityCondition == null || this.bientityCondition.test(new Pair<>(powerHolder, mob));
    }

    @Override
    public void tick() {
        if (entity.age % 10 != 0) return;

        ((ServerWorldAccessor)entity.world).getEntityManager().getLookup().forEach(TypeFilter.instanceOf(MobEntity.class), mob -> {
            if (this.doesApply(entity, mob)) {
                modifiableEntities.add(mob);
            }
        });

        modifiableEntities.removeIf(entity -> entity.isDead() || entity.isRemoved());

        if (this.isActive()) {
            for (Iterator<MobEntity> iterator = modifiableEntities.stream().filter(mob -> this.doesApply(entity, mob) && !mobBehavior.hasAppliedGoals(mob)).iterator(); iterator.hasNext();) {
                MobEntity mob = iterator.next();
                mobBehavior.initGoals(mob);
                this.modifiedEntities.add(mob);
            }
        }

        for (Iterator<MobEntity> iterator = modifiedEntities.stream().filter(mob -> !this.doesApply(entity, mob) || !this.isActive()).iterator(); iterator.hasNext();) {
            MobEntity mob = iterator.next();
            if (this.getMobBehavior().isHostile(mob, entity) && (mob.getTarget() == this.entity || mob instanceof Angerable && ((Angerable) mob).getAngryAt() == entity.getUuid())) {
                if (mob instanceof Angerable && ((Angerable) mob).getTarget() == entity) {
                    ((Angerable) mob).stopAnger();
                }
                mob.setTarget(null);
            }
            this.mobBehavior.removeGoals(mob);
        }
        modifiedEntities.removeIf(mob -> !mobBehavior.hasAppliedGoals(mob) && (!this.doesApply(entity, mob) || !this.isActive()));
    }

    @Override
    public void onAdded() {
        if (entity.world.isClient) return;
        tick();
    }

    @Override
    public void onRemoved() {
        if (entity.world.isClient) return;
        for (Iterator<MobEntity> iterator = modifiedEntities.stream().iterator(); iterator.hasNext();) {
            MobEntity mob = iterator.next();
            if (mob.getTarget() == this.entity || mob instanceof Angerable && ((Angerable) mob).getAngryAt() == entity.getUuid()) {
                if (mob instanceof Angerable && ((Angerable) mob).getTarget() == entity) {
                    ((Angerable) mob).stopAnger();
                }
                mob.setTarget(null);
            }
            this.mobBehavior.removeGoals(mob);
        }
    }

    public void addMobPredicate(Predicate<Pair<LivingEntity, LivingEntity>> predicate) {
        mobBehavior.addMobRelatedPredicate(predicate);
    }

    public void addPowerHolderPredicate(Predicate<LivingEntity> predicate) {
        mobBehavior.addEntityRelatedPredicate(predicate);
    }

    public MobBehavior getMobBehavior() {
        return mobBehavior;
    }
}