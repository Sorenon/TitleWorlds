package net.sorenon.titleworlds.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sorenon.titleworlds.TitleWorldsMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static net.sorenon.titleworlds.TitleWorldsMod.levelSource;

@Environment(EnvType.CLIENT)
public class GlobalConfigGUI {

    // Config system borrowed from https://github.com/qouteall/ImmersivePortalsMod and adapted here thank you

    public static Screen createConfigScreen(Screen parent) {

        var config = ConfigUtil.loadConfig();

        ArrayList<String> worldList = new ArrayList<>();
        Map<Integer, String> worldDict = new HashMap<>();

        levelSource.findLevelCandidates().levels().forEach(levelDirectory -> {
            worldDict.put(levelSource.findLevelCandidates().levels().indexOf(levelDirectory), levelDirectory.directoryName());
        });

        worldDict.forEach((integer, s) -> {
            worldList.add(integer + " " + s);
        });

        ConfigBuilder configBuilder = ConfigBuilder.create();
        ConfigCategory clientcategory = configBuilder.getOrCreateCategory(Component.translatable("Client Config"));

        BooleanListEntry screenshotOnExit = configBuilder.entryBuilder().startBooleanToggle(
                Component.translatable("titleworlds.config.screenshot_on_exit"),
                config.screenshotOnExit
        ).setDefaultValue(false).build();

        BooleanListEntry profiling = configBuilder.entryBuilder().startBooleanToggle(
                Component.translatable("titleworlds.config.profiling"),
                config.profiling
        ).setDefaultValue(false).build();


        BooleanListEntry useTitleWorldOverride = configBuilder.entryBuilder().startBooleanToggle(
                        Component.translatable("titleworlds.config.usetitleworldoverride"),
                        config.useTitleWorldOverride
                ).setDefaultValue(false)
                .build();

        DropdownBoxEntry<String> titleWorldOverride = configBuilder.entryBuilder().startStringDropdownMenu(
                        Component.translatable("titleworlds.config.titleworldoverride"),
                        Integer.toString(config.titleWorldOverride)
                ).setDefaultValue("0")
                .setSelections(worldList)
                .setSaveConsumer(item -> config.titleWorldOverride = Integer.parseInt(item.split(" ")[0]))
                .build();

        clientcategory.addEntry(screenshotOnExit);
        clientcategory.addEntry(profiling);
        clientcategory.addEntry(useTitleWorldOverride);
        clientcategory.addEntry(titleWorldOverride);

        return configBuilder.setParentScreen(parent)
                .setSavingRunnable(() -> {
                    var newConfig = new GlobalConfig();
                    newConfig.profiling = profiling.getValue();
                    newConfig.screenshotOnExit = !useTitleWorldOverride.getValue() && screenshotOnExit.getValue();
                    newConfig.useTitleWorldOverride = useTitleWorldOverride.getValue();
                    newConfig.titleWorldOverride = Integer.parseInt(titleWorldOverride.getValue().split(" ")[0]);
                    newConfig.save();
                    TitleWorldsMod.CONFIG = newConfig;
                })
                .build();

    }

}
