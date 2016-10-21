package sblectric.openharvest.events;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import sblectric.openharvest.config.HarvestConfig;
import sblectric.openharvest.util.HarvestUtils;

public class HarvestEvents {
	
	private int blocks;
	private boolean skip;

	/** Harvest crops on this event */
	@SubscribeEvent
	public void onHarvestCrops(PlayerInteractEvent.RightClickBlock event) {
		World world = event.getWorld();
		if(world.isRemote) return; // do nothing on client thread

		BlockPos pos = event.getPos();
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		EntityPlayerMP ply = (EntityPlayerMP)event.getEntityPlayer();

		// make sure the off-hand is active and right-clicking a crop block
		if(event.getHand() == EnumHand.OFF_HAND && block instanceof BlockCrops) {
			
			// make sure the block isn't blacklisted
			ResourceLocation name = block.getRegistryName();
			String modid = name.getResourceDomain();
			if(!HarvestConfig.modBlacklist.contains(modid) && !HarvestConfig.blockBlacklist.contains(name.toString())) {
	
				// get the age type
				PropertyInteger age = null;
				int maxAge = -1;
				for(IProperty prop : state.getPropertyNames()) {
					if(prop instanceof PropertyInteger && prop.getName().equals("age")) {
						age = (PropertyInteger)prop;
						maxAge = HarvestUtils.max(age.getAllowedValues());
						break;
					}
				}
	
				// make sure the age property is valid
				if(age != null && maxAge > -1) {
					if(state.getValue(age) == maxAge) {
						for(ItemStack s : block.getDrops(world, pos, state, 0)) {
							// check if the item can be added to the player inventory
							world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F,
									((world.rand.nextFloat() - world.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
							if(!ply.inventory.addItemStackToInventory(s)) {
								Block.spawnAsEntity(world, pos, s); // spawn it in the world if not
							}
						}
						world.setBlockState(pos, block.getDefaultState());
					}
				}
				
			}
		}
	}
	
	/** Harvest a log */
	@SubscribeEvent
	public void onHarvestLog(BlockEvent.HarvestDropsEvent event) {
		World world = event.getWorld();
		if(world.isRemote || !HarvestConfig.doTreeChop || skip || event.getHarvester() == null) return; // do nothing here
		
		EntityPlayerMP player = (EntityPlayerMP)event.getHarvester();
		BlockPos pos = event.getPos();
		IBlockState state = event.getState();
		
		// ignore blacklisted mods and logs
		if(HarvestConfig.modLogBlacklist.contains(state.getBlock().getRegistryName().getResourceDomain())) return;
		if(HarvestConfig.logBlacklist.contains(state.getBlock().getRegistryName().toString())) return;
		
		// make sure the tool is a valid axe and the player isn't crouching
		ItemStack heldStack = player.getHeldItemMainhand();
		if(heldStack != null && heldStack.getItem() instanceof ItemTool && !player.isSneaking()) {
			ItemTool tool = (ItemTool)heldStack.getItem();
			for(String tc : tool.getToolClasses(heldStack)) {
				if(tc.equals("axe")) {
					// now iterate through the drops to make sure the block is an oredict log
					for(ItemStack s : event.getDrops()) {
						for(int id : OreDictionary.getOreIDs(s)) {
							if(OreDictionary.getOreName(id).equals("logWood")) {
								// now perform the treecapitation while checking for stack overflow
								try {
									clearInvalidBlocks();
									blocks = 0;
									skip = true;
									treecapitate(world, pos, state, player, heldStack); // c h o p
								} catch (StackOverflowError e) {}
								clearInvalidBlocks();
								skip = false;
								return; // nothing more to do here
							}
						}
					}
					
				}
			}
		}
		
	}
	
	/** Perform the treecapitation at the current position in the world (recursive function) */
	private void treecapitate(World world, BlockPos pos, IBlockState state, EntityPlayerMP player, ItemStack tool) {
		
		// don't execute anything more with a broken tool
		if(tool == null || tool.stackSize <= 0) return;
		
		// don't exceed the max amount set in the config
		blocks++; if(blocks > HarvestConfig.maxLogsAtOnce) return;
		
		// mark this position as treecapitated
		setBlockInvalid(pos);
		
		// check the surrounding blocks and try to break them
		for(EnumFacing f : EnumFacing.values()) {
			for(EnumFacing f2 : EnumFacing.values()) { // works on diagonals and 1-block spaces
				for(int i = 0; i <= 1; i++) {
					BlockPos newPos = pos.offset(f).offset(f2, i);
					if(isBlockValid(world, newPos, state)) treecapitate(world, newPos, state, player, tool);
				}
			}
		}
		
		// finally, break the current block and damage the axe
		world.destroyBlock(pos, true);
		tool.damageItem(1, player);
	}
	
	private List<BlockPos> invalidBlocks;
	
	private void clearInvalidBlocks() {
		invalidBlocks = new LinkedList();
	}
	
	private void setBlockInvalid(BlockPos pos) {
		invalidBlocks.add(pos);
	}

	/** Can the block be treecapitated? */
	private boolean isBlockValid(World world, BlockPos pos, IBlockState stateCompare) {
		return !invalidBlocks.contains(pos) && (stateCompare.getBlock() == world.getBlockState(pos).getBlock());
	}	
	
}

