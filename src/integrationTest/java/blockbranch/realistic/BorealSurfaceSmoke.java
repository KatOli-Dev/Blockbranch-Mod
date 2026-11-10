package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Opt-in native audit: fresh soil, append-only saved meanings, and explicit player edits. */
public final class BorealSurfaceSmoke {
    private static final int HEIGHT=80;
    public static void verify(ServerLevel level,RealisticGenerator generator,boolean reload) {
        selectors(level);
        compatibility(level);
        var random=level.getChunkSource().randomState();
        var drainage=generator.drainage(random);
        var pine=OldGrowthTaigaFixtures.fixture(drainage,random.seed(),OldGrowthTaiga.PINE);
        var spruce=OldGrowthTaigaFixtures.fixture(drainage,random.seed(),OldGrowthTaiga.SPRUCE);
        var fixtures=List.of(TaigaSmoke.fixture(generator,random),
            new BlockPos(pine.x(),drainage.sample(pine.x(),pine.z()).height(),pine.z()),
            new BlockPos(spruce.x(),drainage.sample(spruce.x(),spruce.z()).height(),spruce.z()),
            SnowyTaigaSmoke.fixture(generator,random),GroveSmoke.fixture(generator,random));
        int carpets=0,columns=0,caveWrappers=0;
        var ecology=new GroundEcology.Field(drainage::sample,new ClimateField(random.seed()));
        for(var bed:fixtures) {
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++) {
                var chunk=level.getChunk((bed.getX()>>4)+dx,(bed.getZ()>>4)+dz);
                var base=((SparseColumn)chunk).blockbranch$terrain().base();
                var tag=base.encode(level.palettedContainerFactory());
                var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
                if(tag.getIntOr("version",-1)==72&&tag.getIntOr("base_profile_version",-1)==70)caveWrappers++;
                var cover=tag.getByteArray("boreal").orElseThrow();
                var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
                for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
                    int wx=chunk.getPos().getMinBlockX()+x,wz=chunk.getPos().getMinBlockZ()+z,h=drainage.sample(wx,wz).height();
                    require(!base.state(wx,h,wz).is(Blocks.MOSS_BLOCK),"Fresh native profile emitted a moss cube at "+wx+","+wz);
                    if(cover[z*16+x]==0)continue;
                    columns++;
                    for(int y=h-2;y<=h+1;y++) {
                        var expected=base.state(wx,y,wz);
                        require(copy.state(wx,y,wz)==expected&&sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x,y&15,z)==expected,"Saved/native section materialization disagrees");
                        require(level.getBlockState(new BlockPos(wx,y,wz))==expected,"Actual native boreal floor disagrees");
                    }
                    var pos=new BlockPos(wx,h+1,wz);
                    if(level.getBlockState(pos).is(Blocks.MOSS_CARPET)) {
                        carpets++;
                        require(level.getBlockState(pos.below()).is(Blocks.PODZOL),"Carpet must retain real podzol soil");
                        require(level.getBlockState(pos.below(2)).is(Blocks.DIRT),"Carpet must retain dirt subsoil");
                        require(level.getBlockState(pos).canSurvive(level,pos),"Native carpet support");
                        require(GroundEcology.borealMossSite(ecology.sample(wx,wz).shade(base.canopy().topY(wx,wz)>h+2)),"Carpet outside eligible ecology");
                    }
                }
            }
            // Fixed positions in this isolated smoke world exercise actual sparse overlays.
            // A moss block placed by the player must never be mistaken for generated cover.
            var anchor=bed.above(48);
            var edits=List.of(Blocks.MOSS_BLOCK,Blocks.DIRT,Blocks.STONE);
            for(int i=0;i<edits.size();i++) {
                var pos=anchor.east(i);var expected=edits.get(i).defaultBlockState();
                if(reload)require(level.getBlockState(pos)==expected,"Saved explicit edit lost on reopen: "+pos);
                level.setBlock(pos,expected,2);
                var chunk=level.getChunkAt(pos);var terrain=((SparseColumn)chunk).blockbranch$terrain();
                chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
                require(level.getBlockState(pos)==expected,"Explicit edit lost on eviction: "+pos);
            }
        }
        require(columns>100&&carpets>0,"Actual boreal fixtures must include soil and eligible carpets");
        Blockbranch.LOGGER.info("BOREAL_SURFACE_NATIVE_PASS mode={} fixtures={} columns={} carpets={} caveFoundation70Wrappers={} explicitEdits={}",reload?"RELOAD":"NEW",fixtures.size(),columns,carpets,caveWrappers,fixtures.size()*3);
    }

    private static void selectors(ServerLevel level) {
        var wet=new GroundEcology.Site(.95,.5,0,0,0,true);
        var dry=new GroundEcology.Site(.1,-.5,1,0,0,false);
        for(int family=0;family<5;family++) {
            int carpets=0,soil=0;
            for(var site:List.of(wet,dry))for(int cz=-4;cz<4;cz++)for(int cx=-4;cx<4;cx++) {
                var codes=new byte[256];
                for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
                    int wx=cx*16+x,wz=cz*16+z;
                    int code=switch(family) {
                        case 0->GroundEcology.forest(424242,wx,wz,1,site,true);
                        case 1->Taiga.cover(424242,wx,wz,site,true);
                        case 2->OldGrowthFloor.cover(424242,wx,wz,site,true);
                        case 3->SnowyTaiga.cover(424242,wx,wz,site,true,false);
                        default->Grove.cover(424242,wx,wz,site,true,false);
                    };
                    codes[z*16+x]=(byte)code;
                    if(family>=3) {
                        int covered=family==3?SnowyTaiga.cover(424242,wx,wz,site,true,true):Grove.cover(424242,wx,wz,site,true,true);
                        require(!material(covered,HEIGHT).is(Blocks.MOSS_BLOCK)&&!material(covered,HEIGHT+1).is(Blocks.MOSS_CARPET),"Covered cold floor emitted a moss cube or buried carpet");
                    }
                    require(!material(code,HEIGHT).is(Blocks.MOSS_BLOCK),"Fresh selector family="+family+" emitted historical moss cube code="+code);
                    if(code==BorealSurface.SOIL||code==BorealSurface.CARPET) {
                        soil++;
                        require(material(code,HEIGHT).is(Blocks.PODZOL)&&material(code,HEIGHT-1).is(Blocks.DIRT),"Fresh moss role replaces real soil");
                        require(GroundEcology.borealMossSite(site),"Fresh moss role outside eligible ecology");
                    }
                    if(code==BorealSurface.CARPET){carpets++;require(material(code,HEIGHT+1).is(Blocks.MOSS_CARPET),"Fresh carpet missing");}
                }
                var profile=profile(level,codes);
                var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
                for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int y=HEIGHT-2;y<=HEIGHT+1;y++)
                    require(sections.computeIfAbsent(y>>4,profile::generate).getBlockState(x,y&15,z)==material(codes[z*16+x],y),"Selector/profile materialization differs");
            }
            require(soil>0&&carpets>0,"Selector family="+family+" lacks eligible soil/carpet witnesses");
        }
        Blockbranch.LOGGER.info("BOREAL_SURFACE_SELECTORS_PASS families=5 sites=2 columns=163840");
    }

    private static void compatibility(ServerLevel level) {
        for(int code:new int[]{3,6,15,16}) {
            var codes=new byte[256];Arrays.fill(codes,(byte)code);
            var profile=profile(level,codes);var tag=profile.encode(level.palettedContainerFactory());
            boolean fresh=code>=15;
            require(tag.getIntOr("version",-1)==(fresh?72:60),"Unexpected profile version for code="+code);
            require(tag.contains("base_profile_version")==fresh,"Legacy profile encoding changed");
            if(fresh)require(tag.getIntOr("base_profile_version",-1)==60,"Wrong saved foundation");
            var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
            priorReader(level,tag,fresh);
            var ground=fresh?Blocks.PODZOL:Blocks.MOSS_BLOCK;
            var plant=code==6||code==16?Blocks.MOSS_CARPET:Blocks.AIR;
            require(material(code,HEIGHT).is(ground)&&material(code,HEIGHT+1).is(plant),"Append-only direct material semantics changed");
            for(var p:List.of(profile,copy)) {
                require(p.state(0,HEIGHT,0).is(ground)&&p.state(0,HEIGHT+1,0).is(plant)&&p.state(0,HEIGHT-1,0).is(Blocks.DIRT),"Saved cover semantics changed");
                require(p.generate(HEIGHT>>4).getBlockState(0,HEIGHT&15,0).is(ground),"Saved cover materialization changed");
            }
            require(copy.encode(level.palettedContainerFactory()).equals(tag),"Profile roundtrip changed saved descriptor");
            if(!fresh)for(int version:new int[]{57,58,59,60}) {
                var historical=tag.copy();historical.putInt("version",version);
                var saved=TerrainProfile.decode(historical,level.palettedContainerFactory());
                require(saved.state(0,HEIGHT,0).is(Blocks.MOSS_BLOCK)&&saved.state(0,HEIGHT+1,0).is(plant),"Historical profile version="+version+" lost legacy cover code="+code);
            }
            if(fresh) {
                var missing=tag.copy();missing.remove("base_profile_version");reject(level,missing);
                var unknown=tag.copy();unknown.putInt("base_profile_version",71);reject(level,unknown);
                var nested=tag.copy();nested.putInt("base_profile_version",72);reject(level,nested);
                var legacy=tag.copy();legacy.remove("base_profile_version");legacy.putInt("version",60);reject(level,legacy);
            }else {
                var marked=tag.copy();marked.putInt("base_profile_version",60);reject(level,marked);
                var emptyWrapper=tag.copy();emptyWrapper.putInt("version",72);emptyWrapper.putInt("base_profile_version",60);reject(level,emptyWrapper);
            }
        }
        Blockbranch.LOGGER.info("BOREAL_SURFACE_COMPATIBILITY_PASS legacyCodes=3,6 freshCodes=15,16 malformedDescriptorsRejected=true");
    }
    private static void priorReader(ServerLevel level,CompoundTag tag,boolean fresh) {
        String jar=System.getProperty("blockbranch.borealPriorProfileJar");
        if(jar==null)return;
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{java.nio.file.Path.of(jar).toUri().toURL()},TerrainProfile.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.equals("blockbranch.cubic.TerrainProfile")&&!name.startsWith("blockbranch.cubic.TerrainProfile$"))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)) {
                    var result=findLoadedClass(name);if(result==null)result=findClass(name);
                    if(resolve)resolveClass(result);return result;
                }
            }
        }) {
            var prior=loader.loadClass("blockbranch.cubic.TerrainProfile");
            require(prior!=TerrainProfile.class,"Prior-reader fixture reused the current class");
            var decode=prior.getMethod("decode",CompoundTag.class,net.minecraft.world.level.chunk.PalettedContainerFactory.class);
            try {
                var decoded=decode.invoke(null,tag,level.palettedContainerFactory());
                require(!fresh,"Actual prior reader accepted wrapper72");
                var state=(BlockState)prior.getMethod("state",int.class,int.class,int.class).invoke(decoded,0,HEIGHT,0);
                require(state.is(Blocks.MOSS_BLOCK),"Actual prior reader changed legacy moss meaning");
            }catch(java.lang.reflect.InvocationTargetException failure) {
                if(!fresh||!(failure.getCause() instanceof IllegalArgumentException))throw failure;
            }
        }catch(Exception failure){throw new AssertionError("Actual prior-reader fixture failed",failure);}
    }
    private static TerrainProfile profile(ServerLevel level,byte[] boreal) {
        var ground=new int[256];Arrays.fill(ground,HEIGHT);
        var water=new int[256];Arrays.fill(water,Integer.MIN_VALUE);
        var biomes=((SparseColumn)level.getChunk(0,0)).blockbranch$terrain().base().biomes();
        return new TerrainProfile(0,256,ground,water,new byte[256],new byte[256],new byte[256],new byte[256],new byte[256],new byte[256],new byte[256],boreal,CanopyLayer.EMPTY,biomes,level.palettedContainerFactory().blockStatesStrategy());
    }
    private static BlockState material(int code,int y) {return TerrainProfile.material(0,256,y,HEIGHT,Integer.MIN_VALUE,0,0,0,0,0,0,0,code);}
    private static void reject(ServerLevel level,CompoundTag tag) {
        try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(IllegalArgumentException expected){return;}
        throw new AssertionError("Malformed boreal descriptor accepted");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private BorealSurfaceSmoke(){}
}
