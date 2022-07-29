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
                Component.translatable("titleworlds.config.screenshotonexit"),
                config.screenshotOnExit
        ).setDefaultValue(false).build();

        BooleanListEntry UseTitleWorldOverride = configBuilder.entryBuilder().startBooleanToggle(
                        Component.translatable("titleworlds.config.usetitleworldoverride"),
                        config.useTitleWorldOverride
                ).setDefaultValue(false)
                .build();

        DropdownBoxEntry<String> TitleWorldOverride = configBuilder.entryBuilder().startStringDropdownMenu(
                        Component.translatable("titleworlds.config.titleworldoverride"),
                        Integer.toString(config.titleWorldOverride)
                ).setDefaultValue("0")
                .setSelections(worldList)
                .setSaveConsumer(item -> config.titleWorldOverride = Integer.parseInt(item.split(" ")[0]))
                .build();

        clientcategory.addEntry(screenshotOnExit);
        clientcategory.addEntry(UseTitleWorldOverride);
        clientcategory.addEntry(TitleWorldOverride);

        return configBuilder.setParentScreen(parent)
                .setSavingRunnable(() -> {
                    var newConfig = new GlobalConfig();
                    // Prevents using a specific title world and using screenshot on exit at the same time.
                    newConfig.screenshotOnExit = !UseTitleWorldOverride.getValue() && screenshotOnExit.getValue();
                    newConfig.useTitleWorldOverride = UseTitleWorldOverride.getValue();
                    newConfig.titleWorldOverride = Integer.parseInt(TitleWorldOverride.getValue().split(" ")[0]);
                    newConfig.save();
                    TitleWorldsMod.CONFIG = newConfig;
                })
                .build();

    }

}
