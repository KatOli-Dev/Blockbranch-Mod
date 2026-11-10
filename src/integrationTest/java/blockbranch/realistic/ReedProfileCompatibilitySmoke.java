package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

/** Explicit short-stack format gates, independently of the fresh colony selector. */
public final class ReedProfileCompatibilitySmoke {
    private static final int HEIGHT=78;
    public static void verify(ServerLevel level,RealisticGenerator generator) {
        String jar=System.getProperty("blockbranch.reedPriorProfileJar","build/reed-evidence/prior-production-577f4ca.jar");
        try(var loader=new URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},TerrainProfile.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                // Cave validators take TerrainProfile in their signatures. Keep the
                // archived cubic closure together; share only native Minecraft and
                // unchanged realistic helpers across the reflection boundary.
                if(!name.startsWith("blockbranch.cubic."))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)) {
                    var type=findLoadedClass(name);if(type==null)type=findClass(name);
                    if(resolve)resolveClass(type);return type;
                }
            }
        }) {
            var prior=loader.loadClass("blockbranch.cubic.TerrainProfile");
            require(prior!=TerrainProfile.class,"Prior reader fixture reused current codec");
            for(String name:new String[]{"TerrainProfile","DripstoneLayer","LitCaveLayer","CaveBiomeLayer","UndergroundLayer","CanopyLayer","FloatingIceLayer"})
                require(loader.loadClass("blockbranch.cubic."+name).getClassLoader()==loader,"Prior reader dependency reused current class: "+name);
            var oldDecode=prior.getMethod("decode",CompoundTag.class,net.minecraft.world.level.chunk.PalettedContainerFactory.class);
            int profiles=0;
            for(int depth:new int[]{1,2})for(boolean shortStack:new boolean[]{false,true})for(boolean boreal:new boolean[]{false,true})for(int foundation:new int[]{60,61,62,63,64,66,68,70}) {
                int stems=depth+(shortStack?1:2);
                var profile=profile(level,depth,stems,boreal,foundation);
                var tag=profile.encode(level.palettedContainerFactory());
                int inner=boreal?72:foundation;
                require(tag.getIntOr("version",-1)==(shortStack?74:inner),"Conditional reed wrapper");
                require(tag.contains("reed_base_profile_version")==shortStack,"Legacy reed descriptor changed");
                if(shortStack)require(tag.getIntOr("reed_base_profile_version",-1)==inner,"Reed foundation lost");
                if(boreal)require(tag.getIntOr("base_profile_version",-1)==foundation,"Boreal foundation lost");
                var restored=TerrainProfile.decode(tag,level.palettedContainerFactory());
                require(restored.encode(level.palettedContainerFactory()).equals(tag),"Profile roundtrip changed descriptors");
                for(int y=HEIGHT-3;y<=HEIGHT+5;y++) {
                    var state=profile.state(0,y,0);
                    require(restored.state(0,y,0)==state&&restored.generate(y>>4).getBlockState(0,y&15,0)==state,"Saved/native section disagreement");
                    if(y>HEIGHT&&y<=HEIGHT+stems)require(state.is(ReedbedContent.LAKE_REED)&&state.getValue(LakeReedBlock.TIP)==(y==HEIGHT+stems)&&state.getValue(LakeReedBlock.WATERLOGGED)==(y<HEIGHT+depth+1),"Stem tip/fluid meanings changed");
                    if(y==HEIGHT+stems+1)require(state.isAir(),"Shortened top must be air");
                }
                for(int qy:new int[]{-3000,0,HEIGHT>>2,1000})for(int z=0;z<4;z++)for(int x=0;x<4;x++)
                    require(profile.biome(x,qy,z).equals(restored.biome(x,qy,z)),"Full cave biome query changed");
                try {
                    var old=oldDecode.invoke(null,tag,level.palettedContainerFactory());
                    require(!shortStack,"Actual prior reader accepted short stack");
                    var state=prior.getMethod("state",int.class,int.class,int.class);
                    for(int y=HEIGHT-2;y<=HEIGHT+5;y++)require(state.invoke(old,0,y,0)==profile.state(0,y,0),"Actual prior reader changed legacy material");
                }catch(java.lang.reflect.InvocationTargetException failure) {
                    if(!shortStack||!(failure.getCause() instanceof IllegalArgumentException))throw failure;
                }
                malformed(level,tag,shortStack,inner);
                profiles++;
            }
            for(int depth:new int[]{1,2,3})for(int stems:new int[]{2,3,4}) {
                boolean valid=depth<=2&&(stems==depth+1||stems==depth+2),accepted=true;
                try{profile(level,depth,stems,false,60);}catch(IllegalArgumentException expected){accepted=false;}
                require(accepted==valid,"Constructor accepted invalid depth/stem combination");
            }
            Blockbranch.LOGGER.info("REED_PROFILE_COMPATIBILITY_PASS profiles={} foundations=8 depths=2 borealAndCaveRecursion=true actualPriorReader=true",profiles);
        }catch(Exception failure){throw new AssertionError("Reed profile compatibility failed",failure);}
    }
    private static TerrainProfile profile(ServerLevel level,int depth,int stems,boolean boreal,int foundation) {
        var ground=new int[256];Arrays.fill(ground,HEIGHT);
        var water=new int[256];Arrays.fill(water,DrainageNetwork.DRY);water[0]=HEIGHT+depth+1;
        var reeds=new byte[256];reeds[0]=(byte)stems;
        var cover=new byte[256];if(boreal)cover[1]=15;
        var biomes=((SparseColumn)level.getChunk(0,0)).blockbranch$terrain().base().biomes();
        var p=new TerrainProfile(TerrainShape.FLOOR,TerrainShape.CEILING,ground,water,new byte[256],reeds,new byte[256],new byte[256],new byte[256],new byte[256],new byte[256],cover,CanopyLayer.EMPTY,biomes,level.palettedContainerFactory().blockStatesStrategy());
        if(foundation==60)return p;
        var under=new UndergroundLayer(424242,0,0);p=p.withUnderground(under);
        if(foundation==61)return p;
        p=p.withLayers(under,new OreLayer(424242,0,0,foundation==62?1:2,0,0));
        if(foundation==62||foundation==63)return p;
        var registry=level.registryAccess().lookupOrThrow(Registries.BIOME);
        p=p.withDripstone(new DripstoneLayer(424242,0,0,registry.getOrThrow(Biomes.DRIPSTONE_CAVES),new int[0],foundation<=66?1:2));
        if(foundation==64)return p;
        var growth=new LitCaveLayer(424242,0,0,new int[0]);p=p.withLitCaves(growth);
        if(foundation==66||foundation==68)return p;
        return p.withCaveBiomes(new CaveBiomeLayer(424242,0,0,registry.getOrThrow(Biomes.LUSH_CAVES),registry.getOrThrow(Biomes.SULFUR_CAVES),growth));
    }
    private static void malformed(ServerLevel level,CompoundTag tag,boolean shortStack,int inner) {
        if(shortStack) {
            var missing=tag.copy();missing.remove("reed_base_profile_version");reject(level,missing);
            for(int version:new int[]{57,58,59,65,71,73,74,999}) {
                var bad=tag.copy();bad.putInt("reed_base_profile_version",version);reject(level,bad);
            }
            var legacy=tag.copy();legacy.putInt("version",inner);legacy.remove("reed_base_profile_version");reject(level,legacy);
            var downgraded=tag.copy();downgraded.putInt("version",inner);reject(level,downgraded);
            var wrong=tag.copy();wrong.putInt("reed_base_profile_version",inner==72?60:72);reject(level,wrong);
            var unneeded=tag.copy();var cover=unneeded.getByteArray("reedbed").orElseThrow();cover[0]++;unneeded.putByteArray("reedbed",cover);reject(level,unneeded);
            var truncated=tag.copy();truncated.putByteArray("reedbed",new byte[255]);reject(level,truncated);
            var invalid=tag.copy();var cover2=invalid.getByteArray("reedbed").orElseThrow();cover2[2]=4;invalid.putByteArray("reedbed",cover2);reject(level,invalid);
        }else {
            var stray=tag.copy();stray.putInt("reed_base_profile_version",inner);reject(level,stray);
            var empty=tag.copy();empty.putInt("version",74);empty.putInt("reed_base_profile_version",inner);reject(level,empty);
        }
    }
    private static void reject(ServerLevel level,CompoundTag tag) {
        try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(RuntimeException expected){return;}
        throw new AssertionError("Malformed reed descriptor accepted");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private ReedProfileCompatibilitySmoke(){}
}
