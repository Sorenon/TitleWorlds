package net.sorenon.titleworlds.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sorenon.titleworlds.TitleWorldsMod;

import java.util.ArrayList;

import static net.sorenon.titleworlds.TitleWorldsMod.LEVEL_SOURCE;

@Environment(EnvType.CLIENT)
public class GlobalConfigGUI {

    // Config system borrowed from https://github.com/qouteall/ImmersivePortalsMod and adapted here thank you

    public static Screen createConfigScreen(Screen parent) {

        var config = ConfigUtil.loadConfig();

        ArrayList<String> worldList = new ArrayList<>();

        for (var levelDirectory : LEVEL_SOURCE.findLevelCandidates()) {
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

        var preloadRadius = configBuilder.entryBuilder().startIntSlider(Component.literal("poop"), config.preloadChunksRadius, 0, 11)
                .setTooltip(Component.literal("Radius of world that will be pre loaded"),
                        Component.literal("Bigger numbers are stabler but slower"),
                        Component.literal("Recommended for pure speed: 0"),
                        Component.literal("Recommended for balance: 1"),
                        Component.literal("Recommended for stability: 4")
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
                        TitleWorldsMod.state.reloading = true;
                        Minecraft.getInstance().clearLevel();
                    }
                })
                .build();

    }

}
