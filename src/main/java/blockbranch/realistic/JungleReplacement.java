package blockbranch.realistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Complete jungle layout 1. A central stepped temple gives the two stair shrines a shared
 * hierarchy; reused stairs, bridge gaps and the native trap rooms remain gameplay components.
 * Writes are local and owned by the current chunk. The caller selects this saved recipe before
 * the old layout runs, so earlier pieces never inherit this replacement accidentally. */
final class JungleReplacement {
    interface Writer {
        void set(int x,int y,int z,BlockState state);
        BlockState get(int x,int y,int z);
        Direction direction(int dx,int dz);
    }
    private JungleReplacement() {}
    private static final BlockState AIR=Blocks.AIR.defaultBlockState();
    static void generate(Writer w,long seed) {
        tower(w,-13,-4,21,seed);tower(w,16,13,27,seed);
        temple(w,seed);
        // Original exploration circuit is now cut through the temple's terraced shoulders.
        bridge(w,-5,9,1,0,3);
        bridge(w,11,9,12,16,14);
        bridge(w,-10,18,4,-8,21);bridge(w,-8,18,19,16,21);
        bridge(w,15,9,10,21,12);
        clear(w,-5,10,1,-5,12,3);clear(w,-1,10,1,0,12,3);
        clear(w,11,10,12,12,12,14);clear(w,15,10,12,16,12,12);
        clear(w,-10,19,4,-8,21,6);
        clear(w,19,10,12,21,12,13);clear(w,16,19,19,16,21,21);
        clear(w,4,19,19,7,21,19);
        nativeBridgeLandings(w);
        // Deliberate one-block gaps remain readable; no decoration fills their flight paths.
        clear(w,-2,1,1,-2,12,3);clear(w,14,1,12,14,12,14);
        box(w,-1,-12,2,-1,8,2,Blocks.MOSSY_COBBLESTONE);
        box(w,12,-12,13,12,8,13,Blocks.MOSSY_COBBLESTONE);
        // A shared ground terrace joins the stair shrines to the original lower temple entry.
        box(w,-13,-12,-8,24,-1,-5,Blocks.COBBLESTONE);
        box(w,-13,0,-8,24,0,-5,Blocks.MOSSY_COBBLESTONE);
        clear(w,-13,1,-8,24,4,-5);
        box(w,-13,1,-8,-13,2,-5,Blocks.MOSSY_STONE_BRICKS);
        box(w,24,1,-8,24,2,-5,Blocks.MOSSY_STONE_BRICKS);
        cistern(w);
        interior(w,seed);
        for(int[] p:new int[][]{{-12,-7},{22,-7},{-12,3},{17,20}}) {
            set(w,p[0],0,p[1],Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            set(w,p[0],1,p[1],Blocks.TORCH.defaultBlockState());
        }
    }
    private static void nativeBridgeLandings(Writer w) {
        // These six original landing blocks overlap the vanilla temple bounds. They must be
        // written explicitly: a fresh native temple has air here, unlike the composed baseline.
        for(int z=1;z<=3;z++)w.set(0,9,z,Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        for(int z=12;z<=14;z++)w.set(11,9,z,Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        w.set(0,10,1,Blocks.COBBLESTONE_WALL.defaultBlockState());
        w.set(0,10,3,Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        w.set(11,10,12,Blocks.COBBLESTONE_WALL.defaultBlockState());
        w.set(11,10,14,Blocks.COBBLESTONE_WALL.defaultBlockState());
    }
    private static void temple(Writer w,long seed) {
        // Battered courses replace the isolated small shrine with the main architectural mass.
        // The original room/roof head space is reserved through y12, not filled by the shell.
        for(int tier=0;tier<6;tier++) {
            int inset=tier,loX=-4+inset,hiX=15-inset,loZ=-3+inset,hiZ=18-inset;
            int low=tier*3,high=low+2;
            for(int x=loX;x<=hiX;x++)for(int z=loZ;z<=hiZ;z++)for(int y=low;y<=high;y++) {
                boolean edge=x==loX||x==hiX||z==loZ||z==hiZ;
                var state=stone(x,y,z,seed);
                if(edge&&y==high)state=stair(w,inward(x,z,loX,hiX,loZ,hiZ),Half.BOTTOM);
                set(w,x,y,z,state);
            }
        }
        // Outer foundation courses stay within the new body, without clearing natural terrain.
        for(int x=-4;x<=15;x++)for(int z=-3;z<=18;z++)if(!nativeCore(x,-1,z))
            for(int y=-12;y<0;y++)set(w,x,y,z,stone(x,y,z,seed));
        // The reserved native roof walkway stays open through y12. Seat the upper mass on
        // visible piers outside that volume, rather than suspending it from the high bridge.
        for(int x:new int[]{0,10})box(w,x,9,-1,x+1,13,-1,Blocks.MOSSY_STONE_BRICKS);
        box(w,0,13,-1,11,13,0,Blocks.CHISELED_STONE_BRICKS);
        for(int x:new int[]{-1,12})for(int z:new int[]{6,9})
            box(w,x,12,z,x,13,z,Blocks.MOSSY_STONE_BRICKS);
        // Broad axial portal passes through the battered front rather than adding a portico.
        box(w,3,-12,-4,8,0,-4,Blocks.MOSSY_STONE_BRICKS);
        clear(w,4,1,-4,7,4,0);
        for(int x:new int[]{3,8})for(int y=1;y<=4;y++)set(w,x,y,-3,Blocks.STONE_BRICKS.defaultBlockState());
        for(int x=3;x<=8;x++)set(w,x,5,-3,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        // The high circuit leads into a three-high roof sanctuary, with real supporting walls.
        box(w,3,18,4,8,18,12,Blocks.STONE_BRICKS);
        box(w,4,18,12,7,18,21,Blocks.MOSSY_STONE_BRICKS);
        clear(w,4,19,5,7,21,21);
        for(int x:new int[]{3,8})for(int z:new int[]{4,11})
            box(w,x,19,z,x,21,z,Blocks.MOSSY_STONE_BRICKS);
        for(int x=3;x<=8;x++)for(int z=4;z<=11;z++)if(x==3||x==8||z==4||z==11) {
            if(z==11&&x>=4&&x<=7)continue;
            set(w,x,19,z,Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
            set(w,x,21,z,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        }
        for(int tier=0;tier<3;tier++) {
            int lx=2+tier,hx=9-tier,lz=3+tier,hz=12-tier,y=22+tier;
            box(w,lx,y,lz,hx,y,hz,Blocks.MOSSY_STONE_BRICKS);
            for(int x=lx;x<=hx;x++)for(int z=lz;z<=hz;z++)if(x==lx||x==hx||z==lz||z==hz)
                set(w,x,y,z,stair(w,inward(x,z,lx,hx,lz,hz),Half.BOTTOM));
        }
        // Window slots and recessed relief articulate the roof sanctuary without blocking entry.
        for(int x:new int[]{3,8})clear(w,x,20,6,x,20,9);
    }
    private static void tower(Writer w,int x,int z,int height,long seed) {
        box(w,x,-12,z,x+8,-1,z+8,Blocks.COBBLESTONE);
        // Attached battered feet replace the abrupt vertical wall-to-clearing junction.
        for(int a=-1;a<=9;a++)for(int b=-1;b<=9;b++)if(a==-1||a==9||b==-1||b==9) {
            box(w,x+a,-12,z+b,x+a,-1,z+b,Blocks.MOSSY_COBBLESTONE);
            set(w,x+a,0,z+b,stair(w,inward(a,b,-1,9,-1,9),Half.BOTTOM));
        }
        for(int a=0;a<=8;a++)for(int b=0;b<=8;b++)for(int y=-1;y<=height;y++) {
            boolean wall=a==0||a==8||b==0||b==8;
            set(w,x+a,y,z+b,wall||y==-1||y==height?stone(x+a,y,z+b,seed):AIR);
        }
        for(int h=2;h<height-2;h+=6) {
            clear(w,x+3,h,z,x+5,h+2,z);clear(w,x+8,h,z+3,x+8,h+2,z+5);
        }
        clear(w,x+3,0,z,x+5,3,z);clear(w,x+3,0,z+8,x+5,3,z+8);
        for(int h:new int[]{9,18})if(h<height) {
            box(w,x+1,h,z+1,x+7,h,z+7,Blocks.MOSSY_COBBLESTONE_SLAB);
            clear(w,x+3,h+1,z,x+5,h+3,z);
            clear(w,x+3,h+1,z+8,x+5,h+3,z+8);
            clear(w,x,h+1,z+3,x,h+3,z+5);
            clear(w,x+8,h+1,z+3,x+8,h+3,z+5);
        }
        int[][] ring={{1,1},{2,1},{3,1},{4,1},{5,1},{6,1},{7,1},{7,2},{7,3},{7,4},{7,5},{7,6},{7,7},{6,7},{5,7},{4,7},{3,7},{2,7},{1,7},{1,6},{1,5},{1,4},{1,3},{1,2}};
        for(int h=0;h<height;h++) {
            var p=ring[h%24];var n=ring[(h+1)%24];
            clear(w,x+p[0],h+1,z+p[1],x+p[0],h+3,z+p[1]);
            set(w,x+p[0],h-1,z+p[1],Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            set(w,x+p[0],h,z+p[1],Blocks.MOSSY_COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,w.direction(n[0]-p[0],n[1]-p[1])));
        }
        clear(w,x+2,height,z+2,x+6,height,z+6);
        // Broad stone jambs and paired horizontal courses replace plain castle shafts.
        for(int a:new int[]{0,8})for(int b:new int[]{0,8})for(int y=0;y<=height;y++)
            set(w,x+a,y,z+b,Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        for(int h:new int[]{5,14,23})if(h<height)for(int a=1;a<8;a++)for(int edge:new int[]{0,8}) {
            set(w,x+a,h,z+edge,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            set(w,x+edge,h,z+a,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        }
        for(int tier=0;tier<3;tier++)for(int a=-1+tier;a<=9-tier;a++)for(int b=-1+tier;b<=9-tier;b++) {
            if(a!=-1+tier&&a!=9-tier&&b!=-1+tier&&b!=9-tier)continue;
            // Keep the highest stair's standing/head cells and the open central light well.
            if(a>=1&&a<=7&&b>=1&&b<=7)continue;
            set(w,x+a,height+tier,z+b,stair(w,inward(a,b,-1+tier,9-tier,-1+tier,9-tier),Half.BOTTOM));
        }
        // The upper shafts become open pavilions with broad corner piers, not competing boxes.
        for(int y=12;y<=height-2;y++)for(int a=2;a<=6;a++)for(int edge:new int[]{0,8}) {
            if(y==18||y==height-2)continue;
            set(w,x+a,y,z+edge,AIR);set(w,x+edge,y,z+a,AIR);
        }
        clear(w,x+3,0,z-1,x+5,3,z);clear(w,x+3,0,z+8,x+5,3,z+9);
        // Growth remains attached to the retained lower masonry, below the open pavilion bays.
        for(int y=3;y<Math.min(12,height-3);y++) {
            set(w,x+2,y,z-1,vine(w,0,1));
            if(y<height/2)set(w,x-1,y,z+2,vine(w,1,0));
        }
    }
    private static void cistern(Writer w) {
        box(w,18,24,15,27,24,19,Blocks.STONE_BRICKS);box(w,18,25,15,27,25,19,Blocks.MOSSY_STONE_BRICKS);
        clear(w,19,25,16,26,25,18);
        box(w,25,0,16,27,24,18,Blocks.STONE_BRICKS);clear(w,26,1,17,26,25,17);
        box(w,13,0,16,26,0,18,Blocks.STONE_BRICKS);
        box(w,13,1,16,25,2,16,Blocks.MOSSY_COBBLESTONE);box(w,13,1,18,25,2,18,Blocks.MOSSY_COBBLESTONE);
        clear(w,13,1,17,26,2,17);
        box(w,11,0,16,15,0,21,Blocks.STONE_BRICKS);box(w,11,1,16,15,1,21,Blocks.MOSSY_COBBLESTONE);
        clear(w,12,1,17,14,1,20);clear(w,15,1,17,15,1,17);
        set(w,26,25,17,Blocks.WATER.defaultBlockState());
    }
    private static void interior(Writer w,long seed) {
        // Capture each original column before carving. All reads stay in that owned column;
        // neighboring chunks and generation order cannot alter eligibility.
        for(int x=0;x<12;x++)for(int z=0;z<15;z++) {
            BlockState[] old=new BlockState[18];for(int y=-6;y<=11;y++)old[y+6]=w.get(x,y,z);
            for(int y=-5;y<=10;y++) {
                var at=old[y+6];if(at==null||!masonry(at))continue;
                var above=old[y+7];var below=old[y+5];var state=stone(x,y,z,seed);
                if(y==0||y==-4)state=(x==5||x==6||Math.floorMod(x+z,4)==0?Blocks.STONE_BRICKS:Blocks.MOSSY_STONE_BRICKS).defaultBlockState();
                int dx=x==2?-1:x==9?1:0,dz=0;
                boolean side=(x==2||x==9)&&(z==4||z==5||z==7||z==8);
                boolean end=(z==2||z==12)&&(x==3||x==8);
                if(end){dx=0;dz=z==2?-1:1;}
                if((y==1||y==2)&&(side||end)&&above!=null&&below!=null&&masonry(above)&&masonry(below))
                    state=stair(w,new int[]{dx,dz},Half.TOP);
                else if(y>=3&&above!=null&&masonry(above)&&below!=null&&below.isAir()&&x%3==1&&z%3==1)
                    state=Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP);
                w.set(x,y,z,state);
            }
        }
    }
    private static boolean masonry(BlockState s){return s.is(Blocks.COBBLESTONE)||s.is(Blocks.MOSSY_COBBLESTONE)||s.is(Blocks.STONE_BRICKS)||s.is(Blocks.MOSSY_STONE_BRICKS)||s.is(Blocks.CRACKED_STONE_BRICKS)||s.is(Blocks.CHISELED_STONE_BRICKS);}
    private static BlockState stone(int x,int y,int z,long seed) {
        // Broad damp patches belong to the low courses; deliberate cornices are placed separately.
        double damp=Math.sin(x*.43+(seed&255))*.45+Math.cos(z*.37+((seed>>>8)&255))*.4+.5-y*.025;
        return (damp>.15?Blocks.MOSSY_COBBLESTONE:Blocks.COBBLESTONE).defaultBlockState();
    }
    private static int[] inward(int x,int z,int lx,int hx,int lz,int hz){return x==lx?new int[]{1,0}:x==hx?new int[]{-1,0}:z==lz?new int[]{0,1}:new int[]{0,-1};}
    private static BlockState stair(Writer w,int[] d,Half half){return Blocks.MOSSY_STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,w.direction(d[0],d[1])).setValue(StairBlock.HALF,half);}
    private static BlockState vine(Writer w,int dx,int dz){return Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(w.direction(dx,dz)),true);}
    private static boolean nativeCore(int x,int y,int z){return x>=0&&x<12&&z>=0&&z<15&&y>=-5&&y<=12;}
    private static void set(Writer w,int x,int y,int z,BlockState s){if(!nativeCore(x,y,z))w.set(x,y,z,s);}
    private static void box(Writer w,int x,int y,int z,int hx,int hy,int hz,Block b){for(int a=x;a<=hx;a++)for(int c=z;c<=hz;c++)for(int k=y;k<=hy;k++)set(w,a,k,c,b.defaultBlockState());}
    private static void clear(Writer w,int x,int y,int z,int hx,int hy,int hz){box(w,x,y,z,hx,hy,hz,Blocks.AIR);}
    private static void bridge(Writer w,int x,int y,int z,int hx,int hz){
        box(w,x,y,z,hx,y,hz,Blocks.MOSSY_COBBLESTONE);
        clear(w,x,y+1,z,hx,y+3,hz);
        if(hz-z>hx-x){box(w,x,y+1,z,x,y+1,hz,Blocks.MOSSY_STONE_BRICKS);box(w,hx,y+1,z,hx,y+1,hz,Blocks.MOSSY_STONE_BRICKS);}
        else {box(w,x,y+1,z,hx,y+1,z,Blocks.MOSSY_STONE_BRICKS);box(w,x,y+1,hz,hx,y+1,hz,Blocks.MOSSY_STONE_BRICKS);}
    }
}
