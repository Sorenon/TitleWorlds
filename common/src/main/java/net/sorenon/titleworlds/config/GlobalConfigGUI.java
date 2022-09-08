package net.sorenon.titleworlds.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sorenon.titleworlds.ModService;
import net.sorenon.titleworlds.Platform;
import net.sorenon.titleworlds.TitleWorldsMod;

import java.util.ArrayList;
import java.util.ServiceLoader;

import static net.sorenon.titleworlds.TitleWorldsState.STATE;

@Environment(EnvType.CLIENT)
public class GlobalConfigGUI {

    // Config system borrowed from https://github.com/qouteall/ImmersivePortalsMod and adapted here thank you

    public static Screen createConfigScreen(Screen parent) {

        var config = ConfigUtil.loadConfig();

        ArrayList<String> worldList = new ArrayList<>();

        var mod = ServiceLoader.load(ModService.class);

        for (var levelDirectory : Platform.getLevelStorageSource().findLevelCandidates()) {
            worldList.add(levelDirectory.directoryName());
        }

        ConfigBuilder configBuilder = ConfigBuilder.create();
        ConfigCategory category = configBuilder.getOrCreateCategory(Component.translatable("Client Config"));

        var screenshotOnExit = configBuilder.entryBuilder().startBooleanToggle(
                Component.translatable("titleworlds.config.screenshot_on_exit"),
                config.screenshotOnExit
        ).setDefaultValue(false).build();

        var reloadButton = configBuilder.entryBuilder().startBooleanToggle(
                Component.translatable("titleworlds.config.reload_button"),
                config.reloadButton
        ).setDefaultValue(true).build();

        var profiling = configBuilder.entryBuilder().startBooleanToggle(
                Component.translatable("titleworlds.config.profiling"),
                config.profiling
        ).setDefaultValue(false).build();

        var enabled = configBuilder.entryBuilder().startBooleanToggle(
                Component.translatable("titleworlds.config.enabled"),
                config.enabled
        ).setDefaultValue(true).build();

        var useTitleWorldOverride = configBuilder.entryBuilder().startBooleanToggle(
                        Component.translatable("titleworlds.config.usetitleworldoverride"),
                        config.useTitleWorldOverride
                ).setDefaultValue(false)
                .build();

        var titleWorldOverride = configBuilder.entryBuilder().startStringDropdownMenu(
                        Component.translatable("titleworlds.config.titleworldoverride"),
                        Integer.toString(config.titleWorldOverride)
                ).setDefaultValue("0")
                .setSelections(worldList)
                .setSaveConsumer(item -> config.titleWorldOverride = Integer.parseInt(item.split(" ")[0]))
                .build();

        var preloadRadius = configBuilder.entryBuilder().startIntSlider(Component.translatable("titleworlds.config.preload_radius"), config.preloadChunksRadius, 0, 11)
                .setTooltip(Component.translatable("titleworlds.config.preload_radius.tooltip1"),
                        Component.translatable("titleworlds.config.preload_radius.tooltip2"),
                        Component.translatable("titleworlds.config.preload_radius.tooltip3"),
                        Component.translatable("titleworlds.config.preload_radius.tooltip4"),
                        Component.translatable("titleworlds.config.preload_radius.tooltip5")
                )
                .setDefaultValue(1)
                .build();

        category.addEntry(enabled);
        category.addEntry(reloadButton);
        category.addEntry(preloadRadius);
        category.addEntry(profiling);
        category.addEntry(screenshotOnExit);
        category.addEntry(useTitleWorldOverride);
        category.addEntry(titleWorldOverride);

        return configBuilder.setParentScreen(parent)
                .setSavingRunnable(() -> {
                    var newConfig = new GlobalConfig();
                    newConfig.preloadChunksRadius = preloadRadius.getValue();
                    newConfig.profiling = profiling.getValue();
                    newConfig.screenshotOnExit = !useTitleWorldOverride.getValue() && screenshotOnExit.getValue();
                    newConfig.useTitleWorldOverride = useTitleWorldOverride.getValue();
                    newConfig.titleWorldOverride = Integer.parseInt(titleWorldOverride.getValue().split(" ")[0]);
                    newConfig.reloadButton = reloadButton.getValue();
                    newConfig.enabled = enabled.getValue();
                    newConfig.save();
                    TitleWorldsMod.CONFIG = newConfig;

                    if (config.enabled != newConfig.enabled) {
                        STATE.reloading = true;
                        Minecraft.getInstance().clearLevel();
                    }
                })
                .build();

    }

}
