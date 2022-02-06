package io.github.merchantpug.apugli;

import io.github.merchantpug.apugli.registry.ApugliPowerFactories;
import io.github.merchantpug.apugli.registry.action.ApugliBlockActions;
import io.github.merchantpug.apugli.registry.action.ApugliEntityActions;
import io.github.merchantpug.apugli.registry.action.ApugliItemActions;
import io.github.merchantpug.apugli.registry.condition.ApugliBlockConditions;
import io.github.merchantpug.apugli.registry.condition.ApugliDamageConditions;
import io.github.merchantpug.apugli.registry.condition.ApugliEntityConditions;
import io.github.merchantpug.apugli.util.ApugliNamespaceAlias;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Apugli {
    public static final String MODID = "apugli";
    public static final Logger LOGGER = LogManager.getLogger(Apugli.class);

    public static String VERSION = "";

    public static void init() {
        LOGGER.info("Apugli " + VERSION + " is initializing. Powering up your powered up game.");

        ApugliBlockActions.register();
        ApugliEntityActions.register();
        ApugliItemActions.register();

        ApugliBlockConditions.register();
        ApugliDamageConditions.register();
        ApugliEntityConditions.register();

        ApugliPowerFactories.register();

        ApugliNamespaceAlias.addAlias("ope");
    }

    public static Identifier identifier(String path) {
        return new Identifier(MODID, path);
    }
}