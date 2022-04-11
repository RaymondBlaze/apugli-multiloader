package io.github.merchantpug.apugli.behavior;

import io.github.apace100.calio.data.SerializableData;
import io.github.merchantpug.apugli.access.MobEntityAccess;
import io.github.merchantpug.apugli.mixin.MobEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class MobBehavior {
    private BehaviorFactory<?> factory;

    protected int priority;
    protected Predicate<Pair<LivingEntity, LivingEntity>> mobRelatedPredicates;
    protected Predicate<LivingEntity> entityRelatedPredicates;

    @Nullable public LivingEntity entity;

    public MobBehavior(int priority) {
        this.priority = priority;
    }

    private BehaviorFactory<?> getFactory() {
        return this.factory;
    }

    public void setFactory(BehaviorFactory<?> factory) {
        this.factory = factory;
    }

    public void initGoals(MobEntity mob) {
    }

    public void removeGoals(MobEntity mob) {
        ((MobEntityAccess)mob).getModifiedTargetSelectorGoals().stream().filter(pair -> pair.getLeft() == this).forEach(pair -> ((MobEntityAccessor)mob).getTargetSelector().remove(pair.getRight()));
        ((MobEntityAccess)mob).getModifiedTargetSelectorGoals().removeIf(pair -> pair.getLeft() == this);
        ((MobEntityAccess)mob).getModifiedGoalSelectorGoals().stream().filter(pair -> pair.getLeft() == this).forEach(pair -> ((MobEntityAccessor)mob).getGoalSelector().remove(pair.getRight()));
        ((MobEntityAccess)mob).getModifiedGoalSelectorGoals().removeIf(pair -> pair.getLeft() == this);
    }

    public boolean hasAppliedGoals(MobEntity mob) {
        return ((MobEntityAccess)mob).getModifiedTargetSelectorGoals().stream().filter(pair -> pair.getLeft() == this).toList().size() > 0 || ((MobEntityAccess)mob).getModifiedGoalSelectorGoals().stream().filter(pair -> pair.getLeft() == this).toList().size() > 0;
    }

    public boolean isPassive(MobEntity mob, LivingEntity target) {
        return false;
    }

    public boolean isHostile(MobEntity mob, LivingEntity target) {
        return false;
    }

    public void onMobDamage(MobEntity mob, Entity attacker) {

    }

    public void addMobRelatedPredicate(Predicate<Pair<LivingEntity, LivingEntity>> predicate) {
        mobRelatedPredicates = mobRelatedPredicates == null ? predicate : mobRelatedPredicates.and(predicate);
    }

    public void addEntityRelatedPredicate(Predicate<LivingEntity> predicate) {
        entityRelatedPredicates = entityRelatedPredicates == null ? predicate : entityRelatedPredicates.and(predicate);
    }

    protected void setToDataInstance(SerializableData.Instance dataInstance) {
        dataInstance.set("priority", this.priority);
    }

    public void send(PacketByteBuf buffer) {
        BehaviorFactory<?> factory = getFactory();
        buffer.writeIdentifier(factory.getSerializerId());
        SerializableData data = factory.getData();
        SerializableData.Instance dataInstance = data.new Instance();
        this.setToDataInstance(dataInstance);
        data.write(buffer, dataInstance);
    }
}