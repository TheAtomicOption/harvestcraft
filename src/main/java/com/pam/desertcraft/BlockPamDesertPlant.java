package com.pam.desertcraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.event.ForgeEventFactory;


public class BlockPamDesertPlant extends BlockBush {
    public static final Map<String, List<ItemStack>> drops = new HashMap<String, List<ItemStack>>();

    public enum Region {
        PLAINS(EnumPlantType.Plains, Blocks.GRASS, Blocks.DIRT),
        DESERT(EnumPlantType.Desert, Blocks.SAND, Blocks.HARDENED_CLAY, Blocks.STAINED_HARDENED_CLAY);

        private final EnumPlantType plantType;

        private final Block[] soilBlocks;

        Region(EnumPlantType plantType, Block... soilBlocks) {
            this.plantType = plantType;
            this.soilBlocks = soilBlocks;
        }

        public EnumPlantType getPlantType() {
            return plantType;
        }

        public Block[] getSoilBlocks() {
            return soilBlocks;
        }
    }

    private final String type;
    private final String name;
    private final Region region;

    public BlockPamDesertPlant(String type, Region region) {
        super(Material.GRASS);
        this.type = type;
        this.name = type.toLowerCase();
        this.region = region;

        setCreativeTab(desertcraft.tabDesertcraft);
        this.setSoundType(SoundType.PLANT);
        this.setTickRandomly(true);

        BlockRegistry.registerBlock(name, this);
    }

    
    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return worldIn.isAirBlock(pos) && checkSoilBlock(worldIn, pos);
    }

    private boolean checkSoilBlock(World world, BlockPos pos) {
        final Block soilBlock = world.getBlockState(pos.down()).getBlock();

        for (Block acceptableBlock : region.getSoilBlocks()) {
            if (acceptableBlock == soilBlock) return true;
        }
        return false;
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
        return region.getPlantType();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    }

    public String getName() {
        return name;
    }

    /**
     * Overriding this in order to allow dropping the garden when sneaking.
     */
    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack) {
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.025F);

        if (player.isSneaking() || canSilkHarvest(worldIn, pos, state, player) && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0) {
            List<ItemStack> items = new ArrayList<ItemStack>();
            ItemStack itemstack = this.getSilkTouchDrop(state);

            if (itemstack != null) {
                items.add(itemstack);
            }

            ForgeEventFactory.fireBlockHarvesting(items, worldIn, pos, state, 0, 1.0f, true, player);
            for (ItemStack item : items) {
                spawnAsEntity(worldIn, pos, item);
            }
        } else {
            harvesters.set(player);
            final int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack);
            dropBlockAsItem(worldIn, pos, state, i);
            harvesters.set(null);
        }
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        List<ItemStack> newStack = new ArrayList<ItemStack>();
        List<ItemStack> ourDrops = drops.get(type);
        Collections.shuffle(ourDrops);

        int len = Math.min(1, ourDrops.size());

        for (int i = 0; i < len; i++) {
            ItemStack drop = ourDrops.get(i);

            // This should never happen, but check it anyway...
            if (drop == null) {
                System.err.println("Tried to get a null item for garden '" + type + "'.");
                continue;
            }

            // Add it to our drops...
            newStack.add(drop.copy());
        }
        return newStack;
    }

    @Override
    public IBlockState getPlant(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != this) return getDefaultState();
        return state;
    }
}
