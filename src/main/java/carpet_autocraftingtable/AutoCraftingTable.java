package carpet_autocraftingtable;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class AutoCraftingTable implements CarpetExtension
{
    public static void noop() { }
    static {
        CarpetServer.manageExtension(new AutoCraftingTable());
    }

    @Override
    public void onGameStarted() {
        // let's /carpet handle our few simple settings
        CarpetServer.settingsManager.parseSettingsClass(AutoCraftingTableSettings.class);
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        InputStream langFile = AutoCraftingTable.class.getClassLoader().getResourceAsStream("assets/carpet-autocraftingtable/lang/%s.json".formatted(lang));
        if (langFile == null) {
            return Collections.emptyMap();
        }
        String jsonData;
        try {
            jsonData = IOUtils.toString(langFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
        return new GsonBuilder().create().fromJson(jsonData, new TypeToken<Map<String, String>>() {}.getType());
    }
}
