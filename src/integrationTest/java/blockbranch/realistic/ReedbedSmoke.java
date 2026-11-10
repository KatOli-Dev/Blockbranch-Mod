package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Registry, material, fluid, support, crafting and persistence checks for real lake shores. */
public final class ReedbedSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random) {
        var p=blockbranch.realistic.InlandLakeReedbedFixtures.fixture(generator.drainage(random),random.seed());
        return new BlockPos(p.x(),generator.drainage(random).sample(p.x(),p.z()).height(),p.z());
    }
    public static void restore(ServerLevel level,BlockPos bed) {
        var profile=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
        for(int y=bed.getY();y<=bed.getY()+6;y++) level.setBlock(new BlockPos(bed.getX(),y,bed.getZ()),profile.state(bed.getX(),y,bed.getZ()),3);
    }
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload) {
        for(var plant:java.util.List.of(ReedbedContent.LAKE_REED,MeadowContent.MEADOW_FESCUE)){
            var offsets=new java.util.HashSet<net.minecraft.world.phys.Vec3>();
            for(int z=-17;z<=17;z++)for(int x=-17;x<=17;x++){var p=new BlockPos(x,300,z);var a=plant.defaultBlockState().getOffset(p);require(a.equals(plant.defaultBlockState().getOffset(p.above(3))),"Stalk segments retain identical column-stable offsets");offsets.add(a);var state=plant.defaultBlockState();var bounds=state.getShape(level,p).bounds();require(Math.abs(bounds.minX-(.125+a.x))<1e-8&&Math.abs(bounds.minZ-(.125+a.z))<1e-8,"Selection shape follows rendered horizontal offset");require(state.getCollisionShape(level,p).isEmpty(),"Reed/fescue remains non-colliding");}
            require(offsets.size()>32,"Plants no longer share a block-centre render lattice");
        }
        var random=level.getChunkSource().randomState();
        var chunk=level.getChunkAt(bed); var sparse=(SparseColumn)chunk; var terrain=sparse.blockbranch$terrain();
        var base=terrain.base(); var restored=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
        require(level.getBlockState(bed).is(Blocks.MUD),"Natural mud bed");
        require(level.getBlockState(bed.above()).is(ReedbedContent.LAKE_REED),"Natural emergent reeds");
        require(level.getBiome(bed).is(Blockbranch.id("inland_lake_reedbed")),"Natural reedbed biome identity");
        var resolver=generator.getBiomeSource().createUncachedResolver(random);
        int plants=0,mud=0;
        for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
            int wx=chunk.getPos().getMinBlockX()+x,wz=chunk.getPos().getMinBlockZ()+z;
            var sample=generator.drainage(random).sample(wx,wz); int h=sample.height();
            int cover=generator.reedbedCover(wx,wz,random);
            int meadowCover=generator.meadowCover(wx,wz,random);
            if(cover>1) plants++; if(cover>0) mud++;
            for(int y=h-3;y<=Math.max(h+5,sample.waterLevel());y++) {
                var expected=generator.surfaceState(wx,y,wz,random);
                require(base.state(x,y,z)==expected && restored.state(x,y,z)==expected,"Serialized mud/reed/fluid state agreement");
                require(base.generate(y>>4).getBlockState(x,y&15,z)==expected,"Materialized/client reed agreement");
                if(expected.is(ReedbedContent.LAKE_REED)) {
                    require(expected.getValue(LakeReedBlock.WATERLOGGED)==(y<sample.waterLevel()),"Only submerged stalks hold water");
                    require(expected.canSurvive(level,new BlockPos(wx,y,wz)),"Every generated stalk has a viable root");
                }
            }
            for(var type:Heightmap.Types.values()) {
                int expected=h+1;
                for(int y=Math.max(Math.max(base.canopy().topY(x,z),h+Math.max(cover,meadowCover>1 || generator.woodlandCover(wx,wz,random)>=4 ? 1 : 0)),sample.waterLevel()==DrainageNetwork.DRY ? h : sample.waterLevel()-1);y>h;y--)
                    if(type.isOpaque().test(base.state(x,y,z))) { expected=y+1; break; }
                require(base.top(type.isOpaque(),x,z,ColumnLimits.MAX_Y-1)==expected,"Heightmap includes fluids or foliage correctly");
            }
            for(int bottom:new int[]{h-2,h+1,h+3,h+5}) {
                var bounds=net.minecraft.world.level.LevelHeightAccessor.create(bottom,2);
                var column=generator.getBaseColumn(wx,wz,bounds,random);
                for(var type:Heightmap.Types.values()) {
                    int expected=bottom;
                    for(int y=bounds.getMaxY();y>=bottom;y--) if(type.isOpaque().test(column.getBlock(y))) { expected=y+1; break; }
                    require(generator.getBaseHeight(wx,wz,type,bounds,random)==expected,"Clipped mud/water/stalk height query");
                }
            }
            for(int qy:new int[]{-3000,0,bed.getY()>>2,1000})
                require(chunk.getNoiseBiome(wx>>2,qy,wz>>2).equals(resolver.getNoiseBiome(wx>>2,qy,wz>>2))
                    && restored.biome(x>>2,qy,z>>2).equals(resolver.getNoiseBiome(wx>>2,qy,wz>>2)),"Source/stored/decoded biome agreement");
        }
        require(plants>0 && mud>0,"Generated shallow reed margin");
        var invalid=base.encode(level.palettedContainerFactory()); invalid.putInt("version",2);
        boolean rejected=false; try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Pre-reedbed profile rejected");
        invalid=base.encode(level.palettedContainerFactory()); invalid.putByteArray("reedbed",new byte[255]); rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Malformed reed cover rejected");
        boundary(level,generator,bed);
        var state=level.getBlockState(bed.above());
        var drops=Block.getDrops(state,level,bed.above(),null,null,ItemStack.EMPTY);
        require(drops.size()==1 && drops.getFirst().is(ReedbedContent.REED_FIBRE),"Ordinary harvest yields fibre");
        drops=Block.getDrops(state,level,bed.above(),null,null,new ItemStack(Items.SHEARS));
        require(drops.size()==1 && drops.getFirst().is(ReedbedContent.REED_ITEM),"Shears recover planting stalk");
        recipe(level,"paper_from_reeds",3,1,Items.PAPER,3);
        recipe(level,"woven_reed_mat",2,2,ReedbedContent.MAT_ITEM,2);
        int cover=generator.reedbedCover(bed.getX(),bed.getZ(),random);
        var tip=bed.above(cover);
        level.destroyBlock(tip,false);
        require(ReedbedContent.LAKE_REED.isValidBonemealTarget(level,bed.above(),state,BonemealSource.INTERACTION),"Cut stalk can regrow");
        ReedbedContent.LAKE_REED.performBonemeal(level,level.getRandom(),bed.above(),state,BonemealSource.INTERACTION);
        require(level.getBlockState(tip).is(ReedbedContent.LAKE_REED) && level.getBlockState(tip).getValue(LakeReedBlock.TIP),"Bonemeal restores the seed head");
        while(ReedbedContent.LAKE_REED.isValidBonemealTarget(level,bed.above(),state,BonemealSource.INTERACTION))
            ReedbedContent.LAKE_REED.performBonemeal(level,level.getRandom(),bed.above(),state,BonemealSource.INTERACTION);
        require(!level.getBlockState(bed.above(5)).is(ReedbedContent.LAKE_REED),"Growth is capped at four metres");
        require(!ReedbedContent.LAKE_REED.defaultBlockState().canSurvive(level,bed.above(5)),"Overheight placement rejected");
        restore(level,bed);
        level.destroyBlock(bed.above(),false);
        require(!level.getBlockState(bed.above(cover)).is(ReedbedContent.LAKE_REED),"Root harvest breaks unsupported upper stems");
        require(level.getFluidState(bed.above()).isSource(),"Root harvest preserves lake water");
        restore(level,bed);
        var witness=bed.below(6);
        if(reload) require(level.getBlockState(witness).is(ReedbedContent.WOVEN_REED_MAT),"Saved mat overlay survives reopen");
        level.setBlock(witness.below(),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(witness,ReedbedContent.WOVEN_REED_MAT.defaultBlockState(),2);
        chunk.markUnsaved(); terrain.flush(); terrain.evictOutside(-100,-90);
        require(level.getBlockState(witness).is(ReedbedContent.WOVEN_REED_MAT),"Reed mat survives overlay eviction");
        require(level.getBlockState(bed.above()).is(ReedbedContent.LAKE_REED) && level.getFluidState(bed.above()).isSource(),"Procedural reeds and water survive eviction");
        Blockbranch.LOGGER.info("INLAND_LAKE_REEDBED_PASS {} bed={} plants={} mud={} retainedSections={}",reload ? "RELOAD" : "NEW",bed,plants,mud,sparse.blockbranch$sections().size());
    }
    /** Exercise both shortened natural forms through the native world, including growth overlays. */
    public static void verifyShortForms(ServerLevel level,RealisticGenerator generator,BlockPos center) {
        var random=level.getChunkSource().randomState();
        BlockPos[] sites=new BlockPos[3];
        for(int dz=-192;dz<=192 && (sites[1]==null||sites[2]==null);dz++)
            for(int dx=-192;dx<=192 && (sites[1]==null||sites[2]==null);dx++) {
                int x=center.getX()+dx,z=center.getZ()+dz;
                var sample=generator.drainage(random).sample(x,z);
                int depth=sample.waterLevel()-sample.height()-1;
                if(depth>=1&&depth<=2&&sites[depth]==null&&generator.reedbedCover(x,z,random)==depth+1)
                    sites[depth]=new BlockPos(x,sample.height(),z);
            }
        for(int depth=1;depth<=2;depth++) {
            var bed=sites[depth]; require(bed!=null,"Short natural form present at water depth "+depth);
            restore(level,bed);
            var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();var base=terrain.base();
            var encoded=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(encoded,level.palettedContainerFactory());
            int height=depth+1;
            for(int dy=-2;dy<=5;dy++) {
                var at=bed.above(dy);var expected=generator.surfaceState(at.getX(),at.getY(),at.getZ(),random);
                require(level.getBlockState(at)==expected&&copy.state(at.getX(),at.getY(),at.getZ())==expected&&copy.generate(at.getY()>>4).getBlockState(at.getX()&15,at.getY()&15,at.getZ()&15)==expected,"Short native/decoded/materialized state depth="+depth);
                if(dy>=1&&dy<=height) {
                    require(expected.is(ReedbedContent.LAKE_REED)&&expected.getValue(LakeReedBlock.TIP)==(dy==height),"Only shortened top has seed head");
                    require(expected.getValue(LakeReedBlock.WATERLOGGED)==(dy<=depth)&&expected.canSurvive(level,at),"Short stems retain submerged fluid and support");
                    require(level.getFluidState(at).isSource()==(dy<=depth),"Short native fluid query");
                }
            }
            require(base.top(s->s.is(ReedbedContent.LAKE_REED),bed.getX(),bed.getZ(),bed.getY()+5)==bed.getY()+height+1,"Short profile predicate top");
            for(var type:Heightmap.Types.values())for(int bottom:new int[]{bed.getY()-2,bed.getY()+1,bed.getY()+height}) {
                var bounds=net.minecraft.world.level.LevelHeightAccessor.create(bottom,3);var column=generator.getBaseColumn(bed.getX(),bed.getZ(),bounds,random);
                int expected=bottom;for(int y=bounds.getMaxY();y>=bottom;y--)if(type.isOpaque().test(column.getBlock(y))){expected=y+1;break;}
                require(generator.getBaseHeight(bed.getX(),bed.getZ(),type,bounds,random)==expected,"Short clipped height query "+type);
            }
            var root=bed.above();var state=level.getBlockState(root);
            require(Block.getDrops(state,level,root,null,null,ItemStack.EMPTY).getFirst().is(ReedbedContent.REED_FIBRE)&&Block.getDrops(state,level,root,null,null,new ItemStack(Items.SHEARS)).getFirst().is(ReedbedContent.REED_ITEM),"Short stems retain harvest drops");
            level.destroyBlock(bed.above(height),false);
            require(ReedbedContent.LAKE_REED.isValidBonemealTarget(level,root,level.getBlockState(root),BonemealSource.INTERACTION),"Short cut head regrows");
            int steps=0;while(ReedbedContent.LAKE_REED.isValidBonemealTarget(level,root,level.getBlockState(root),BonemealSource.INTERACTION)) {
                require(++steps<=4,"Short growth terminates");ReedbedContent.LAKE_REED.performBonemeal(level,level.getRandom(),root,level.getBlockState(root),BonemealSource.INTERACTION);
            }
            require(level.getBlockState(bed.above(4)).is(ReedbedContent.LAKE_REED)&&!level.getBlockState(bed.above(5)).is(ReedbedContent.LAKE_REED),"Short bonemeal reaches maximum four");
            require(encoded.equals(base.encode(level.palettedContainerFactory())),"Harvest and growth cannot mutate natural profile");
            restore(level,bed);level.destroyBlock(root,false);
            require(level.getFluidState(root).isSource()&&!level.getBlockState(bed.above(height)).is(ReedbedContent.LAKE_REED),"Short root harvest cascades and retains lake water");
            crossSection(level,bed,depth);
            restore(level,bed);var edit=bed.above(height);level.setBlock(edit,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
            require(level.getBlockState(edit).is(Blocks.DIAMOND_BLOCK)&&encoded.equals(base.encode(level.palettedContainerFactory())),"Player replacement survives eviction without changing short base");
            restore(level,bed);
            Blockbranch.LOGGER.info("REED_SHORT_NATIVE_PASS depth={} height={} bed={} fluidTipMaterialHeightSurvivalHarvestGrowthEdit=true",depth,height,bed);
        }
    }
    private static void crossSection(ServerLevel level,BlockPos natural,int depth) {
        var bed=new BlockPos(natural.getX(),((natural.getY()+48)>>4)*16+14,natural.getZ());
        var previous=new net.minecraft.world.level.block.state.BlockState[7];
        for(int i=0;i<previous.length;i++)previous[i]=level.getBlockState(bed.above(i));
        try {
            level.setBlock(bed,Blocks.MUD.defaultBlockState(),2);
            for(int i=1;i<=6;i++)level.setBlock(bed.above(i),Blocks.AIR.defaultBlockState(),2);
            for(int i=1;i<=depth+1;i++)level.setBlock(bed.above(i),ReedbedContent.LAKE_REED.defaultBlockState().setValue(LakeReedBlock.WATERLOGGED,i<=depth).setValue(LakeReedBlock.TIP,i==depth+1),2);
            var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();terrain.flush();terrain.evictOutside(-100,-90);
            require((bed.above().getY()>>4)!=(bed.above(depth+1).getY()>>4),"Short stem crosses native section boundary");
            for(int i=1;i<=depth+1;i++) {
                var at=bed.above(i);var state=level.getBlockState(at);
                require(state.is(ReedbedContent.LAKE_REED)&&state.canSurvive(level,at)&&state.getValue(LakeReedBlock.TIP)==(i==depth+1)&&state.getValue(LakeReedBlock.WATERLOGGED)==(i<=depth),"Cross-section native support, head and fluid survive eviction");
            }
            int steps=0;while(ReedbedContent.LAKE_REED.isValidBonemealTarget(level,bed.above(),level.getBlockState(bed.above()),BonemealSource.INTERACTION)) {
                require(++steps<=4,"Cross-section growth bounded");ReedbedContent.LAKE_REED.performBonemeal(level,level.getRandom(),bed.above(),level.getBlockState(bed.above()),BonemealSource.INTERACTION);
            }
            require(level.getBlockState(bed.above(4)).is(ReedbedContent.LAKE_REED)&&level.getBlockState(bed.above(4)).getValue(LakeReedBlock.TIP)&&!level.getBlockState(bed.above(5)).is(ReedbedContent.LAKE_REED),"Cross-section growth capped with one head");
            level.destroyBlock(bed.above(),false);
            require(level.getFluidState(bed.above()).isSource()&&!level.getBlockState(bed.above(4)).is(ReedbedContent.LAKE_REED),"Cross-section root harvest cascades with water retained");
        } finally {for(int i=0;i<previous.length;i++)level.setBlock(bed.above(i),previous[i],2);}
        Blockbranch.LOGGER.info("REED_CROSS_SECTION_NATIVE_PASS depth={} bed={}",depth,bed);
    }
    private static void recipe(ServerLevel level,String name,int width,int height,Item output,int count) {
        var recipe=(CraftingRecipe)level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Blockbranch.id(name))).orElseThrow().value();
        var input=CraftingInput.of(width,height,java.util.stream.IntStream.range(0,width*height).mapToObj(i->new ItemStack(ReedbedContent.REED_FIBRE)).toList());
        require(recipe.matches(input,level) && recipe.assemble(input).is(output) && recipe.assemble(input).getCount()==count,"Fibre recipe "+name);
        require(!recipe.matches(CraftingInput.of(1,1,List.of(new ItemStack(Items.DIRT))),level),"Recipe rejects wrong materials");
    }
    private static void boundary(ServerLevel level,RealisticGenerator generator,BlockPos bed) {
        var random=level.getChunkSource().randomState(); var resolver=generator.getBiomeSource().createUncachedResolver(random);
        for(int dz=-96;dz<=96;dz+=16) for(int dx=-96;dx<=96;dx+=16) {
            int wx=Math.floorDiv(bed.getX()+dx,16)*16,wz=Math.floorDiv(bed.getZ()+dz,16)*16,count=0;
            for(int z=0;z<4;z++) for(int x=0;x<4;x++) if(resolver.getNoiseBiome((wx>>2)+x,0,(wz>>2)+z).is(Blockbranch.id("inland_lake_reedbed"))) count++;
            if(count==0 || count==16) continue;
            for(int x=0;x<20;x++) for(int z=0;z<16;z++) {
                var chunk=level.getChunk((wx+x)>>4,(wz+z)>>4);
                var sample=generator.drainage(random).sample(wx+x,wz+z); int cover=generator.reedbedCover(wx+x,wz+z,random);
                for(int y=sample.height();y<=sample.height()+5;y++)
                    require(chunk.getBlockState(new BlockPos(wx+x,y,wz+z))==generator.surfaceState(wx+x,y,wz+z,random),"Biome/chunk edge material agreement");
                require(chunk.getNoiseBiome((wx+x)>>2,0,(wz+z)>>2).equals(resolver.getNoiseBiome((wx+x)>>2,0,(wz+z)>>2)),"Biome edge identity");
            }
            Blockbranch.LOGGER.info("INLAND_LAKE_REEDBED_BOUNDARY_PASS x={} z={} reedbedQuartCells={}",wx,wz,count); return;
        }
        throw new AssertionError("No mixed reedbed boundary");
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private ReedbedSmoke() {}
}
