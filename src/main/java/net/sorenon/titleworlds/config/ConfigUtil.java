package net.sorenon.titleworlds.config;

import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

public class ConfigUtil {

    public static GlobalConfig loadConfig() {
        return readConfigFromFile(getConfigFileLocation());
    }

    public static File getConfigFileLocation() {
        return FabricLoader.getInstance().getGameDir().resolve("config/titleworlds.toml").toFile();
    }

    public static GlobalConfig readConfigFromFile(File configFile) {
        if (configFile.exists()) {
            try {
                var config = FileConfig.of(configFile);
                config.load();
                ObjectConverter converter = new ObjectConverter();
                return converter.toObject(config, GlobalConfig::new);
            } catch (Throwable e) {
                e.printStackTrace();
                return new GlobalConfig().save();
            }
        } else {
            return new GlobalConfig().save();
        }
    }
}
