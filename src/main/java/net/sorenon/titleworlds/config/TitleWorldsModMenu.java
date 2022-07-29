package net.sorenon.titleworlds.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class TitleWorldsModMenu implements ModMenuApi {

    public TitleWorldsModMenu() {
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GlobalConfigGUI::createConfigScreen;
    }

}
