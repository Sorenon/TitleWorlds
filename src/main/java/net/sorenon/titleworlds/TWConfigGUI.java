package net.sorenon.titleworlds;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class TWConfigGUI {

    // Config system borrowed from https://github.com/qouteall/ImmersivePortalsMod and adapted here thank you

    public static Screen createConfigScreen(Screen parent) {

        TWConfigUtil config = TWConfigUtil.getConfig();

        ConfigBuilder configBuilder = ConfigBuilder.create();
        ConfigCategory clientcategory = configBuilder.getOrCreateCategory(Component.translatable("Client Config"));

        BooleanListEntry screenshotOnExit = configBuilder.entryBuilder().startBooleanToggle(
                Component.translatable("3D screenshot on world exit"),
                config.ScreenshotOnExit
        ).setDefaultValue(false).build();

        clientcategory.addEntry(screenshotOnExit);
        return configBuilder.setParentScreen(parent)
                .setSavingRunnable(() -> {
                    TWConfigUtil newConfig = new TWConfigUtil();
                    newConfig.ScreenshotOnExit = screenshotOnExit.getValue();
                    newConfig.saveConfigFile();
                    newConfig.onConfigChanged();
                })
                .build();

    }

}
