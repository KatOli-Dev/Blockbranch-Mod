package blockbranch.realistic;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Saved perimeter definition 1. Walls keep the original apertures and walk;
 * roof additions stay above the existing routes and within the original bounds.
 * Workshop/archive facades and native rooms are outside these commands. */
final class MansionPerimeterStonework {
    @FunctionalInterface interface Writer { void set(int x,int y,int z,BlockState state); }
    private MansionPerimeterStonework() {}

    static void apply(Writer w,int x0,int x1,int z0,int z1) {
        for(int z:new int[]{z0,z1})wall(w,true,x0,x1,z);
        for(int x:new int[]{x0,x1})wall(w,false,z0,z1,x);
        for(int x:new int[]{x0,x1})for(int z:new int[]{z0,z1})tower(w,x-3,z-3);
    }

    private static void wall(Writer w,boolean alongX,int start,int end,int side) {
        int mid=(start+end)/2;
        var beam=Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,alongX?Direction.Axis.X:Direction.Axis.Z);
        // The corner towers own their complete seven-wide footprints.
        for(int a=start+4;a<=end-4;a++) {
            for(int y=-3;y<=7;y++) {
                if(y>=0&&y<=5&&Math.abs(a-mid)<=3)continue;
                BlockState state;
                if(y<0)state=(y==-1?Blocks.STONE_BRICKS:Blocks.CRACKED_STONE_BRICKS).defaultBlockState();
                else if(y==6)state=beam;
                else if(y==7)state=Blocks.DARK_OAK_PLANKS.defaultBlockState();
                else if(Math.abs(a-mid)==4||Math.floorMod(a-start-4,8)==0)
                    state=(y==0?Blocks.CHISELED_STONE_BRICKS:Blocks.DARK_OAK_LOG).defaultBlockState();
                else state=Blocks.STONE_BRICKS.defaultBlockState();
                put(w,alongX,a,y,side,state);
            }
            // Existing isolated wall posts become narrower timber balusters.
            if((a-start)%3==0)for(int y:new int[]{9,10})put(w,alongX,a,y,side,Blocks.DARK_OAK_FENCE.defaultBlockState());
        }
    }

    private static void tower(Writer w,int x,int z) {
        for(int a=0;a<=6;a++)for(int y=-3;y<=14;y++)for(int face=0;face<4;face++) {
            boolean front=face<2;
            if(a>=2&&a<=4&&(y>=9&&y<=12||front&&y>=0&&y<=3))continue;
            BlockState state;
            if(y<0)state=Blocks.STONE_BRICKS.defaultBlockState();
            else if(y==7)state=Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,front?Direction.Axis.X:Direction.Axis.Z);
            else if(y==13)state=Blocks.DARK_OAK_PLANKS.defaultBlockState();
            else if(a==0||a==6)state=(y<=1?Blocks.CHISELED_STONE_BRICKS:Blocks.DARK_OAK_LOG).defaultBlockState();
            else state=Blocks.STONE_BRICKS.defaultBlockState();
            w.set(x+(front?a:face==2?0:6),y,z+(front?face==0?0:6:a),state);
        }
        // Open the old one-block-high attic while preserving its floor and ladder.
        for(int a=0;a<=6;a++)for(int b=0;b<=6;b++)for(int y=15;y<=17;y++)w.set(x+a,y,z+b,Blocks.AIR.defaultBlockState());
        for(int a=0;a<=6;a++)for(int b=0;b<=6;b++)if(a==0||a==6||b==0||b==6)
            w.set(x+a,15,z+b,Blocks.DARK_OAK_PLANKS.defaultBlockState());
        // A king post ties the inner roof deck to the unchanged attic floor.
        for(int y=15;y<=16;y++)w.set(x+3,y,z+3,Blocks.DARK_OAK_LOG.defaultBlockState());
        for(int a=2;a<=4;a++)for(int b=2;b<=4;b++)w.set(x+a,17,z+b,Blocks.DARK_OAK_PLANKS.defaultBlockState());
        roofRing(w,x,z,-1,7,15);roofRing(w,x,z,0,6,16);roofRing(w,x,z,1,5,17);
        for(int a=2;a<=4;a++)for(int b=2;b<=4;b++)w.set(x+a,18,z+b,Blocks.DARK_OAK_SLAB.defaultBlockState());
        for(int y:new int[]{6,13}) {
            torch(w,x+1,y,z+3,Direction.EAST);torch(w,x+5,y,z+3,Direction.WEST);
        }
        torch(w,x+3,16,z+2,Direction.NORTH);torch(w,x+3,16,z+4,Direction.SOUTH);
    }

    private static void roofRing(Writer w,int x,int z,int lo,int hi,int y) {
        for(int a=lo;a<=hi;a++) {
            stair(w,x+a,y,z+lo,Direction.SOUTH);stair(w,x+a,y,z+hi,Direction.NORTH);
            stair(w,x+lo,y,z+a,Direction.EAST);stair(w,x+hi,y,z+a,Direction.WEST);
        }
        for(int a:new int[]{lo,hi})for(int b:new int[]{lo,hi})w.set(x+a,y,z+b,Blocks.DARK_OAK_PLANKS.defaultBlockState());
    }
    private static void stair(Writer w,int x,int y,int z,Direction facing){w.set(x,y,z,Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,facing));}
    private static void torch(Writer w,int x,int y,int z,Direction facing){w.set(x,y,z,Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,facing));}
    private static void put(Writer w,boolean alongX,int a,int y,int side,BlockState state){w.set(alongX?a:side,y,alongX?side:a,state);}
}
