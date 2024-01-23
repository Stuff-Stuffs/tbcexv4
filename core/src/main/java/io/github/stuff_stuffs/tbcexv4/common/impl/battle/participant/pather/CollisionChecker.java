package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.pather;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleBounds;
import net.minecraft.block.BlockState;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class CollisionChecker {
    private final double lowerWidth;
    private final double upperWidth;
    private final double height;
    private final BattleBounds bounds;
    private final BlockView environment;
    private final BlockPos.Mutable mut;
    private final VoxelShape boxShape;
    public double lastFloorHeight = 0;

    public CollisionChecker(final double width, final double height, final BattleBounds bounds, final BlockView environment) {
        lowerWidth = 0.5 - width * 0.5;
        upperWidth = 0.5 + width * 0.5;
        this.height = height;
        this.bounds = bounds;
        this.environment = environment;
        mut = new BlockPos.Mutable();
        final Box box = new Box(lowerWidth, 0, lowerWidth, upperWidth, height, upperWidth);
        boxShape = VoxelShapes.cuboid(box);
    }

    public boolean inBounds(final int x, final int y, final int z) {
        if (x + lowerWidth < bounds.x0() || y < bounds.y0() || z + lowerWidth < bounds.z0()) {
            return false;
        }
        if (x + upperWidth > (bounds.x1() - 1) || y + height > (bounds.y1() - 1) || z + upperWidth > (bounds.z1() - 1)) {
            return false;
        }
        return true;
    }

    public double floorHeight(final int x, final int y, final int z) {
        final int lowestX = MathHelper.floor(x + lowerWidth);
        final int highestX = MathHelper.ceil(x + upperWidth);
        final int lowestZ = MathHelper.floor(z + lowerWidth);
        final int highestZ = MathHelper.ceil(z + upperWidth);
        double height = 0.0;
        for (int i = lowestX; i <= highestX; i++) {
            for (int j = lowestZ; j <= highestZ; j++) {
                final BlockState state = environment.getBlockState(mut.set(i, y, j));
                final VoxelShape shape = state.getCollisionShape(environment, mut);
                if (!shape.isEmpty()) {
                    height = Math.max(height, shape.getMax(Direction.Axis.Y));
                }
            }
        }
        return height;
    }

    public boolean check(final int x, final int y, final int z, double floorHeight) {
        if (!inBounds(x, y, z)) {
            return false;
        }
        if (Double.isNaN(floorHeight)) {
            floorHeight = floorHeight(x, y, z);
        }
        lastFloorHeight = floorHeight;
        if (floorHeight > 0.99999) {
            return false;
        }
        final int lowestX = MathHelper.floor(x + lowerWidth);
        final int highestX = MathHelper.ceil(x + upperWidth);
        final int lowestY = MathHelper.floor(y + floorHeight);
        final int highestY = MathHelper.floor(y + floorHeight + height);
        final int lowestZ = MathHelper.floor(z + lowerWidth);
        final int highestZ = MathHelper.ceil(z + upperWidth);
        final Box box = new Box(x + lowerWidth, y + floorHeight, z + lowerWidth, x + upperWidth, y + floorHeight + height, z + upperWidth);
        for (int i = lowestX; i <= highestX; i++) {
            for (int j = lowestZ; j <= highestZ; j++) {
                for (int k = lowestY; k <= highestY; k++) {
                    final BlockState state = environment.getBlockState(mut.set(i, y, j));
                    final VoxelShape shape = state.getCollisionShape(environment, mut);
                    if (shape == VoxelShapes.fullCube()) {
                        if (box.intersects(i, j, k, i + 1, j + 1, k + 1)) {
                            return false;
                        }
                    } else if (!(shape == VoxelShapes.empty() || shape.isEmpty())) {
                        if (VoxelShapes.matchesAnywhere(boxShape, shape.offset(-i, -j, -k), BooleanBiFunction.AND)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
