package io.github.merchantpug.apugli.networking;

import io.github.merchantpug.apugli.Apugli;
import io.github.merchantpug.apugli.access.LivingEntityAccess;
import io.github.merchantpug.apugli.util.HitsOnTargetUtil;
import io.github.merchantpug.apugli.util.ItemStackFoodComponentAPI;
import io.github.merchantpug.apugli.util.StackFoodComponentUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.DefaultedList;

public class ApugliPacketsS2C {
    @Environment(EnvType.CLIENT)
    public static void register() {
        ClientPlayConnectionEvents.INIT.register(((clientPlayNetworkHandler, minecraftClient) -> {
            ClientPlayNetworking.registerReceiver(ApugliPackets.REMOVE_STACK_FOOD_COMPONENT, ApugliPacketsS2C::onFoodComponentSync);
            ClientPlayNetworking.registerReceiver(ApugliPackets.SYNC_HITS_ON_TARGET, ApugliPacketsS2C::onHitsOnTargetSync);
        }));
    }

    private static void onHitsOnTargetSync(MinecraftClient minecraftClient, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        int targetId = packetByteBuf.readInt();
        int attackerId = packetByteBuf.readInt();
        HitsOnTargetUtil.PacketType type = HitsOnTargetUtil.PacketType.values()[packetByteBuf.readByte()];
        int amount = 0;
        if (type == HitsOnTargetUtil.PacketType.SET) {
            amount = packetByteBuf.readInt();
        }
        int finalAmount = amount;

        minecraftClient.execute(() -> {
            Entity target = clientPlayNetworkHandler.getWorld().getEntityById(targetId);
            Entity attacker = clientPlayNetworkHandler.getWorld().getEntityById(attackerId);
            if (!(target instanceof LivingEntity)) {
                Apugli.LOGGER.warn("Received unknown target");
            } else if (!(attacker instanceof LivingEntity)) {
                Apugli.LOGGER.warn("Received unknown attacker");
            } else switch (type) {
                case SET -> ((LivingEntityAccess)target).getHits().put(attacker, finalAmount);
                case REMOVE -> {
                    if (!((LivingEntityAccess)target).getHits().containsKey(attacker)) return;
                    ((LivingEntityAccess)target).getHits().remove(attacker);
                }
            }
        });
    }

    private static void onFoodComponentSync(MinecraftClient minecraftClient, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        int targetId = packetByteBuf.readInt();

        boolean usesEquipmentSlot = packetByteBuf.readBoolean();
        String equipmentSlotId = "";
        if (usesEquipmentSlot) {
            equipmentSlotId = packetByteBuf.readString(PacketByteBuf.DEFAULT_MAX_STRING_LENGTH);
        }
        String finalEquipmentSlotId = equipmentSlotId;

        boolean usesInventoryIndex = packetByteBuf.readBoolean();
        StackFoodComponentUtil.InventoryLocation inventoryLocation = null;
        int inventoryIndex = 0;
        if (usesInventoryIndex) {
            inventoryLocation = StackFoodComponentUtil.InventoryLocation.values()[packetByteBuf.readByte()];
            inventoryIndex = packetByteBuf.readInt();
        }
        StackFoodComponentUtil.InventoryLocation finalInventoryLocation = inventoryLocation;
        int finalInventoryIndex = inventoryIndex;

        minecraftClient.execute(() -> {
            Entity entity = clientPlayNetworkHandler.getWorld().getEntityById(targetId);
            if (!(entity instanceof PlayerEntity)) {
                Apugli.LOGGER.warn("Received unknown target");
            } else {
                if (usesEquipmentSlot) {
                    EquipmentSlot equipmentSlot = EquipmentSlot.byName(finalEquipmentSlotId);
                    ItemStack stack = ((PlayerEntity)entity).getEquippedStack(equipmentSlot);
                    ItemStackFoodComponentAPI.removeStackFood(stack);
                }
                if (usesInventoryIndex) {
                    DefaultedList<ItemStack> inventory;
                    switch(finalInventoryLocation) {
                        case MAIN -> inventory = ((PlayerEntity) entity).getInventory().main;
                        case ARMOR -> inventory = ((PlayerEntity) entity).getInventory().armor;
                        case OFFHAND -> inventory = ((PlayerEntity) entity).getInventory().offHand;
                        default -> throw new IllegalStateException("Unexpected value: " + finalInventoryLocation);
                    }
                    ItemStackFoodComponentAPI.removeStackFood(inventory.get(finalInventoryIndex));
                }
            }
        });
    }
}