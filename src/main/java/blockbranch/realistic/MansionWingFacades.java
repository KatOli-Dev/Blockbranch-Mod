package blockbranch.realistic;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/** Saved facade definition 1; writes only the existing wing shells and their admitted one-block approach/eave margins. */
final class MansionWingFacades {
    @FunctionalInterface interface Writer { void set(int x,int y,int z,BlockState state); }
    private MansionWingFacades() {}
    static void apply(Writer writer,int x,int z,int width,int length,boolean workshop,int westSteps,int eastSteps) {
        var vertical=Blocks.DARK_OAK_LOG.defaultBlockState();
        var cross=vertical.setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
        for(int side:new int[]{0,width}) {
            for(int a=0;a<=length;a++) {
                put(writer,x+side,0,z+a,Blocks.COBBLESTONE);
                writer.set(x+side,5,z+a,vertical.setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));
            }
            for(int post:new int[]{0,7,13,19,length})for(int y=0;y<=6;y++)writer.set(x+side,y,z+post,vertical);
            for(int bay:new int[]{7,13}) {
                for(int a=bay+(workshop?2:1);a<=bay+(workshop?4:5);a++) {
                    put(writer,x+side,1,z+a,Blocks.COBBLESTONE);
                    for(int y=2;y<=4;y++)writer.set(x+side,y,z+a,!workshop&&a==bay+3?vertical:Blocks.GLASS.defaultBlockState());
                }
            }
            int outward=side==0?-1:1;
            int stairX=x+side+outward,steps=side==0?westSteps:eastSteps;
            for(int a=3;a<=4;a++){for(int y=-12;y<=0;y++)put(writer,stairX,y,z+a,Blocks.COBBLESTONE);for(int y=1;y<=3;y++)put(writer,stairX,y,z+a,Blocks.AIR);}
            for(int i=0;i<steps;i++){
                for(int y=-12;y<-i;y++)put(writer,stairX,y,z+5+i,Blocks.COBBLESTONE);
                for(int y=1-i;y<=3;y++)put(writer,stairX,y,z+5+i,Blocks.AIR);
                writer.set(stairX,-i,z+5+i,Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH));
            }

            var bracket=Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF,Half.TOP)
                .setValue(StairBlock.FACING,side==0?Direction.EAST:Direction.WEST);
            for(int a=1;a<=7;a++)put(writer,x+side+outward,5,z+a,Blocks.DARK_OAK_SLAB);
            for(int a:new int[]{1,7})writer.set(x+side+outward,4,z+a,bracket);
            for(int a:new int[]{2,6}) {
                put(writer,x+side+outward,4,z+a,Blocks.IRON_CHAIN);
                writer.set(x+side+outward,3,z+a,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));
            }
        }
        for(int end:new int[]{0,length}) {
            for(int a=0;a<=width;a++) {
                put(writer,x+a,0,z+end,Blocks.COBBLESTONE);
                writer.set(x+a,5,z+end,cross);
                writer.set(x+a,6,z+end,cross);
            }
            for(int a=3;a<=5;a++)for(int y=2;y<=4;y++)put(writer,x+a,y,z+end,Blocks.GLASS);
        }
        // The old interior ceiling and all furnishings remain below the roof edits.
        for(int tier=0;tier<width/2;tier++)for(int a=-1;a<=length+1;a++) {
            writer.set(x+tier,7+tier,z+a,Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST));
            writer.set(x+width-tier,7+tier,z+a,Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.WEST));
        }
        for(int a=-1;a<=length+1;a++)put(writer,x+width/2,7+width/2,z+a,Blocks.DARK_OAK_SLAB);
    }
    private static void put(Writer writer,int x,int y,int z,Block block){writer.set(x,y,z,block.defaultBlockState());}
}
