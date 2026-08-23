package com.awesomehippo.historicships.block;

import com.awesomehippo.historicships.blockentity.ShipwrightWorkbenchBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

public class ShipwrightWorkbenchBlock extends BaseEntityBlock {
    public static final MapCodec<ShipwrightWorkbenchBlock> CODEC = simpleCodec(ShipwrightWorkbenchBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE_N = Shapes.or(Block.box(-6.0, 0.0, 1.5, -3.0, 7.5, 14.5), Block.box(19.0, 0.0, 1.5, 22.0, 7.5, 14.5), Block.box(-7.5, 7.5, 0.5, 23.5, 16.0, 15.5));

    private static final VoxelShape[] SHAPES = makeShapes(SHAPE_N);

    public ShipwrightWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static VoxelShape[] makeShapes(VoxelShape north) {
        VoxelShape[] shapes = new VoxelShape[4];
        shapes[Direction.NORTH.get2DDataValue()] = north;
        shapes[Direction.EAST.get2DDataValue()] = rotateShape(north, Direction.EAST);
        shapes[Direction.SOUTH.get2DDataValue()] = rotateShape(north, Direction.SOUTH);
        shapes[Direction.WEST.get2DDataValue()] = rotateShape(north, Direction.WEST);
        return shapes;
    }

    private static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        VoxelShape[] buffer = new VoxelShape[] {shape, Shapes.empty()};
        int times = (facing.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1], Shapes.box(1.0 - maxZ, minY, minX, 1.0 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }

    public static VoxelShape shapeOf(BlockState state) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    public static boolean footprintBlocked(LevelReader level, BlockPos origin, Direction facing) {
        VoxelShape local = SHAPES[facing.get2DDataValue()];
        VoxelShape worldShape = local.move(origin.getX(), origin.getY(), origin.getZ());
        AABB bounds = worldShape.bounds().inflate(1.0E-4);

        int minX = (int) Math.floor(bounds.minX);
        int maxX = (int) Math.floor(bounds.maxX - 1.0E-6);
        int minY = (int) Math.floor(bounds.minY);
        int maxY = (int) Math.floor(bounds.maxY - 1.0E-6);
        int minZ = (int) Math.floor(bounds.minZ);
        int maxZ = (int) Math.floor(bounds.maxZ - 1.0E-6);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (cursor.equals(origin)) {
                        continue;
                    }
                    if (!level.getWorldBorder().isWithinBounds(cursor)) {
                        return true;
                    }
                    BlockState other = level.getBlockState(cursor);
                    if (other.isAir()) {
                        continue;
                    }
                    VoxelShape otherShape = other.getCollisionShape(level, cursor);
                    if (otherShape.isEmpty()) {
                        continue;
                    }
                    VoxelShape otherWorld = otherShape.move(cursor.getX(), cursor.getY(), cursor.getZ());
                    if (Shapes.joinIsNotEmpty(worldShape, otherWorld, BooleanOp.AND)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    public MapCodec<ShipwrightWorkbenchBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof ShipwrightWorkbenchBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShipwrightWorkbenchBlockEntity(pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (footprintBlocked(level, pos, facing)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !footprintBlocked(level, pos, state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return this.rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
