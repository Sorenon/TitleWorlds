package net.sorenon.titleworlds;

import net.sorenon.titleworlds.config.ConfigUtil;
import net.sorenon.titleworlds.config.GlobalConfig;

public class TitleWorldsMod {
    public static final String MOD_ID = "titleworlds";

    public static GlobalConfig CONFIG = ConfigUtil.loadConfig();

    public static void init() {
//        CONFIG = ConfigUtil.loadConfig();
    }

    public static void load() {

    }
}
