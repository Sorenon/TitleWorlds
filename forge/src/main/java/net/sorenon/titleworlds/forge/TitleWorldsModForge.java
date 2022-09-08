package net.sorenon.titleworlds.forge;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sorenon.titleworlds.Platform;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TitleWorldsMod.MOD_ID)
public class TitleWorldsModForge {

    public static final Logger LOGGER = LogManager.getLogger("Title World");

    public TitleWorldsModForge() {
//        TitleWorldsMod.init();
        Platform.INSTANCE = new ModServiceImpl();

        // Submit our event bus to let architectury register our content on the right time
//        EventBuses.registerModEventBus(ExampleMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
//        FMLJavaModLoadingContext.get().getModEventBus().addListener(event -> {});
//        ExampleMod.init();

    }
}
