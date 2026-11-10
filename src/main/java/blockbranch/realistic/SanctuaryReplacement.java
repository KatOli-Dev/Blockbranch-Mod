package blockbranch.realistic;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.BooleanOp;

/** Replacement layout 1: a terraced temple body, hipped lantern roof and open processional court.
 * Native rooms are gameplay components, not a shell to extend. The conservative room masks include
 * child writes outside their nominal boxes; native generation and RNG ordering remain authoritative.
 * The retained ambulatories are routes within this layout. No new building or footprint is appended. */
final class SanctuaryReplacement {
    interface Writer {
        void set(int x,int y,int z,BlockState state);
        BlockState get(int x,int y,int z);
        Direction direction(int dx,int dz);
    }
    private SanctuaryReplacement() {}
    static final BlockState WATER=Blocks.WATER.defaultBlockState();

    /** Reserve every possible native room, including protruding ceilings, wing writes and connectors. */
    static boolean gameplay(int x,int y,int z) {
        return inside(x,y,z,9,0,22,48,8,53)||inside(x,y,z,17,8,22,40,12,37)
                ||inside(x,y,z,22,12,22,35,17,35)
                ||inside(x,y,z,1,1,1,23,8,22)||inside(x,y,z,34,1,1,56,8,22)
                ||inside(x,y,z,25,0,0,32,8,20)||inside(x,y,z,25,0,21,32,4,21);
    }
    private static boolean inside(int x,int y,int z,int a,int b,int c,int d,int e,int f) {
        return x>=a&&x<=d&&y>=b&&y<=e&&z>=c&&z<=f;
    }
    static void generate(Writer w) {
        // Reuse the two established swimming circuits as open-windowed ambulatory walls.
        for(int y:new int[]{1,12}) {
            hall(w,-12,y,-12,69,y+4,-6);hall(w,-12,y,63,69,y+4,69);
            hall(w,-12,y,-6,-6,y+4,63);hall(w,63,y,-6,69,y+4,63);
        }
        pylon(w,-14,-14,22);pylon(w,63,-14,29);pylon(w,-14,63,34);pylon(w,63,63,25);
        court(w);
        // The established bounded foundations reach below the lowest admitted seabed.
        for(int along=-12;along<=69;along+=9)for(int side:new int[]{-10,67})
            for(int dx=0;dx<=1;dx++)for(int dz=0;dz<=1;dz++)for(int y=-34;y<=0;y++) {
                var s=block(y==-2?Blocks.DARK_PRISMARINE:Blocks.PRISMARINE_BRICKS);
                outer(w,along+dx,y,side+dz,s);outer(w,side+dx,y,along+dz,s);
            }
        body(w);interior(w);vaults(w);terraceTiles(w);foundations(w);podium(w);
    }
    private static void hall(Writer w,int x,int y,int z,int mx,int my,int mz) {
        box(w,x,y-1,z,mx,my,mz,Blocks.PRISMARINE_BRICKS);
        box(w,x+1,y,z+1,mx-1,my-1,mz-1,Blocks.WATER);
        if(mx-x>mz-z)for(int a=x+4;a<mx-2;a+=8) {
            box(w,a,y+1,z,a+2,y+2,z,Blocks.WATER);box(w,a,y+1,mz,a+2,y+2,mz,Blocks.WATER);
            outer(w,a,y-1,(z+mz)/2,block(Blocks.SEA_LANTERN));
        } else for(int a=z+4;a<mz-2;a+=8) {
            box(w,x,y+1,a,x,y+2,a+2,Blocks.WATER);box(w,mx,y+1,a,mx,y+2,a+2,Blocks.WATER);
            outer(w,(x+mx)/2,y-1,a,block(Blocks.SEA_LANTERN));
        }
        SanctuaryStonework.hall((a,b,c,d)->outer(w,a,b,c,block(d)),x,y,z,mx,my,mz);
    }
    private static void pylon(Writer w,int x,int z,int height) {
        box(w,x,-34,z,x+8,height,z+8,Blocks.PRISMARINE);
        box(w,x+1,1,z+1,x+7,height-1,z+7,Blocks.WATER);
        for(int y:new int[]{1,12}) {
            box(w,x+2,y,z,x+6,y+3,z+8,Blocks.WATER);box(w,x,y,z+2,x+8,y+3,z+6,Blocks.WATER);
        }
        // Reused lower shafts and crowns retain their openings and original light positions.
        for(int y=6;y<height;y+=6) {
            box(w,x,y,z,x+8,y,z,Blocks.DARK_PRISMARINE);box(w,x,y,z+8,x+8,y,z+8,Blocks.DARK_PRISMARINE);
            outer(w,x+4,y,z,block(Blocks.SEA_LANTERN));outer(w,x+4,y,z+8,block(Blocks.SEA_LANTERN));
        }
        box(w,x+2,height,z+2,x+6,height,z+6,Blocks.WATER);
        SanctuaryStonework.tower((a,b,c,d)->outer(w,a,b,c,block(d)),x,z,height);
        // Broad jambs and a central mullion retain a monumental pier around paired upper windows.
        for(int y=18;y<=height-3;y++)for(int edge:new int[]{0,8}) {
            for(int a:new int[]{1,7}){outer(w,x+a,y,z+edge,block(Blocks.PRISMARINE_BRICKS));outer(w,x+edge,y,z+a,block(Blocks.PRISMARINE_BRICKS));}
            if(y!=20&&y!=26&&y!=32){outer(w,x+4,y,z+edge,block(Blocks.DARK_PRISMARINE));outer(w,x+edge,y,z+4,block(Blocks.DARK_PRISMARINE));}
        }
        // Paired bays give the existing upper shafts lightness without a thin scaffold silhouette.
        for(int y=18;y<=height-3;y++)for(int a:new int[]{2,3,5,6})for(int edge:new int[]{0,8}) {
            if(y==20||y==26||y==32)continue;
            outer(w,x+a,y,z+edge,WATER);outer(w,x+edge,y,z+a,WATER);
        }
    }
    private static void court(Writer w) {
        box(w,24,-1,-12,33,0,-1,Blocks.PRISMARINE_BRICKS);
        box(w,24,1,-12,33,10,-1,Blocks.WATER);
        for(int z:new int[]{-12,-6,-1})for(int x:new int[]{24,33})
            box(w,x,1,z,x,9,z,Blocks.PRISMARINE_BRICKS);
        // The outer bridge is supported; the inner court opens directly to the main pediment.
        box(w,24,10,-12,33,10,-6,Blocks.PRISMARINE_BRICKS);
        box(w,24,10,-1,33,10,-1,Blocks.PRISMARINE_BRICKS);
        for(int x:new int[]{24,33})box(w,x,5,-11,x,5,-7,Blocks.PRISMARINE_BRICKS);
        for(int z=-12;z<=-1;z++)for(int x:new int[]{28,29})outer(w,x,0,z,block(Blocks.DARK_PRISMARINE));
    }
    private static void body(Writer w) {
        // The main body becomes a grounded battered mass, rather than thin floating shell strips.
        for(int z=23;z<=53;z++)for(int step=0;step<4;step++)for(int side:new int[]{0,1}) {
            int x=side==0?step:57-step,h=step+1;
            for(int y=1;y<h;y++)core(w,x,y,z,block(Blocks.PRISMARINE_BRICKS));
            core(w,x,h,z,stair(w,side==0?1:-1,0));
        }
        for(int z=23;z<=53;z++)for(int x:new int[]{4,5,6,51,52,53})
            for(int y=1;y<=4;y++)core(w,x,y,z,block(y==1?Blocks.DARK_PRISMARINE:Blocks.PRISMARINE_BRICKS));
        for(int z=23;z<=43;z++)for(int step=0;step<2;step++)for(int side:new int[]{0,1}) {
            int x=side==0?7+step:50-step,h=5+step;
            for(int y=1;y<h;y++)core(w,x,y,z,block(Blocks.PRISMARINE_BRICKS));
            core(w,x,h,z,stair(w,side==0?1:-1,0));
        }
        for(int z=23;z<=39;z++)for(int step=0;step<3;step++)for(int side:new int[]{0,1}) {
            int x=side==0?14+step:43-step,h=9+step;
            for(int y=9;y<h;y++)core(w,x,y,z,block(Blocks.PRISMARINE_BRICKS));
            core(w,x,h,z,stair(w,side==0?1:-1,0));
        }
        for(int step=0;step<4;step++)for(int x=step+1;x<=56-step;x++) {
            int z=57-step,h=step+1;
            for(int y=1;y<h;y++)core(w,x,y,z,block(Blocks.PRISMARINE_BRICKS));
            core(w,x,h,z,stair(w,0,-1));
        }
        for(int x=8;x<=49;x++) {
            for(int y=1;y<5;y++)core(w,x,y,54,block(Blocks.PRISMARINE_BRICKS));
            core(w,x,5,54,stair(w,0,-1));
        }
        for(int step=0;step<4;step++)for(int x=15+step;x<=42-step;x++) {
            int z=43-step,h=9+step;
            for(int y=9;y<h;y++)core(w,x,y,z,block(Blocks.PRISMARINE_BRICKS));
            core(w,x,h,z,stair(w,0,-1));
        }
        // Low hipped ridges replace the rear and elder-wing bead ornaments within their old roof mass.
        for(int x=12;x<=45;x++)for(int z=45;z<=52;z++)for(int y=9;y<=12;y++)core(w,x,y,z,WATER);
        // A broad central causeway explains the rear terrace and climbs the existing upper steps.
        for(int x=26;x<=31;x++)for(int z=44;z<=52;z++)core(w,x,9,z,lightSlab());
        for(int x=26;x<=31;x++)for(int step=0;step<4;step++)core(w,x,9+step,43-step,stair(w,0,-1));
        for(int x:new int[]{12,45})ridge(w,x,11,20);
        // Broad stepped shoulders are part of the rear roof mass, not isolated ornaments.
        for(int lo:new int[]{17,37})for(int x=lo;x<=lo+3;x++)for(int z=40;z<=50;z++) {
            int top=z<=44?12:z<=46?11:z<=48?10:9;
            for(int y=9;y<top;y++)core(w,x,y,z,block(Blocks.PRISMARINE_BRICKS));
            core(w,x,top,z,stair(w,0,-1));
        }
        for(int z=23;z<=52;z++)for(int x:new int[]{5,52})core(w,x,5,z,WATER);
        // Continuous, posted cornices replace isolated roof studs; openings above native routes win.
        for(int z=21;z<=52;z++)for(int x:new int[]{12,45})cornice(w,x,9,z,z-21,z==52);
        for(int x=12;x<=45;x++)if(x<26||x>31)cornice(w,x,9,52,x-12,x==45);
        for(int z=23;z<=38;z++)for(int x:new int[]{19,38})cornice(w,x,13,z,z-23,z==38);
        for(int x=19;x<=38;x++)cornice(w,x,13,38,x-19,x==38);
        pediment(w);crown(w);
    }
    private static void ridge(Writer w,int x,int from,int to) {
        for(int z=from;z<=to;z++) {
            core(w,x-1,9,z,stair(w,1,0));core(w,x+1,9,z,stair(w,-1,0));
            core(w,x,9,z,block(Blocks.DARK_PRISMARINE));
            if(z>from&&z<to)core(w,x,10,z,slab());
        }
    }
    private static void pediment(Writer w) {
        for(int x=15;x<=42;x++)for(int y=5;y<=12;y++) {
            var old=w.get(x,y,21);if(old==null||!masonry(old)||gameplay(x,y,21))continue;
            boolean rib=x==24||x==33||y==11;
            core(w,x,y,21,block(rib?Blocks.PRISMARINE_BRICKS:Blocks.PRISMARINE));
            if(x>=26&&x<=31&&y>=6&&y<=10&&Math.abs(2*x-57)+2*Math.abs(y-8)<=7)
                core(w,x,y,21,block(Blocks.DARK_PRISMARINE));
        }
        for(int x:new int[]{19,20,37,38})for(int y:new int[]{9,10})core(w,x,y,21,stair(w,0,1));
        for(int x=21;x<=36;x++)core(w,x,12,21,slab());
        for(int x=17;x<=20;x++)core(w,x,11,21,stair(w,1,0));
        for(int x=37;x<=40;x++)core(w,x,11,21,stair(w,-1,0));
    }
    private static void crown(Writer w) {
        for(int x=25;x<=32;x++)for(int z=25;z<=32;z++)for(int y=19;y<=22;y++)core(w,x,y,z,WATER);
        for(int tier=0;tier<3;tier++) {
            int lo=25+tier,hi=32-tier,y=19+tier;
            for(int x=lo;x<=hi;x++)for(int z=lo;z<=hi;z++) {
                boolean ex=x==lo||x==hi,ez=z==lo||z==hi;
                // The upper corners suspend the original lanterns over open light wells.
                var s=ex&&ez?block(Blocks.PRISMARINE_BRICKS):ex?stair(w,x==lo?1:-1,0):ez?stair(w,0,z==lo?1:-1):block(Blocks.DARK_PRISMARINE);
                core(w,x,y,z,s);
            }
        }

        // Keep the old downward light path into the penthouse and expose its lantern faces.
        // The canopy above supports each hanging lantern without an opaque floor below it.
        for(int x:new int[]{27,30})for(int z:new int[]{27,30}) {
            core(w,x,19,z,WATER);
            core(w,x==27?26:31,20,z,WATER);
            core(w,x,20,z==27?26:31,WATER);
        }
    }

    private static void cornice(Writer w,int x,int y,int z,int along,boolean end) {
        core(w,x,y,z,along%8==0||end?block(Blocks.PRISMARINE_BRICKS):lightSlab());
    }
    private static void interior(Writer w) {
        // Rearticulate existing rooms without moving a wall, light, reward or water cell.
        // Floors, jambs and ceiling courses use the native eight-block room rhythm.
        for(int x=0;x<=57;x++)for(int z=0;z<=57;z++)for(int y=0;y<=17;y++)if(gameplay(x,y,z)) {
            int a=Math.floorMod(x-9,8),b=Math.floorMod(z-22,8),course=Math.floorMod(y,4);
            Block material;
            if(course==0)material=a==3||a==4||b==3||b==4?Blocks.PRISMARINE_BRICKS:Blocks.DARK_PRISMARINE;
            else if(course==3)material=Blocks.PRISMARINE_BRICKS;
            else if((a==0||a==7)&&(b==2||b==5)||(b==0||b==7)&&(a==2||a==5))material=Blocks.PRISMARINE_BRICKS;
            else material=course==1?Blocks.DARK_PRISMARINE:Blocks.PRISMARINE;
            color(w,x,y,z,material);
        }
    }
    private static void terraceTiles(Writer w) {
        // Paired inset stone panels and a pale axial route organize the retained rear roof deck.
        for(int x=14;x<=43;x++)for(int z=44;z<=53;z++) {
            boolean panel=(x>=15&&x<=22||x>=35&&x<=42)&&z>=45&&z<=51;
            color(w,x,8,z,panel?Blocks.DARK_PRISMARINE:Blocks.PRISMARINE_BRICKS);
        }
    }
    private static void color(Writer w,int x,int y,int z,Block material) {
        var old=w.get(x,y,z);if(old!=null&&masonry(old))w.set(x,y,z,block(material));
    }
    private static void foundations(Writer w) {
        // Reshape only the existing native pier solids; each original ground contact stays put.
        // Natural seabed cells and fluids are never excavated or overwritten.
        for(int gx=0;gx<=6;gx++)for(int gz=0;gz<=6;gz++) {
            if(gx!=0&&gx!=6&&gz!=0&&gz!=6||gx==3&&gz==0)continue;
            for(int dx=0;dx<4;dx++)for(int dz=0;dz<4;dz++) {
                int x=gx*9+dx,z=gz*9+dz,bottom=0;
                for(int y=-34;y<0;y++){var old=w.get(x,y,z);if(old==null)break;if(masonry(old)){bottom=y;break;}}
                // An unobserved deeper contact or short stub keeps its original geometry.
                if(bottom==-34||bottom>-7)continue;
                boolean corner=(dx==0||dx==3)&&(dz==0||dz==3);
                for(int y=bottom;y<0;y++) {
                    var old=w.get(x,y,z);if(old==null||!masonry(old))continue;
                    BlockState state;
                    if(y==bottom)state=block(Blocks.DARK_PRISMARINE);
                    else if(y==bottom+1||y==-1)state=block(Blocks.PRISMARINE_BRICKS);
                    else if(y==-2)state=corner?slab().setValue(SlabBlock.TYPE,SlabType.TOP):block(Blocks.PRISMARINE_BRICKS);
                    else if(y==bottom+2)state=corner?slab():block(Blocks.DARK_PRISMARINE);
                    else if(corner)state=WATER;
                    else state=block(y==-3?Blocks.DARK_PRISMARINE:Blocks.PRISMARINE_BRICKS);
                    w.set(x,y,z,state);
                }
            }
        }
    }

    private static void vaults(Writer w) {
        // Carve only existing solid cells. Full backing floors and full wall back-faces remain,
        // so the extra half-block relief never connects rooms or closes an original passage.
        for(int x=0;x<=57;x++)for(int z=0;z<=57;z++)for(int y=1;y<=17;y++)if(gameplay(x,y,z)) {
            var old=w.get(x,y,z);if(old==null||!masonry(old))continue;
            int a=Math.floorMod(x-9,8),b=Math.floorMod(z-22,8),course=Math.floorMod(y,4);
            if(course==3&&a>=1&&a<=6&&b>=1&&b<=6&&a!=2&&a!=5&&b!=2&&b!=5) {
                var above=w.get(x,y+1,z);var below=w.get(x,y-1,z);
                if(above!=null&&masonry(above)&&below!=null&&below.is(Blocks.WATER))
                    w.set(x,y,z,slab().setValue(SlabBlock.TYPE,SlabType.TOP));
            } else if(course==2) {
                int dx=0,dz=0;
                if((a==0||a==7)&&(b==3||b==4))dx=a==0?-1:1;
                else if((b==0||b==7)&&(a==3||a==4))dz=b==0?-1:1;
                if(dx==0&&dz==0)continue;
                var above=w.get(x,y+1,z);var below=w.get(x,y-1,z);
                if(above!=null&&below!=null&&masonry(above)&&masonry(below))
                    w.set(x,y,z,stair(w,dx,dz).setValue(StairBlock.HALF,Half.TOP));
            }
        }
    }
    private static void podium(Writer w) {
        // The replacement's low battered seat follows the actual seabed within the old body.
        // Only water is occupied; natural terrain, native piers and the entrance channel stay put.
        for(int x=0;x<=57;x++)for(int z=0;z<=57;z++) {
            int edge=Math.min(Math.min(x,57-x),Math.min(z,57-z));if(edge>6)continue;
            var floor=w.get(x,0,z);if(floor==null||gameplay(x,0,z)&&floor.is(Blocks.WATER))continue;
            int bed=0;
            for(int y=-1;y>=-34;y--) {
                var old=w.get(x,y,z);if(old==null)break;
                if(!masonry(old)&&!old.isAir()&&old.getFluidState().isEmpty()) {
                    var support=old.getCollisionShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO);
                    if(!support.isEmpty()) {
                        // A partial-height top cannot support the next integer block course.
                        // Stop here, rather than tunnelling through it to a deeper solid.
                        if(!Shapes.joinIsNotEmpty(support.getFaceShape(Direction.UP),Shapes.block(),BooleanOp.NOT_SAME))bed=y;
                        break;
                    }
                }
            }
            if(bed==0)continue;
            int top=Math.min(-1,bed+Math.min(3,edge+1));
            boolean supported=true;
            for(int y=bed+1;y<=top;y++){var old=w.get(x,y,z);if(old==null||!(old.is(Blocks.WATER)||masonry(old))){supported=false;break;}}
            if(!supported)continue;
            for(int y=bed+1;y<=top;y++) {
                var old=w.get(x,y,z);if(old==null||!old.is(Blocks.WATER))continue;
                BlockState state=block(y==bed+1?Blocks.PRISMARINE:Blocks.PRISMARINE_BRICKS);
                if(y==top) {
                    if(edge<2){int dx=x<=z&&x<=57-z?1:57-x<=z&&57-x<=57-z?-1:0;
                        int dz=dx==0?(z<29?1:-1):0;state=stair(w,dx,dz);}
                    else state=lightSlab();
                }
                w.set(x,y,z,state);
            }
        }
    }
    private static BlockState lightSlab(){return Blocks.PRISMARINE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM).setValue(SlabBlock.WATERLOGGED,true);}
    private static BlockState block(Block b){return b.defaultBlockState();}
    private static BlockState slab(){return Blocks.DARK_PRISMARINE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM).setValue(SlabBlock.WATERLOGGED,true);}
    private static BlockState stair(Writer w,int dx,int dz){return Blocks.PRISMARINE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,w.direction(dx,dz)).setValue(StairBlock.WATERLOGGED,true);}
    private static boolean masonry(BlockState s){return s.is(Blocks.PRISMARINE)||s.is(Blocks.PRISMARINE_BRICKS)||s.is(Blocks.DARK_PRISMARINE);}
    private static void core(Writer w,int x,int y,int z,BlockState s) {
        if(x<0||x>57||z<0||z>57||y<0||y>22||gameplay(x,y,z))return;
        // A protected foot cell also owns its swimming/standing headroom above the mask.
        // The read stays in the same chunk column; unknown vertical clips fail closed.
        if(gameplay(x,y-1,z)){var below=w.get(x,y-1,z);if(below==null||below.is(Blocks.WATER))return;}
        var old=w.get(x,y,z);if(old==null||old.getLightEmission()>0||!(masonry(old)||old.is(Blocks.WATER)||old.is(Blocks.PRISMARINE_BRICK_STAIRS)||old.is(Blocks.DARK_PRISMARINE_SLAB)))return;
        w.set(x,y,z,s);
    }
    private static void outer(Writer w,int x,int y,int z,BlockState s){if(x<0||x>57||z<0||z>57)w.set(x,y,z,s);}
    private static void box(Writer w,int x,int y,int z,int mx,int my,int mz,Block block){for(int a=x;a<=mx;a++)for(int b=y;b<=my;b++)for(int c=z;c<=mz;c++)outer(w,a,b,c,block(block));}
}
