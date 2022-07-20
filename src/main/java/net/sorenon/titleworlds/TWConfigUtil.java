package net.sorenon.titleworlds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class TWConfigUtil {

    // Config system borrowed from https://github.com/qouteall/ImmersivePortalsMod and adapted here thank you

    public boolean ScreenshotOnExit = false;

    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static File getGameDir() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        return gameDir.toFile();
    }

    public static TWConfigUtil getConfig() {

        File ConfigFile = getConfigFileLocation();

        return readConfigFromFile(ConfigFile);

    }

    public static File getConfigFileLocation() {
        return new File(
                getGameDir(), "config/titleworlds.json"
        );
    }

    public static TWConfigUtil readConfigFromFile(File configFile) {
        if (configFile.exists()) {
            try {
                String data = Files.lines(configFile.toPath()).collect(Collectors.joining());
                TWConfigUtil result = gson.fromJson(data, TWConfigUtil.class);

                if (result == null) {
                    return new TWConfigUtil();
                }

                return result;
            }
            catch (Throwable e) {
                e.printStackTrace();
                return new TWConfigUtil();
            }
        }
        else {
            TWConfigUtil configObj = new TWConfigUtil();
            configObj.saveConfigFile();
            return configObj;
        }

    }

    public void saveConfigFile() {
        File configFile  = getConfigFileLocation();

        try {
            configFile.getParentFile().mkdir();
            configFile.createNewFile();
            FileWriter fw = new FileWriter(configFile);

            fw.write(gson.toJson(this));
            fw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onConfigChanged() {
        TWConfigGlobal.ScreenshotOnExit = ScreenshotOnExit;
    }

}
