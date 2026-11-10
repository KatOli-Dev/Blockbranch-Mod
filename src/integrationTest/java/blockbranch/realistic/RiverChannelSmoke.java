package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Natural routed-river profiles, unchanged source fluids, native macrophyte behavior and independent saved snapshots. */
public final class RiverChannelSmoke {
    public static BlockPos fixture(RealisticGenerator g,RandomState random){if(random.seed()==424242)return new BlockPos(1511135,g.drainage(random).sample(1511135,-1799382).height(),-1799382);var p=blockbranch.realistic.RiverChannelFixtures.fixture(g.drainage(random),random.seed());return new BlockPos(p.x(),g.drainage(random).sample(p.x(),p.z()).height(),p.z());}
    public static void restore(ServerLevel level,BlockPos bed){var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int dy=0;dy<=2;dy++){var p=bed.above(dy);level.setBlock(p,base.state(p.getX(),p.getY(),p.getZ()),3);}}
    public static void verify(ServerLevel level,RealisticGenerator g,BlockPos bed,boolean reload){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var source=g.getBiomeSource().createUncachedResolver(random);var site=RiverChannel.site(d,bed.getX(),bed.getZ());
        require(level.getBiome(bed).is(Blockbranch.id("open_routed_river_channel"))&&RiverChannel.eligible(site),"Confirmed natural open inland river");
        require(level.getBlockState(bed.above()).is(Blocks.SEAGRASS),"Natural shallow submerged vegetation");
        require(level.getBrightness(LightLayer.SKY,bed.above())>=8,"Actual native sky light reaches the shallow macrophyte");
        int[] codes=new int[15];
        for(int cz=(bed.getZ()>>4)-1;cz<=(bed.getZ()>>4)+1;cz++)for(int cx=(bed.getX()>>4)-1;cx<=(bed.getX()>>4)+1;cx++){
            var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
            for(int oz=0;oz<16;oz+=4)for(int ox=0;ox<16;ox+=4){
                int x=cx*16+ox,z=cz*16+oz;var s=d.sample(x,z);int h=s.height(),water=s.waterLevel()==DrainageNetwork.DRY?h+1:s.waterLevel(),cover=g.riverMarginCover(x,z,random);codes[cover]++;
                int top=Math.max(Math.max(h+6,water+1),base.canopy().topY(x,z));
                for(int y=h-3;y<=top;y++){
                    var p=new BlockPos(x,y,z);var expected=g.surfaceState(x,y,z,random);require(chunk.getBlockState(p)==expected&&decoded.state(x,y,z)==expected,"Native/encoded river bed, plant and water agreement");
                    require(sections.computeIfAbsent(y>>4,decoded::generate).getBlockState(x&15,y&15,z&15)==expected,"Client section materialization matches river definition");
                    if(cover>=5&&y>h&&y<water)require(expected.getFluidState().is(net.minecraft.tags.FluidTags.WATER)&&expected.getFluidState().isSource(),"Every river-water cell remains source water, including plants");
                }
                for(var bounds:new LevelHeightAccessor[]{level,LevelHeightAccessor.create(h-2,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(water-1,2),LevelHeightAccessor.create(water+1,2)}){
                    var column=g.getBaseColumn(x,z,bounds,random);
                    for(var type:Heightmap.Types.values()){
                        int expected=bounds.getMinY();for(int y=Math.min(bounds.getMaxY(),top);y>=bounds.getMinY();y--)if(type.isOpaque().test(column.getBlock(y))){expected=y+1;break;}
                        require(g.getBaseHeight(x,z,type,bounds,random)==expected,"Clipped river bed/vegetation/fluid height query");
                        if(bounds==level)require(chunk.getHeight(type,x&15,z&15)+1==expected&&base.top(type.isOpaque(),x,z,ColumnLimits.MAX_Y-1)==expected,"Native river heightmaps distinguish bed and water");
                    }
                }
                if(cover>=5)require(!d.terminalLakeWater(x,z,s)&&!d.oceanWater(x,z,s)&&d.channelAt(x,z).edge()>=0,"River material never leaks into ocean or terminal lake");
                for(int qy:new int[]{-3000,0,h>>2,2000})require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(source.getNoiseBiome(x>>2,qy,z>>2))&&decoded.biomes().get((x&15)>>2,qy&3,(z&15)>>2).equals(source.getNoiseBiome(x>>2,qy,z>>2)),"All-altitude river biome palette survives encoding");
            }
        }
        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();var base=terrain.base();int index=(bed.getZ()&15)*16+(bed.getX()&15);
        for(int code=5;code<=12;code++){
            var tag=base.encode(level.palettedContainerFactory());var cover=tag.getByteArray("river_margin").orElseThrow();cover[index]=(byte)code;var decoded=TerrainProfile.decode(tag,level.palettedContainerFactory());
            for(int y=bed.getY()-2;y<site.water();y++){var expected=RealisticGenerator.material(y,bed.getY(),site.water(),0,0,code);require(decoded.state(bed.getX(),y,bed.getZ())==expected&&decoded.generate(y>>4).getBlockState(bed.getX()&15,y&15,bed.getZ()&15)==expected,"Every aquatic palette state survives native encoding/materialization, including conservative rare rock fallback");}
            require(decoded.top(s->s.getFluidState().is(net.minecraft.tags.FluidTags.WATER),bed.getX(),bed.getZ(),ColumnLimits.MAX_Y-1)==site.water(),"Every aquatic palette state preserves water-plane top");
        }
        var grass=Blocks.SEAGRASS.defaultBlockState();var pos=bed.above();
        var drops=Block.getDrops(grass,level,pos,null,null,new ItemStack(Items.SHEARS));require(drops.stream().anyMatch(s->s.is(Items.SEAGRASS)),"Sheared submerged plants retain existing usable item");
        require(Block.getDrops(grass,level,pos,null,null,ItemStack.EMPTY).isEmpty(),"Ordinary seagrass harvest adds no new item type");
        for(var soil:List.of(Blocks.MUD,Blocks.CLAY,Blocks.SAND)){level.setBlock(bed,soil.defaultBlockState(),3);level.setBlock(pos,grass,3);require(grass.canSurvive(level,pos),"Native aquatic grass supports each generated fine sediment");}
        restore(level,bed);
        var plant=(BonemealableBlock)Blocks.SEAGRASS;require(plant.isValidBonemealTarget(level,pos,grass,BonemealSource.INTERACTION),"Submerged shallow plant accepts native growth");
        plant.performBonemeal(level,level.getRandom(),pos,grass,BonemealSource.INTERACTION);require(level.getBlockState(pos).is(Blocks.TALL_SEAGRASS)&&level.getBlockState(pos.above()).is(Blocks.TALL_SEAGRASS),"Native two-block growth");
        require(level.getFluidState(pos).isSource()&&level.getFluidState(pos.above()).isSource(),"Both grown halves retain source water");
        level.destroyBlock(pos,false);require(level.getBlockState(pos).is(Blocks.WATER)&&level.getBlockState(pos.above()).is(Blocks.WATER),"Removing aquatic plant restores water in both cells");
        restore(level,bed);level.setBlock(bed,Blocks.WATER.defaultBlockState(),3);require(level.getBlockState(pos).is(Blocks.WATER),"Unsupported submerged plant yields source water");restore(level,bed);
        var original=base.encode(level.palettedContainerFactory());var originalCover=original.getByteArray("river_margin").orElseThrow().clone();var malformed=base.encode(level.palettedContainerFactory());malformed.getByteArray("river_margin").orElseThrow()[index]=13;boolean rejected=false;
        try{TerrainProfile.decode(malformed,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Unknown aquatic state rejected");require(Arrays.equals(originalCover,base.encode(level.palettedContainerFactory()).getByteArray("river_margin").orElseThrow()),"Malformed NBT snapshot cannot mutate source river profile");
        var saved=bed.below(8);if(reload)require(level.getBlockState(saved).is(Blocks.CLAY),"Saved river-bed edit survives reopen");level.setBlock(saved,Blocks.CLAY.defaultBlockState(),2);level.getChunkAt(bed).markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(saved).is(Blocks.CLAY),"Edited river layer survives section eviction");
        boundary(level,g);
        Blockbranch.LOGGER.info("OPEN_ROUTED_RIVER_CHANNEL_PASS {} bed={} water={} depth={} nearbyCodes={}",reload?"RELOAD":"NEW",bed,site.water(),site.depth(),Arrays.toString(codes));
    }
    private static void boundary(ServerLevel level,RealisticGenerator g){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var edge=blockbranch.realistic.RiverChannelFixtures.transition(d,random.seed());var source=g.getBiomeSource().createUncachedResolver(random);
        for(int dz=-32;dz<=32;dz+=16)for(int dx=-32;dx<=32;dx+=16){
            int x=Math.floorDiv(edge.insideX()+dx,16)*16,z=Math.floorDiv(edge.insideZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++)for(int qx=0;qx<4;qx++)if(source.getNoiseBiome((x>>2)+qx,0,(z>>2)+qz).is(Blockbranch.id("open_routed_river_channel")))count++;
            if(count==0||count==16)continue;
            for(int oz=0;oz<16;oz++)for(int ox=0;ox<20;ox++){
                int wx=x+ox,wz=z+oz;var t=d.sample(wx,wz);var chunk=level.getChunk(wx>>4,wz>>4);int top=Math.max(t.height()+6,t.waterLevel()==DrainageNetwork.DRY?t.height():t.waterLevel());
                for(int y=t.height()-3;y<=top;y++)require(chunk.getBlockState(new BlockPos(wx,y,wz))==g.surfaceState(wx,y,wz,random),"River/reed/dry native boundary preserves material and source-fluid agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"River boundary identity agrees with source");
            }
            Blockbranch.LOGGER.info("OPEN_ROUTED_RIVER_BOUNDARY_PASS x={} z={} riverQuartCells={}",x,z,count);return;
        }
        throw new AssertionError("No mixed river habitat boundary");
    }
    private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
    private RiverChannelSmoke(){}
}
