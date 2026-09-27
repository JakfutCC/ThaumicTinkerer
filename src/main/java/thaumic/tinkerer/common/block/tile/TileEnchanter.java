/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [14 Sep 2013, 01:07:25 (GMT)]
 */
package thaumic.tinkerer.common.block.tile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

import org.apache.commons.lang3.ArrayUtils;

import appeng.api.movable.IMovableTile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumic.tinkerer.common.core.helper.Tuple4Int;
import thaumic.tinkerer.common.core.helper.WorldBounds;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibFeatures;

@Optional.Interface(iface = "appeng.api.movable.IMovableTile", modid = "appliedenergistics2")
public class TileEnchanter extends TileEntity implements ISidedInventory, IMovableTile {

    private static final String TAG_ENCHANTS = "enchantsIntArray";
    private static final String TAG_LEVELS = "levelsIntArray";
    private static final String TAG_TOTAL_ASPECTS = "totalAspects";
    private static final String TAG_CURRENT_ASPECTS = "currentAspects";
    private static final String TAG_WORKING = "working";
    private static final int PILLAR_NOT_FOUND = -1;
    private static final int PILLAR_UNLOADED = -2;

    public List<Integer> enchantments = new ArrayList<>();
    public List<Integer> levels = new ArrayList<>();

    public AspectList totalAspects = new AspectList();
    public AspectList currentAspects = new AspectList();

    public boolean working = false;
    ItemStack[] inventorySlots = new ItemStack[2];
    private List<Tuple4Int> pillars = new ArrayList<>();

    public void clearEnchants() {
        enchantments.clear();
        levels.clear();
    }

    public void appendEnchant(int enchant) {
        enchantments.add(enchant);
    }

    public void appendLevel(int level) {
        levels.add(level);
    }

    public void removeEnchant(int index) {
        enchantments.remove(index);
    }

    public void removeLevel(int index) {
        levels.remove(index);
    }

    public void setEnchant(int index, int enchant) {
        enchantments.set(index, enchant);
    }

    public void setLevel(int index, int level) {
        levels.set(index, level);
    }

    @Override
    public void updateEntity() {
        if (getStackInSlot(0) == null) {
            enchantments.clear();
            levels.clear();
        }

        if (working) {
            ItemStack tool = getStackInSlot(0);
            if (tool == null) {
                working = false;
                return;
            }

            if (!checkPillars()) return;

            enchantItem: {
                for (Aspect aspect : LibFeatures.PRIMAL_ASPECTS) {
                    int currentAmount = currentAspects.getAmount(aspect);
                    int totalAmount = totalAspects.getAmount(aspect);

                    if (currentAmount < totalAmount) break enchantItem;
                }

                working = false;
                currentAspects = new AspectList();
                totalAspects = new AspectList();

                for (int i = 0; i < enchantments.size(); i++) {
                    int enchant = enchantments.get(i);
                    int level = levels.get(i);

                    if (!worldObj.isRemote) tool.addEnchantment(Enchantment.enchantmentsList[enchant], level);
                }

                enchantments.clear();
                levels.clear();
                worldObj.playSoundEffect(xCoord, yCoord, zCoord, "thaumcraft:wand", 1F, 1F);
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                return;
            }

            ItemStack wand = getStackInSlot(1);

            if (wand != null && wand.getItem() instanceof ItemWandCasting
                    && !((ItemWandCasting) wand.getItem()).isStaff(wand)) {
                ItemWandCasting wandItem = (ItemWandCasting) wand.getItem();
                AspectList wandAspects = wandItem.getAllVis(wand);

                int missing, onWand;
                List<Aspect> aspectsThatCanGet = new ArrayList<>();

                for (Aspect aspect : LibFeatures.PRIMAL_ASPECTS) {
                    missing = totalAspects.getAmount(aspect) - currentAspects.getAmount(aspect);
                    onWand = wandAspects.getAmount(aspect);

                    if (missing > 0 && onWand >= 100) aspectsThatCanGet.add(aspect);
                }

                int i = aspectsThatCanGet.isEmpty() ? 0 : worldObj.rand.nextInt(aspectsThatCanGet.size());
                Aspect aspect = aspectsThatCanGet.isEmpty() ? null : aspectsThatCanGet.get(i);

                if (aspect != null) {
                    this.consumeAllVisCrafting(wand, null, new AspectList().add(aspect, 1), true, wandItem);
                    currentAspects.add(aspect, 1);
                    Tuple4Int p = pillars.get(i);
                    if (worldObj.rand.nextBoolean()) {

                        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
                            Thaumcraft.proxy.essentiaTrailFx(
                                    getWorldObj(),
                                    p.i1,
                                    p.i4,
                                    p.i3,
                                    xCoord,
                                    yCoord,
                                    zCoord,
                                    4,
                                    aspect.getColor(),
                                    worldObj.rand.nextFloat() * 0.5F + 0.5F);
                            Thaumcraft.proxy.blockRunes(
                                    worldObj,
                                    p.i1,
                                    p.i4 - 0.75,
                                    p.i3,
                                    0.3F + worldObj.rand.nextFloat() * 0.7F,
                                    0.0F,
                                    0.3F + worldObj.rand.nextFloat() * 0.7F,
                                    15,
                                    worldObj.rand.nextFloat() / 8F);
                            Thaumcraft.proxy.blockRunes(
                                    worldObj,
                                    xCoord,
                                    yCoord + 0.25,
                                    zCoord,
                                    0.3F + worldObj.rand.nextFloat() * 0.7F,
                                    0.0F,
                                    0.3F + worldObj.rand.nextFloat() * 0.7F,
                                    15,
                                    worldObj.rand.nextFloat() / 8F);
                        }
                        if (worldObj.rand.nextInt(5) == 0)
                            worldObj.playSoundEffect(p.i1, p.i2, p.i3, "thaumcraft:egidle", 0.5F, 1F);
                    }
                }
            }
        }
    }

    public boolean consumeAllVisCrafting(ItemStack is, EntityPlayer player, AspectList aspects, boolean doit,
            ItemWandCasting wandItem) {
        if (aspects != null && aspects.size() != 0) {
            AspectList aspectList = new AspectList();
            Aspect[] aspectArray = aspects.getAspects();
            int arrayLength = aspectArray.length;

            for (Aspect aspect : aspectArray) {
                int cost = aspects.getAmount(aspect) * 100;
                aspectList.add(aspect, cost);
            }
            aspects = aspectList;
            if (aspects != null && aspects.size() != 0) {
                AspectList nl = new AspectList();
                Aspect[] arr$ = aspects.getAspects();
                int len$ = arr$.length;

                int i$;
                Aspect aspect;
                for (i$ = 0; i$ < len$; ++i$) {
                    aspect = arr$[i$];
                    int cost = aspects.getAmount(aspect);
                    cost = (int) ((float) cost * wandItem.getConsumptionModifier(is, player, aspect, true));
                    nl.add(aspect, cost);
                }

                arr$ = nl.getAspects();
                len$ = arr$.length;

                for (i$ = 0; i$ < len$; ++i$) {
                    aspect = arr$[i$];
                    if (wandItem.getVis(is, aspect) < nl.getAmount(aspect)) {
                        return false;
                    }
                }

                if (doit && FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
                    arr$ = nl.getAspects();
                    len$ = arr$.length;

                    for (i$ = 0; i$ < len$; ++i$) {
                        aspect = arr$[i$];
                        wandItem.storeVis(is, aspect, wandItem.getVis(is, aspect) - nl.getAmount(aspect));
                    }
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (!worldObj.isRemote && !working) {
            enchantments.clear();
            levels.clear();
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public boolean checkPillars() {
        if (pillars.isEmpty()) {
            int result = assignLoadedPillars();
            if (result != 0) {
                if (result == PILLAR_NOT_FOUND) {
                    working = false;
                    currentAspects = new AspectList();
                }
                return false;
            }
            return true;
        }

        for (int i = 0; i < pillars.size(); i++) {
            Tuple4Int pillar = pillars.get(i);
            int pillarHeight = findPillarHeight(pillar.i1, pillar.i2, pillar.i3);
            if (pillarHeight == PILLAR_UNLOADED) return false;
            if (pillarHeight == PILLAR_NOT_FOUND) {
                pillars.clear();
                return checkPillars();
            } else pillar.i4 = pillar.i2 + pillarHeight;
        }

        return true;
    }

    public boolean assignPillars() {
        return assignLoadedPillars() != 0;
    }

    private int assignLoadedPillars() {
        int y = yCoord;
        boolean unavailable = false;
        for (int x = xCoord - 4; x <= xCoord + 4; x++) for (int z = zCoord - 4; z <= zCoord + 4; z++) {
            int height = findPillarHeight(x, y, z);
            if (height == PILLAR_UNLOADED) unavailable = true;
            if (height >= 0) pillars.add(new Tuple4Int(x, y, z, y + height));

            if (pillars.size() == 6) return 0;
        }

        pillars.clear();
        return unavailable ? PILLAR_UNLOADED : PILLAR_NOT_FOUND;
    }

    public int findPillar(int x, int y, int z) {
        int height = findPillarHeight(x, y, z);
        return height < 0 ? -1 : y + height;
    }

    // Use a relative height internally: a valid pillar top can be at Y=-1.
    private int findPillarHeight(int x, int y, int z) {
        for (int i = 0; i <= 12; i++) {
            long blockY = (long) y + i;
            if (!WorldBounds.contains(worldObj, blockY)) return PILLAR_NOT_FOUND;
            int targetY = (int) blockY;
            if (WorldBounds.isCubic(worldObj) && !worldObj.checkChunksExist(x, targetY, z, x, targetY, z))
                return PILLAR_UNLOADED;

            Block id = worldObj.getBlock(x, targetY, z);
            int meta = worldObj.getBlockMetadata(x, targetY, z);
            if (id == ConfigBlocks.blockCosmeticSolid && meta == 0) {
                continue;
            }
            if (id == ConfigBlocks.blockAiry && meta == 1) {
                return i >= 2 ? i : PILLAR_NOT_FOUND;
            }

            return PILLAR_NOT_FOUND;
        }
        return PILLAR_NOT_FOUND;
    }

    public void updateAspectList() {
        totalAspects = new AspectList();
        for (int i = 0; i < enchantments.size(); i++) {
            int enchant = enchantments.get(i);
            int level = levels.get(i);

            AspectList aspects = EnchantmentManager.enchantmentData.get(enchant).get(level).aspects;
            for (Aspect aspect : aspects.getAspectsSorted()) totalAspects.add(aspect, aspects.getAmount(aspect));
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound par1NBTTagCompound) {
        super.readFromNBT(par1NBTTagCompound);

        readCustomNBT(par1NBTTagCompound);
    }

    @Override
    public void writeToNBT(NBTTagCompound par1NBTTagCompound) {
        super.writeToNBT(par1NBTTagCompound);

        writeCustomNBT(par1NBTTagCompound);
    }

    public void readCustomNBT(NBTTagCompound par1NBTTagCompound) {
        working = par1NBTTagCompound.getBoolean(TAG_WORKING);
        currentAspects.readFromNBT(par1NBTTagCompound.getCompoundTag(TAG_CURRENT_ASPECTS));
        totalAspects.readFromNBT(par1NBTTagCompound.getCompoundTag(TAG_TOTAL_ASPECTS));

        enchantments.clear();
        for (int i : par1NBTTagCompound.getIntArray(TAG_ENCHANTS)) enchantments.add(i);
        levels.clear();
        for (int i : par1NBTTagCompound.getIntArray(TAG_LEVELS)) levels.add(i);

        NBTTagList var2 = par1NBTTagCompound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        inventorySlots = new ItemStack[getSizeInventory()];
        for (int var3 = 0; var3 < var2.tagCount(); ++var3) {
            NBTTagCompound var4 = var2.getCompoundTagAt(var3);
            byte var5 = var4.getByte("Slot");
            if (var5 >= 0 && var5 < inventorySlots.length) inventorySlots[var5] = ItemStack.loadItemStackFromNBT(var4);
        }
    }

    public void writeCustomNBT(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.setIntArray(TAG_LEVELS, ArrayUtils.toPrimitive(levels.toArray(new Integer[levels.size()])));

        par1NBTTagCompound.setIntArray(
                TAG_ENCHANTS,
                ArrayUtils.toPrimitive(enchantments.toArray(new Integer[enchantments.size()])));

        NBTTagCompound totalAspectsCmp = new NBTTagCompound();
        totalAspects.writeToNBT(totalAspectsCmp);

        NBTTagCompound currentAspectsCmp = new NBTTagCompound();
        currentAspects.writeToNBT(currentAspectsCmp);

        par1NBTTagCompound.setBoolean(TAG_WORKING, working);
        par1NBTTagCompound.setTag(TAG_TOTAL_ASPECTS, totalAspectsCmp);
        par1NBTTagCompound.setTag(TAG_CURRENT_ASPECTS, currentAspectsCmp);
        NBTTagList var2 = new NBTTagList();
        for (int var3 = 0; var3 < inventorySlots.length; ++var3) {
            if (inventorySlots[var3] != null) {
                NBTTagCompound var4 = new NBTTagCompound();
                var4.setByte("Slot", (byte) var3);
                inventorySlots[var3].writeToNBT(var4);
                var2.appendTag(var4);
            }
        }
        par1NBTTagCompound.setTag("Items", var2);
    }

    @Override
    public int getSizeInventory() {
        return inventorySlots.length;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return inventorySlots[i];
    }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        if (inventorySlots[i] != null) {
            ItemStack stackAt;

            if (inventorySlots[i].stackSize <= j) {
                stackAt = inventorySlots[i];
                inventorySlots[i] = null;
                return stackAt;
            } else {
                stackAt = inventorySlots[i].splitStack(j);

                if (inventorySlots[i].stackSize == 0) inventorySlots[i] = null;

                return stackAt;
            }
        }

        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i) {
        return getStackInSlot(i);
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        inventorySlots[i] = itemstack;
    }

    @Override
    public String getInventoryName() {
        return LibBlockNames.ENCHANTER;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                && entityplayer.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return false;
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        readCustomNBT(pkt.func_148857_g());
    }

    @Override
    public S35PacketUpdateTileEntity getDescriptionPacket() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        writeCustomNBT(nbttagcompound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, -999, nbttagcompound);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int var1) {
        return new int[0];
    }

    @Override
    public boolean canInsertItem(int i, ItemStack itemstack, int j) {
        return false;
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemstack, int j) {
        return false;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public boolean prepareToMove() {
        return true;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public void doneMoving() {}
}
