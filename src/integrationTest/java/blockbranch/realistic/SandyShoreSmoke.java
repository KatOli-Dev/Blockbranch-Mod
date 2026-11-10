package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Ocean shore grading, root binding, tool drops, boundaries and saved procedural/edited data. */
public final class SandyShoreSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random) {
        var drainage=generator.drainage(random);
        var point=blockbranch.realistic.LowGradientSandyShoreFixtures.fixture(drainage,random.seed());
        // The legacy anchor may now be intentionally bare. Follow actual retained
        // pioneers for the root-physics fixture instead of forcing old vegetation.
        for(int radius=0;radius<=192;radius+=4)
            for(int dz=-radius;dz<=radius;dz+=4)for(int dx=-radius;dx<=radius;dx+=4){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                int x=point.x()+dx,z=point.z()+dz;
                if(generator.sandyShoreCover(x,z,random)==2)
                    return new BlockPos(x,drainage.sample(x,z).height(),z);
            }
        throw new AssertionError("No retained rooted backshore near the natural shore fixture: "+point);
    }
    private static List<BlockPos> cases(RealisticGenerator generator,RandomState random,BlockPos center) {
        BlockPos wet=null,dry=null;
        for(int z=center.getZ()-192;z<=center.getZ()+192;z+=4) for(int x=center.getX()-192;x<=center.getX()+192;x+=4) {
            int cover=generator.sandyShoreCover(x,z,random);
            if(cover!=1) continue;
            var s=LowGradientSandyShore.site(generator.drainage(random),x,z);
            var point=new BlockPos(x,s.height(),z);
            if(s.oceanWater() && wet==null) wet=point;
            if(s.dry() && dry==null) dry=point;
            if(wet!=null && dry!=null) return List.of(wet,dry,center);
        }
        throw new AssertionError("Wet beach, bare dry beach and rooted backshore witnesses required");
    }
    public static BlockPos testRoot(BlockPos bed) { return bed.offset((bed.getX()&15)<8 ? 4 : -4,6,0); }
    public static BlockPos controlStart(BlockPos bed) { return testRoot(bed).offset((bed.getX()&15)<8 ? 1 : -1,0,0); }
    public static BlockPos landing(ServerLevel level,BlockPos start) {
        var base=((SparseColumn)level.getChunkAt(start)).blockbranch$terrain().base();
        return new BlockPos(start.getX(),base.top(Heightmap.Types.OCEAN_FLOOR.isOpaque(),start.getX(),start.getZ(),ColumnLimits.MAX_Y-1),start.getZ());
    }
    public static void restorePhysics(ServerLevel level,BlockPos bed) {
        for(var column:List.of(testRoot(bed),controlStart(bed))) {
            var base=((SparseColumn)level.getChunkAt(column)).blockbranch$terrain().base();
            int bottom=landing(level,column).getY()-3;
            for(int y=bottom;y<=testRoot(bed).getY()+3;y++) level.setBlock(new BlockPos(column.getX(),y,column.getZ()),base.state(column.getX(),y,column.getZ()),3);
        }
    }
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload) {
        var random=level.getChunkSource().randomState();var resolver=generator.getBiomeSource().createUncachedResolver(random);
        require(level.getBiome(bed).is(net.minecraft.world.level.biome.Biomes.BEACH),"Actual shore biome identity");
        require(level.getBlockState(bed).is(SandyShoreContent.ROOT_BOUND_SAND) && level.getBlockState(bed.above()).is(SandyShoreContent.BEACH_GRASS),"Natural rooted backshore");
        for(var witness:cases(generator,random,bed)) {
            var chunk=level.getChunkAt(witness);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            int x=witness.getX(),z=witness.getZ(),h=witness.getY(),cover=generator.sandyShoreCover(x,z,random);
            var sample=generator.drainage(random).sample(x,z);
            for(int y=h-6;y<=Math.max(h+2,sample.waterLevel());y++) {
                var expected=RealisticGenerator.material(y,h,sample.waterLevel(),0,0,0,cover);
                require(chunk.getBlockState(new BlockPos(x,y,z))==expected && decoded.state(x,y,z)==expected,"Generated/decoded shore layers, grass and sea water");
                require(decoded.generate(y>>4).getBlockState(x&15,y&15,z&15)==expected,"Materialized/client shore definition");
                if(y>h && y<sample.waterLevel()) require(expected.is(Blocks.WATER) && expected.getFluidState().isSource(),"Ocean source water preserved");
            }
            require(base.top(s->s.is(Blocks.STONE),x,z,h)==h-4,"Stone starts below the five-metre deposit");
            require(base.top(s->s.is(Blocks.SANDSTONE),x,z,h)==h-2,"Underlying sandstone layer");
            if(cover==2) {
                require(level.getBlockState(witness.above()).canSurvive(level,witness.above()),"Beach grass supported on dry rooted sand");
                require(RootBoundSandBlock.bound(level,witness) && RootBoundSandBlock.bound(level,witness.below()),"Both natural root layers bound");
            }
            for(var bounds:new LevelHeightAccessor[]{level,LevelHeightAccessor.create(h-5,2),LevelHeightAccessor.create(h-3,2),LevelHeightAccessor.create(h-1,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(h+4,2)}) {
                var column=generator.getBaseColumn(x,z,bounds,random);
                for(var type:Heightmap.Types.values()) {
                    int expected=bounds.getMinY();
                    for(int y=Math.min(bounds.getMaxY(),Math.max(h+2,sample.waterLevel()==DrainageNetwork.DRY ? h : sample.waterLevel()));y>=bounds.getMinY();y--)
                        if(type.isOpaque().test(column.getBlock(y))) { expected=y+1;break; }
                    require(generator.getBaseHeight(x,z,type,bounds,random)==expected,"Clipped shore height/fluid/vegetation query "+type);
                    if(bounds==level) require(chunk.getHeight(type,x&15,z&15)+1==expected,"Actual shore heightmap "+type);
                }
            }
            for(int qy:new int[]{-3000,0,h>>2,2000}) require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2))
                && decoded.biomes().get((x&15)>>2,qy&3,(z&15)>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2)),"Shore biome source/storage round trip");
        }
        boundary(level,generator,bed);
        var grass=SandyShoreContent.BEACH_GRASS.defaultBlockState();
        require(grass.canBeReplaced(),"Pioneer grass can be displaced by sediment or a placed block");
        var drops=Block.getDrops(grass,level,bed.above(),null,null,new ItemStack(Items.SHEARS));
        require(drops.size()==1 && drops.getFirst().is(SandyShoreContent.GRASS_ITEM),"Shears recover a planting tuft");
        require(Block.getDrops(grass,level,bed.above(),null,null,ItemStack.EMPTY).isEmpty(),"Ordinary uprooting does not duplicate plants");
        var shovel=new ItemStack(Items.IRON_SHOVEL);var rooted=SandyShoreContent.ROOT_BOUND_SAND.defaultBlockState();
        drops=Block.getDrops(rooted,level,bed,null,null,shovel);
        require(drops.size()==1 && drops.getFirst().is(Items.SAND),"Root-bound sand harvest returns sand");
        shovel.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        drops=Block.getDrops(rooted,level,bed,null,null,shovel);
        require(drops.size()==1 && drops.getFirst().is(SandyShoreContent.ROOT_SAND_ITEM),"Silk touch retains rooted material");
        var planted=bed.above(10);
        for(int dy=1;dy<=3;dy++) level.setBlock(planted.below(dy),Blocks.SAND.defaultBlockState(),2);
        level.setBlock(planted.below(4),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(planted,grass,3);
        require(level.getBlockState(planted.below()).is(SandyShoreContent.ROOT_BOUND_SAND) && level.getBlockState(planted.below(2)).is(SandyShoreContent.ROOT_BOUND_SAND),"Planting binds two metres of sand");
        require(level.getBlockState(planted.below(3)).is(Blocks.SAND),"Roots do not bind unbounded depth");
        level.destroyBlock(planted,false);
        for(int dy=1;dy<=2;dy++) level.getBlockState(planted.below(dy)).tick(level,planted.below(dy),level.getRandom());
        require(level.getBlockState(planted.below()).is(Blocks.SAND) && level.getBlockState(planted.below(2)).is(Blocks.SAND),"Uprooting releases both root layers");
        var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
        for(int y=planted.getY()-4;y<=planted.getY();y++) level.setBlock(new BlockPos(bed.getX(),y,bed.getZ()),base.state(bed.getX(),y,bed.getZ()),2);
        var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();
        var invalid=base.encode(level.palettedContainerFactory());invalid.putInt("version",4);
        boolean rejected=false;try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Pre-shore profile rejected");
        invalid=base.encode(level.palettedContainerFactory());invalid.putByteArray("sandy_shore",new byte[255]);rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Malformed shoreline cover rejected");
        var saved=bed.below(8);
        if(reload) require(level.getBlockState(saved).is(SandyShoreContent.ROOT_BOUND_SAND) && level.getBlockState(saved.above()).is(SandyShoreContent.BEACH_GRASS),"Saved rooted sand and planting tuple survives reopen");
        level.setBlock(saved.below(),rooted,2);level.setBlock(saved,rooted,2);level.setBlock(saved.above(),grass,3);
        chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
        require(level.getBlockState(saved).is(SandyShoreContent.ROOT_BOUND_SAND) && level.getBlockState(saved.above()).is(SandyShoreContent.BEACH_GRASS),"Edited root/plant section survives eviction");
        require(level.getBlockState(bed).is(SandyShoreContent.ROOT_BOUND_SAND),"Natural root cover survives overlay reload");
        Blockbranch.LOGGER.info("LOW_GRADIENT_SANDY_SHORE_PASS {} bed={} witnesses=wet,dry,rooted retainedSections={}",reload ? "RELOAD" : "NEW",bed,((SparseColumn)chunk).blockbranch$sections().size());
    }
    private static void boundary(ServerLevel level,RealisticGenerator generator,BlockPos bed) {
        var random=level.getChunkSource().randomState();var source=generator.getBiomeSource().createUncachedResolver(random);
        for(int dz=-256;dz<=256;dz+=16) for(int dx=-256;dx<=256;dx+=16) {
            int x=Math.floorDiv(bed.getX()+dx,16)*16,z=Math.floorDiv(bed.getZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++) for(int qx=0;qx<4;qx++) if(LowGradientSandyShore.contains(x+qx*4,z+qz*4,(wx,wz)->LowGradientSandyShore.site(generator.drainage(random),wx,wz))) count++;
            if(count==0 || count==16) continue;
            for(int oz=0;oz<16;oz++) for(int ox=0;ox<20;ox++) {
                int wx=x+ox,wz=z+oz;var sample=generator.drainage(random).sample(wx,wz);var chunk=level.getChunk(wx>>4,wz>>4);
                int shore=generator.sandyShoreCover(wx,wz,random);
                for(int y=sample.height()-5;y<=sample.height()+2;y++) require(chunk.getBlockState(new BlockPos(wx,y,wz))
                    ==generator.surfaceState(wx,y,wz,random),"Shore/native chunk edge agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Shore biome edge identity");
            }
            Blockbranch.LOGGER.info("LOW_GRADIENT_SANDY_SHORE_BOUNDARY_PASS x={} z={} shoreQuartCells={}",x,z,count);return;
        }
        throw new AssertionError("No mixed shoreline biome boundary");
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private SandyShoreSmoke() {}
}
