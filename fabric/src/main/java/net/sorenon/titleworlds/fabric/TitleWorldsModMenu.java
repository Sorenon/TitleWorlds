package net.sorenon.titleworlds.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.sorenon.titleworlds.config.GlobalConfigGUI;

public class TitleWorldsModMenu implements ModMenuApi {

    public TitleWorldsModMenu() {
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GlobalConfigGUI::createConfigScreen;
    }
}
