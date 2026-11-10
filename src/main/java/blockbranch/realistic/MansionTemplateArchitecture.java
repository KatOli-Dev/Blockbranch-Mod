package blockbranch.realistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.DyeColor;

/** Template-local architectural backgrounds and stonework; room furnishings remain native.
 * Every returned state occupies the same cell and collision volume as its native counterpart;
 * native air, floors, windows, furniture, lights and circulation geometry remain authoritative. */
public final class MansionTemplateArchitecture {
    private MansionTemplateArchitecture() {}

    public static BlockState apply(String template,BlockPos local,BlockState state,int worldY,int floor) {
        int height=worldY-floor;
        if(state.isAir()||state.hasBlockEntity()||state.getLightEmission()>0||height<=0||height==8||height==19)return state;
        String name=template.substring(template.lastIndexOf('/')+1);
        if(name.startsWith("1x")||name.startsWith("2x")||name.startsWith("roof")||name.startsWith("wall_")||name.startsWith("small_wall")||name.startsWith("indoors_")||name.equals("entrance")) {
            var stone=roomStone(state);if(stone!=state)return stone;
        }
        return switch(name) {
            case "roof","roof_front","roof_corner","roof_inner_corner" -> roof(state);
            case "wall_window","wall_flat","small_wall" -> wall(local,state,height);
            case "wall_corner","small_wall_corner" -> state.is(Blocks.COBBLESTONE)?Blocks.STONE_BRICKS.defaultBlockState():state;
            case "indoors_wall_1","indoors_wall_2","indoors_door_1","indoors_door_2" -> partition(local,state);
            case "entrance" -> entrance(local,state,height);
            default -> name.startsWith("1x")||name.startsWith("2x")?roomStone(state):state;
        };
    }

    private static BlockState roof(BlockState state) {
        if(state.is(Blocks.DARK_OAK_PLANKS))return Blocks.SPRUCE_PLANKS.defaultBlockState();
        if(state.is(Blocks.DARK_OAK_STAIRS))return Blocks.SPRUCE_STAIRS.withPropertiesOf(state);
        return state.is(Blocks.COBBLESTONE)?Blocks.STONE_BRICKS.defaultBlockState():state;
    }
    private static BlockState roomStone(BlockState state) {
        if(state.is(Blocks.COBBLESTONE))return Blocks.STONE_BRICKS.defaultBlockState();
        if(state.is(Blocks.COBBLESTONE_STAIRS))return Blocks.STONE_BRICK_STAIRS.withPropertiesOf(state);
        return state.is(Blocks.COBBLESTONE_WALL)?Blocks.STONE_BRICK_WALL.withPropertiesOf(state):state;
    }
    private static BlockState wall(BlockPos p,BlockState state,int height) {
        if(state.is(Blocks.COBBLESTONE))return Blocks.STONE_BRICKS.defaultBlockState();
        if(!state.is(Blocks.DARK_OAK_PLANKS))return state;
        if(height<=2)return Blocks.STONE_BRICKS.defaultBlockState();
        if(p.getY()==6)return beam(Direction.Axis.Z);
        if(p.getZ()==0||p.getZ()==7)return beam(Direction.Axis.Y);
        // The second cell contains the original projecting corbels/backing.
        if(p.getX()==0&&p.getY()>=2&&p.getY()<=5)return Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE).defaultBlockState();
        return state;
    }
    private static BlockState partition(BlockPos p,BlockState state) {
        if(!state.is(Blocks.DARK_OAK_PLANKS))return state;
        // The birch floor course, native door aperture and its stair arch are untouched.
        if(p.getY()==0)return state;
        if(p.getY()==6)return beam(Direction.Axis.Z);
        if(p.getZ()==0||p.getZ()==7)return beam(Direction.Axis.Y);
        if(p.getY()>=2&&p.getY()<=5)return Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE).defaultBlockState();
        return state;
    }
    private static BlockState entrance(BlockPos p,BlockState state,int height) {
        if(state.is(Blocks.COBBLESTONE))return Blocks.STONE_BRICKS.defaultBlockState();
        if(!state.is(Blocks.DARK_OAK_PLANKS)||height==8||height==19)return state;
        // Restrict plaster to the tall outer background, away from the stair/floor planes.
        if(p.getY()>=2&&(p.getX()==0||p.getX()==20||p.getZ()==0||p.getZ()==15)) {
            if(p.getY()==6||p.getY()==14)return beam(p.getZ()==0||p.getZ()==15?Direction.Axis.X:Direction.Axis.Z);
            return Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE).defaultBlockState();
        }
        return state;
    }
    private static BlockState beam(Direction.Axis axis) {
        return Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis);
    }
}
