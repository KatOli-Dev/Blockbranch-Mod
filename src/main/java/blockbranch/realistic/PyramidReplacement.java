package blockbranch.realistic;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Complete desert layout 1: an exposed stepped temple above the retained native tomb.
 * The planner admits at most ten blocks of relief and fits the native origin six below
 * the lowest surface. Battered courses rise from local y6 through the admitted sand,
 * with the hall above y16 and no tall vertical pedestal at the terrain edge.
 * Native loot, TNT, pressure plate and archaeology remain the original gameplay components. */
final class PyramidReplacement {
    interface Writer {
        void set(int x,int y,int z,BlockState state);
        BlockState get(int x,int y,int z);
        Direction direction(int dx,int dz);
    }
    private PyramidReplacement() {}
    private static final BlockState AIR=Blocks.CAVE_AIR.defaultBlockState();
    private static final Block ORANGE=Blocks.DYED_TERRACOTTA.pick(DyeColor.ORANGE);
    private static final Block BLUE=Blocks.DYED_TERRACOTTA.pick(DyeColor.BLUE);
    static void generate(Writer w,int entry,long seed) {
        // These ambient desert plants are not placed by the native temple. Remove them inside
        // the replacement footprint before new adjacent masonry invalidates their attachments.
        for(int x=-15;x<=35;x++)for(int z=-15;z<=35;z++)for(int y=6;y<=40;y++)plant(w,x,y,z);
        for(int x=6;x<=14;x++)for(int z=-26;z< -15;z++)for(int y=6;y<=40;y++)plant(w,x,y,z);
        // The old buried native temple becomes the tomb within this composition, not a base
        // to decorate. Outboard masonry carries the exposed mass around its reserved roof space.
        // The admitted natural surface ends at y16; clear its plants before building the roof.
        clear(w,-5,17,-5,25,40,25);
        box(w,-15,-12,-15,35,5,35,Blocks.SANDSTONE);
        for(int y=6;y<=31;y++) {
            int lo=y<=17?-7:y<=21?-3:y-21,hi=20-lo;
            for(int x=lo;x<=hi;x++)for(int z=lo;z<=hi;z++) {
                // Unequal weathered lower corners interrupt the manufactured square outline.
                int wear=y<=21?3:0;
                if(Math.min(x-lo,hi-x)+Math.min(z-lo,hi-z)<wear)continue;
                boolean edge=x==lo||x==hi||z==lo||z==hi;
                BlockState state=Blocks.SANDSTONE.defaultBlockState();
                if(edge&&(y>=22||y==17||y==21))state=Blocks.SANDSTONE_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING,w.direction(inward(x,z,lo,hi)[0],inward(x,z,lo,hi)[1]));
                else if(edge&&(y==16||y==20))state=Blocks.CUT_SANDSTONE.defaultBlockState();
                set(w,x,y,z,state);
            }
        }
        set(w,10,31,10,Blocks.CUT_SANDSTONE.defaultBlockState());
        // Two distinct architectural registers, rather than identical rooms on every course.
        for(int along:new int[]{4,16}){panel(w,along,14,-7,0,1);panel(w,along,18,-3,0,1);}
        frieze(w,-7,14,10,1,0,3);frieze(w,27,14,16,-1,0,2);
        frieze(w,10,14,27,0,-1,3);
        seal(w,23,18,10,-1,0);seal(w,10,18,23,0,-1);
        panel(w,-3,18,16,1,0);
        burial(w,seed);
        templeHall(w);
        crypt(w);
        approach(w,entry);
        descent(w);
        nativeEntry(w);
        observation(w);
    }
    private static void templeHall(Writer w) {
        box(w,6,17,6,14,17,14,Blocks.SMOOTH_SANDSTONE);
        clear(w,6,18,6,14,23,14);
        box(w,8,17,-5,12,17,6,Blocks.CUT_SANDSTONE);
        clear(w,8,18,-5,12,21,6);
        // A recessed frame sits in the pyramid face rather than in a separate gateway.
        for(int x:new int[]{7,13})box(w,x,18,-3,x,20,-3,Blocks.CUT_SANDSTONE);
        box(w,7,21,-3,13,21,-3,Blocks.CHISELED_SANDSTONE);
        set(w,8,21,-3,ORANGE.defaultBlockState());set(w,12,21,-3,ORANGE.defaultBlockState());
        set(w,10,21,-3,BLUE.defaultBlockState());
        for(int x:new int[]{6,14})for(int z:new int[]{6,14}) {
            box(w,x,18,z,x,23,z,Blocks.CUT_SANDSTONE);
            set(w,x,18,z,Blocks.CHISELED_SANDSTONE.defaultBlockState());
            set(w,x,23,z,Blocks.CHISELED_SANDSTONE.defaultBlockState());
        }
        for(int x=7;x<=13;x++)for(int z=7;z<=13;z++) {
            if(x==10||z==10)set(w,x,17,z,Blocks.CUT_SANDSTONE.defaultBlockState());
            if(x>=9&&x<=11&&z>=9&&z<=11)set(w,x,17,z,ORANGE.defaultBlockState());
            if(Math.floorMod(x,3)==1&&Math.floorMod(z,3)==1)
                set(w,x,24,z,Blocks.CUT_SANDSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP));
        }
        set(w,10,17,10,BLUE.defaultBlockState());
        // Deep wall bays and a stepped funerary stele give the hall a visible destination.
        for(int x:new int[]{5,15}) {
            clear(w,x,19,8,x,22,12);
            for(int z:new int[]{7,13})box(w,x,18,z,x,23,z,Blocks.CUT_SANDSTONE);
            box(w,x,23,7,x,23,13,Blocks.CHISELED_SANDSTONE);
        }
        clear(w,8,19,15,12,22,15);
        for(int x:new int[]{7,13})box(w,x,18,15,x,23,15,Blocks.CUT_SANDSTONE);
        box(w,7,23,15,13,23,15,Blocks.CHISELED_SANDSTONE);
        set(w,10,23,15,BLUE.defaultBlockState());
        for(int x:new int[]{8,12})box(w,x,19,16,x,22,16,ORANGE);
        box(w,8,18,12,12,18,13,Blocks.CUT_SANDSTONE_SLAB);
        box(w,9,18,13,11,18,13,Blocks.CUT_SANDSTONE);
        box(w,9,19,13,11,20,13,Blocks.CUT_SANDSTONE);
        set(w,10,21,13,Blocks.CHISELED_SANDSTONE.defaultBlockState());
        for(int z:new int[]{9,11}){torch(w,5,20,z,1,0);torch(w,15,20,z,-1,0);}
        torch(w,8,20,-3,1,0);torch(w,12,20,-3,-1,0);
        // Three-wide west landing reaches the single descending flight within the foundation.
        box(w,-3,17,3,10,17,5,Blocks.CUT_SANDSTONE);
        clear(w,-3,18,3,10,21,5);
        for(int z:new int[]{2,6})box(w,-3,18,z,-3,20,z,Blocks.CUT_SANDSTONE);
        box(w,-3,21,2,-3,21,6,Blocks.CHISELED_SANDSTONE);set(w,-3,21,4,ORANGE.defaultBlockState());
        for(int x=-2;x<=9;x++)set(w,x,17,4,Math.floorMod(x,3)==0?Blocks.CHISELED_SANDSTONE.defaultBlockState():ORANGE.defaultBlockState());
    }
    private static void crypt(Writer w) {
        // Suitable old gallery positions form the crypt of the replacement. No outlying rooms
        // are appended: the circuit, two chambers and observation path are authored together.
        box(w,-10,-12,-10,30,-7,30,Blocks.SANDSTONE);
        room(w,-10,-5,-10,30,5,30,Blocks.SANDSTONE);
        box(w,-5,-5,-5,25,0,25,Blocks.CUT_SANDSTONE);
        room(w,-16,-5,-5,-10,5,5,Blocks.CUT_SANDSTONE);
        room(w,30,-5,15,34,6,27,Blocks.SANDSTONE);
        clear(w,-12,-5,-1,-6,-2,1);clear(w,26,-5,18,31,-2,22);
        for(int t=-7;t<=27;t+=6) {
            archX(w,t,-5,-7);archX(w,t,-5,27);archZ(w,-7,-5,t);archZ(w,27,-5,t);
        }
        for(int along=-10;along<=30;along++)for(int side:new int[]{-10,30})for(int y=-5;y<0;y++) {
            wall(w,along,y,side,along);wall(w,side,y,along,along);
        }
        for(int along=-5;along<=25;along++)for(int side:new int[]{-5,25})for(int y=-5;y<0;y++) {
            wall(w,along,y,side,along);wall(w,side,y,along,along);
        }
        for(int x=-9;x<=29;x++)for(int z=-9;z<=29;z++)if((x< -5||x>25||z< -5||z>25)&&Math.floorMod(x+z,6)<2)
            set(w,x,0,z,Blocks.CUT_SANDSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP));
        for(int z:new int[]{-4,3}) {
            box(w,-15,-5,z,-11,-5,z+1,Blocks.CHISELED_SANDSTONE);
            box(w,-15,-4,z,-11,-4,z+1,Blocks.CUT_SANDSTONE_SLAB);
        }
        // The collapsed sand bay is scenery without new loot or brushable blocks.
        box(w,29,-5,16,33,-4,19,Blocks.SAND);box(w,31,-3,17,33,-3,19,Blocks.SAND);
        box(w,32,-2,18,33,-2,19,Blocks.SANDSTONE);
        for(int a:new int[]{-2,10,26}) {
            torch(w,a,-2,-9,0,1);torch(w,a,-2,29,0,-1);
            torch(w,-9,-2,a,1,0);torch(w,29,-2,a,-1,0);
        }
        for(int[] p:new int[][]{{27,8},{14,27}}) {
            set(w,p[0],-5,p[1],Blocks.CHISELED_SANDSTONE.defaultBlockState());
            set(w,p[0],-4,p[1],Blocks.TORCH.defaultBlockState());
        }
        // Keep the west arch post solid; a standing lamp here would sever its support.
        torch(w,-8,-3,-5,-1,0);
    }
    private static void approach(Writer w,int entry) {
        // The saved height belongs to the same central surface column as this approach.
        // Open the entire column above each step so unsupported natural sand cannot fall in.
        for(int i=0;i<=20;i++) {
            int z=-24+i,face=z+21,y=Math.max(entry,Math.min(17,face));
            box(w,8,-12,z,12,y,z,Blocks.SANDSTONE);
            clear(w,8,y+1,z,12,40,z);
            for(int x=8;x<=12;x++)set(w,x,y,z,face>entry?stairs(w,new int[]{0,1}):Blocks.CUT_SANDSTONE.defaultBlockState());
            for(int x:new int[]{7,13})if(face>entry) {
                box(w,x,-12,z,x,y,z,Blocks.SANDSTONE);
                set(w,x,y+1,z,(i%4==0?Blocks.CHISELED_SANDSTONE:Blocks.CUT_SANDSTONE).defaultBlockState());
            }
        }
        // The start is a full, five-wide landing at the sampled natural ground, not a stair lip.
        box(w,8,-12,-26,12,entry,-25,Blocks.SANDSTONE);
        clear(w,8,entry+1,-26,12,40,-25);
    }
    private static void descent(Writer w) {
        for(int i=0;i<=22;i++) {
            int y=17-i,z=5+i;
            box(w,-3,-12,z,-1,y,z,Blocks.SANDSTONE);
            clear(w,-3,y+1,z,-1,y+4,z);
            for(int x=-3;x<=-1;x++)set(w,x,y,z,stairs(w,new int[]{0,-1}));
            if(i>=4&&i%5==4) {
                box(w,-4,y+1,z,-4,y+4,z,Blocks.CUT_SANDSTONE);
                box(w,0,y+1,z,0,y+4,z,Blocks.CUT_SANDSTONE);
                box(w,-3,y+4,z,-1,y+4,z,Blocks.CHISELED_SANDSTONE);
                torch(w,-3,y+2,z,1,0);
            }
        }
    }
    private static void nativeEntry(Writer w) {
        // A flight from the front gallery reaches the unchanged native central doorway.
        for(int i=0;i<=6;i++) {
            int z=-7+i,y=-6+i;
            box(w,8,-12,z,12,y,z,Blocks.SANDSTONE);
            clear(w,8,y+1,z,12,y+4,z);
            for(int x=8;x<=12;x++)set(w,x,y,z,stairs(w,new int[]{0,1}));
        }
        // Cross beneath the approach parapets from either side of the gallery.
        // Adjacent foundation carries the masonry above these four-high apertures.
        for(int x:new int[]{7,13})clear(w,x,-5,-9,x,-2,-7);
    }
    private static void observation(Writer w) {
        // These are precisely the earlier passage writes inside the native bounds. A fresh
        // vanilla tomb has solid masonry here; preserving a composed baseline is insufficient.
        for(int x=-7;x<=8;x++)for(int z=9;z<=11;z++) {
            for(int y=-5;y<=-3;y++)w.set(x,y,z,AIR);
            w.set(x,-6,z,Blocks.CUT_SANDSTONE.defaultBlockState());
        }
        for(int z=9;z<=11;z++) {
            for(int y=-5;y<=-4;y++)w.set(8,y,z,Blocks.IRON_BARS.defaultBlockState());
            for(int x=6;x<=7;x++)w.set(x,-6,z,ORANGE.defaultBlockState());
        }
        for(int x=0;x<=7;x++)w.set(x,-6,10,(x%3==1?Blocks.CHISELED_SANDSTONE:ORANGE).defaultBlockState());
        // This is the existing custom observation passage, outside the native mechanisms.
        for(int x:new int[]{1,4,6}) {
            for(int z:new int[]{9,11})for(int y=-5;y<=-3;y++)w.set(x,y,z,(y==-4?Blocks.CUT_SANDSTONE:Blocks.CHISELED_SANDSTONE).defaultBlockState());
            w.set(x,-3,10,Blocks.CHISELED_SANDSTONE.defaultBlockState());
        }
        torch(w,-4,-3,9,0,1);torch(w,-1,-3,9,0,1);
    }
    private static void plant(Writer w,int x,int y,int z) {
        var old=w.get(x,y,z);if(old!=null&&(old.is(Blocks.CACTUS)||old.is(Blocks.DEAD_BUSH)))w.set(x,y,z,AIR);
    }
    private static void burial(Writer w,long seed) {
        // Rounded wind lobes taper to y6 at the admitted edge. Every sand column rests on
        // the existing full foundation; no reads of mutable neighboring chunks are needed.
        double phase=(seed&255)*Math.PI/128;
        for(int x=-15;x<=35;x++)for(int z=-15;z<=35;z++) {
            if(Math.abs(x-10)<=15&&Math.abs(z-10)<=15)continue;
            int inset=Math.min(Math.min(x+15,35-x),Math.min(z+15,35-z));
            int dx=Math.max(0,Math.abs(x-10)-17),dz=Math.max(0,Math.abs(z-10)-17);
            double wind=1.8*Math.sin(x*.19+z*.08+phase)+Math.cos(z*.15-phase);
            int top=Math.min(6+inset,Math.max(6,(int)Math.floor(13-Math.hypot(dx,dz)+wind)));
            for(int y=6;y<=top;y++)set(w,x,y,z,Blocks.SAND.defaultBlockState());
        }
    }
    private static void frieze(Writer w,int x,int y,int z,int dx,int dz,int half) {
        int ax=-dz,az=dx;
        for(int a=-half-1;a<=half+1;a++)for(int h=0;h<=2;h++) {
            int px=x+a*ax,pz=z+a*az;
            if(Math.abs(a)==half+1||h==2)set(w,px,y+h,pz,(h==2?Blocks.CHISELED_SANDSTONE:Blocks.CUT_SANDSTONE).defaultBlockState());
            else {
                set(w,px+dx,y+h,pz+dz,(Math.abs(a)==1&&h==1?ORANGE:Blocks.CHISELED_SANDSTONE).defaultBlockState());
                set(w,px,y+h,pz,h==0?Blocks.CUT_SANDSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP):AIR);
            }
        }
    }
    private static void seal(Writer w,int x,int y,int z,int dx,int dz) {
        int ax=-dz,az=dx;
        for(int a=-3;a<=3;a++)for(int h=0;h<=2;h++) {
            int px=x+a*ax,pz=z+a*az;
            set(w,px+dx,y+h,pz+dz,Blocks.CHISELED_SANDSTONE.defaultBlockState());
            BlockState face=Math.abs(a)==3?Blocks.CUT_SANDSTONE.defaultBlockState():
                    Math.abs(a)+Math.abs(h-1)<=1?ORANGE.defaultBlockState():AIR;
            set(w,px,y+h,pz,face);
        }
    }
    private static void panel(Writer w,int x,int y,int z,int dx,int dz) {
        // Shallow niches are cut into the terrace face, with a solid backed relief.
        int ax=-dz,az=dx;
        for(int a=-2;a<=2;a++)for(int h=0;h<=2;h++) {
            int px=x+a*ax,pz=z+a*az;
            if(Math.abs(a)==2||h==2||h==0)set(w,px,y+h,pz,(h==2?Blocks.CHISELED_SANDSTONE:Blocks.CUT_SANDSTONE).defaultBlockState());
            else {
                set(w,px,y+h,pz,AIR);
                set(w,px+dx,y+h,pz+dz,(a==0?ORANGE:Blocks.SMOOTH_SANDSTONE).defaultBlockState());
            }
        }
    }
    private static void wall(Writer w,int x,int y,int z,int along) {
        if(y<=-2&&(x==-10&&z>=-1&&z<=1||x==30&&z>=18&&z<=22))return;
        int phase=Math.floorMod(along+1,6);
        Block b=y==-5||y==-1?Blocks.SMOOTH_SANDSTONE:y==-3?(phase==0?Blocks.CHISELED_SANDSTONE:phase==2||phase==3?ORANGE:Blocks.CUT_SANDSTONE):Blocks.CUT_SANDSTONE;
        set(w,x,y,z,b.defaultBlockState());
    }
    private static void archX(Writer w,int x,int y,int z) {
        box(w,x,y,z-2,x,y+3,z-2,Blocks.CUT_SANDSTONE);box(w,x,y,z+2,x,y+3,z+2,Blocks.CUT_SANDSTONE);
        box(w,x,y+4,z-1,x,y+4,z+1,Blocks.CHISELED_SANDSTONE);
    }
    private static void archZ(Writer w,int x,int y,int z) {
        box(w,x-2,y,z,x-2,y+3,z,Blocks.CUT_SANDSTONE);box(w,x+2,y,z,x+2,y+3,z,Blocks.CUT_SANDSTONE);
        box(w,x-1,y+4,z,x+1,y+4,z,Blocks.CHISELED_SANDSTONE);
    }
    private static void room(Writer w,int x,int y,int z,int hx,int height,int hz,Block b) {
        box(w,x,y-1,z,hx,y+height,hz,b);clear(w,x+1,y,z+1,hx-1,y+height-1,hz-1);
    }
    private static void torch(Writer w,int x,int y,int z,int dx,int dz) {
        set(w,x,y,z,Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,w.direction(dx,dz)));
    }
    private static BlockState stairs(Writer w,int[] d){return Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,w.direction(d[0],d[1]));}
    private static int[] inward(int x,int z,int lo,int hi){return x==lo?new int[]{1,0}:x==hi?new int[]{-1,0}:z==lo?new int[]{0,1}:new int[]{0,-1};}
    private static boolean nativeCore(int x,int y,int z){return x>=0&&x<=20&&z>=0&&z<=20&&y>=-14&&y<=16;}
    private static void set(Writer w,int x,int y,int z,BlockState state){if(!nativeCore(x,y,z))w.set(x,y,z,state);}
    private static void box(Writer w,int x,int y,int z,int hx,int hy,int hz,Block b){for(int a=x;a<=hx;a++)for(int c=z;c<=hz;c++)for(int k=y;k<=hy;k++)set(w,a,k,c,b.defaultBlockState());}
    private static void clear(Writer w,int x,int y,int z,int hx,int hy,int hz){for(int a=x;a<=hx;a++)for(int c=z;c<=hz;c++)for(int k=y;k<=hy;k++)set(w,a,k,c,AIR);}
}
