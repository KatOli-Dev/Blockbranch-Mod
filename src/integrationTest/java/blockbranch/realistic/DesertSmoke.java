package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;
/** Native pavement/soil/vegetation, biome boundaries, drops, disturbance and persistence. */
public final class DesertSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random){var p=blockbranch.realistic.GravelDesertFixtures.fixture(generator.drainage(random),random.seed());return new BlockPos(p.x(),generator.drainage(random).sample(p.x(),p.z()).height(),p.z());}
    /** Fixed photographic anchor is independent of the current natural pavement physics witness. */
    public static BlockPos captureAnchor(RealisticGenerator generator,RandomState random){
        String value=System.getProperty("blockbranch.gravelDesertCapturePoint","");
        if(value.isBlank())return fixture(generator,random);
        var xyz=value.split(",");if(xyz.length!=3)throw new IllegalArgumentException("gravelDesertCapturePoint requires x,y,z");
        return new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
    }
    public static void capture(net.minecraft.server.level.ServerPlayer player,int stage){
        var level=player.level();var server=level.getServer();var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
        var anchor=captureAnchor(generator,level.getChunkSource().randomState());boolean overlook=stage==124;
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,server);
        player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" "+(anchor.getX()+.5)+" "+(anchor.getY()+(overlook?36:2))+" "+(anchor.getZ()+.5)+" 45 "+(overlook?35:10));
        Blockbranch.LOGGER.info("GRAVEL_DESERT_FIXED_CAMERA stage={} anchor={},{},{} yaw=45 pitch={} height={}",stage,anchor.getX(),anchor.getY(),anchor.getZ(),overlook?35:10,overlook?36:2);
    }
    public static BlockPos gravelStart(BlockPos bed){return RiverMarginSmoke.gravelStart(bed);}
    public static BlockPos gravelLanding(ServerLevel level,BlockPos bed){return RiverMarginSmoke.gravelLanding(level,bed);}
    public static void restorePhysics(ServerLevel level,BlockPos bed){RiverMarginSmoke.restorePhysics(level,bed);}
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload){
        var random=level.getChunkSource().randomState();var resolver=generator.getBiomeSource().createUncachedResolver(random);
        var pavement=DesertContent.PAVEMENT;var saved=bed.below(8);
        require(level.getBiome(bed).is(Blockbranch.id("continental_gravel_desert")),"Actual gravel desert identity");
        if(reload){require(level.getBlockState(bed).is(Blocks.GRAVEL),"Saved disturbed natural pavement survives reopen");require(level.getBlockState(saved).is(pavement),"Saved pavement edit survives reopen");}
        restorePhysics(level,bed);require(level.getBlockState(bed).is(pavement),"Natural compact pavement");
        var witnesses=new BlockPos[8];
        for(int z=bed.getZ()-256;z<=bed.getZ()+256;z++)for(int x=bed.getX()-256;x<=bed.getX()+256;x++){
            int c=generator.desertCover(x,z,random);if(c>0&&c<8&&witnesses[c]==null)witnesses[c]=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
            if(Arrays.stream(witnesses,1,8).allMatch(Objects::nonNull))break;
        }
        for(int cover=1;cover<=7;cover++) {
            var pos=witnesses[cover];require(pos!=null,"Natural pavement/soil/plant component "+cover);
            int x=pos.getX(),z=pos.getZ(),h=pos.getY();var chunk=level.getChunkAt(pos);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            require(base.canopy().size()==0,"Dry climate remains treeless, without replacing prior woodland");
            for(int y=h-4;y<=h+2;y++) {
                var expected=RealisticGenerator.material(y,h,DrainageNetwork.DRY,0,0,0,0,0,0,0,0,0,0,cover);
                require(chunk.getBlockState(new BlockPos(x,y,z))==expected && decoded.state(x,y,z)==expected,"Native/procedural/decoded desert structure "+cover);
                require(decoded.generate(y>>4).getBlockState(x&15,y&15,z&15)==expected,"Materialized client desert definition");
            }
            require(base.top(s->s.is(Blocks.STONE),x,z,h)==h-1,"Bounded two metre desert surface over unchanged rock");
            require(base.top(s->s.is(Blocks.DIRT),x,z,h)==(cover>=4?h:ColumnLimits.MIN_Y),"Fine soil below surface");
            require(base.top(s->s.is(Blocks.GRAVEL),x,z,h)==(cover==2 ? h+1 : ColumnLimits.MIN_Y),"Gravel lag versus buried stony layer");
            if(cover>=5) require(level.getBlockState(pos.above()).canSurvive(level,pos.above()),"Every natural plant has suitable dry support");
            for(var bounds:new LevelHeightAccessor[]{level,LevelHeightAccessor.create(h-3,2),LevelHeightAccessor.create(h-1,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(h+4,2)}) {
                var column=generator.getBaseColumn(x,z,bounds,random);
                for(var type:Heightmap.Types.values()) {
                    int expected=bounds.getMinY();for(int y=Math.min(bounds.getMaxY(),h+2);y>=bounds.getMinY();y--) if(type.isOpaque().test(column.getBlock(y))) { expected=y+1;break; }
                    require(generator.getBaseHeight(x,z,type,bounds,random)==expected,"Cropped desert heightmap "+type);
                    if(bounds==level) require(chunk.getHeight(type,x&15,z&15)+1==expected,"Native soil/vegetation heightmap "+type);
                }
            }
            for(int qy:new int[]{-3000,0,h>>2,2000}) require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2))
                && decoded.biomes().get((x&15)>>2,qy&3,(z&15)>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2)),"All-altitude biome palette round trip");
        }
        boundary(level,generator);
        require(pavement.defaultBlockState().is(net.minecraft.tags.BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP)&&pavement.defaultBlockState().is(net.minecraft.tags.BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP_NO_LEAVES),"Pavement participates in both native motion heightmap tags");
        require(pavement.defaultBlockState().isCollisionShapeFullBlock(level,bed),"Compacted pavement supports falling gravel");
        var shovel=new ItemStack(Items.IRON_SHOVEL);var drops=Block.getDrops(pavement.defaultBlockState(),level,bed,null,null,shovel);
        require(drops.size()==1&&drops.getFirst().is(Items.GRAVEL),"Normal excavation yields existing loose gravel");
        shovel.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        drops=Block.getDrops(pavement.defaultBlockState(),level,bed,null,null,shovel);require(drops.size()==1&&drops.getFirst().is(DesertContent.PAVEMENT_ITEM),"Silk touch preserves pavement veneer");
        require(new ItemStack(Items.IRON_SHOVEL).getDestroySpeed(pavement.defaultBlockState())>new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(pavement.defaultBlockState()),"Pavement is shovel mineable");
        require(!FallingBlock.class.isInstance(pavement)&&!pavement.defaultBlockState().isRandomlyTicking(),"Compacted veneer stable without reformation scans");
        require(pavement.disturb(level,bed)&&level.getBlockState(bed).is(Blocks.GRAVEL)&&!pavement.disturb(level,bed),"One-way disturbance into loose gravel");
        var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();
        var invalid=terrain.base().encode(level.palettedContainerFactory());invalid.putInt("version",11);boolean rejected=false;
        try{TerrainProfile.decode(invalid,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Pre-desert profile rejected");
        invalid=terrain.base().encode(level.palettedContainerFactory());invalid.putByteArray("desert",new byte[255]);rejected=false;
        try{TerrainProfile.decode(invalid,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Truncated desert array rejected");
        level.setBlock(saved,pavement.defaultBlockState(),2);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
        require(level.getBlockState(saved).is(pavement)&&level.getBlockState(bed).is(Blocks.GRAVEL),"Pavement and disturbed gravel persist through section eviction");
        Blockbranch.LOGGER.info("CONTINENTAL_GRAVEL_DESERT_PASS {} bed={} components=pavement,gravel,sand,fine-soil,dead-stems,sparse-dry-grass,sparse-shrub",reload?"RELOAD":"NEW",bed);
    }
    private static void boundary(ServerLevel level,RealisticGenerator generator) {
        var random=level.getChunkSource().randomState();var edge=blockbranch.realistic.GravelDesertFixtures.transition(generator.drainage(random),random.seed(),false);var source=generator.getBiomeSource().createUncachedResolver(random);
        for(int dz=-32;dz<=32;dz+=16) for(int dx=-32;dx<=32;dx+=16) {
            int x=Math.floorDiv(edge.insideX()+dx,16)*16,z=Math.floorDiv(edge.insideZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++) for(int qx=0;qx<4;qx++) if(source.getNoiseBiome((x>>2)+qx,0,(z>>2)+qz).is(Blockbranch.id("continental_gravel_desert"))) count++;
            if(count==0 || count==16) continue;
            for(int oz=0;oz<16;oz++) for(int ox=0;ox<20;ox++) {
                int wx=x+ox,wz=z+oz,h=generator.drainage(random).sample(wx,wz).height();var chunk=level.getChunk(wx>>4,wz>>4);
                for(int y=h-3;y<=h+4;y++) require(chunk.getBlockState(new BlockPos(wx,y,wz))==generator.surfaceState(wx,y,wz,random),"Desert/native chunk edge structure agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Desert biome edge agrees with source");
            }
            Blockbranch.LOGGER.info("CONTINENTAL_GRAVEL_DESERT_BOUNDARY_PASS x={} z={} desertQuartCells={} transition={}",x,z,count,edge);return;
        }
        throw new AssertionError("No mixed desert native biome boundary");
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private DesertSmoke() {}
}
