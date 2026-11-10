package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;

/** Actual newly admitted soil/canopy, native seams, profile round trips and saved timber/ground edits. */
public final class SlopeCorrectionSmoke {
    public static BlockPos fixture(SlopeCorrection.Kind kind,RealisticGenerator generator,RandomState random){var p=blockbranch.realistic.SlopeCorrectionFixtures.fixture(kind,generator.drainage(random),random.seed());return new BlockPos(p.x(),generator.drainage(random).sample(p.x(),p.z()).height(),p.z());}
    public static void verify(ServerLevel level,RealisticGenerator generator,boolean reload){
        var random=level.getChunkSource().randomState();
        for(var kind:SlopeCorrection.Kind.values()){
            var bed=fixture(kind,generator,random);var saved=bed.below(8);var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();var base=terrain.base();var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            if(reload&&(!level.getBlockState(saved).is(Blocks.GOLD_BLOCK)||kind==SlopeCorrection.Kind.BOREAL&&!level.getBlockState(bed.above()).isAir()))throw new AssertionError("Saved corrected soil/timber edit missing");
            if(kind==SlopeCorrection.Kind.BOREAL)level.setBlock(bed.above(),base.state(bed.getX(),bed.getY()+1,bed.getZ()),3);
            var id=Blockbranch.id(switch(kind){case MEADOW->"rolling_lowland_forb_grass_meadow";case BOREAL->"boreal_spruce_birch_woodland";case STEPPE->"dry_continental_scrub_steppe";});
            boolean snowy=kind==SlopeCorrection.Kind.BOREAL&&SnowyTaiga.contains(random.seed(),bed.getX(),bed.getZ(),generator.drainage(random),new ClimateField(random.seed()));
            boolean taiga=kind==SlopeCorrection.Kind.BOREAL&&Taiga.contains(random.seed(),bed.getX(),bed.getZ(),generator.drainage(random),new ClimateField(random.seed()));
            boolean grove=kind==SlopeCorrection.Kind.BOREAL&&Grove.contains(random.seed(),bed.getX(),bed.getZ(),generator.drainage(random),new ClimateField(random.seed()));
            boolean montane=kind==SlopeCorrection.Kind.MEADOW&&MontaneMeadow.contains(random.seed(),bed.getX(),bed.getZ(),generator.drainage(random),new ClimateField(random.seed()));
            if(grove)id=net.minecraft.world.level.biome.Biomes.GROVE.identifier();else if(snowy)id=net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA.identifier();else if(taiga)id=net.minecraft.world.level.biome.Biomes.TAIGA.identifier();else if(montane)id=net.minecraft.world.level.biome.Biomes.MEADOW.identifier();
            for(int y:new int[]{-3000,0,bed.getY()>>2,2000})if(!chunk.getNoiseBiome(bed.getX()>>2,y,bed.getZ()>>2).is(id)||!decoded.biomes().get((bed.getX()&15)>>2,y&3,(bed.getZ()&15)>>2).is(id))throw new AssertionError("Corrected native biome "+kind+" at "+bed+" expected="+id);
            int canopy=0;
            for(int dz=-8;dz<16;dz++)for(int dx=-8;dx<20;dx++){
                int x=bed.getX()+dx,z=bed.getZ()+dz,h=generator.drainage(random).sample(x,z).height();var actual=level.getChunk(x>>4,z>>4);var profile=((SparseColumn)actual).blockbranch$terrain().base();int top=Math.max(h+2,profile.canopy().topY(x,z));
                if(profile.canopy().topY(x,z)>h)canopy++;
                for(int y=h-3;y<=top;y++)if(actual.getBlockState(new BlockPos(x,y,z))!=generator.surfaceState(x,y,z,random))throw new AssertionError("Corrected native soil/canopy seam "+kind+" "+x+","+y+","+z);
            }
            if(kind==SlopeCorrection.Kind.BOREAL&&(canopy==0||!level.getBlockState(bed.above()).is(taiga||snowy||grove?Blocks.SPRUCE_WOOD:Blocks.SPRUCE_LOG)))throw new AssertionError("New boreal domain lacks supported canopy");
            for(int y=bed.getY()-3;y<=Math.max(bed.getY()+2,base.canopy().topY(bed.getX(),bed.getZ()));y++)if(decoded.state(bed.getX(),y,bed.getZ())!=base.state(bed.getX(),y,bed.getZ())||decoded.generate(y>>4).getBlockState(bed.getX()&15,y&15,bed.getZ()&15)!=base.state(bed.getX(),y,bed.getZ()))throw new AssertionError("Corrected profile/client materialization");
            level.setBlock(saved,Blocks.GOLD_BLOCK.defaultBlockState(),2);if(kind==SlopeCorrection.Kind.BOREAL)level.setBlock(bed.above(),Blocks.AIR.defaultBlockState(),3);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
            if(!level.getBlockState(saved).is(Blocks.GOLD_BLOCK)||kind==SlopeCorrection.Kind.BOREAL&&!level.getBlockState(bed.above()).isAir())throw new AssertionError("Corrected edited section eviction");
            Blockbranch.LOGGER.info("SUPPORTED_SLOPE_ALTITUDE_NATIVE_PASS {} kind={} bed={} canopyColumns={}",reload?"RELOAD":"NEW",kind,bed,canopy);
        }
    }
    private SlopeCorrectionSmoke(){}
}
