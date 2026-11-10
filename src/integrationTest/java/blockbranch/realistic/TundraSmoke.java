package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Steppe structure, transitions, fescue use/drop/state behavior and saved crown regression. */
public final class TundraSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random) {
        var p=blockbranch.realistic.ColdTundraFixtures.fixture(generator.drainage(random),random.seed());
        return new BlockPos(p.x(),generator.drainage(random).sample(p.x(),p.z()).height(),p.z());
    }
    public static BlockPos captureAnchor(RealisticGenerator generator,RandomState random){
        String value=System.getProperty("blockbranch.tundraFloorCapturePoint","");if(value.isBlank())return fixture(generator,random);
        var xyz=value.split(",");if(xyz.length!=3)throw new IllegalArgumentException("tundraFloorCapturePoint requires x,y,z");
        return new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
    }
    public static void capture(net.minecraft.server.level.ServerPlayer player,int stage){
        var level=player.level();var server=level.getServer();var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
        var anchor=captureAnchor(generator,random);boolean overlook=stage==126;int floor=anchor.getY();
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)floor=Math.max(floor,generator.drainage(random).sample(anchor.getX()+dx,anchor.getZ()+dz).height());
        int y=overlook?anchor.getY()+30:floor+2;
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,server);player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" "+(anchor.getX()+.5)+" "+y+" "+(anchor.getZ()+.5)+" 45 "+(overlook?35:10));
        Blockbranch.LOGGER.info("TUNDRA_FLOOR_FIXED_CAMERA stage={} anchor={},{},{} feetY={} yaw=45 pitch={} nineFeetColumnsChecked=true",stage,anchor.getX(),anchor.getY(),anchor.getZ(),y,overlook?35:10);
    }
    public static void restore(ServerLevel level,BlockPos bed) {
        var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
        for(int dy=0;dy<=1;dy++) level.setBlock(bed.above(dy),base.state(bed.getX(),bed.getY()+dy,bed.getZ()),3);
        var berry=berry(level,bed);for(var pos:List.of(berry.below(),berry)){var profile=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();level.setBlock(pos,profile.state(pos.getX(),pos.getY(),pos.getZ()),3);}
    }
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload) {
        var random=level.getChunkSource().randomState();var resolver=generator.getBiomeSource().createUncachedResolver(random);
        var fescue=MeadowContent.MEADOW_FESCUE;var mature=fescue.defaultBlockState().setValue(MeadowFescueBlock.CROPPED,false);
        require(level.getBiome(bed).is(Blockbranch.id("cold_dwarf_shrub_tundra")),"Actual steppe identity");
        require(level.getBlockState(bed).is(Blocks.GRASS_BLOCK) && level.getBlockState(bed.above())==mature,"Natural mature bunchgrass on dry sward");
        verifyFreshFloor(level,generator,bed);
        var witnesses=new BlockPos[BorealSurface.LAST+1];
        for(int z=bed.getZ()-64;z<=bed.getZ()+64;z++) for(int x=bed.getX()-64;x<=bed.getX()+64;x++) {
            if(!ColdTundra.retained(x,z,generator.drainage(random),new ClimateField(random.seed())))continue;
            int c=generator.borealCover(x,z,random);if(c>0 && witnesses[c]==null) witnesses[c]=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
        }
        for(int cover:new int[]{2,BorealSurface.SOIL,BorealSurface.CARPET,8,9,10,11}) {
            var pos=witnesses[cover];require(pos!=null,"Natural soil/sward/fescue component "+cover);
            int x=pos.getX(),z=pos.getZ(),h=pos.getY();var chunk=level.getChunkAt(pos);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            require(Arrays.stream(base.canopy().encode()).allMatch(v->SnowyTaiga.snow(v&blockbranch.cubic.CanopyLayer.STATE_MASK)),"Retained tundra remains treeless; neighbouring terrain snow is allowed");
            for(int y=h-4;y<=h+2;y++) {
                var rawMaterial=RealisticGenerator.material(y,h,DrainageNetwork.DRY,0,0,0,0,0,0,0,cover,0,0,0);
                // Complete states include permitted snow from neighbouring terrain.
                // Soil identities below the surface remain literal palette assertions.
                var expected=generator.surfaceState(x,y,z,random);
                if(BorealSurface.mossSite(cover)&&y>=h-2&&y<=h)require(expected==rawMaterial,"Tundra podzol/dirt palette changed below its snow overlay");
                require(chunk.getBlockState(new BlockPos(x,y,z))==expected && decoded.state(x,y,z)==expected,"Native/procedural/decoded tundra structure cover="+cover+" pos="+new BlockPos(x,y,z)+" native="+chunk.getBlockState(new BlockPos(x,y,z))+" stored="+base.state(x,y,z)+" decoded="+decoded.state(x,y,z)+" generator="+expected+" rawMaterial="+rawMaterial);
                require(decoded.generate(y>>4).getBlockState(x&15,y&15,z&15)==expected,"Materialized client steppe definition");
            }
            require(base.top(s->s.is(Blocks.STONE),x,z,h)==h-2,"Bounded three metre root zone over unchanged rock");

            if(cover==10)require(base.top(s->s.is(Blocks.GRAVEL),x,z,h)==h+1,"Exposed gravel on stony tundra gaps");
            if(TerrainProfile.borealPlantPresent(cover)) require(level.getBlockState(pos.above()).canSurvive(level,pos.above()),"Every natural plant has suitable dry support");
            for(var bounds:new LevelHeightAccessor[]{level,LevelHeightAccessor.create(h-3,2),LevelHeightAccessor.create(h-1,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(h+4,2)}) {
                var column=generator.getBaseColumn(x,z,bounds,random);
                for(var type:Heightmap.Types.values()) {
                    int expected=bounds.getMinY();for(int y=Math.min(bounds.getMaxY(),h+2);y>=bounds.getMinY();y--) if(type.isOpaque().test(column.getBlock(y))) { expected=y+1;break; }
                    require(generator.getBaseHeight(x,z,type,bounds,random)==expected,"Cropped steppe heightmap "+type);
                    if(bounds==level) require(chunk.getHeight(type,x&15,z&15)+1==expected,"Native soil/vegetation heightmap "+type);
                }
            }
            for(int qy:new int[]{-3000,0,h>>2,2000}) require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2))
                && decoded.biomes().get((x&15)>>2,qy&3,(z&15)>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2)),"All-altitude biome palette round trip");
        }
        boundary(level,generator);
        var shears=new ItemStack(Items.SHEARS);var drops=Block.getDrops(mature,level,bed.above(),null,null,shears);
        require(drops.size()==1 && drops.getFirst().is(MeadowContent.FESCUE_ITEM),"Shears retain a live planting crown");
        drops=Block.getDrops(mature,level,bed.above(),null,null,ItemStack.EMPTY);
        require(drops.isEmpty(),"Ordinary fescue breaking produces no extra ingredient");
        require(fescue.defaultBlockState().getValue(MeadowFescueBlock.CROPPED),"Transplants start cropped");
        require(fescue.divide(level,bed.above(),mature),"Clip mature fescue");var cropped=level.getBlockState(bed.above());
        require(cropped.is(fescue) && cropped.getValue(MeadowFescueBlock.CROPPED) && !fescue.divide(level,bed.above(),cropped),"Retained crown cannot be cropped repeatedly");
        require(cropped.getShape(level,bed.above()).max(Direction.Axis.Y)<=0.375 && mature.getShape(level,bed.above()).max(Direction.Axis.Y)>0.8,"Cropped versus mature geometry");
        require(Block.getDrops(cropped,level,bed.above(),null,null,ItemStack.EMPTY).isEmpty(),"No extra ingredient from a cropped crown");
        drops=Block.getDrops(cropped,level,bed.above(),null,null,shears);require(drops.size()==1 && drops.getFirst().is(MeadowContent.FESCUE_ITEM),"Cropped crown remains transplantable");
        require(!cropped.isRandomlyTicking() && fescue.isValidBonemealTarget(level,bed.above(),cropped,BonemealSource.INTERACTION),"Deliberate regrowth without world scans");
        fescue.performBonemeal(level,level.getRandom(),bed.above(),cropped,BonemealSource.INTERACTION);
        require(level.getBlockState(bed.above())==mature && !fescue.isValidBonemealTarget(level,bed.above(),mature,BonemealSource.INTERACTION),"Regrowth stops at one mature fescue");
        for(var soil:List.of(Blocks.STONE,Blocks.SAND,Blocks.MUD)) {
            level.setBlock(bed,soil.defaultBlockState(),3);require(level.getBlockState(bed.above()).isAir() && !cropped.canSurvive(level,bed.above()),"Unsuitable soil removes fescue");restore(level,bed);
        }
        var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();var invalid=terrain.base().encode(level.palettedContainerFactory());invalid.putInt("version",12);boolean rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }require(rejected,"Pre-steppe profile rejected");
        invalid=terrain.base().encode(level.palettedContainerFactory());invalid.putByteArray("boreal",new byte[255]);rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }require(rejected,"Truncated steppe array rejected");
        var berry=Blocks.SWEET_BERRY_BUSH;var low=berry.defaultBlockState().setValue(SweetBerryBushBlock.AGE,1);var berryPos=bed.below(12);
        level.setBlock(berryPos.below(),Blocks.GRASS_BLOCK.defaultBlockState(),2);level.setBlock(berryPos,low,2);
        var bush=(SweetBerryBushBlock)berry;bush.performBonemeal(level,level.getRandom(),berryPos,low,BonemealSource.INTERACTION);
        var ripe=level.getBlockState(berryPos);require(ripe.getValue(SweetBerryBushBlock.AGE)==2,"Native low berry shrub can grow to fruit");
        require(Block.getDrops(ripe,level,berryPos,null,null,ItemStack.EMPTY).stream().anyMatch(stack->stack.is(Items.SWEET_BERRIES)),"Native berries retain usable food drops");
        for(var plant:List.of(low,Blocks.MOSS_CARPET.defaultBlockState(),mature)){
            level.setBlock(berryPos,plant,2);require(plant.canSurvive(level,berryPos),"All tundra low plants have valid soil support");level.setBlock(berryPos.below(),Blocks.AIR.defaultBlockState(),3);require(level.getBlockState(berryPos).isAir(),"Tundra plants lose invalid support");level.setBlock(berryPos.below(),Blocks.GRASS_BLOCK.defaultBlockState(),2);
        }
        var original=terrain.base().encode(level.palettedContainerFactory());var originalCold=original.getByteArray("boreal").orElseThrow().clone();var originalGround=original.getIntArray("ground").orElseThrow().clone();
        var malformed=terrain.base().encode(level.palettedContainerFactory());var bad=malformed.getByteArray("boreal").orElseThrow();bad[0]=(byte)(BorealSurface.LAST+1);malformed.putByteArray("boreal",bad);rejected=false;
        try{TerrainProfile.decode(malformed,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Unknown cold-floor code rejected");
        malformed.getIntArray("ground").orElseThrow()[0]=TerrainShape.FLOOR;
        var untouched=terrain.base().encode(level.palettedContainerFactory());require(Arrays.equals(originalCold,untouched.getByteArray("boreal").orElseThrow())&&Arrays.equals(originalGround,untouched.getIntArray("ground").orElseThrow()),"Mutating malformed NBT snapshot must not corrupt immutable source profile");
        var saved=bed.below(8);
        if(reload) require(level.getBlockState(saved).is(fescue) && level.getBlockState(saved).getValue(MeadowFescueBlock.CROPPED),"Saved cropped fescue survives reopen");
        level.setBlock(saved.below(),Blocks.COARSE_DIRT.defaultBlockState(),2);level.setBlock(saved,cropped,3);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
        require(level.getBlockState(saved)==cropped,"Cropped property survives section eviction");
        Blockbranch.LOGGER.info("COLD_DWARF_SHRUB_TUNDRA_PASS {} bed={} components=coarse-soil,moss-ground,moss-mat,short-grass,low-shrub,gravel,fescue",reload ? "RELOAD" : "NEW",bed);
    }
    private static void verifyFreshFloor(ServerLevel level,RealisticGenerator generator,BlockPos bed){
        var random=level.getChunkSource().randomState();var climate=new ClimateField(random.seed());int columns=0,soil=0,carpets=0;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var tag=base.encode(level.palettedContainerFactory());var ground=tag.getIntArray("ground").orElseThrow();var roles=tag.getByteArray("boreal").orElseThrow();var decoded=TerrainProfile.decode(tag,level.palettedContainerFactory());
            var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
            for(int i=0;i<256;i++){
                int x=cx*16+(i&15),z=cz*16+(i>>4),h=ground[i],cover=roles[i];if(!ColdTundra.retained(x,z,generator.drainage(random),climate))continue;
                columns++;var p=new BlockPos(x,h,z);var nativeGround=level.getBlockState(p);var procedural=generator.surfaceState(x,h,z,random);var copied=decoded.state(x,h,z);var materialized=sections.computeIfAbsent(h>>4,decoded::generate).getBlockState(x&15,h&15,z&15);
                require(!nativeGround.is(Blocks.MOSS_BLOCK)&&!procedural.is(Blocks.MOSS_BLOCK)&&!copied.is(Blocks.MOSS_BLOCK)&&!materialized.is(Blocks.MOSS_BLOCK),"Fresh tundra never emits cubic moss "+p);
                if(BorealSurface.mossSite(cover)){
                    soil++;require(nativeGround.is(Blocks.PODZOL)&&procedural.is(Blocks.PODZOL)&&copied.is(Blocks.PODZOL)&&materialized.is(Blocks.PODZOL),"Fresh tundra mat/soil retains podzol in all native representations "+p);
                    require(base.state(x,h-1,z).is(Blocks.DIRT)&&level.getBlockState(p.below()).is(Blocks.DIRT),"Fresh tundra podzol retains dirt underlayer");
                }
                if(level.getBlockState(p.above()).is(Blocks.MOSS_CARPET)){
                    carpets++;require(cover==BorealSurface.CARPET&&nativeGround.is(Blocks.PODZOL)&&procedural.is(Blocks.PODZOL)&&copied.is(Blocks.PODZOL)&&materialized.is(Blocks.PODZOL),"Every fresh native moss carpet has podzol support");
                    require(generator.surfaceState(x,h+1,z,random).is(Blocks.MOSS_CARPET)&&decoded.state(x,h+1,z).is(Blocks.MOSS_CARPET)&&sections.computeIfAbsent((h+1)>>4,decoded::generate).getBlockState(x&15,(h+1)&15,z&15).is(Blocks.MOSS_CARPET),"Fresh carpet exists in procedural/decoded/materialized definitions");
                }
            }
        }
        require(columns>100&&soil>0&&carpets>0,"Fresh tundra soil/carpet coverage");Blockbranch.LOGGER.info("TUNDRA_FLOOR_NATIVE_IDENTITIES_PASS columns={} soil={} carpets={} mossCubes=0 podzolSupport=true",columns,soil,carpets);
    }
    private static void boundary(ServerLevel level,RealisticGenerator generator) {
        var random=level.getChunkSource().randomState();var edge=blockbranch.realistic.ColdTundraFixtures.transition(generator.drainage(random),random.seed(),false);var source=generator.getBiomeSource().createUncachedResolver(random);
        for(int dz=-32;dz<=32;dz+=16) for(int dx=-32;dx<=32;dx+=16) {
            int x=Math.floorDiv(edge.insideX()+dx,16)*16,z=Math.floorDiv(edge.insideZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++) for(int qx=0;qx<4;qx++) if(source.getNoiseBiome((x>>2)+qx,0,(z>>2)+qz).is(Blockbranch.id("cold_dwarf_shrub_tundra"))) count++;
            if(count==0 || count==16) continue;
            for(int oz=0;oz<16;oz++) for(int ox=0;ox<20;ox++) {
                int wx=x+ox,wz=z+oz,h=generator.drainage(random).sample(wx,wz).height();var chunk=level.getChunk(wx>>4,wz>>4);
                for(int y=h-3;y<=h+2;y++) require(chunk.getBlockState(new BlockPos(wx,y,wz))==generator.surfaceState(wx,y,wz,random),"Steppe/native chunk edge structure agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Steppe biome edge agrees with source");
            }
            Blockbranch.LOGGER.info("COLD_DWARF_SHRUB_TUNDRA_BOUNDARY_PASS x={} z={} steppeQuartCells={} transition={}",x,z,count,edge);return;
        }
        throw new AssertionError("No mixed steppe native biome boundary");
    }
    public static BlockPos berry(ServerLevel level,BlockPos bed){
        var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var d=generator.drainage(random);var c=new ClimateField(random.seed());
        for(int z=bed.getZ()-64;z<=bed.getZ()+64;z++)for(int x=bed.getX()-64;x<=bed.getX()+64;x++)if(ColdTundra.retained(x,z,d,c)&&generator.borealCover(x,z,random)==9)return new BlockPos(x,d.sample(x,z).height()+1,z);
        throw new AssertionError("No natural low berry shrub near tundra fixture");
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private TundraSmoke() {}
}
