package blockbranch.realistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.function.IntBinaryOperator;

/** The manor's open gallery, corner pavilions and furnished end ranges share one frame. */
final class MansionReplacement {
    @FunctionalInterface interface Writer { void set(int x,int y,int z,BlockState state); }
    private static final Block PALE=Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.WHITE);
    private MansionReplacement() {}

    static int[] entryProfile(int floor,BlockPos entrance,Direction outward,IntBinaryOperator terrain) {
        return accessProfile(floor,entrance,outward,terrain,-1,1);
    }
    static int[][] galleryProfiles(BoundingBox core,int floor,IntBinaryOperator terrain) {
        return new int[][]{
            accessProfile(floor,new BlockPos(core.minX()-6,floor,core.minZ()-6),Direction.NORTH,terrain,-1,0),
            accessProfile(floor,new BlockPos(core.maxX()+6,floor,core.maxZ()+6),Direction.SOUTH,terrain,-1,0)
        };
    }
    private static int[] accessProfile(int floor,BlockPos entrance,Direction outward,IntBinaryOperator terrain,int firstLane,int lastLane) {
        int dx=outward.getStepX(),dz=outward.getStepZ(),x=entrance.getX(),z=entrance.getZ();
        int[] ground=new int[10],height=new int[10];
        for(int i=0;i<10;i++) {
            int maximum=Integer.MIN_VALUE;
            for(int lane=firstLane;lane<=lastLane;lane++)maximum=Math.max(maximum,terrain.applyAsInt(x+dx*(i+1)+dz*lane,z+dz*(i+1)+dx*lane)+1-floor);
            if(maximum< -8||maximum>0)throw new IllegalArgumentException("Manor approach outside admitted relief");
            ground[i]=maximum;
        }
        // The least 1-Lipschitz surface above the terrain and the descending porch
        // envelope. This bridges sharp grade reversals without excavating soil.
        for(int i=0;i<10;i++) {
            height[i]=-i;
            for(int j=0;j<10;j++)height[i]=Math.max(height[i],ground[j]-Math.abs(j-i));
        }
        for(int i=0;i<10;i++)if(height[i]==ground[i])return validateEntryProfile(java.util.Arrays.copyOf(height,i+1));
        throw new IllegalStateException("Admitted manor approach has no ground contact");
    }
    static int[] validateEntryProfile(int[] profile) {
        if(profile.length<1||profile.length>10||profile[0]!=0)throw new IllegalArgumentException("Invalid manor entry profile");
        for(int i=0;i<profile.length;i++)if(profile[i]< -8||profile[i]>0||i>0&&Math.abs(profile[i]-profile[i-1])>1)throw new IllegalArgumentException("Invalid manor entry grade");
        return profile.clone();
    }
    static int[][] readGalleryProfiles(int[] saved) {
        if(saved.length<4||saved.length>22)throw new IllegalArgumentException("Invalid gallery approach profiles");
        int first=saved[0];if(first<1||first>10||first+1>=saved.length)throw new IllegalArgumentException("Invalid first gallery approach");
        int second=saved[first+1];if(second<1||second>10||saved.length!=first+second+2)throw new IllegalArgumentException("Invalid second gallery approach");
        return new int[][]{validateEntryProfile(java.util.Arrays.copyOfRange(saved,1,first+1)),validateEntryProfile(java.util.Arrays.copyOfRange(saved,first+2,saved.length))};
    }
    static int[] saveGalleryProfiles(int[][] profiles) {
        if(profiles.length!=2)throw new IllegalArgumentException("Two gallery approaches required");
        int[] a=validateEntryProfile(profiles[0]),b=validateEntryProfile(profiles[1]),saved=new int[a.length+b.length+2];
        saved[0]=a.length;System.arraycopy(a,0,saved,1,a.length);saved[a.length+1]=b.length;System.arraycopy(b,0,saved,a.length+2,b.length);return saved;
    }
    static void generate(Writer w,BoundingBox core,int[] steps,BlockPos entrance,Direction outward,int[] entryProfile,int[][] galleryProfiles) {
        int x0=core.minX()-10,x1=core.maxX()+10,z0=core.minZ()-10,z1=core.maxZ()+10;
        int face=front(outward),gate=face<2?entrance.getZ():entrance.getX();
        wall(w,true,x0,x1,z0,face==2?gate:Integer.MIN_VALUE,-1);
        wall(w,true,x0,x1,z1,face==3?gate:Integer.MIN_VALUE,1);
        wall(w,false,z0,z1,x0,face==0?gate:Integer.MIN_VALUE,-1);
        wall(w,false,z0,z1,x1,face==1?gate:Integer.MIN_VALUE,1);
        // The inward porch lamps keep their original light boundary at the house.
        // Opaque service bays screen their full propagation radius; gates stay open.
        serviceWall(w,x0,core.minZ()+7,core.minZ()+17,(z0+z1)/2,face==0?gate:Integer.MIN_VALUE);
        serviceWall(w,x1,core.maxZ()-29,core.maxZ()-19,(z0+z1)/2,face==1?gate:Integer.MIN_VALUE);
        for(int x:new int[]{x0,x1})for(int z:new int[]{z0,z1})pavilion(w,x-3,z-3);
        range(w,core.minX()-20,core.minZ()+8,8,22,true,steps[0],steps[1]);
        range(w,core.maxX()+12,core.maxZ()-28,8,20,false,steps[2],steps[3]);
        for(int i=0;i<9;i++)for(int a=0;a<3;a++) {
            stairApproach(w,x0+3+a,z0+4+i,i,Direction.SOUTH);
            stairApproach(w,x1-5+a,z1-4-i,i,Direction.NORTH);
        }
        box(w,x0,8,z0+13,x0+5,8,z0+15,Blocks.SPRUCE_PLANKS);
        box(w,x1-5,8,z1-15,x1,8,z1-13,Blocks.SPRUCE_PLANKS);
        entry(w,entrance,outward,entryProfile,-1,1);
        entry(w,new BlockPos(x0+4,0,z0+4),Direction.NORTH,galleryProfiles[0],-1,0);
        entry(w,new BlockPos(x1-4,0,z1-4),Direction.SOUTH,galleryProfiles[1],-1,0);
    }
    private static void entry(Writer w,BlockPos entrance,Direction out,int[] profile,int firstLane,int lastLane) {
        int dx=out.getStepX(),dz=out.getStepZ(),x=entrance.getX(),z=entrance.getZ();
        for(int i=0;i<profile.length-1;i++)for(int lane=firstLane;lane<=lastLane;lane++) {
            int px=x+dx*(i+1)+dz*lane,pz=z+dz*(i+1)+dx*lane;
            int current=profile[i],next=profile[i+1],base=next>current?current:current-1;
            box(w,px,-12,pz,px,base-1,pz,Blocks.STONE_BRICKS);
            box(w,px,base+1,pz,px,base+3,pz,Blocks.AIR);
            w.set(px,base,pz,next==current?Blocks.STONE_BRICKS.defaultBlockState():Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,next>current?out:out.getOpposite()));
        }
    }
    private static int front(Direction out) {
        return switch(out){case WEST->0;case EAST->1;case NORTH->2;case SOUTH->3;default->throw new IllegalArgumentException("Vertical manor entrance");};
    }
    private static void serviceWall(Writer w,int x,int start,int end,int gate,int entrance) {
        for(int z=start;z<=end;z++)for(int y=-12;y<=6;y++) {
            boolean opening=Math.abs(z-gate)<=3||entrance!=Integer.MIN_VALUE&&Math.abs(z-entrance)<=4;
            if(opening&&y<=5)continue;
            BlockState state=y<=1?Blocks.STONE_BRICKS.defaultBlockState():y==6?Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z):z==start||z==end?Blocks.DARK_OAK_LOG.defaultBlockState():PALE.defaultBlockState();
            w.set(x,y,z,state);
        }
    }
    private static void wall(Writer w,boolean alongX,int start,int end,int side,int entrance,int outward) {
        int mid=(start+end)/2;
        for(int a=start+4;a<=end-4;a++) {
            boolean gate=Math.abs(a-mid)<=3||entrance!=Integer.MIN_VALUE&&Math.abs(a-entrance)<=4;
            boolean jamb=Math.abs(a-mid)==4||entrance!=Integer.MIN_VALUE&&Math.abs(a-entrance)==5;
            boolean pier=!gate&&(jamb||Math.floorMod(a-start-4,8)==0);
            if(alongX) {
                if(a==start+4||a==start+5||a==end-5||a==end-4)pier=false;
                else if(a==start+6||a==end-6)pier=!gate;
            }
            if(pier) {
                for(int y=-12;y<=0;y++)for(int n=0;n<=2;n++)put(w,alongX,a,y,side-outward*n,y<0?Blocks.STONE_BRICKS.defaultBlockState():Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
                for(int y=1;y<=6;y++)put(w,alongX,a,y,side,Blocks.DARK_OAK_LOG.defaultBlockState());
            }
            put(w,alongX,a,7,side,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,alongX?Direction.Axis.X:Direction.Axis.Z));
            for(int n=-1;n<=1;n++)put(w,alongX,a,8,side+n,Blocks.SPRUCE_PLANKS.defaultBlockState());
            var rail=Blocks.DARK_OAK_FENCE.defaultBlockState();
            rail=alongX?rail.setValue(FenceBlock.EAST,true).setValue(FenceBlock.WEST,true):rail.setValue(FenceBlock.NORTH,true).setValue(FenceBlock.SOUTH,true);
            put(w,alongX,a,9,side+outward,rail);
            // Keep the two inner deck lanes clear; the outer post carries the crossbeam.
            if(pier)for(int y=9;y<=11;y++)put(w,alongX,a,y,side+outward,Blocks.DARK_OAK_LOG.defaultBlockState());
            if(pier)for(int n=-2;n<=2;n++)put(w,alongX,a,11,side+n,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,alongX?Direction.Axis.Z:Direction.Axis.X));
            for(int n=-2;n<=2;n++) {
                int y=14-Math.abs(n);
                for(int support=12;support<y;support++)put(w,alongX,a,support,side+n,Blocks.SPRUCE_PLANKS.defaultBlockState());
                var state=n==0?Blocks.SPRUCE_SLAB.defaultBlockState():Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,alongX?(n<0?Direction.SOUTH:Direction.NORTH):(n<0?Direction.EAST:Direction.WEST));
                put(w,alongX,a,y,side+n,state);
            }
        }
    }
    private static void pavilion(Writer w,int x,int z) {
        box(w,x,-12,z,x+6,-1,z+6,Blocks.STONE_BRICKS);
        box(w,x,0,z,x+6,14,z+6,PALE);
        box(w,x+1,0,z+1,x+5,13,z+5,Blocks.AIR);
        box(w,x+2,0,z,x+4,3,z+6,Blocks.AIR);
        box(w,x+2,9,z,x+4,12,z+6,Blocks.AIR);
        box(w,x,9,z+2,x+6,12,z+4,Blocks.AIR);
        for(int a:new int[]{0,6})for(int b:new int[]{0,6})for(int y=0;y<=14;y++)set(w,x+a,y,z+b,y==0?Blocks.CHISELED_STONE_BRICKS:Blocks.DARK_OAK_LOG);
        for(int y:new int[]{7,13})for(int a=0;a<=6;a++)for(int face=0;face<4;face++) {
            int px=x+(face<2?a:face==2?0:6),pz=z+(face<2?face==0?0:6:a);
            w.set(px,y,pz,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,face<2?Direction.Axis.X:Direction.Axis.Z));
        }
        box(w,x+1,8,z+1,x+5,8,z+5,Blocks.SPRUCE_PLANKS);
        box(w,x+1,14,z+1,x+5,14,z+5,Blocks.SPRUCE_PLANKS);
        for(int y=1;y<=14;y++)w.set(x+1,y,z+1,Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.SOUTH));
        for(int y=15;y<=18;y++)set(w,x+3,y,z+3,Blocks.DARK_OAK_LOG);
        for(int a=0;a<=6;a++) {
            int roof=16+Math.min(a,6-a);
            for(int b=-1;b<=7;b++) {
                var state=a==3?Blocks.SPRUCE_SLAB.defaultBlockState():Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,a<3?Direction.EAST:Direction.WEST);
                w.set(x+a,roof,z+b,state);
            }
            for(int b:new int[]{0,6})for(int y=15;y<roof;y++)set(w,x+a,y,z+b,a==3?Blocks.DARK_OAK_LOG:PALE);
        }
        box(w,x+3,18,z,x+3,18,z+6,Blocks.SPRUCE_PLANKS);
        for(int y:new int[]{6,13}){torch(w,x+1,y,z+3,Direction.EAST);torch(w,x+5,y,z+3,Direction.WEST);}
        torch(w,x+3,16,z+2,Direction.NORTH);torch(w,x+3,16,z+4,Direction.SOUTH);
    }
    private static void range(Writer w,int x,int z,int width,int length,boolean workshop,int west,int east) {
        box(w,x,-12,z,x+width,-1,z+length,Blocks.STONE_BRICKS);
        box(w,x,0,z,x+width,7,z+length,PALE);
        box(w,x+1,0,z+1,x+width-1,6,z+length-1,Blocks.AIR);
        for(int side:new int[]{0,width}) {
            for(int a=0;a<=length;a++) {
                set(w,x+side,0,z+a,Blocks.STONE_BRICKS);
                if(a%6==0||a==length)for(int y=1;y<=7;y++)set(w,x+side,y,z+a,Blocks.DARK_OAK_LOG);
                w.set(x+side,6,z+a,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));
            }
            box(w,x+side,1,z+3,x+side,3,z+5,Blocks.AIR);
            for(int bay:new int[]{9,15})for(int a=bay;a<=Math.min(bay+3,length-2);a++)for(int y=2;y<=4;y++)set(w,x+side,y,z+a,Blocks.GLASS);
            int outward=side==0?-1:1,stairX=x+side+outward,steps=side==0?west:east;
            box(w,stairX,-12,z+3,stairX,0,z+4,Blocks.STONE_BRICKS);
            box(w,stairX,1,z+3,stairX,3,z+4,Blocks.AIR);
            for(int i=0;i<steps;i++) {
                box(w,stairX,-12,z+5+i,stairX,-i-1,z+5+i,Blocks.STONE_BRICKS);
                box(w,stairX,1-i,z+5+i,stairX,3,z+5+i,Blocks.AIR);
                w.set(stairX,-i,z+5+i,Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH));
            }
            for(int a=1;a<=7;a++)set(w,x+side+outward,5,z+a,Blocks.DARK_OAK_SLAB);
            for(int a:new int[]{1,7})w.set(x+side+outward,4,z+a,Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF,Half.TOP).setValue(StairBlock.FACING,side==0?Direction.EAST:Direction.WEST));
            for(int a:new int[]{2,6}){set(w,x+side+outward,4,z+a,Blocks.IRON_CHAIN);w.set(x+side+outward,3,z+a,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));}
        }
        for(int end:new int[]{0,length}) {
            for(int a=0;a<=width;a++)w.set(x+a,6,z+end,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));
            for(int a=3;a<=5;a++)for(int y=2;y<=4;y++)set(w,x+a,y,z+end,Blocks.GLASS);
        }
        for(int a=z+8;a<z+length-2;a+=5) {
            box(w,x+2,0,a,x+3,0,a,workshop?Blocks.CRAFTING_TABLE:Blocks.DARK_OAK_PLANKS);
            set(w,x+2,1,a,workshop?Blocks.ANVIL:Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.RED));
            set(w,x+width-2,0,a,workshop?Blocks.FURNACE:Blocks.BOOKSHELF);
        }
        for(int a=1;a<length;a+=6)for(int b=1;b<width;b++)w.set(x+b,6,z+a,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));
        for(int a=0;a<=width;a++)for(int b=-1;b<=length+1;b++) {
            int y=8+Math.min(a,width-a);
            for(int support=8;support<y;support++)set(w,x+a,support,z+b,Blocks.SPRUCE_PLANKS);
            w.set(x+a,y,z+b,a==width/2?Blocks.SPRUCE_SLAB.defaultBlockState():Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,a<width/2?Direction.EAST:Direction.WEST));
            for(int end:new int[]{0,length})for(int fill=7;fill<y;fill++)set(w,x+a,fill,z+end,a==width/2?Blocks.DARK_OAK_LOG:PALE);
        }
        box(w,x+width/2,7,z,x+width/2,11,z+length,Blocks.DARK_OAK_LOG);
    }
    private static void stairApproach(Writer w,int x,int z,int y,Direction facing) {
        box(w,x,-12,z,x,y-1,z,Blocks.STONE_BRICKS);
        w.set(x,y,z,Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,facing));
    }
    private static void torch(Writer w,int x,int y,int z,Direction facing){w.set(x,y,z,Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,facing));}
    private static void put(Writer w,boolean alongX,int a,int y,int side,BlockState state){w.set(alongX?a:side,y,alongX?side:a,state);}
    private static void set(Writer w,int x,int y,int z,Block b){w.set(x,y,z,b.defaultBlockState());}
    private static void box(Writer w,int x,int y,int z,int mx,int my,int mz,Block b){for(int a=x;a<=mx;a++)for(int c=z;c<=mz;c++)for(int y0=y;y0<=my;y0++)set(w,a,y0,c,b);}
}
