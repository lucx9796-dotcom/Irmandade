package gg.irmandade;

import gg.irmandade.expurgo.ExpurgoManager;
import gg.irmandade.net.ExpurgoPayload;
import gg.irmandade.relic.ModEffects;
import gg.irmandade.relic.RelicAbilities;
import gg.irmandade.relic.RelicCommand;
import gg.irmandade.tab.TabManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Irmandade implements ModInitializer {
    public static final String MOD_ID = "irmandade";
    public static final Logger LOGGER = LoggerFactory.getLogger("IMDD");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(ExpurgoPayload.TYPE, ExpurgoPayload.CODEC);
        ModEffects.register();
        RelicAbilities.register();
        ExpurgoManager.register();
        TabManager.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> RelicCommand.register(dispatcher));
        LOGGER.info("Irmandade (IMDD) carregado - by LucxMC_");
    }
}
