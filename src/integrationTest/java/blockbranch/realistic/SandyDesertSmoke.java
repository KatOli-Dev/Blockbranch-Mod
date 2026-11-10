package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import java.nio.file.*;
import java.io.IOException;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Native sandy substrate, cactus behavior and immutable save snapshots in disposable fixtures. */
public final class SandyDesertSmoke {
    public static BlockPos fixture(RealisticGenerator g,RandomState r){
        String fixed=System.getProperty("blockbranch.desertFixturePoint");
        if(fixed!=null){String[] point=fixed.split(",");require(point.length==2,"Expected desert fixture x,z");int x=Integer.parseInt(point[0]),z=Integer.parseInt(point[1]);var d=g.drainage(r);require(SandyDesert.contains(r.seed(),x,z,d,new ClimateField(r.seed()))&&SandyDesert.cover(r.seed(),x,z,true,d)==10,"Fixed fixture must be a natural one-block deep-sand cactus");return new BlockPos(x,d.sample(x,z).height(),z);}
        var p=blockbranch.realistic.SandyDesertFixtures.fixture(g.drainage(r),r.seed());return new BlockPos(p.x(),g.drainage(r).sample(p.x(),p.z()).height(),p.z());}
    public static BlockPos clientFixture(RealisticGenerator g,RandomState r){var d=g.drainage(r);if(Boolean.getBoolean("blockbranch.desertComparison")&&r.seed()==424242){int x=420029,z=-1116057;if(Boolean.getBoolean("blockbranch.desertSecondSite")){var c=new ClimateField(r.seed());for(int[] delta:new int[][]{{512,512},{-512,-512},{-512,512},{512,-512},{1024,0}})for(int dz=-64;dz<=64;dz++)for(int dx=-64;dx<=64;dx++){int wx=x+delta[0]+dx,wz=z+delta[1]+dz;if(d.sample(wx,wz).height()<400||!SandyDesert.contains(r.seed(),wx,wz,d,c))continue;int code=SandyDesert.cover(r.seed(),wx,wz,true,d);if(code==10||code==18)return new BlockPos(wx,d.sample(wx,wz).height(),wz);}throw new AssertionError("No second sandy site");}return new BlockPos(x,d.sample(x,z).height(),z);}var p=blockbranch.realistic.SandyDesertFixtures.fixture(d,r.seed(),400);return new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());}
    public static BlockPos nativeWindowFixture(RealisticGenerator g,RandomState r){var p=blockbranch.realistic.SandyDesertFixtures.fixture(g.drainage(r),r.seed(),Integer.MIN_VALUE,319);return new BlockPos(p.x(),g.drainage(r).sample(p.x(),p.z()).height(),p.z());}
    public static void restore(ServerLevel level,BlockPos bed){for(int y=bed.getY()-5;y<=bed.getY()+5;y++){var p=new BlockPos(bed.getX(),y,bed.getZ());var base=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain().base();level.setBlock(p,base.state(p.getX(),y,p.getZ()),3);}}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var r=level.getChunkSource().randomState();var bed=fixture(g,r);var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();var saved=bed.below(8);
        require(level.getBiome(bed).is(Biomes.DESERT),"Actual vanilla Desert registry identity");
        if(reload){require(level.getBlockState(bed.above()).isAir(),"Saved cactus harvest persists");require(level.getBlockState(saved).is(Blocks.SANDSTONE),"Saved subsurface edit persists");}
        restore(level,bed);require(level.getBlockState(bed.above()).is(Blocks.CACTUS)&&level.getBlockState(bed.above(2)).isAir(),"Natural one-block cactus fixture");
        if(Boolean.getBoolean("blockbranch.desertConversionAudit")){require(!reload,"Conversion audit requires a fresh world");verifyConversion(level,g,bed,r);}
        var witness=new BlockPos[13];int remaining=5;
        for(int z=bed.getZ()-128;z<=bed.getZ()+128&&remaining>0;z++)for(int x=bed.getX()-128;x<=bed.getX()+128&&remaining>0;x++){int code=g.desertCover(x,z,r);if(code>=8&&code<=12&&witness[code]==null){witness[code]=new BlockPos(x,g.drainage(r).sample(x,z).height(),z);remaining--;}}
        require(remaining==0,"All sandy Desert cover codes naturally present");
        var resolver=g.getBiomeSource().createUncachedResolver(r);
        for(int code=8;code<=12;code++){
            var p=witness[code];int x=p.getX(),z=p.getZ(),h=p.getY();var nativeChunk=level.getChunkAt(p);var base=((SparseColumn)nativeChunk).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());var decoded=TerrainProfile.decode(tag,level.palettedContainerFactory());
            for(int y=h-7;y<=h+5;y++){var expected=g.surfaceState(x,y,z,r);var at=new BlockPos(x,y,z);require(nativeChunk.getBlockState(at)==expected&&decoded.state(x,y,z)==expected&&decoded.generate(y>>4).getBlockState(x&15,y&15,z&15)==expected,"Native/snapshot/materialized sandy profile equality");require(level.getFluidState(at).isEmpty(),"Dry substrate has no generated fluid");}
            require(base.top(s->s.is(Blocks.SAND),x,z,h)==h+1&&base.top(s->s.is(Blocks.SANDSTONE),x,z,h)==h-2&&base.top(s->s.is(Blocks.STONE),x,z,h)==h-4,"Three sand over two sandstone, original rock below");
            for(int y=h+1;y<=h+TerrainProfile.desertPlantHeight(code);y++)require(level.getBlockState(new BlockPos(x,y,z)).canSurvive(level,new BlockPos(x,y,z)),"Natural cactus/dead stem support");
            for(var bounds:List.of((LevelHeightAccessor)level,LevelHeightAccessor.create(h-5,2),LevelHeightAccessor.create(h-2,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(h+3,2))){var col=g.getBaseColumn(x,z,bounds,r);for(var type:Heightmap.Types.values()){int expected=bounds.getMinY();for(int y=Math.min(bounds.getMaxY(),h+5);y>=bounds.getMinY();y--)if(type.isOpaque().test(col.getBlock(y))){expected=y+1;break;}require(g.getBaseHeight(x,z,type,bounds,r)==expected,"Clipped cactus/substrate heightmap "+type);if(bounds==level)require(nativeChunk.getHeight(type,x&15,z&15)+1==expected,"Native sandy heightmap "+type);}}
            for(int y:new int[]{-3000,0,h>>2,2000})require(nativeChunk.getNoiseBiome(x>>2,y,z>>2).equals(resolver.getNoiseBiome(x>>2,y,z>>2))&&decoded.biomes().get((x&15)>>2,y&3,(z&15)>>2).equals(resolver.getNoiseBiome(x>>2,y,z>>2)),"Saved/source/native all-altitude Desert identity");
            if(code>=10)for(int y=h+1;y<=h+TerrainProfile.desertPlantHeight(code);y++)require(base.randomTickSections().contains(y>>4)&&base.view(y>>4).isRandomlyTicking(),"Only thin cactus-bearing sections enter random ticking");
            var original=tag.getByteArray("desert").orElseThrow().clone();tag.getByteArray("desert").orElseThrow()[(z&15)*16+(x&15)]=21;boolean rejected=false;try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected&&Arrays.equals(original,base.encode(level.palettedContainerFactory()).getByteArray("desert").orElseThrow()),"Invalid cover rejected without profile mutation");
        }
        verifyVeneerCases(level,terrain.base(),bed);
        var original=terrain.base().encode(level.palettedContainerFactory());var frozen=TerrainProfile.decode(original,level.palettedContainerFactory());
        var cactus=bed.above();level.setBlock(cactus,Blocks.CACTUS.defaultBlockState().setValue(CactusBlock.AGE,15),2);level.getBlockState(cactus).randomTick(level,cactus,level.getRandom());require(level.getBlockState(cactus.above()).is(Blocks.CACTUS),"Native age fifteen cactus growth");require(frozen.state(cactus.getX(),cactus.getY(),cactus.getZ()).getValue(CactusBlock.AGE)==0&&frozen.state(cactus.getX(),cactus.getY()+1,cactus.getZ()).isAir(),"Native growth cannot mutate saved procedural snapshot");
        var drops=Block.getDrops(Blocks.CACTUS.defaultBlockState(),level,cactus,null,null,new ItemStack(Items.IRON_AXE));require(drops.size()==1&&drops.getFirst().is(Items.CACTUS),"Native cactus harvesting item");
        restore(level,bed);terrain.flush();terrain.evictOutside(-100,-90);boolean resident=terrain.resident(cactus.getY()>>4);require(!VerticalRuntime.active(level,cactus.getX(),cactus.getY(),cactus.getZ()),"Native fixture has no active player window");level.tickChunk(chunk,8192);require(level.getBlockState(cactus).getValue(CactusBlock.AGE)==0&&terrain.resident(cactus.getY()>>4)==resident,"Off-window procedural cactus deferred without section allocation");
        var old=terrain.base().encode(level.palettedContainerFactory());old.putInt("version",19);boolean rejected=false;try{TerrainProfile.decode(old,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Pre-sandy profile explicitly rejected");
        level.setBlock(saved,Blocks.SANDSTONE.defaultBlockState(),2);level.setBlock(cactus,Blocks.AIR.defaultBlockState(),2);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(saved).is(Blocks.SANDSTONE)&&level.getBlockState(cactus).isAir(),"Sandy edits survive eviction");
        Blockbranch.LOGGER.info("SANDY_DESERT_NATIVE_PASS {} bed={} covers=8..12 snapshotIsolated=true inactiveTicksDeferred=true",reload?"RELOAD":"NEW",bed);
    }
    /** Frozen c76b141 vegetation selector; substrate helpers are unchanged by the candidate. */
    private static int legacyCover(long seed,int x,int z,DrainageNetwork d){
        var t=d.sample(x,z);if(t.waterLevel()!=DrainageNetwork.DRY||d.channelAt(x,z).edge()>=0)return 0;
        int bare=SandyDesert.bareCover(SandyDesert.mantle(seed,x,z,d));if(bare!=8&&bare!=13)return bare;
        long h=legacyCell(seed,x,z);
        if(legacyPotential(seed,x,z)){
            boolean clear=true;for(int[] step:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})if(d.sample(x+step[0],z+step[1]).height()>t.height())clear=false;
            for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)if((dx!=0||dz!=0)&&legacyPotential(seed,x+dx,z+dz)&&Long.compareUnsigned(legacyCell(seed,x+dx,z+dz),h)>0)clear=false;
            if(clear)return(bare==13?18:10)+(int)Long.remainderUnsigned(h>>>24,3);
        }
        double scrub=GroundEcology.colonyStrength(seed,0x53414E4442555348L,x,z,32,6,15,GroundEcology.NEUTRAL);
        return((OakBirchWoodland.mix(h)>>>11)*0x1.0p-53)<.025*scrub?(bare==13?17:9):bare;
    }
    private static long legacyCell(long seed,int x,int z){return OakBirchWoodland.mix(seed^0x434143545553L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);}
    private static boolean legacyPotential(long seed,int x,int z){return(legacyCell(seed,x,z)>>>11)*0x1.0p-53<.008*GroundEcology.colonyStrength(seed,0x53414E4443414354L,x,z,40,10,23,GroundEcology.NEUTRAL);}
    private static void verifyConversion(ServerLevel level,RealisticGenerator g,BlockPos bed,RandomState random){
        var drainage=g.drainage(random);
        for(int dz=-128;dz<=128;dz++)for(int dx=-128;dx<=128;dx++){
            int x=bed.getX()+dx,z=bed.getZ()+dz,code=g.desertCover(x,z,random);if(!SandyDesert.cactus(code))continue;
            int old=legacyCover(random.seed(),x,z,drainage);if(old!=9&&old!=17)continue;
            int h=drainage.sample(x,z).height(),height=TerrainProfile.desertPlantHeight(code);var p=new BlockPos(x,h,z);var chunk=level.getChunkAt(p);var profile=((SparseColumn)chunk).blockbranch$terrain().base();var copy=TerrainProfile.decode(profile.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            require(level.getBiome(p).is(Biomes.DESERT)&&level.getBlockState(p).is(Blocks.SAND)&&height>=1&&height<=3,"Fresh conversion has real sand support and bounded cactus height");
            for(int stem=1;stem<=height;stem++){
                var at=p.above(stem);var actual=level.getBlockState(at);require(actual.is(Blocks.CACTUS)&&actual.canSurvive(level,at),"Fresh converted cactus stem survives");
                require(copy.state(x,h+stem,z)==actual&&copy.generate((h+stem)>>4).getBlockState(x&15,(h+stem)&15,z&15)==actual,"Converted native/saved/materialized cactus states agree");
                require(profile.randomTickSections().contains((h+stem)>>4)&&profile.view((h+stem)>>4).isRandomlyTicking()&&profile.view((h+stem)>>4).maybeHas(state->state.is(Blocks.CACTUS)),"Converted cactus advertises random ticking and section state");
            }
            require(level.getBlockState(p.above(height+1)).isAir()&&copy.top(state->state.is(Blocks.CACTUS),x,z,h+4)==h+height+1,"Converted cactus predicate top matches actual height");
            Blockbranch.LOGGER.info("SANDY_DESERT_FRESH_CONVERSION_PASS bed={} oldCode={} newCode={} stems={} nativeSavedMaterialized=true",p,old,code,height);return;
        }
        throw new AssertionError("No fresh dead-bush-to-cactus conversion within bounded native fixture");
    }
    private static Path ledgerPath(){return Path.of(System.getProperty("blockbranch.memorySmokeResult")).resolveSibling("desert-witness.properties");}
    /** Retain actual native growth as a separate player-like edit, after the destructive fixture assertions. */
    public static void saveGrownWitness(ServerLevel level,RealisticGenerator g)throws IOException{
        var r=level.getChunkSource().randomState();var bed=fixture(g,r);BlockPos grown=null;
        search:for(int dz=-64;dz<=64;dz++)for(int dx=-64;dx<=64;dx++){
            int x=bed.getX()+dx,z=bed.getZ()+dz;if(Math.abs(dx)+Math.abs(dz)<8||g.desertCover(x,z,r)!=10)continue;
            var p=new BlockPos(x,g.drainage(r).sample(x,z).height(),z);
            var profile=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain().base();
            if(!profile.state(x,p.getY()+1,z).is(Blocks.CACTUS)||!level.getBlockState(p.above()).is(Blocks.CACTUS)||!level.getBlockState(p.above(2)).isAir())continue;
            level.setBlock(p.above(),Blocks.CACTUS.defaultBlockState().setValue(CactusBlock.AGE,15),2);
            level.getBlockState(p.above()).randomTick(level,p.above(),level.getRandom());
            require(level.getBlockState(p.above(2)).is(Blocks.CACTUS)||level.getBlockState(p.above(2)).is(Blocks.CACTUS_FLOWER),"Native witness grows cactus or flower");grown=p;break search;
        }
        require(grown!=null,"Separate natural cactus growth witness");
        var ledger=new Properties();ledger.setProperty("bed",bed.getX()+","+bed.getY()+","+bed.getZ());ledger.setProperty("grown",grown.getX()+","+grown.getY()+","+grown.getZ());
        for(var entry:Map.of("bed",bed,"grown",grown).entrySet()){
            var p=entry.getValue();var terrain=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain();
            for(int dy=-8;dy<=4;dy++){var at=p.above(dy);ledger.setProperty(entry.getKey()+".live."+dy,level.getBlockState(at).toString());ledger.setProperty(entry.getKey()+".base."+dy,terrain.base().state(at.getX(),at.getY(),at.getZ()).toString());}
            terrain.flush();level.getChunkAt(p).markUnsaved();
        }
        try(var out=Files.newBufferedWriter(ledgerPath())){ledger.store(out,"Native old-selector cactus witnesses; saved-only verification must not restore these edits");}
        Blockbranch.LOGGER.info("SANDY_DESERT_GROWN_LEDGER_PASS bed={} grown={} state={}",bed,grown,level.getBlockState(grown.above(2)));
    }
    /** Upgrade audit compares immutable saved bases and existing edits, never current generator results. */
    public static void verifySaved(ServerLevel level)throws IOException{
        var ledger=new Properties();try(var in=Files.newBufferedReader(ledgerPath())){ledger.load(in);}
        for(String name:List.of("bed","grown")){
            String[] xyz=ledger.getProperty(name).split(",");var p=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
            var chunk=level.getChunkAt(p);var base=((SparseColumn)chunk).blockbranch$terrain().base();var copy=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            require(level.getBiome(p).is(Biomes.DESERT),"Saved native desert identity");
            for(int dy=-8;dy<=4;dy++){var at=p.above(dy);String expected=ledger.getProperty(name+".base."+dy);require(level.getBlockState(at).toString().equals(ledger.getProperty(name+".live."+dy)),"Saved live witness preserved "+name+" dy="+dy);require(base.state(at.getX(),at.getY(),at.getZ()).toString().equals(expected)&&copy.state(at.getX(),at.getY(),at.getZ()).toString().equals(expected)&&copy.generate(at.getY()>>4).getBlockState(at.getX()&15,at.getY()&15,at.getZ()&15).toString().equals(expected),"Saved immutable cactus base/materialization preserved "+name+" dy="+dy);}
            if(name.equals("bed"))require(level.getBlockState(p.below(8)).is(Blocks.SANDSTONE)&&level.getBlockState(p.above()).isAir()&&base.state(p.getX(),p.getY()+1,p.getZ()).is(Blocks.CACTUS),"Harvest and subsurface edit retained against old natural cactus base");
            else require(level.getBlockState(p.above(2)).is(Blocks.CACTUS)||level.getBlockState(p.above(2)).is(Blocks.CACTUS_FLOWER),"Actual native grown edit retained");
        }
        Blockbranch.LOGGER.info("SANDY_DESERT_SAVED_WITNESSES_PASS restore=false currentGeneratorEquality=false");
    }
    private static void verifyVeneerCases(ServerLevel level,TerrainProfile base,BlockPos bed){
        int x=bed.getX(),z=bed.getZ(),h=bed.getY();
        for(int code=13;code<=20;code++){
            var tag=base.encode(level.palettedContainerFactory());tag.getByteArray("desert").orElseThrow()[(z&15)*16+(x&15)]=(byte)code;var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
            for(int y=h-6;y<=h+4;y++){
                var expected=veneerExpected(code,y,h);require(copy.state(x,y,z)==expected&&copy.generate(y>>4).getBlockState(x&15,y&15,z&15)==expected,"Thin/lag/outcrop veneer snapshot and materialization "+code);
            }
            for(var block:List.of(Blocks.SAND,Blocks.SANDSTONE,Blocks.GRAVEL,Blocks.STONE,Blocks.CACTUS,Blocks.DEAD_BUSH)){
                int expected=ColumnLimits.MIN_Y;for(int y=h+4;y>=TerrainShape.FLOOR;y--)if(veneerExpected(code,y,h).is(block)){expected=y+1;break;}
                require(copy.top(state->state.is(block),x,z,h+4)==expected,"Bounded veneer predicate top "+code+" "+block);
            }
            if(SandyDesert.cactus(code))require(copy.randomTickSections().contains((h+1)>>4),"Thin-sand cactus ticks rebuilt from snapshot");
        }
        Blockbranch.LOGGER.info("SANDY_DESERT_VENEER_CODEC_MATERIAL_HEIGHT_PASS codes=13..20");
    }
    private static net.minecraft.world.level.block.state.BlockState veneerExpected(int code,int y,int h){
        if(y==TerrainShape.FLOOR)return Blocks.BEDROCK.defaultBlockState();
        int plant=code==17?1:code>=18?code-17:0;
        if(y>h)return(y<=h+plant?(code==17?Blocks.DEAD_BUSH:Blocks.CACTUS):Blocks.AIR).defaultBlockState();
        if(code==13||code>=17){if(y==h)return Blocks.SAND.defaultBlockState();if(y>=h-2)return Blocks.SANDSTONE.defaultBlockState();}
        if(code==14&&y==h)return Blocks.GRAVEL.defaultBlockState();
        if((code==14||code==15)&&y>=h-1)return Blocks.SANDSTONE.defaultBlockState();
        return(y>=0?Blocks.STONE:Blocks.DEEPSLATE).defaultBlockState();
    }
    public static void activeGrowth(ServerLevel level,BlockPos bed){
        var pos=bed.above();var chunk=level.getChunkAt(pos);var terrain=((SparseColumn)chunk).blockbranch$terrain();require(VerticalRuntime.active(level,pos.getX(),pos.getY(),pos.getZ()),"Real client has activated cactus surface");
        terrain.flush();terrain.evictOutside(-100,-90);require(!terrain.resident(pos.getY()>>4),"Natural cactus starts as unresident procedural section");
        int attempts=0;while(level.getBlockState(pos.above()).isAir()&&attempts++<128)level.tickChunk(chunk,4096);
        require(level.getBlockState(pos.above()).is(Blocks.CACTUS)||level.getBlockState(pos.above()).is(Blocks.CACTUS_FLOWER),"Active native random dispatcher grows cactus or native flower");
        Blockbranch.LOGGER.info("SANDY_DESERT_ACTIVE_DISPATCH_PASS bed={} attempts={} nativeWindow={} growth={}",bed,attempts,(pos.getY()>>4)>=((SparseColumn)chunk).blockbranch$baseMinSection()&&(pos.getY()>>4)<((SparseColumn)chunk).blockbranch$baseMinSection()+chunk.getSections().length,level.getBlockState(pos.above()));
    }
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private SandyDesertSmoke(){}
}
