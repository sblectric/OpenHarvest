package sblectric.openharvest.events;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import sblectric.openharvest.config.HarvestConfig;
import sblectric.openharvest.util.HarvestUtils;

/** All harvest-related events for the mod */
public class HarvestEvents {

	private boolean skip;
	private int blocks;
	private int leaves;

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
	
	/** Is the player's sneak mode acceptable? */
	private boolean playerSneakModeValid(EntityPlayerMP player) {
		return HarvestConfig.shiftMode ? player.isSneaking() : !player.isSneaking();
	}

	/** Can the player treecapitate based on the block selected? */
	private boolean canTreecapitate(EntityPlayerMP player, BlockPos pos, IBlockState blockState) {
		// can't harvest blacklisted mods and logs
		if(HarvestConfig.modLogBlacklist.contains(blockState.getBlock().getRegistryName().getResourceDomain())) return false;
		if(HarvestConfig.logBlacklist.contains(blockState.getBlock().getRegistryName().toString())) return false;
		
		// make sure the tool is a valid axe and such
		ItemStack heldStack = player.getHeldItemMainhand();
		if(heldStack != null && heldStack.getItem() instanceof ItemTool && playerSneakModeValid(player)) {
			ItemTool tool = (ItemTool)heldStack.getItem();
			for(String tc : tool.getToolClasses(heldStack)) {
				if(tc.equals("axe")) {
					// now iterate through the drops to make sure the block is an oredict log
					for(ItemStack s : blockState.getBlock().getDrops(player.worldObj, pos, blockState, 0)) {
						for(int id : OreDictionary.getOreIDs(s)) {
							if(OreDictionary.getOreName(id).equals("logWood")) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/** Try to harvest a log, and give the player Mining Fatigue in treecapitate mode */
	@SubscribeEvent
	public void onStartHarvestLog(PlayerEvent.BreakSpeed event) {
		World world = event.getEntity().worldObj;
		if(world.isRemote || !HarvestConfig.doTreeChop) return; // nothing clientside
		
		EntityPlayerMP player = (EntityPlayerMP)event.getEntityPlayer();
		
		if(player != null && canTreecapitate(player, event.getPos(), event.getState())) {
			player.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 5, HarvestConfig.harvestSpeed, true, false));
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

		// make sure treecapitation is possible
		if(canTreecapitate(player, pos, state)) {
			// now perform the treecapitation while checking for stack overflow
			try {
				clearInvalidBlocks();
				blocks = leaves = 0;
				skip = true;
				treecapitate(world, pos, state, player, player.getHeldItemMainhand()); // c h o p
			} catch (StackOverflowError e) {}
			clearInvalidBlocks();
			skip = false;
			return; // nothing more to do here
		}

	}

	/** Perform the treecapitation at the current position in the world (recursive function) */
	private void treecapitate(World world, BlockPos pos, IBlockState stateFirstBlock, EntityPlayerMP player, ItemStack tool) {

		// don't execute anything more with a broken tool
		if(tool == null || tool.stackSize <= 0) return;

		// don't exceed the max amounts set in the config
		if(blocks > HarvestConfig.maxLogsAtOnce || leaves > HarvestConfig.maxLeavesAtOnce) return;

		// mark this position as searched
		setBlockInvalid(pos);

		// check the surrounding blocks and try to break them
		for(EnumFacing f : EnumFacing.values()) {
			for(EnumFacing f2 : EnumFacing.values()) { // works on diagonals and 1-block spaces
				for(int i = 0; i <= 1; i++) {
					if(tool == null || tool.stackSize <= 0) return; // check again here
					BlockPos newPos = pos.offset(f).offset(f2, i);
					if(isBlockSearchable(world, newPos, stateFirstBlock)) treecapitate(world, newPos, stateFirstBlock, player, tool);
				}
			}
		}

		// finally, break the current block and damage or destroy the axe
		if(isBlockHarvestable(world, pos, stateFirstBlock)) {
			world.destroyBlock(pos, true);
			tool.damageItem(1, player);
			if(tool.stackSize <= 0) player.setHeldItem(EnumHand.MAIN_HAND, null);
			blocks++; // increase the broken count
		} else if(world.getBlockState(pos).getBlock().isLeaves(world.getBlockState(pos), world, pos)) {
			leaves++; // increase the leaf counter if the block is leaves
		}
	}

	private List<BlockPos> invalidBlocks;

	/** Clears the invalid block list */
	private void clearInvalidBlocks() {
		invalidBlocks = new LinkedList();
	}

	/** Sets the block as searched */
	private void setBlockInvalid(BlockPos pos) {
		invalidBlocks.add(pos);
	}

	/** Is the specified block position valid? */
	private boolean isBlockValid(BlockPos pos) {
		return !invalidBlocks.contains(pos);
	}

	/** Can the block be used as a valid position for further searches? (Similar logs and leaf types are considered) */
	private boolean isBlockSearchable(World world, BlockPos pos, IBlockState stateCompare) {
		IBlockState currentState = world.getBlockState(pos);
		return isBlockValid(pos) && (isBlockHarvestable(world, pos, stateCompare) || currentState.getBlock().isLeaves(currentState, world, pos));
	}

	/** Can the block be harvested and use axe durability? (Only log block matches are valid) */
	private boolean isBlockHarvestable(World world, BlockPos pos, IBlockState stateCompare) {
		return stateCompare.getBlock() == world.getBlockState(pos).getBlock();
	}

}

