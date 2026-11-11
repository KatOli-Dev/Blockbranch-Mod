package blockbranch.realistic;

import java.util.*;
import java.util.function.BiFunction;

/** Immutable world-coordinate tree geometry, rasterized into the requesting rectangle only. */
public final class WoodlandTrees {
    public static final int RADIUS=8,MAX_HEIGHT=24,HUMID_HEIGHT=32;
    public record Root(int x,int y,int z,boolean birch,int height,int variant,int family) {
        public Root(int x,int y,int z,boolean birch,int height,int variant){this(x,y,z,birch,height,variant,0);}
    }
    public record Voxel(int x,int y,int z,int code) {}
    private record Crown(int x,int y,int z,int rx,int ry,int rz) {}
    public static List<Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork drainage,ClimateField climate) {
        return roots(seed,minX,minZ,maxX,maxZ,drainage,climate,0);
    }
    public static List<Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork drainage,ClimateField climate,int transitions) {
        var result=new ArrayList<Root>();int radius=WoodlandTrees.RADIUS;
        var ecology=new GroundEcology.Field(drainage::sample,climate);
        for(var point:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius,TreeScatter.Spacing.CLOSED)){
            int x=point.x(),z=point.z();long v=OakBirchWoodland.mix(point.priority()^0L);
            boolean redwood=RedwoodForest.enabled(transitions)&&RedwoodForest.contains(seed,x,z,drainage,climate);
            if(redwood)continue;
            boolean edge=(transitions&1)!=0&&ForestGrasslandTransition.active(seed,x,z,drainage,climate);
            if(Swamp.contains(seed,x,z,drainage,climate)||OakBirchWoodland.glade(seed,x,z) || (edge?!ForestGrasslandTransition.tree(seed,point):!OakBirchWoodland.potential(seed,x,z,drainage,climate)) || !TreeScatter.establishes(seed,point,0,ecology.sample(x,z)))continue;
                // Infill stems exist only in closed-canopy stands; open woodland keeps its historical, sparser stems.
                if(!point.legacy()&&!closedCanopy(seed,x,z,drainage,climate))continue;
                var center=drainage.sample(x,z);boolean safe=true;
                // Keep stems and crowns off lake/river niches and away from buried hillsides.
                for(int dz=-RADIUS;dz<=RADIUS;dz+=RADIUS) for(int dx=-RADIUS;dx<=RADIUS;dx+=RADIUS) {
                    var s=drainage.sample(x+dx,z+dz);
                    if(Math.abs(s.height()-center.height())>3 || LandVegetation.reserved(drainage,x+dx,z+dz)) safe=false;
                }
                if(!safe) continue;
                if(Forest.potential(seed,x,z,drainage,climate)){var root=ForestTrees.root(seed,point,center,ecology.sample(x,z),BirchForest.contains(seed,x,z,drainage,climate));if(root!=null)result.add(root);continue;}
                int variant=TemperateForestVariants.kind(seed,x,z,drainage,climate);
                if(variant>0){var root=TemperateForestVariants.root(seed,point,center,ecology.sample(x,z),variant);if(root!=null)result.add(root);continue;}
                if(DappledForest.contains(seed,x,z,drainage,climate)){result.add(DappledForest.root(point,center));continue;}
                int darkCherry=DarkCherryWoodland.kind(seed,x,z,drainage,climate);
                if(darkCherry>0){result.add(DarkCherryWoodland.root(point,center,darkCherry));continue;}
                boolean birch=Long.remainderUnsigned(v>>>16,10)<3;
                int height=point.young()?(birch?9:10)+(int)Long.remainderUnsigned(v>>>24,3):(birch?12:14)+(int)Long.remainderUnsigned(v>>>24,birch?5:7);
                result.add(new Root(x,center.height()+1,z,birch,height,(int)(v>>>32)));
            }
        return result;
    }
    /** Stands whose crowns close (forest, birch forest, old-growth birch, poplar, dark forest), in the order {@link #roots} classifies them. */
    private static boolean closedCanopy(long seed,int x,int z,DrainageNetwork drainage,ClimateField climate){
        if(Forest.potential(seed,x,z,drainage,climate))return true;
        int variant=TemperateForestVariants.kind(seed,x,z,drainage,climate);
        if(variant>0)return variant==TemperateForestVariants.OLD_BIRCH;
        return DappledForest.contains(seed,x,z,drainage,climate)||DarkCherryWoodland.kind(seed,x,z,drainage,climate)==DarkCherryWoodland.DARK;
    }
    /** Codes 1..6 are oak/birch logs on Y/X/Z; 7..18 are leaves at support distance 1..6. */
    public static List<Voxel> tree(Root root) { return root.family()==DappledForest.FAMILY?DappledForest.tree(root):root.family()==12||root.family()==13?JungleTrees.tree(root):root.family()==10||root.family()==11?DarkCherryWoodland.tree(root):root.family()==3?SwampTrees.tree(root):root.family()==RedwoodForest.FAMILY?RedwoodTrees.tree(root):root.family()>0?ForestTrees.tree(root):shape(root,false); }
    public static List<Voxel> humidTree(Root root) { return shape(root,true); }
    private static List<Voxel> shape(Root root,boolean humid) {
        int size=RADIUS*2+1,height=humid?HUMID_HEIGHT:MAX_HEIGHT,count=size*size*height;
        byte[] blocks=new byte[count],distance=new byte[count];Arrays.fill(distance,(byte)7);
        int wood=root.birch() ? 4 : 1;
        int stem=root.height()-(root.birch() ? 4 : 6);
        line(blocks,0,0,0,0,stem,0,wood);
        var crowns=new ArrayList<Crown>();
        // Humid crowns keep pace with their taller stems; every other family keeps its radius of 4.
        int wide=humid?Math.max(4,(int)Math.round(4*root.height()/22.0)):4;
        if(root.birch()) {
            line(blocks,0,stem,0,0,root.height()-2,0,wood);
            crowns.add(new Crown(0,root.height()-3,0,3,4,3));
            crowns.add(new Crown((root.variant()&1)==0 ? 1 : -1,root.height()-5,0,3,3,3));
        } else {
            int rotation=Math.floorMod(root.variant(),4);
            for(int branch=0;branch<3;branch++) {
                int[] dx={3,0,-3,0},dz={0,3,0,-3};int direction=(branch+rotation)&3;
                int bx=dx[direction],bz=dz[direction],by=root.height()-4+(branch&1);
                line(blocks,0,stem-1,0,bx,by,bz,wood);
                crowns.add(new Crown(bx,by+1,bz,3+(branch==0 ? 1 : 0),3,3+(branch==1 ? 1 : 0)));
            }
            line(blocks,0,stem,0,0,root.height()-2,0,wood);
            crowns.add(new Crown(0,root.height()-2,0,wide,3,wide));
        }
        if(humid && root.height()>=18){
            int rotation=Math.floorMod(root.variant()>>>4,4);int[] dx={3,0,-3,0},dz={0,3,0,-3};
            for(int branch=0;branch<2;branch++){
                int direction=(rotation+branch*2)&3,by=root.height()-10+(branch&1);
                line(blocks,0,Math.max(2,by-3),0,dx[direction],by,dz[direction],wood);
                crowns.add(new Crown(dx[direction],by+1,dz[direction],wide,3,wide));
            }
        }
        // The first containing crown owns the edge/hole decision, even when
        // it leaves a hole. Preserve that precedence while rasterizing only each
        // crown's exact axis-aligned bounding box (all radii/centers are integers).
        boolean[] claimed=new boolean[count];
        for(var crown:crowns)
            for(int y=Math.max(1,crown.y()-crown.ry());y<=Math.min(height-1,crown.y()+crown.ry());y++)
                for(int z=Math.max(-RADIUS,crown.z()-crown.rz());z<=Math.min(RADIUS,crown.z()+crown.rz());z++)
                    for(int x=Math.max(-RADIUS,crown.x()-crown.rx());x<=Math.min(RADIUS,crown.x()+crown.rx());x++){
                        int index=index(x,y,z);if(blocks[index]!=0||claimed[index])continue;
                        double d=square((x-crown.x())/(double)crown.rx())+square((y-crown.y())/(double)crown.ry())+square((z-crown.z())/(double)crown.rz());
                        if(d>1)continue;claimed[index]=true;
                        long edge=OakBirchWoodland.mix(root.variant() ^ x*0x9E3779B97F4A7C15L ^ y*0xC2B2AE3D27D4EB4FL ^ z*0x165667B19E3779F9L);
                        if(d<0.72 || (edge&7)!=0)blocks[index]=19;
                    }
        // Calculate actual six-neighbour support through foliage, rather than marking
        // natural leaves persistent or assuming distance through holes in a crown.
        int[] queue=new int[count];int head=0,tail=0;
        for(int i=0;i<count;i++) if(blocks[i]>0 && blocks[i]<=6) { distance[i]=0;queue[tail++]=i; }
        while(head<tail) {
            int i=queue[head++],x=i%size-RADIUS,z=i/size%size-RADIUS,y=i/(size*size);
            if(distance[i]>=6) continue;
            for(int[] direction:DIRECTIONS) {
                int nx=x+direction[0],ny=y+direction[1],nz=z+direction[2];
                if(Math.abs(nx)>RADIUS || Math.abs(nz)>RADIUS || ny<0 || ny>=height) continue;
                int n=index(nx,ny,nz);
                if(blocks[n]==19 && distance[n]>distance[i]+1) { distance[n]=(byte)(distance[i]+1);queue[tail++]=n; }
            }
        }
        var result=new ArrayList<Voxel>();
        for(int y=0;y<height;y++) for(int z=-RADIUS;z<=RADIUS;z++) for(int x=-RADIUS;x<=RADIUS;x++) {
            int i=index(x,y,z),code=blocks[i];
            if(code==19) code=distance[i]>6 ? 0 : (root.birch() ? 12 : 6)+distance[i];
            if(code>0) result.add(new Voxel(root.x()+x,root.y()+y,root.z()+z,code));
        }
        return result;
    }
    /** Packed, sorted local positions and bounded state codes, independent of chunk request order. */
    public static int[] column(long seed,int chunkX,int chunkZ,DrainageNetwork drainage,ClimateField climate) {
        return column(seed,chunkX,chunkZ,drainage,climate,true);
    }
    public static int[] column(long seed,int chunkX,int chunkZ,DrainageNetwork drainage,ClimateField climate,boolean whorled) {
        return column(seed,chunkX,chunkZ,drainage,climate,whorled,0);
    }
    public static int[] column(long seed,int chunkX,int chunkZ,DrainageNetwork drainage,ClimateField climate,boolean whorled,int transitions) {
        return column(seed,chunkX,chunkZ,drainage,climate,whorled,transitions,null);
    }
    public static int[] column(long seed,int chunkX,int chunkZ,DrainageNetwork drainage,ClimateField climate,boolean whorled,int transitions,java.util.function.BiPredicate<Integer,Integer> dryForest) {
        return column(seed,chunkX,chunkZ,drainage,climate,whorled,transitions,dryForest,null);
    }
    public static int[] column(long seed,int chunkX,int chunkZ,DrainageNetwork drainage,ClimateField climate,boolean whorled,int transitions,java.util.function.BiPredicate<Integer,Integer> dryForest,java.util.function.BiPredicate<Integer,Integer> summerDry) {
        int minX=chunkX*16,minZ=chunkZ*16,halo=6,width=16+halo*2;
        int startX=minX-halo,startZ=minZ-halo;
        var roots=new ArrayList<>(roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate,transitions));
        roots.removeIf(root->ColdUplands.surfaceKind(seed,root.x(),root.z(),drainage,climate)>0);
        roots.addAll(ColdUplands.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate));
        roots.removeIf(root->Badlands.surfaceKind(seed,root.x(),root.z(),drainage,climate)>0);
        roots.addAll(Badlands.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate));
        var boreal=BorealTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate);
        boreal.removeIf(root->ColdUplands.surfaceKind(seed,root.x(),root.z(),drainage,climate)>0);
        var geometry=new ArrayList<Voxel>();
        for(var root:roots) geometry.addAll(root.family()==15?TaigaTrees.tree(root):tree(root));
        for(var root:boreal) geometry.addAll(whorled?BorealTrees.tree(root):BorealTrees.legacyTree(root));
        roots.addAll(boreal);
        var warm=WarmTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate);
        warm.removeIf(root->Badlands.surfaceKind(seed,root.x(),root.z(),drainage,climate)>0);
        if(dryForest!=null)warm.removeIf(root->dryForest.test(root.x(),root.z()));
        if(summerDry!=null)warm.removeIf(root->summerDry.test(root.x(),root.z()));
        for(var root:warm){geometry.addAll(WarmTrees.tree(root));roots.add(new Root(root.x(),root.y(),root.z(),false,root.height(),root.variant()));}
        if(dryForest!=null){var dry=DryForestTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate,dryForest);for(var root:dry)geometry.addAll(ForestTrees.tree(root));roots.addAll(dry);}
        if(summerDry!=null){var dry=SummerDryTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate,summerDry);for(var root:dry)geometry.addAll(SummerDryTrees.tree(root));roots.addAll(dry);}
        var redwoods=RedwoodForest.enabled(transitions)?RedwoodTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate):List.<Root>of();
        for(var root:redwoods)geometry.addAll(RedwoodTrees.tree(root));roots.addAll(redwoods);
        var humid=HumidTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate);
        for(var root:humid)geometry.addAll(HumidTrees.tree(root));roots.addAll(humid);
        var savanna=SavannaTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate);
        for(var root:savanna)geometry.addAll(SavannaTrees.tree(root));roots.addAll(savanna);
        var upland=SavannaUplands.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate);
        for(var root:upland)geometry.addAll(SavannaUplands.tree(root));roots.addAll(upland);
        var jungle=JungleTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate);
        jungle.addAll(TropicalVariantTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate));
        for(var root:jungle)geometry.addAll(JungleTrees.tree(root));roots.addAll(jungle);
        var windswept=WindsweptTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate);
        for(var root:windswept)geometry.addAll(WindsweptTrees.tree(root));roots.addAll(windswept);
        var swamp=SwampTrees.roots(seed,startX,startZ,minX+15+halo,minZ+15+halo,drainage,climate);
        var swampTrees=new LinkedHashMap<Root,List<Voxel>>();var swampGeometry=new ArrayList<Voxel>();
        for(var root:swamp){var voxels=SwampTrees.tree(root);swampTrees.put(root,voxels);swampGeometry.addAll(voxels);}roots.addAll(swamp);
        if(roots.isEmpty()) return SwampCanopy.merge(SwampCanopy.decorate(seed,chunkX,chunkZ,swampTrees,(x,y,z)->0,drainage,climate),WoodlandDetails.logs(seed,chunkX,chunkZ,drainage,climate,transitions));
        int structuralHeight=jungle.isEmpty()?MAX_HEIGHT:JungleTrees.MAX_HEIGHT;if(roots.stream().anyMatch(r->r.family()>0))structuralHeight=Math.max(structuralHeight,ForestTrees.MAX_HEIGHT);
        if(!humid.isEmpty())structuralHeight=Math.max(structuralHeight,HUMID_HEIGHT);
        if(!redwoods.isEmpty())structuralHeight=Math.max(structuralHeight,RedwoodTrees.MAX_HEIGHT);
        int minY=roots.stream().mapToInt(Root::y).min().orElseThrow()-(redwoods.isEmpty()?0:RedwoodTrees.ROOT_DEPTH),maxY=roots.stream().mapToInt(Root::y).max().orElseThrow()+structuralHeight;
        int height=maxY-minY,plane=width*width,count=plane*height;
        short[] grid=new short[count];byte[] distance=new byte[count];Arrays.fill(distance,(byte)7);
        boolean[] allowed=new boolean[plane];int[] ground=new int[plane];
        for(int z=0;z<width;z++) for(int x=0;x<width;x++) {
            var terrain=drainage.sample(startX+x,startZ+z);ground[z*width+x]=terrain.height();
            allowed[z*width+x]=terrain.waterLevel()==DrainageNetwork.DRY && drainage.channelAt(startX+x,startZ+z).edge()<0 && !LandVegetation.reserved(drainage,startX+x,startZ+z);
        }
        for(var voxel:geometry) {
            int x=voxel.x()-startX,z=voxel.z()-startZ;
            if(x<0 || x>=width || z<0 || z>=width) continue;
            int column=z*width+x;
            if(!allowed[column] || voxel.y()<=ground[column] || voxel.y()>=TerrainShape.CEILING) continue;
            int i=(voxel.y()-minY)*plane+column;
            if(grid[i]==0 || (logCode(voxel.code()) && !logCode(grid[i]))) grid[i]=(short)voxel.code();
        }
        for(var voxel:swampGeometry){
            int x=voxel.x()-startX,z=voxel.z()-startZ;if(x<0||x>=width||z<0||z>=width)continue;int column=z*width+x;
            var surface=drainage.sample(voxel.x(),voxel.z());if(voxel.y()<=ground[column]||voxel.y()>=TerrainShape.CEILING||drainage.channelAt(voxel.x(),voxel.z()).edge()>=0)continue;
            int lower=surface.waterLevel();if(lower!=DrainageNetwork.DRY){var rs=InlandLakeReedbed.site(drainage,voxel.x(),voxel.z());if(InlandLakeReedbed.cover(seed,voxel.x(),voxel.z(),InlandLakeReedbed.contains(voxel.x(),voxel.z(),(xx,zz)->InlandLakeReedbed.site(drainage,xx,zz)),rs)>1)lower+=2;if(voxel.y()<lower)continue;}
            int i=(voxel.y()-minY)*plane+column;if(grid[i]==0||logCode(voxel.code())&&!logCode(grid[i]))grid[i]=(short)voxel.code();
        }
        // Merge crowns and clip terrain before calculating support. The six-block
        // halo contains every possible support path for a retained distance<=6 leaf.
        // Thus adjacent chunks calculate the same distances without reading each other.
        int[] queue=new int[count];int head=0,tail=0;
        for(int i=0;i<count;i++) if(logCode(grid[i])) { distance[i]=0;queue[tail++]=i; }
        while(head<tail) {
            int i=queue[head++],x=i%width,z=i/width%width,y=i/plane;
            if(distance[i]>=6) continue;
            for(var direction:DIRECTIONS) {
                int nx=x+direction[0],ny=y+direction[1],nz=z+direction[2];
                if(nx<0 || nx>=width || nz<0 || nz>=width || ny<0 || ny>=height) continue;
                int n=ny*plane+nz*width+nx;
                if(grid[n]>0 && !logCode(grid[n]) && distance[n]>distance[i]+1) { distance[n]=(byte)(distance[i]+1);queue[tail++]=n; }
            }
        }
        var result=new ArrayList<Integer>();
        for(int y=0;y<height;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
            int i=y*plane+(z+halo)*width+x+halo,code=grid[i];
            if(code>0 && !logCode(code)) code=distance[i]>6 ? 0 : leafBase(code)+distance[i];
            if(code==0) continue;
            int key=((minY+y-TerrainShape.FLOOR)<<8)|(z*16+x);
            result.add((key<<blockbranch.cubic.CanopyLayer.STATE_BITS)|code);
        }
        var decor=SwampCanopy.decorate(seed,chunkX,chunkZ,swampTrees,(wx,wy,wz)->{
            int x=wx-startX,z=wz-startZ,y=wy-minY;if(x<0||x>=width||z<0||z>=width||y<0||y>=height)return 0;
            int i=y*plane+z*width+x,code=grid[i];return code==0?0:logCode(code)?code:distance[i]>6?0:leafBase(code)+distance[i];
        },drainage,climate);
        return SwampCanopy.merge(SwampCanopy.merge(result.stream().mapToInt(Integer::intValue).toArray(),decor),WoodlandDetails.logs(seed,chunkX,chunkZ,drainage,climate,transitions));
    }
    public static boolean logCode(int code) { return code==blockbranch.cubic.CanopyLayer.ACACIA_WOOD_CODE || (code>=1 && code<=6) || (code>=19 && code<=21) || (code>=28 && code<=30) || (code>=37 && code<=39) || (code>=78&&code<=80) || (code>=92&&code<=94) || (code>=101&&code<=103) || (code>=128&&code<=130) || RedwoodTrees.woodCode(code) || DarkOakBranches.branch(code); }
    public static int leafBase(int code) { return code>=131&&code<=148 ? 130+(code-131)/6*6 : code>=104 ? 103 : code>=95 ? 94 : code<=12 ? 6 : code<=18 ? 12 : code<=27 ? 21 : code<=36 ? 30 : 39; }
    public static int leafDistance(int code) { return code-leafBase(code); }
    private static final int[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static int index(int x,int y,int z) { int size=RADIUS*2+1;return (y*size+z+RADIUS)*size+x+RADIUS; }
    private static double square(double x) { return x*x; }
    private static void line(byte[] blocks,int x,int y,int z,int tx,int ty,int tz,int wood) {
        blocks[index(x,y,z)]=(byte)wood;
        // Connected rising branch steps with directional log axes.
        while(x!=tx || y!=ty || z!=tz) {
            if(y<ty) { y++;blocks[index(x,y,z)]=(byte)wood; }
            if(x!=tx) { x+=Integer.signum(tx-x);blocks[index(x,y,z)]=(byte)(wood+1); }
            if(z!=tz) { z+=Integer.signum(tz-z);blocks[index(x,y,z)]=(byte)(wood+2); }
        }
    }
    private WoodlandTrees() {}
}
