/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Nov 24, 2013, 6:40:49 PM (GMT)]
 */
package thaumic.tinkerer.common.block.tile.transvector;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import thaumic.tinkerer.common.block.tile.TileCamo;
import thaumic.tinkerer.common.core.helper.WorldBounds;

public abstract class TileTransvector extends TileCamo {

    private static final String TAG_X_TARGET = "xt";
    private static final String TAG_Y_TARGET = "yt";
    private static final String TAG_Z_TARGET = "zt";
    private static final String TAG_CHEATY_MODE = "cheatyMode";
    private static final String TAG_HAS_TARGET = "hasTarget";

    public int x = 0, y = -1, z = 0;
    private boolean cheaty;
    private boolean targetPresent;

    public final boolean hasTarget() {
        // Retain legacy direct assignments of the public non-sentinel coordinates.
        return targetPresent || y != -1;
    }

    public final void setTarget(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        targetPresent = true;
        markDirty();
    }

    private void clearTarget() {
        targetPresent = false;
        y = -1;
        markDirty();
    }

    public final boolean isTargetLoaded() {
        return hasTarget() && WorldBounds.isLoaded(worldObj, x, y, z);
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound cmp) {
        super.writeCustomNBT(cmp);

        cmp.setInteger(TAG_X_TARGET, x);
        cmp.setInteger(TAG_Y_TARGET, y);
        cmp.setInteger(TAG_Z_TARGET, z);
        cmp.setBoolean(TAG_CHEATY_MODE, cheaty);
        cmp.setBoolean(TAG_HAS_TARGET, hasTarget());
    }

    @Override
    public void readCustomNBT(NBTTagCompound cmp) {
        super.readCustomNBT(cmp);

        x = cmp.getInteger(TAG_X_TARGET);
        y = cmp.hasKey(TAG_Y_TARGET) ? cmp.getInteger(TAG_Y_TARGET) : -1;
        z = cmp.getInteger(TAG_Z_TARGET);
        cheaty = cmp.getBoolean(TAG_CHEATY_MODE);
        targetPresent = cmp.hasKey(TAG_HAS_TARGET) ? cmp.getBoolean(TAG_HAS_TARGET) : y != -1;
        if (!targetPresent) y = -1;
    }

    public final TileEntity getTile() {
        if (!hasTarget()) return null;
        if (!cheaty && (Math.abs((long) x - xCoord) > getMaxDistance() || Math.abs((long) y - yCoord) > getMaxDistance()
                || Math.abs((long) z - zCoord) > getMaxDistance())) {
            clearTarget();
            return null;
        }
        if (!isTargetLoaded()) return null;

        TileEntity tile = worldObj.getTileEntity(x, y, z);

        if (tile == null && tileRequiredAtLink()) {
            clearTarget();
            return null;
        }

        return tile;
    }

    public abstract int getMaxDistance();

    boolean tileRequiredAtLink() {
        return !cheaty;
    }
}
