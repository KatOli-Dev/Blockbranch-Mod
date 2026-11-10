package blockbranch.realistic;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/** Saved crypt stonework 1. The input is the final legacy command map for this
 * owned chunk, never neighboring world state. The native feature footprint is
 * excluded. Niches, lamp clearances and corbels only remove volume;
 * other replacements keep their shape. */
final class CryptStonework {
    @FunctionalInterface interface Writer { void set(int x,int y,int z,BlockState state); }
    private CryptStonework() {}

    static void apply(Map<BlockPos,BlockState> shell,Writer w,long seed,int xx,int xz,int zx,int zz) {
        for(var entry:shell.entrySet()) {
            var p=entry.getKey();int x=p.getX(),y=p.getY(),z=p.getZ();var original=entry.getValue();
            if(Math.abs(x)<=5&&Math.abs(z)<=5)continue;
            BlockState next=original;
            boolean lampClearance=(x==-10&&z==-8||x==10&&z==8)&&(y==2||y==3);
            boolean ceilingCorbel=y==4&&(x==-10&&z==-9||x==10&&z==9);
            boolean westNiche=x==-20&&y==2&&(z==-5||z==-4||z==1||z==2);
            boolean northNiche=z==-21&&y==2&&(x==-3||x==-2||x==3||x==4);
            if(lampClearance&&masonry(original)) {
                next=Blocks.CAVE_AIR.defaultBlockState();
            } else if(ceilingCorbel&&masonry(original)) {
                // Shape an existing header edge directly against the solid
                // ceiling; do not leave a suspended pier over the lamp.
                var towardArch=x<0?direction(-zx,-zz):direction(zx,zz);
                next=Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,towardArch).setValue(StairBlock.HALF,Half.TOP);
            } else if((westNiche||northNiche)&&masonry(original)) {
                var outward=westNiche?direction(-xx,-xz):direction(-zx,-zz);
                next=Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,outward).setValue(StairBlock.HALF,Half.TOP);
            } else if(original.is(Blocks.STONE_BRICK_SLAB)) {
                boolean head=x==-19||z==-20;
                next=(head?Blocks.POLISHED_ANDESITE_SLAB:Blocks.SMOOTH_STONE_SLAB).defaultBlockState()
                    .setValue(SlabBlock.TYPE,original.getValue(SlabBlock.TYPE)).setValue(SlabBlock.WATERLOGGED,original.getValue(SlabBlock.WATERLOGGED));
            } else if(masonry(original)) {
                boolean arch=(x>=-10&&x<=10&&x%5==0&&(z==-12||z==-8||z==8||z==12))
                    ||(z>=-10&&z<=10&&z%5==0&&(x==-12||x==-8||x==8||x==12));
                boolean mainWall=Math.abs(x)==13||Math.abs(z)==13||Math.abs(x)==7||Math.abs(z)==7;
                boolean chamberWall=x==-20||z==-7&&x<=-12||z==4&&x<=-12||z==-21||x==-5&&z<=-12||x==7&&z<=-12||x==22||z==13&&x>=12;
                if(y==-1&&Math.abs(x)<=12&&Math.abs(z)<=12)
                    next=(Math.abs(x)==10||Math.abs(z)==10?Blocks.POLISHED_ANDESITE:Blocks.STONE_BRICKS).defaultBlockState();
                else if(arch&&y>=0&&y<=3)
                    next=(y==0||y==3?Blocks.CHISELED_STONE_BRICKS:Blocks.STONE_BRICKS).defaultBlockState();
                else if((mainWall||chamberWall)&&y>=0&&y<=4) {
                    int along=Math.abs(x)==7||Math.abs(x)==13||x==-20||x==22?z:x;
                    if(y==0)next=Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                    else if(y==4)next=(Math.floorMod(along,5)==0?Blocks.CHISELED_STONE_BRICKS:Blocks.STONE_BRICKS).defaultBlockState();
                    else if(Math.floorMod(along,5)==0)next=Blocks.STONE_BRICKS.defaultBlockState();
                    else {
                        long weather=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L^y*0x9E3779B97F4A7C15L);
                        next=((weather&7)==0?Blocks.CRACKED_STONE_BRICKS:(weather&3)==0?Blocks.MOSSY_STONE_BRICKS:Blocks.STONE_BRICKS).defaultBlockState();
                    }
                } else if(y>=4&&y<=6&&Math.abs(x)<=12&&Math.abs(z)<=12)
                    next=(x%5==0||z%5==0?Blocks.POLISHED_ANDESITE:Blocks.STONE_BRICKS).defaultBlockState();
            } else if(original.isAir()) {
                // These four lamps are at least fourteen Manhattan blocks from
                // the protected room: emission 14 cannot illuminate its spawner.
                if(x==-19&&y==3&&(z==-6||z==3))next=Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,direction(xx,xz));
                if(z==-20&&y==2&&(x==-4||x==6))next=Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,direction(zx,zz));
            }
            if(!next.equals(original))w.set(x,y,z,next);
        }
    }
    private static Direction direction(int x,int z){return x>0?Direction.EAST:x<0?Direction.WEST:z>0?Direction.SOUTH:Direction.NORTH;}
    private static boolean masonry(BlockState state){return state.is(Blocks.STONE_BRICKS)||state.is(Blocks.MOSSY_STONE_BRICKS)||state.is(Blocks.CRACKED_STONE_BRICKS)||state.is(Blocks.COBBLESTONE)||state.is(Blocks.MOSSY_COBBLESTONE);}
}
