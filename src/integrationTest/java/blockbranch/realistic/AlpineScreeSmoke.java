package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/** Actual registries, columns, save/reload, recipes, loot and off-window tick deferral.
 * Active falling is exercised by the real-client fixture, which supplies a simulation window.
 */
public final class AlpineScreeSmoke {
    public static BlockPos fixture(RealisticGenerator generator, RandomState random) {
        var drainage=generator.drainage(random);
        for(int z=-1700000;z<=1700000;z+=8000) for(int x=-1700000;x<=1700000;x+=8000)
            if(generator.screeDepth(x,z,random)>0&&!SnowySlopes.contains(x,z,drainage,new ClimateField(random.seed()))&&MountainPeaks.kind(random.seed(),x,z,drainage,new ClimateField(random.seed()))==0&&MountainPeaks.surfaceKind(random.seed(),x,z,drainage,new ClimateField(random.seed()))==0) return new BlockPos(x,drainage.sample(x,z).height(),z);
        throw new AssertionError("No generated alpine scree fixture");
    }

    public static void verify(ServerLevel level, RealisticGenerator generator, BlockPos pos, boolean reload) {
        var random=level.getChunkSource().randomState();
        var chunk=level.getChunkAt(pos); var sparse=(SparseColumn)chunk;
        var profile=sparse.blockbranch$terrain().base();
        require(level.getBlockState(pos).is(ScreeContent.LOOSE_SCREE),"Natural scree cover");
        require(level.getBiome(pos).is(Blockbranch.id("alpine_scree_slope")),"Actual biome identity");
        var resolver=generator.getBiomeSource().createUncachedResolver(random);
        for(int z=0;z<4;z++) for(int x=0;x<4;x++) {
            int qx=(chunk.getPos().getMinBlockX()>>2)+x,qz=(chunk.getPos().getMinBlockZ()>>2)+z;
            var expected=resolver.getNoiseBiome(qx,0,qz);
            for(int y:new int[]{-3000,0,pos.getY()>>2,2000})
                require(chunk.getNoiseBiome(qx,y,qz).equals(expected),"Source and stored biomes at every altitude");
        }
        var restored=TerrainProfile.decode(profile.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
        var invalid=profile.encode(level.palettedContainerFactory()); invalid.putInt("version",1);
        boolean rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Older procedural profile rejected");
        invalid=profile.encode(level.palettedContainerFactory()); invalid.putByteArray("scree",new byte[255]); rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Malformed debris array rejected");
        verifyBoundary(level,generator,pos);
        int debris=0;
        for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
            int wx=chunk.getPos().getMinBlockX()+x,wz=chunk.getPos().getMinBlockZ()+z;
            int h=generator.drainage(random).sample(wx,wz).height();
            int depth=generator.screeDepth(wx,wz,random); debris+=depth;
            for(int y=h-4;y<=h+1;y++) {
                require(profile.state(x,y,z)==restored.state(x,y,z),"Serialized surface definition");
                var generated=restored.generate(y>>4);
                require(generated.getBlockState(x,y&15,z)==profile.state(x,y,z),"Materialized/client surface agreement");
            }
            require(profile.top(s->s.is(ScreeContent.LOOSE_SCREE),x,z,h)==(depth>0 ? h+1 : ColumnLimits.MIN_Y),"Scree-only top query");
            require(profile.top(s->s.is(Blocks.STONE),x,z,h)==h-depth+1,"Bedrock exposure beneath scree");
            require(chunk.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z)==h,"Surface height remains unchanged at "+wx+","+wz+": expected="+h+" actual="+chunk.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z)+" depth="+depth+" base="+profile.top(Heightmap.Types.OCEAN_FLOOR.isOpaque(),x,z,ColumnLimits.MAX_Y-1));
        }
        require(debris>0,"Nonempty debris apron");
        var tool=new ItemStack(Items.IRON_SHOVEL);
        var drops=Block.getDrops(ScreeContent.LOOSE_SCREE.defaultBlockState(),level,pos,null,null,tool);
        require(drops.size()==1 && drops.getFirst().is(ScreeContent.ANGULAR_ROCK_FRAGMENT) && drops.getFirst().getCount()==4,"Four angular fragments harvested");
        tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        drops=Block.getDrops(ScreeContent.LOOSE_SCREE.defaultBlockState(),level,pos,null,null,tool);
        require(drops.size()==1 && drops.getFirst().is(ScreeContent.SCREE_ITEM),"Silk touch retains loose scree");
        require(new ItemStack(Items.IRON_SHOVEL).getDestroySpeed(ScreeContent.LOOSE_SCREE.defaultBlockState())
            > new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(ScreeContent.LOOSE_SCREE.defaultBlockState()),"Shovel is effective");
        require(ScreeContent.LOOSE_SCREE.getSpeedFactor()==0.65F,"Loose-footing movement factor");
        recipe(level,"loose_scree",ScreeContent.ANGULAR_ROCK_FRAGMENT,4,ScreeContent.SCREE_ITEM);
        recipe(level,"gravel_from_scree",ScreeContent.SCREE_ITEM,1,Items.GRAVEL);
        var witness=pos.below(5);
        if(reload) require(level.getBlockState(witness).is(Blocks.GOLD_BLOCK),"Scree fixture edited section survives reload");
        level.setBlock(witness,Blocks.GOLD_BLOCK.defaultBlockState(),2);
        sparse.blockbranch$terrain().flush(); sparse.blockbranch$terrain().evictOutside(-5,20);
        require(level.getBlockState(pos).is(ScreeContent.LOOSE_SCREE),"Surface survives edit eviction");
        require(level.getBlockState(witness).is(Blocks.GOLD_BLOCK),"Surface overlay restores from section store");
        Blockbranch.LOGGER.info("ALPINE_SCREE_CONTENT_PASS {} position={} debrisBlocks={}",reload ? "RELOAD" : "NEW",pos,debris);
    }

    private static void verifyBoundary(ServerLevel level,RealisticGenerator generator,BlockPos center) {
        var random=level.getChunkSource().randomState();
        var resolver=generator.getBiomeSource().createUncachedResolver(random);
        for(int offset=-4096;offset<=4096;offset+=16) {
            int wx=center.getX()+offset,wz=center.getZ(),scree=0;
            for(int z=0;z<4;z++) for(int x=0;x<4;x++)
                if(resolver.getNoiseBiome((wx>>2)+x,0,(wz>>2)+z).is(Blockbranch.id("alpine_scree_slope"))) scree++;
            if(scree==0 || scree==16) continue;
            var chunk=level.getChunk(wx>>4,wz>>4);
            var profile=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(profile.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            for(int x=0;x<20;x++) { // Cross the natural habitat's native chunk edge.
                var neighbor=level.getChunk((wx+x)>>4,wz>>4);
                var sample=generator.drainage(random).sample(wx+x,wz);
                int depth=generator.screeDepth(wx+x,wz,random);
                require(neighbor.getBlockState(new BlockPos(wx+x,sample.height(),wz))
                    ==RealisticGenerator.material(sample.height(),sample.height(),sample.waterLevel(),depth),"Boundary material continuity");
                if(x<16) for(int y=0;y<4;y++)
                    require(decoded.biomes().get(x>>2,y,0).equals(resolver.getNoiseBiome((wx+x)>>2,y,wz>>2)),"Mixed palette round trip");
            }
            Blockbranch.LOGGER.info("ALPINE_SCREE_BOUNDARY_PASS chunk={} screeQuartCells={}",chunk.getPos(),scree);
            return;
        }
        throw new AssertionError("No mixed biome boundary near alpine fixture");
    }

    private static void recipe(ServerLevel level,String name,Item ingredient,int count,Item output) {
        var recipe=level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Blockbranch.id(name))).orElseThrow().value();
        require(recipe instanceof CraftingRecipe,"Crafting recipe loaded");
        var input=CraftingInput.of(2,2,java.util.stream.IntStream.range(0,4)
            .mapToObj(i->i<count ? new ItemStack(ingredient) : ItemStack.EMPTY).toList());
        var crafting=(CraftingRecipe)recipe;
        require(crafting.matches(input,level) && crafting.assemble(input).is(output),"Fragment recipe "+name);
        var wrong=CraftingInput.of(1,1,List.of(new ItemStack(Items.DIRT)));
        require(!crafting.matches(wrong,level),"Recipe enforces fragment count");
    }

    public static void register() {
        BlockPos[] falling={null},landing={null}; int[] ticks={0}; boolean[] finished={false};
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            try {
                var level=server.overworld(); var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                var pos=fixture(generator,level.getChunkSource().randomState());
                // Recover only this test's temporary blocks after an interrupted run.
                for(int dy:new int[]{1,5,6}) level.setBlock(pos.above(dy),Blocks.AIR.defaultBlockState(),3);
                var result=Path.of(System.getProperty("blockbranch.screeSmokeResult"));
                verify(level,generator,pos,Files.exists(result.resolveSibling("saved.txt")));
                level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
                falling[0]=pos.above(6); landing[0]=pos.above();
                level.setBlock(falling[0].below(),Blocks.STONE.defaultBlockState(),3);
                level.setBlock(falling[0],ScreeContent.LOOSE_SCREE.defaultBlockState(),3);
                level.setBlock(falling[0].below(),Blocks.AIR.defaultBlockState(),3);
            } catch(Throwable failure) { finished[0]=true; Blockbranch.LOGGER.error("ALPINE_SCREE_SMOKE_FAIL",failure); server.halt(false); }
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(finished[0] || falling[0]==null || ++ticks[0]<40) return;
            finished[0]=true;
            try {
                var level=server.overworld();
                require(level.getBlockState(falling[0]).is(ScreeContent.LOOSE_SCREE),"Off-window scree simulation deferred");
                require(level.getBlockTicks().hasScheduledTick(falling[0],ScreeContent.LOOSE_SCREE),"Deferred fall remains scheduled");
                level.setBlock(falling[0],Blocks.AIR.defaultBlockState(),3);
                level.setChunkForced(falling[0].getX()>>4,falling[0].getZ()>>4,false);
                server.saveEverything(false,true,true);
                var result=Path.of(System.getProperty("blockbranch.screeSmokeResult"));
                Files.writeString(result.resolveSibling("saved.txt"),"saved\n"); Files.writeString(result,"PASS\n");
                Blockbranch.LOGGER.info("ALPINE_SCREE_SMOKE_PASS deferredFallTicks={}",ticks[0]);
            } catch(Throwable failure) { Blockbranch.LOGGER.error("ALPINE_SCREE_SMOKE_FAIL",failure); }
            finally { server.halt(false); }
        });
    }

    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private AlpineScreeSmoke() {}
}
