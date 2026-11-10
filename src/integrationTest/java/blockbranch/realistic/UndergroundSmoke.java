package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Native sparse cave, snapshot, edit, height and lighting regressions. */
public final class UndergroundSmoke {
    public static BlockPos fixture(RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r) {
        var origin=g.getOrigin(r);var field=new UndergroundStructure(r.seed());
        // Deterministic search in one bounded volume near the natural dry origin.
        for(int z=origin.getMinBlockZ()-32;z<origin.getMinBlockZ()+32;z+=2)
            for(int x=origin.getMinBlockX()-32;x<origin.getMinBlockX()+32;x+=2) {
                int surface=g.drainage(r).sample(x,z).height();
                for(int y=-192;y>=-448;y--) {
                    if(field.voidAt(x,y-1,z,surface)||!field.voidAt(x,y,z,surface))continue;
                    boolean open=true;
                    for(int dz=-1;dz<=1&&open;dz++)for(int dx=-1;dx<=1&&open;dx++)for(int dy=0;dy<5;dy++)
                        if(!field.voidAt(x+dx,y+dy,z+dz,g.drainage(r).sample(x+dx,z+dz).height())){open=false;break;}
                    if(open)return new BlockPos(x,y,z);
                }
            }
        throw new AssertionError("No bounded traversable cave fixture");
    }
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload)throws Exception {
        require(g.undergroundEnabled(),"New preset enables native caves");
        var r=level.getChunkSource().randomState();var witness=fixture(g,r);var factory=level.palettedContainerFactory();
        var home=level.getChunkAt(witness);var terrain=((SparseColumn)home).blockbranch$terrain();var base=terrain.base();
        require(base.underground().enabled(),"FULL generation retains cave descriptor");
        if(reload)require(level.getBlockState(witness).is(Blocks.COBBLESTONE),"Cave edit survives complete world reload");
        level.setBlock(witness,base.state(witness.getX(),witness.getY(),witness.getZ()),2);
        require(level.noCollision(new AABB(witness.getX()+.2,witness.getY(),witness.getZ()+.2,witness.getX()+.8,witness.getY()+1.8,witness.getZ()+.8)),"Native player-sized cave clearance");
        require(!level.getBlockState(witness.below()).getCollisionShape(level,witness.below()).isEmpty(),"Native solid cave floor");
        long fingerprint=1;int voids=0,rocks=0,checks=0;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++) {
            var chunk=level.getChunk(home.getPos().x()+dx,home.getPos().z()+dz);var sparse=(SparseColumn)chunk;var p=sparse.blockbranch$terrain().base();
            var encoded=p.encode(factory);require(encoded.getIntOr("version",0)==(p.litCaves().enabled()?66:p.dripstone().enabled()?64:p.ores().enabled()?(p.ores().definition()==2?63:62):61),"Versioned cave profile");
            var decoded=TerrainProfile.decode(encoded,factory);
            var oldTag=encoded.copy();oldTag.putInt("version",60);oldTag.remove("underground");oldTag.remove("ores");oldTag.remove("ore_definition");oldTag.remove("dripstone");oldTag.remove("dripstone_definition");oldTag.remove("lit_caves");var old=TerrainProfile.decode(oldTag,factory);
            int sy=witness.getY()>>4;
            for(int sampleSy=sy-1;sampleSy<=sy+1;sampleSy++) {
                var section=p.generate(sampleSy);var view=p.view(sampleSy);
                require(ColumnMigration.matches(p,sampleSy,section),"Matching cave section may be discarded");
                var filled=new LevelChunkSection(new PalettedContainer<>(Blocks.DEEPSLATE.defaultBlockState(),factory.blockStatesStrategy()),p.biomes());
                boolean differs=false;
                for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
                    int wy=sampleSy*16+y,wx=chunk.getPos().getMinBlockX()+x,wz=chunk.getPos().getMinBlockZ()+z;
                    var expected=p.state(wx,wy,wz);
                    require(section.getBlockState(x,y,z)==expected&&view.getBlockState(x,y,z)==expected&&decoded.state(x,wy,z)==expected,"Generated/virtual/decoded local and world coordinates agree");
                    require(level.getBlockState(new BlockPos(wx,wy,wz))==expected,"FULL native blocks equal immutable cave source");
                    if(expected.isAir()){voids++;differs=true;}else{require(expected.is(Blocks.DEEPSLATE),"Underground remains undecorated rock");rocks++;}
                    fingerprint=31*fingerprint+net.minecraft.world.level.block.Block.getId(expected);checks++;
                }
                require(view.hasOnlyAir()==section.hasOnlyAir(),"Virtual empty-section classification agrees");
                if(differs)require(!ColumnMigration.matches(p,sampleSy,filled),"Uniform solid edit cannot erase interior cave differences");
            }
            // Below-surface clipping must account for holes, not the old solid-rock shortcut.
            for(int z:new int[]{0,15})for(int x:new int[]{0,15}) {
                int wx=chunk.getPos().getMinBlockX()+x,wz=chunk.getPos().getMinBlockZ()+z;
                for(var type:Heightmap.Types.values()) {
                    var bounds=LevelHeightAccessor.create(sy*16,16);int expected=bounds.getMinY();
                    for(int y=bounds.getMaxY();y>=bounds.getMinY();y--)if(type.isOpaque().test(p.state(x,y,z))){expected=y+1;break;}
                    int top=p.top(type.isOpaque(),x,z,bounds.getMaxY());
                    require((top>bounds.getMinY()?top:bounds.getMinY())==expected,"Clipped profile height "+type);
                    require(g.getBaseHeight(wx,wz,type,bounds,r)==expected,"Clipped generator height "+type);
                    require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Surface heightmap unchanged "+type);
                }
                var column=g.getBaseColumn(wx,wz,LevelHeightAccessor.create(sy*16,16),r);
                for(int y=sy*16;y<sy*16+16;y++)require(column.getBlock(y)==p.state(x,y,z)&&g.surfaceState(wx,y,wz,r)==p.state(x,y,z),"Generator column/point matches cave profile");
                int ground=p.groundHeight(x,z);
                for(int y=ground-UndergroundStructure.ROOF_THICKNESS;y<=ground+3;y++)require(p.state(x,y,z)==old.state(x,y,z),"Protected surface matches legacy solid profile");
            }
            require(sparse.blockbranch$sections().size()<40,"Cave queries do not materialize entire vertical volume");
            if(dx!=0||dz!=0) {
                var tag=SerializableChunkData.copyOf(level,chunk).write();
                require(tag.getListOrEmpty("sections").isEmpty(),"Untouched cave layers omitted from disk");
            }
        }
        require(voids>500&&rocks>voids,"Native sampled cave volume retains dominant rock and actual voids");
        // Actual marine source water and its seabed remain equal to the legacy
        // profile; geometry only removes native rock below the protected roof.
        var ocean=DeepOceanSmoke.fixture(g,r);var marine=level.getChunkAt(ocean);var marineBase=((SparseColumn)marine).blockbranch$terrain().base();
        var marineTag=marineBase.encode(factory);marineTag.putInt("version",60);marineTag.remove("underground");var marineLegacy=TerrainProfile.decode(marineTag,factory);
        int bed=marineBase.groundHeight(ocean.getX(),ocean.getZ());
        for(int y=bed-24;y<=bed+16;y++)require(level.getBlockState(new BlockPos(ocean.getX(),y,ocean.getZ()))==marineLegacy.state(ocean.getX(),y,ocean.getZ()),"Actual seabed/roof/source water preserved");
        require(level.getFluidState(new BlockPos(ocean.getX(),bed+2,ocean.getZ())).isSource(),"Marine water remains source fluid");
        // Snapshot corruption must not alter the original immutable geometry.
        var tag=base.encode(factory);tag.getCompoundOrEmpty("underground").putLong("seed",r.seed()+1);
        require(base.encode(factory).getCompoundOrEmpty("underground").getLongOr("seed",0)==r.seed(),"Cave snapshot isolated");
        for(String key:new String[]{"version","seed","column_x","column_z"}) {
            var bad=base.encode(factory);bad.getCompoundOrEmpty("underground").remove(key);reject(bad,factory,"Incomplete descriptor "+key);
        }
        var bad=base.encode(factory);bad.getCompoundOrEmpty("underground").putInt("version",999);reject(bad,factory,"Unknown cave definition");
        bad=base.encode(factory);bad.remove("underground");reject(bad,factory,"Missing cave descriptor");
        for(int version:new int[]{57,58,59,60}) {
            var legacy=base.encode(factory);legacy.putInt("version",version);legacy.remove("underground");
            var old=TerrainProfile.decode(legacy,factory);require(!old.underground().enabled()&&old.state(witness.getX(),witness.getY(),witness.getZ()).is(Blocks.DEEPSLATE),"Legacy profile remains solid v"+version);
        }
        var wrongColumn=base.withUnderground(new UndergroundLayer(r.seed(),home.getPos().x()+1,home.getPos().z()));
        boolean wrongRejected=false;try{new ColumnTerrain(home,wrongColumn,factory,null,new int[0]);}catch(IllegalArgumentException expected){wrongRejected=true;}
        require(wrongRejected,"A saved or received cave descriptor cannot bind to another column");
        bad=base.encode(factory);bad.getCompoundOrEmpty("underground").putInt("column_x",Integer.MAX_VALUE);reject(bad,factory,"Overflowing column coordinate");
        // Persist and evict an actual section edit, then recover it before the
        // separate full-process RELOAD run also checks its disk persistence.
        level.setBlock(witness,Blocks.COBBLESTONE.defaultBlockState(),2);terrain.flush();
        terrain.evictOutside(20,21);
        require(!terrain.resident(witness.getY()>>4),"Edited cave section evicted");
        require(level.getBlockState(witness).is(Blocks.COBBLESTONE),"Edited cave section restores after eviction");
        level.setBlock(witness,base.state(witness.getX(),witness.getY(),witness.getZ()),2);
        var light=level.getChunkSource().getLightEngine();
        // No player is attached to this dedicated fixture. Activate a bounded
        // light window just as VerticalRuntime does for an observing player.
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int sy=(witness.getY()>>4)-1;sy<=(witness.getY()>>4)+1;sy++) {
            var c=level.getChunk(home.getPos().x()+dx,home.getPos().z()+dz);
            light.updateSectionStatus(net.minecraft.core.SectionPos.of(c.getPos(),sy),((SparseColumn)c).blockbranch$read(sy).hasOnlyAir());
        }
        var fence=light.waitForPendingTasks(home.getPos().x(),home.getPos().z());level.getServer().managedBlock(fence::isDone);fence.join();
        require(level.getBrightness(LightLayer.SKY,witness)==0,"Sealed cave receives no skylight");
        level.setBlock(witness,Blocks.GLOWSTONE.defaultBlockState(),3);light.checkBlock(witness);
        fence=light.waitForPendingTasks(home.getPos().x(),home.getPos().z());var wait=fence;level.getServer().managedBlock(wait::isDone);fence.join();
        require(level.getBrightness(LightLayer.BLOCK,witness)==15,"Native cave light source reaches full intensity");
        require(level.getBrightness(LightLayer.BLOCK,witness.above())==14,"Native block light propagates into adjacent cave air");
        level.setBlock(witness,Blocks.COBBLESTONE.defaultBlockState(),3);light.checkBlock(witness);
        var removed=light.waitForPendingTasks(home.getPos().x(),home.getPos().z());level.getServer().managedBlock(removed::isDone);removed.join();
        require(level.getBrightness(LightLayer.BLOCK,witness.above())==0,"Removing temporary source clears cave block light");
        terrain.flush();home.markUnsaved();
        PathEvidence.write(level,witness,fingerprint,voids,rocks,checks,reload);
        Blockbranch.LOGGER.info("UNDERGROUND_NATIVE_PASS mode={} witness={} void={} rock={} blocks={} fingerprint={}",reload?"RELOAD":"NEW",witness,voids,rocks,checks,fingerprint);
    }
    public static void verifyLegacyWorld(ServerLevel level,RealisticGenerator g) {
        require(!g.undergroundEnabled(),"Existing saved generator remains opted out");
        var chunk=level.getChunk(0,0);var base=((SparseColumn)chunk).blockbranch$terrain().base();
        require(!base.underground().enabled(),"Existing stored profile remains opted out");
        int top=Math.min(-64,base.groundHeight(8,8)-100);
        for(int y=top-128;y<top;y+=7)require(base.state(8,y,8).is(Blocks.DEEPSLATE),"Legacy underground unchanged below actual seabed/surface");
        Blockbranch.LOGGER.info("UNDERGROUND_LEGACY_WORLD_PASS generator=false profile=false");
    }
    private static void reject(net.minecraft.nbt.CompoundTag tag,PalettedContainerFactory f,String message){boolean rejected=false;try{TerrainProfile.decode(tag,f);}catch(IllegalArgumentException e){rejected=true;}require(rejected,message);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static class PathEvidence {
        static void write(ServerLevel level,BlockPos p,long fingerprint,int v,int rock,int checks,boolean reload)throws Exception{
            var result=java.nio.file.Path.of(System.getProperty("blockbranch.realisticSmokeResult"));
            var fp=result.resolveSibling("underground-fingerprint.txt");String value=Long.toString(fingerprint);
            if(reload)require(java.nio.file.Files.readString(fp).trim().equals(value),"Cave geometry stable across process reload");
            java.nio.file.Files.writeString(fp,value+"\n");
            java.nio.file.Files.writeString(result.resolveSibling("underground-evidence.txt"),"mode="+(reload?"RELOAD":"NEW")+"\nseed="+level.getSeed()+"\nwitness="+p+"\nvoid="+v+"\nrock="+rock+"\nblocks="+checks+"\nfingerprint="+value+"\n");
        }
    }
    public static List<BlockPos> automaticRoute(RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState state,BlockPos start){
        var random=new UndergroundStructure(state.seed());var heights=new HashMap<Long,Integer>();var free=new HashMap<BlockPos,Boolean>();
        java.util.function.Predicate<BlockPos> open=p->free.computeIfAbsent(p,k->{long key=((long)k.getX()<<32)^(k.getZ()&0xffffffffL);int h=heights.computeIfAbsent(key,q->g.drainage(state).sample(k.getX(),k.getZ()).height());return random.voidAt(k.getX(),k.getY(),k.getZ(),h)&&random.voidAt(k.getX(),k.getY()+1,k.getZ(),h);});
        var previous=new LinkedHashMap<BlockPos,BlockPos>();var queue=new ArrayDeque<BlockPos>();previous.put(start,start);queue.add(start);BlockPos horizontal=null,vertical=null,far=start;
        while(!queue.isEmpty()&&previous.size()<40000){
            var p=queue.removeFirst();if(p.distManhattan(start)>far.distManhattan(start))far=p;
            if(horizontal==null&&(p.getX()>>4!=start.getX()>>4||p.getZ()>>4!=start.getZ()>>4))horizontal=p;
            if(vertical==null&&p.getY()>>4!=start.getY()>>4)vertical=p;
            for(var d:net.minecraft.core.Direction.values()){
                var n=p.relative(d);if(Math.abs(n.getX()-start.getX())>24||Math.abs(n.getZ()-start.getZ())>24||Math.abs(n.getY()-start.getY())>24||previous.containsKey(n)||!open.test(n))continue;
                previous.put(n,p);queue.add(n);
            }
        }
        require(horizontal!=null&&vertical!=null,"Bounded cave component crosses horizontal and vertical sections");
        var out=new ArrayList<BlockPos>();out.add(start);
        for(var target:List.of(horizontal,vertical,far)){
            var path=new ArrayList<BlockPos>();for(var p=target;!p.equals(start);p=previous.get(p))path.add(p);Collections.reverse(path);
            for(int i=0;i<path.size();i++)if(i%4==3||i==path.size()-1)out.add(path.get(i));
            for(int i=path.size()-1;i>=0;i--)if(i%4==0)out.add(path.get(i));out.add(start);
        }
        Blockbranch.LOGGER.info("UNDERGROUND_AUTOMATIC_ROUTE visited={} waypoints={} horizontal={} vertical={} far={}",previous.size(),out.size(),horizontal,vertical,far);
        return List.copyOf(out);
    }
    private UndergroundSmoke(){}
}
