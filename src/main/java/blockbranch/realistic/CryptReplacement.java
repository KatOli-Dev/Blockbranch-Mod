package blockbranch.realistic;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Complete vaulted crypt around the unchanged native monster-room preparation.
 * All commands use immutable local coordinates; native feature RNG is consumed
 * only by the existing start-chunk feature call after this composition is built. */
final class CryptReplacement {
    interface Writer {
        void set(int x,int y,int z,BlockState state);
        Direction direction(int dx,int dz);
    }
    private static final BlockState AIR=Blocks.CAVE_AIR.defaultBlockState();
    private CryptReplacement() {}

    static void generate(Writer w,int cave,long seed) {
        // An octagonal ambulatory has intersecting barrel vaults, rather than a flat cap.
        for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++) {
            if(Math.abs(x)+Math.abs(z)>24)continue;
            for(int y=-5;y<=8;y++)set(w,x,y,z,brick(seed,x,y,z));
            boolean aisle=Math.max(Math.abs(x),Math.abs(z))>=8
                    &&Math.max(Math.abs(x),Math.abs(z))<=12&&Math.abs(x)+Math.abs(z)<=22;
            if(aisle) {
                int offset=Math.min(Math.abs(Math.abs(x)-10),Math.abs(Math.abs(z)-10));
                int ceiling=7-offset;
                for(int y=0;y<ceiling;y++)set(w,x,y,z,AIR);
                set(w,x,-1,z,Blocks.POLISHED_ANDESITE.defaultBlockState());
                set(w,x,ceiling,z,Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        for(int a:new int[]{-5,5})for(int side:new int[]{-10,10}) {
            rib(w,a,side,true);rib(w,side,a,false);
        }
        chamber(w,-20,-7,-12,4,true,-16,7,seed);
        chamber(w,-5,-21,7,-12,true,1,8,seed);
        chamber(w,12,5,22,13,false,9,8,seed);
        chamberRib(w,-19,-13,-16,-2,7);
        chamberRib(w,-4,6,1,-16,8);
        rockTransition(w,cave);
        // The three original chamber positions belong to the same crypt composition.
        passage(w,-16,0,-2,-7,3,1);
        passage(w,7,0,7,17,3,10);
        passage(w,-2,0,-17,1,3,-7);
        for(int z:new int[]{-5,2})tomb(w,-19,z,4,2,-1,0);
        for(int z:new int[]{-5,1})niche(w,-20,2,z,1,0);
        if(cave==3)for(int z:new int[]{-20,-16})wallBurial(w,7,z,1,0);
        else for(int x:new int[]{-3,3})wallBurial(w,x,-21,0,-1);
        // A sealed memorial and attached collapse give the unequal east apse its own focus.
        box(w,20,0,9,21,0,11,Blocks.CHISELED_STONE_BRICKS);
        box(w,21,1,9,21,3,11,Blocks.POLISHED_ANDESITE);
        set(w,21,4,10,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        tomb(w,18,6,4,2,-1,0);
        set(w,18,2,6,AIR);
        entrance(w,cave,seed);
        // Retain the old inner emitters: their path through the east doorway controls
        // the native room's light. New exterior lamps are beyond emission range.
        for(int[] p:new int[][]{{-10,-8},{10,8},{-3,-16}}) {
            set(w,p[0],0,p[1],Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            set(w,p[0],1,p[1],Blocks.TORCH.defaultBlockState());
        }
        for(int z:new int[]{-6,3})torch(w,-19,3,z,1,0);
        for(int x:new int[]{-4,6})torch(w,x,2,-20,0,1);
        prepareNative(w);
        // Retain the published stonework's east-door landing outside the native room.
        set(w,10,-1,0,Blocks.POLISHED_ANDESITE.defaultBlockState());
    }
    private static void chamber(Writer w,int lx,int lz,int hx,int hz,boolean axisZ,int center,int peak,long seed) {
        for(int x=lx;x<=hx;x++)for(int z=lz;z<=hz;z++) {
            int offset=Math.abs((axisZ?x:z)-center);
            int ceiling=Math.max(4,peak-(offset+1)/2);
            for(int y=-5;y<=ceiling+1;y++)set(w,x,y,z,brick(seed,x,y,z));
            if(x==lx||x==hx||z==lz||z==hz)continue;
            for(int y=0;y<ceiling;y++)set(w,x,y,z,AIR);
            set(w,x,-1,z,Blocks.SMOOTH_STONE.defaultBlockState());
            set(w,x,ceiling,z,Blocks.STONE_BRICKS.defaultBlockState());
        }
    }
    private static void chamberRib(Writer w,int lo,int hi,int center,int z,int peak) {
        for(int x=lo;x<=hi;x++) {
            int h=Math.max(4,peak-(Math.abs(x-center)+1)/2);
            set(w,x,h,z,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            if(x==lo||x==hi)for(int y=0;y<h;y++)set(w,x+(x==lo?-1:1),y,z,Blocks.POLISHED_ANDESITE.defaultBlockState());
            else if(x==lo+1||x==hi-1)set(w,x,h-1,z,Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                    .setValue(StairBlock.HALF,Half.TOP).setValue(StairBlock.FACING,w.direction(x<center?-1:1,0)));
        }
    }
    private static void rib(Writer w,int x,int z,boolean acrossZ) {
        for(int a=-2;a<=2;a++) {
            int px=x+(acrossZ?0:a),pz=z+(acrossZ?a:0),h=7-Math.abs(a);
            set(w,px,h,pz,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            if(Math.abs(a)==2) {
                for(int y=0;y<h;y++)set(w,px,y,pz,(y==0?Blocks.POLISHED_ANDESITE:Blocks.STONE_BRICKS).defaultBlockState());
            } else if(a!=0) {
                int dx=acrossZ?0:Integer.signum(a),dz=acrossZ?Integer.signum(a):0;
                set(w,px,h-1,pz,Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING,w.direction(dx,dz)).setValue(StairBlock.HALF,Half.TOP));
            }
        }
    }
    private static void tomb(Writer w,int x,int z,int width,int depth,int dx,int dz) {
        for(int a=0;a<width;a++)for(int b=0;b<depth;b++) {
            boolean head=dx<0?a==0:b==0;
            boolean end=dx<0?a==width-1:b==depth-1;
            set(w,x+a,0,z+b,(head||end?Blocks.CHISELED_STONE_BRICKS:Blocks.STONE_BRICKS).defaultBlockState());
            // Opposed shoulders form a narrow upper neck; the full lid projects half
            // a block on either side while staying supported on the original footprint.
            int sx=dx<0?0:(a==0?1:-1),sz=dx<0?(b==0?1:-1):0;
            set(w,x+a,1,z+b,Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,w.direction(sx,sz)));
            set(w,x+a,2,z+b,(head?Blocks.POLISHED_ANDESITE_SLAB:Blocks.SMOOTH_STONE_SLAB).defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM));
        }
    }
    private static void wallBurial(Writer w,int x,int z,int nx,int nz) {
        // A closed casket sits within an arched loculus, rather than as another bench
        // on the chamber floor. Backing remains solid; the recess opens no new route.
        int ax=-nz,az=nx;
        for(int depth=0;depth<=3;depth++)for(int a=-1;a<=3;a++)for(int y=0;y<=5;y++) {
            int px=x+a*ax+depth*nx,pz=z+a*az+depth*nz;
            boolean pocket=depth<3&&a>=0&&a<=2&&y>=2&&y<=4;
            set(w,px,y,pz,pocket?AIR:Blocks.STONE_BRICKS.defaultBlockState());
        }
        for(int a:new int[]{-1,3})for(int y=0;y<=4;y++)set(w,x+a*ax,y,z+a*az,Blocks.POLISHED_ANDESITE.defaultBlockState());
        set(w,x+ax,5,z+az,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        for(int depth=0;depth<=2;depth++) {
            set(w,x+ax+depth*nx,2,z+az+depth*nz,Blocks.SMOOTH_STONE.defaultBlockState());
            set(w,x+ax+depth*nx,3,z+az+depth*nz,Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        }
        for(int a:new int[]{0,2})set(w,x+a*ax,4,z+a*az,Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.HALF,Half.TOP).setValue(StairBlock.FACING,w.direction(a==0?-ax:ax,a==0?-az:az)));
    }
    private static void niche(Writer w,int x,int y,int z,int dx,int dz) {
        int ax=dz,az=dx;
        for(int a=0;a<=1;a++)for(int h=0;h<=2;h++) {
            int px=x+a*ax,pz=z+a*az;
            set(w,px-dx,y+h,pz-dz,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            set(w,px,y+h,pz,AIR);
        }
        for(int a=0;a<=1;a++)set(w,x+a*ax,y-1,z+a*az,Blocks.POLISHED_ANDESITE.defaultBlockState());
    }
    private static void entrance(Writer w,int cave,long seed) {
        int dx=cave==0?1:cave==1?-1:0,dz=cave==2?1:cave==3?-1:0;
        for(int t=10;t<=22;t++)for(int side=-3;side<=3;side++) {
            int x=dx*t+dz*side,z=dz*t+dx*side;
            if(inside(x,z)) {
                // Existing room vaults and lateral circulation remain open. Only
                // pierce the central throat through a room's enclosing wall.
                if(Math.abs(side)<=1) {
                    for(int y=0;y<=3;y++)set(w,x,y,z,AIR);
                    set(w,x,-1,z,Blocks.POLISHED_ANDESITE.defaultBlockState());
                }
                continue;
            }
            int ceiling=6-Math.abs(side);
            int bottom=-Math.min(5,24-t);
            // The exposed casing is keyed into native-looking host rock; only the
            // carved front frame presents dressed masonry to the cave.
            for(int y=bottom;y<=ceiling+1;y++)set(w,x,y,z,Blocks.STONE.defaultBlockState());
            if(Math.abs(side)<=2) {
                for(int y=0;y<ceiling;y++)set(w,x,y,z,AIR);
                set(w,x,-1,z,Blocks.POLISHED_ANDESITE.defaultBlockState());
            }
            if(t==22) {
                set(w,x,ceiling,z,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
                if(Math.abs(side)==3)for(int y=0;y<ceiling;y++)set(w,x,y,z,Blocks.POLISHED_ANDESITE.defaultBlockState());
            }
        }
        for(int side:new int[]{-2,2})torch(w,dx*22+dz*side,2,dz*22+dx*side,-dz*Integer.signum(side),-dx*Integer.signum(side));
        for(int side=-1;side<=1;side++) {
            int x=dx*23+dz*side,z=dz*23+dx*side;
            set(w,x,-1,z,Blocks.STONE_BRICKS.defaultBlockState());
            set(w,x,0,z,Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,w.direction(dx,dz)));
            for(int y=1;y<=3;y++)set(w,x,y,z,AIR);
            x=dx*24+dz*side;z=dz*24+dx*side;
            set(w,x,-1,z,Blocks.STONE_BRICKS.defaultBlockState());
            set(w,x,0,z,Blocks.POLISHED_ANDESITE.defaultBlockState());
            for(int y=1;y<=2;y++)set(w,x,y,z,AIR);
        }
    }
    private static void rockTransition(Writer w,int cave) {
        int dx=cave==0?1:cave==1?-1:0,dz=cave==2?1:cave==3?-1:0;
        for(int x=-23;x<=25;x++)for(int z=-24;z<=16;z++) {
            if(inside(x,z))continue;
            int t=x*dx+z*dz,side=x*dz+z*dx;
            if(t>=10&&t<=24&&Math.abs(side)<=3)continue;
            int distance=3;
            for(int a=-2;a<=2;a++)for(int b=-2;b<=2;b++)
                if(Math.abs(a)+Math.abs(b)<=2&&inside(x+a,z+b))distance=Math.min(distance,Math.abs(a)+Math.abs(b));
            int thickness=1+(Math.floorMod(Math.floorDiv(x,4)+2*Math.floorDiv(z,5),3)==0?1:0);
            if(distance>thickness)continue;
            int top=6+(int)Math.floor(1.2*Math.sin(x*.6)+1.2*Math.cos(z*.45))-(distance-1)*2;
            int bottom=t>=20&&t<=24?-Math.max(1,24-t):-5;
            for(int y=bottom;y<=top;y++)set(w,x,y,z,Blocks.STONE.defaultBlockState());
        }
    }
    private static boolean inside(int x,int z) {
        return Math.abs(x)<=13&&Math.abs(z)<=13&&Math.abs(x)+Math.abs(z)<=24
                ||x>=-20&&x<=-12&&z>=-7&&z<=4
                ||x>=-5&&x<=7&&z>=-21&&z<=-12
                ||x>=12&&x<=22&&z>=5&&z<=13;
    }
    private static void prepareNative(Writer w) {
        // These are exactly the original four preparation commands, including the east
        // opening and its floor. The existing native feature call follows in postProcess.
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=-1;y<=4;y++)w.set(x,y,z,Blocks.STONE.defaultBlockState());
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=3;y++)w.set(x,y,z,AIR);
        for(int x=2;x<=10;x++)for(int y=0;y<=2;y++)w.set(x,y,0,AIR);
        for(int x=6;x<=10;x++)w.set(x,-1,0,Blocks.STONE_BRICKS.defaultBlockState());
    }
    private static BlockState brick(long seed,int x,int y,int z) {
        if(y<0)return Blocks.STONE_BRICKS.defaultBlockState();
        long h=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L^y*0x9E3779B97F4A7C15L);
        return ((h&15)==0?Blocks.CRACKED_STONE_BRICKS:(h&7)==0?Blocks.MOSSY_STONE_BRICKS:Blocks.STONE_BRICKS).defaultBlockState();
    }
    private static void torch(Writer w,int x,int y,int z,int dx,int dz) {
        set(w,x,y,z,Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,w.direction(dx,dz)));
    }
    private static void passage(Writer w,int x,int y,int z,int hx,int hy,int hz) {
        for(int a=x;a<=hx;a++)for(int c=z;c<=hz;c++)for(int b=y;b<=hy;b++)set(w,a,b,c,AIR);
    }
    private static void box(Writer w,int x,int y,int z,int hx,int hy,int hz,Block block) {
        for(int a=x;a<=hx;a++)for(int c=z;c<=hz;c++)for(int b=y;b<=hy;b++)set(w,a,b,c,block.defaultBlockState());
    }
    private static void set(Writer w,int x,int y,int z,BlockState state) {
        // The native feature reads below its prepared floor before deciding whether
        // to place masonry and consume a random draw. Preserve that original terrain
        // as well as the prepared room; changing it shifts loot and spawner selection.
        if(Math.abs(x)<=5&&Math.abs(z)<=5&&y<=4)return;
        w.set(x,y,z,state);
    }
}
