package carpet_autocraftingtable;

import carpet.settings.Rule;

import static carpet.settings.RuleCategory.CREATIVE;

/**
 * Here is your example Settings class you can plug to use carpetmod /carpet settings command
 */
public class AutoCraftingTableSettings
{
    @Rule(desc = "Auto-crafting table", category = {CREATIVE, "extras"})
    public static boolean autoCraftingTable = false;

}
