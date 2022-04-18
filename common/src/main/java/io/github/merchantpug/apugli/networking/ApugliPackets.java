package io.github.merchantpug.apugli.networking;

import io.github.merchantpug.apugli.Apugli;
import net.minecraft.util.Identifier;

public class ApugliPackets {
    public static final Identifier REMOVE_STACK_FOOD_COMPONENT = Apugli.identifier("sync_stack_food_component");
    public static final Identifier REMOVE_KEYS_TO_CHECK = Apugli.identifier("remove_keys_to_check");
    public static final Identifier SYNC_ACTIVE_KEYS_CLIENT = Apugli.identifier("sync_active_keys_client");

    public static final Identifier SYNC_ACTIVE_KEYS_SERVER = Apugli.identifier("sync_active_keys_server");
}
