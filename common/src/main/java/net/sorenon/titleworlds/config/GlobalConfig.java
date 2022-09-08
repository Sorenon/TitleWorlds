package net.sorenon.titleworlds.config;

import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.sorenon.titleworlds.Platform;

import java.io.File;

public class GlobalConfig {

    public boolean enabled = true;

    public boolean screenshotOnExit = false;

    public boolean useTitleWorldOverride = false;

    public int titleWorldOverride = 0;

    public boolean profiling = false;

    public boolean reloadButton = true;

    public int preloadChunksRadius = 1;

    public String[] filteredTitleWorlds = new String[0];

    public GlobalConfig save() {
        File configFile = ConfigUtil.getConfigFileLocation();

        //noinspection ResultOfMethodCallIgnored
        configFile.getParentFile().mkdirs();

        var config = FileConfig.of(configFile);
        ObjectConverter objectConverter = new ObjectConverter();
        objectConverter.toConfig(this, config);
        config.save();
        return this;
    }
}
