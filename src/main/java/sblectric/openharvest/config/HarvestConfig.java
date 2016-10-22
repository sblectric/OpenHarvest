package sblectric.openharvest.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import net.minecraftforge.common.config.Configuration;
import sblectric.openharvest.ref.RefStrings;

/** The mod is now configurable! */
public class HarvestConfig {
	
	public static Configuration config;
	
	public static void loadConfig(File f) {
		config = new Configuration(f);
		getConfig();
	}
	
	public static List<String> modBlacklist;
	public static List<String> blockBlacklist;
	public static boolean doTreeChop;
	public static boolean shiftMode;
	public static int harvestSpeed;
	public static int maxLogsAtOnce;
	public static int maxLeavesAtOnce;
	public static List<String> modLogBlacklist;
	public static List<String> logBlacklist;
 	
	private static void getConfig() {
		config.load();
		
		// plants
		modBlacklist = Arrays.asList(config.getStringList("Mod Blacklist", "plants", new String[0], 
				"Mods (by mod id) with plants that " + RefStrings.NAME + " should ignore"));
		blockBlacklist = Arrays.asList(config.getStringList("Plant Blacklist", "plants", new String[0], 
				"Specific plants (by registry name) that " + RefStrings.NAME + " should ignore"));
		
		// logs
		doTreeChop = config.getBoolean("Treecapitator mode", "logs", true, 
				"Whether or not to enable Veinminer-esque log breaking mode");
		shiftMode = config.getBoolean("Sneaking mode", "logs", true,
				"If false, only enable Treecapitator mode when the player *isn't* sneaking. If true, do the opposite");
		harvestSpeed = config.getInt("Treecapitator speed", "logs", 1, 0, 5,
				"When treecapitating, the player is given Mining Fatigue with this modifier");
		maxLogsAtOnce = config.getInt("Maximum logs", "logs", 256, 1, Integer.MAX_VALUE, 
				"Maximum amount of logs to chop at once");
		maxLeavesAtOnce = config.getInt("Maximum leaves", "logs", 64, 1, Integer.MAX_VALUE, 
				"Maximum amount of leaves to search through at once");
		modLogBlacklist = Arrays.asList(config.getStringList("Mod Blacklist", "logs", new String[0], 
				"Mods (by mod id) with logs that " + RefStrings.NAME + " should ignore"));
		logBlacklist = Arrays.asList(config.getStringList("Log Blacklist", "logs", new String[0], 
				"Specific logs (by registry name) that " + RefStrings.NAME + " should ignore"));
	
		if(config.hasChanged()) config.save();
	}

}
