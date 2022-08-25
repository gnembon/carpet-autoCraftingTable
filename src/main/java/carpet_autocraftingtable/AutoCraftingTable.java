package carpet_autocraftingtable;

import carpet.CarpetExtension;
import carpet.CarpetServer;

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
}
