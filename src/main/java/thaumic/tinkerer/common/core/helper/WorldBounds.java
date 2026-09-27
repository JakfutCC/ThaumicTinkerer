package thaumic.tinkerer.common.core.helper;

import net.minecraft.world.World;

import com.cardinalstar.cubicchunks.world.ICubicWorld;
import com.cardinalstar.cubicchunks.world.cube.ICubeProvider;

import cpw.mods.fml.common.Loader;

public final class WorldBounds {

    private WorldBounds() {}

    public static boolean contains(World world, long y) {
        if (y < Integer.MIN_VALUE || y > Integer.MAX_VALUE) return false;
        if (Loader.isModLoaded("cubicchunks")) return CubicBounds.contains(world, (int) y);
        return y >= 0 && y < 256;
    }

    public static boolean isCubic(World world) {
        return Loader.isModLoaded("cubicchunks") && CubicBounds.isCubic(world);
    }

    public static boolean isLoaded(World world, int x, int y, int z) {
        return isCubic(world) ? contains(world, y) && world.checkChunksExist(x, y, z, x, y, z)
                : world.blockExists(x, y, z);
    }

    private static final class CubicBounds {

        private static boolean isCubic(World world) {
            return world.getChunkProvider() instanceof ICubeProvider;
        }

        private static boolean contains(World world, int y) {
            if (isCubic(world)) {
                ICubicWorld cubicWorld = (ICubicWorld) world;
                return y >= cubicWorld.getMinHeight() && y < cubicWorld.getMaxHeight();
            }
            return y >= 0 && y < 256;
        }
    }
}
