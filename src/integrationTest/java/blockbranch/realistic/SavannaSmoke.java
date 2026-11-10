package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Native acacia palette, saved snapshots and persistent edits in an isolated fixture only. */
public final class SavannaSmoke {
    public static BlockPos fixture(RealisticGenerator g,RandomState r){var p=blockbranch.realistic.SavannaFixtures.fixture(g.drainage(r),r.seed());return new BlockPos(p.x(),g.drainage(r).sample(p.x(),p.z()).height(),p.z());}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        verify(level,g,reload,false);
    }
    /** Historical saved canopies need not equal the current generator's appearance. */
    public static void verifyLegacy(ServerLevel level,RealisticGenerator g,boolean reload){
        require(reload,"Legacy canopy qualification requires a saved fixture");
        verify(level,g,true,true);
    }
    private static void verify(ServerLevel level,RealisticGenerator g,boolean reload,boolean legacyCanopy){
        var random=level.getChunkSource().randomState();var bed=fixture(g,random);var source=g.getBiomeSource().createUncachedResolver(random);
        require(level.getBiome(bed).is(Biomes.SAVANNA),"Actual vanilla Savanna registry identity");
        require(level.getBlockState(bed.above()).is(Blocks.ACACIA_LOG),"Natural usable acacia trunk");
        var cut=bed.above(2);if(reload)require(level.getBlockState(cut).isAir(),"Saved Savanna trunk cut survives reopen");
        var root=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();level.setBlock(cut,root.state(cut.getX(),cut.getY(),cut.getZ()),2);
        int logs=0,leaves=0,skippedCanopyQueries=0;var covers=new HashSet<Integer>();
        for(int cz=(bed.getZ()>>4)-1;cz<=(bed.getZ()>>4)+1;cz++)for(int cx=(bed.getX()>>4)-1;cx<=(bed.getX()>>4)+1;cx++){
            var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());var decoded=TerrainProfile.decode(tag,level.palettedContainerFactory());
            require(Arrays.equals(base.canopy().encode(),decoded.canopy().encode()),"Eight-bit canopy snapshot round trip");
            for(int packed:base.canopy().encode()){
                int key=packed>>>CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var pos=new BlockPos(x,y,z);var state=base.state(x,y,z);
                require(chunk.getBlockState(pos)==state&&decoded.state(x,y,z)==state&&decoded.generate(y>>4).getBlockState(x&15,y&15,z&15)==state,"Native/saved/materialized canopy equality");
                if(state.is(Blocks.ACACIA_LEAVES)){
                    leaves++;int distance=state.getValue(LeavesBlock.DISTANCE);require(distance<=6&&!state.getValue(LeavesBlock.PERSISTENT)&&!state.isRandomlyTicking(),"Natural supported leaves");boolean support=false;
                    for(var dir:Direction.values()){var near=level.getBlockState(pos.relative(dir));if((near.is(Blocks.ACACIA_LOG)||near.is(Blocks.ACACIA_WOOD))&&distance==1||near.is(Blocks.ACACIA_LEAVES)&&near.getValue(LeavesBlock.DISTANCE)<distance)support=true;}
                    require(support,"Native acacia leaf support across chunk seams");
                }else{require(state.is(Blocks.ACACIA_LOG)||state.is(Blocks.ACACIA_WOOD),"Acacia material identity");logs++;}
            }
            for(int z=cz*16;z<cz*16+16;z+=4)for(int x=cx*16;x<cx*16+16;x+=4){
                int h=g.drainage(random).sample(x,z).height();covers.add(g.warmCover(x,z,random));
                for(int y=h-3;y<=Math.max(h+2,base.canopy().topY(x,z));y++){
                    // Saved/native/decoded/materialized canopy equality is checked
                    // above. Only its equality to newly generated trees is omitted.
                    if(legacyCanopy&&(base.canopy().state(x,y,z)!=null||g.canopy(x,z,random).state(x,y,z)!=null)){skippedCanopyQueries++;continue;}
                    require(base.state(x,y,z)==g.surfaceState(x,y,z,random),"Native Savanna floor and canopy query consistency");
                }
                for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x&15,z&15)+1==base.top(type.isOpaque(),x,z,ColumnLimits.MAX_Y-1),"Native Savanna heightmap");
                for(int y:new int[]{-3000,0,h>>2,2000})require(chunk.getNoiseBiome(x>>2,y,z>>2).equals(source.getNoiseBiome(x>>2,y,z>>2)),"Stored and source biome identity at all altitudes");
            }
            var original=tag.getByteArray("warm").orElseThrow().clone();tag.getByteArray("warm").orElseThrow()[0]=127;boolean rejected=false;
            try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}
            require(rejected&&Arrays.equals(original,base.encode(level.palettedContainerFactory()).getByteArray("warm").orElseThrow()),"Malformed snapshots rejected without profile mutation");
        }
        require(logs>5&&leaves>50,"Nontrivial native acacia canopy");
        if(legacyCanopy){require(skippedCanopyQueries>0,"Legacy fixture must exercise saved canopy queries");Blockbranch.LOGGER.info("SAVANNA_LEGACY_CANOPY_QUERIES_OMITTED count={}",skippedCanopyQueries);}
        boolean[] witness=new boolean[13];int remaining=4;
        for(int dz=-128;dz<=128&&remaining>0;dz++)for(int dx=-128;dx<=128&&remaining>0;dx++){
            int x=bed.getX()+dx,z=bed.getZ()+dz,code=g.warmCover(x,z,random);if(code<9||witness[code])continue;int h=g.drainage(random).sample(x,z).height();var base=((SparseColumn)level.getChunk(x>>4,z>>4)).blockbranch$terrain().base();if(base.canopy().state(x,h+1,z)!=null)continue;
            for(int y=h-3;y<=h+2;y++)require(level.getBlockState(new BlockPos(x,y,z))==g.surfaceState(x,y,z,random),"Every Savanna ground material reaches native terrain");
            if(code==10||code==11){var plant=new BlockPos(x,h+1,z);require(level.getBlockState(plant).canSurvive(level,plant),"Grass has supported dry soil");}
            witness[code]=true;remaining--;
        }
        require(remaining==0,"All four Savanna ground choices verified");
        var drops=Block.getDrops(Blocks.ACACIA_LOG.defaultBlockState(),level,bed.above(),null,null,new ItemStack(Items.IRON_AXE));require(drops.size()==1&&drops.getFirst().is(Items.ACACIA_LOG),"Vanilla acacia timber loot");
        drops=Block.getDrops(Blocks.ACACIA_LEAVES.defaultBlockState(),level,bed.above(),null,null,new ItemStack(Items.SHEARS));require(drops.size()==1&&drops.getFirst().is(Items.ACACIA_LEAVES),"Vanilla acacia shearing loot");
        var old=root.encode(level.palettedContainerFactory());old.putInt("version",17);boolean rejected=false;try{TerrainProfile.decode(old,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Old five-bit profile is explicitly rejected");
        var saved=bed.below(8);if(reload)require(level.getBlockState(saved).is(Blocks.ACACIA_LOG),"Saved Savanna subsurface edit survives reopen");level.setBlock(saved,Blocks.ACACIA_LOG.defaultBlockState(),2);level.setBlock(cut,Blocks.AIR.defaultBlockState(),2);
        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();level.getChunkAt(bed).markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(cut).isAir()&&level.getBlockState(saved).is(Blocks.ACACIA_LOG),"Savanna edits survive section eviction");
        Blockbranch.LOGGER.info("SAVANNA_NATIVE_PASS {} bed={} logs={} leaves={} covers={}",reload?"RELOAD":"NEW",bed,logs,leaves,covers);
    }
    /** Append-only palette and real profile/materialization checks independent of habitat identity. */
    public static void verifyWoodPalette(ServerLevel level,BlockPos bed){
        require(CanopyLayer.STATE_BITS==8,"Existing eight-bit packing retained");
        int[] packed=new int[CanopyLayer.LAST_CODE];
        for(int code=1;code<=packed.length;code++)packed[code-1]=((((100-TerrainShape.FLOOR)<<8)|code)<<8)|code;
        var palette=new CanopyLayer(packed);require(Arrays.equals(packed,palette.encode()),"Legacy and appended packed codes round trip without repacking");
        for(int code=1;code<=168;code++)require(palette.state(code&15,100,code>>4)!=null,"Every existing palette entry remains readable");
        var axes=new Direction.Axis[]{Direction.Axis.Y,Direction.Axis.X,Direction.Axis.Z};
        for(int i=0;i<3;i++)require(palette.state((28+i)&15,100,(28+i)>>4)==Blocks.ACACIA_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]),"Existing acacia axis mappings unchanged");
        require(palette.state(169&15,100,169>>4)==Blocks.ACACIA_WOOD.defaultBlockState(),"Appended native all-bark acacia wood");
        boolean rejected=false;try{new CanopyLayer(new int[]{(((100-TerrainShape.FLOOR)<<8)<<8)|170});}catch(IllegalArgumentException expected){rejected=true;}
        require(rejected,"Unknown palette code rejected");
        var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());
        int y=bed.getY()+30,key=((y-TerrainShape.FLOOR)<<8)|((bed.getZ()&15)*16+(bed.getX()&15));
        tag.putIntArray("canopy",new int[]{(key<<8)|CanopyLayer.ACACIA_WOOD_CODE});
        var decoded=TerrainProfile.decode(tag,level.palettedContainerFactory());
        require(decoded.state(bed.getX(),y,bed.getZ())==Blocks.ACACIA_WOOD.defaultBlockState()&&decoded.generate(y>>4).getBlockState(bed.getX()&15,y&15,bed.getZ()&15).is(Blocks.ACACIA_WOOD),"Native profile decode and section materialization retain appended wood");
        require(Arrays.equals(decoded.canopy().encode(),new int[]{(key<<8)|CanopyLayer.ACACIA_WOOD_CODE}),"Native profile preserves appended packed bytes");
        require(Blocks.ACACIA_WOOD.defaultBlockState().is(net.minecraft.tags.BlockTags.LOGS),"Native wood supports leaves");
        var drops=Block.getDrops(Blocks.ACACIA_WOOD.defaultBlockState(),level,bed.above(),null,null,new ItemStack(Items.IRON_AXE));
        require(drops.size()==1&&drops.getFirst().is(Items.ACACIA_WOOD),"All-bark scaffold intentionally drops native acacia wood");
        Blockbranch.LOGGER.info("SAVANNA_ACACIA_WOOD_PALETTE_PROFILE_MATERIALIZATION_LOOT_PASS");
    }
    /** Opt-in destructive fixture check: run only on a copied legacy world, with an externally verified absent neighbor. */
    public static void verifyLegacyBoundary(ServerLevel level){
        var argument=System.getProperty("blockbranch.savannaLegacyBoundary");if(argument==null)return;
        var parts=argument.split(",",-1);require(parts.length==4,"Legacy boundary requires oldCx,oldCz,newCx,newCz");
        int oldX=Integer.parseInt(parts[0]),oldZ=Integer.parseInt(parts[1]),newX=Integer.parseInt(parts[2]),newZ=Integer.parseInt(parts[3]);
        require(Math.abs((long)oldX-newX)+Math.abs((long)oldZ-newZ)==1,"Legacy boundary chunks must share a face");
        var old=level.getChunk(oldX,oldZ);var sparse=(SparseColumn)old;var terrain=sparse.blockbranch$terrain();
        require(terrain!=null,"Legacy boundary requires a saved procedural profile");
        var canopy=terrain.base().canopy().encode();
        require(canopy.length>0,"Legacy boundary must exercise a nonempty saved canopy");
        for(int value:canopy)require((value&CanopyLayer.STATE_MASK)<=168,"Legacy boundary old canopy must predate appended wood");
        // Preserve existing fixture edits. Add a witness only when there are none.
        if(terrain.modified().length==0){level.setBlock(new BlockPos(oldX*16+2,TerrainShape.FLOOR+4,oldZ*16+2),Blocks.DIAMOND_BLOCK.defaultBlockState(),2);old.markUnsaved();}
        var sections=terrain.modified();require(sections.length<=8,"Legacy boundary edit snapshot is bounded to eight sections");
        var live=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int sy:sections)for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            var pos=new BlockPos(oldX*16+x,sy*16+y,oldZ*16+z);live.put(pos,old.getBlockState(pos));
        }
        for(int packed:canopy){int key=packed>>>CanopyLayer.STATE_BITS;var pos=new BlockPos(oldX*16+(key&15),(key>>>8)+TerrainShape.FLOOR,oldZ*16+((key>>>4)&15));live.put(pos,old.getBlockState(pos));}
        terrain.flush();terrain.evictOutside(ColumnLimits.MAX_Y>>4,ColumnLimits.MAX_Y>>4);
        var neighbor=level.getChunk(newX,newZ);var next=((SparseColumn)neighbor).blockbranch$terrain();
        require(next!=null,"New boundary neighbor has a procedural profile");
        int woods=0;
        for(int packed:next.base().canopy().encode())if((packed&CanopyLayer.STATE_MASK)==CanopyLayer.ACACIA_WOOD_CODE){
            int key=packed>>>CanopyLayer.STATE_BITS,x=key&15,z=(key>>>4)&15,y=(key>>>8)+TerrainShape.FLOOR;
            require(next.base().state(newX*16+x,y,newZ*16+z).is(Blocks.ACACIA_WOOD)&&next.base().generate(y>>4).getBlockState(x,y&15,z).is(Blocks.ACACIA_WOOD)&&neighbor.getBlockState(new BlockPos(newX*16+x,y,newZ*16+z)).is(Blocks.ACACIA_WOOD),"New boundary wood retains native profile/materialized/live identity");woods++;
        }
        require(Arrays.equals(canopy,terrain.base().canopy().encode()),"Generating neighbor preserves exact legacy canopy bytes");
        for(var entry:live.entrySet())require(old.getBlockState(entry.getKey())==entry.getValue(),"Generating neighbor preserves legacy canopy and edited section states at "+entry.getKey());
        Blockbranch.LOGGER.info("SAVANNA_LEGACY_BOUNDARY_PASS old={},{} new={},{} canopy={} editedSections={} liveStates={} newWood={}",oldX,oldZ,newX,newZ,canopy.length,sections.length,live.size(),woods);
    }
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private SavannaSmoke(){}
}
