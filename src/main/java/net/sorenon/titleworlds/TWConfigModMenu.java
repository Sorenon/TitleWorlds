package net.sorenon.titleworlds;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class TWConfigModMenu  implements ModMenuApi {

    public TWConfigModMenu() {}

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TWConfigGUI::createConfigScreen;
    }

}
