package cn.wanyiwige.illusion;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
public final class IllusionBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public final boolean trap;
    public IllusionBlock(boolean trap){super(BlockBehaviour.Properties.of().strength(-1,3600000).noCollission().noOcclusion().noLootTable());this.trap=trap;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){if(!trap)return Shapes.empty();return switch(state.getValue(FACING)){case UP->Shapes.box(0,0,0,1,.05,1);case DOWN->Shapes.box(0,.95,0,1,1,1);case SOUTH->Shapes.box(0,0,0,1,1,.05);case NORTH->Shapes.box(0,0,.95,1,1,1);case EAST->Shapes.box(0,0,0,.05,1,1);case WEST->Shapes.box(.95,0,0,1,1,1);};}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override public VoxelShape getVisualShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos){return true;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new IllusionBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,IllusionContent.BLOCK_ENTITY.get(),IllusionBlockEntity::tick);}
}
