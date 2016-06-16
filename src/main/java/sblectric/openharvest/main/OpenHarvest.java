package sblectric.openharvest.main;

import java.util.Map;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;
import sblectric.openharvest.events.HarvestEvents;
import sblectric.openharvest.ref.RefStrings;

/** The Open Harvest mod class *
  * Works server-side only! */
@Mod(modid = RefStrings.MODID, name = RefStrings.NAME, version = RefStrings.VERSION, acceptedMinecraftVersions = RefStrings.MCVERSIONS)
public class OpenHarvest {

	/** Initialize the event handler */
	@EventHandler
	public void init(FMLInitializationEvent init) {
		MinecraftForge.EVENT_BUS.register(new HarvestEvents());
	}
	
	/** make sure to accept clients without this mod! */
	@NetworkCheckHandler
	public boolean networkCheck(Map<String, String> map, Side side) {
		return true;
	}
}

