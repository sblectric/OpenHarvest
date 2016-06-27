package sblectric.openharvest.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import net.minecraftforge.common.config.Configuration;

/** The mod is now configurable! */
public class HarvestConfig {
	
	public static Configuration config;
	
	public static void loadConfig(File f) {
		config = new Configuration(f);
		getConfig();
	}
	
	public static List<String> modBlacklist;
	public static List<String> blockBlacklist;
 	
	private static void getConfig() {
		config.load();
		
		// mod integration
		modBlacklist = Arrays.asList(config.getStringList("Mod Blacklist", "integration", new String[0], 
				"Mods (by mod id) with plants that OpenHarvest should ignore"));
		blockBlacklist = Arrays.asList(config.getStringList("Plant Blacklist", "integration", new String[0], 
				"Specific plants (by registry name) that OpenHarvest should ignore"));
	
		if(config.hasChanged()) config.save();
	}

}
