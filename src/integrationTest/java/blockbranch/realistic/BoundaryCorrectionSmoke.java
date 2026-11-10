package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.RandomState;

/** Native witnesses showing old altitude holes are corrected without fabricating new ecological categories. */
public final class BoundaryCorrectionSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random,boolean upland) {
        var drainage=generator.drainage(random);
        for(int z=-1700000;z<=1700000;z+=16000)for(int x=-1700000;x<=1700000;x+=16000) {
            int h=drainage.sample(x,z).height(),e=h+1-TerrainShape.SEA_LEVEL;
            if(upland ? e<=900 : e<0 || e>=20)continue;
            if(generator.warmCover(x,z,random)>0)continue;
            if(generator.meadowCover(x,z,random)>0 || generator.woodlandCover(x,z,random)>0)return new BlockPos(x,h,z);
        }
        throw new AssertionError("No corrected "+(upland ? "upland" : "lowland")+" witness");
    }
    public static void verify(ServerLevel level,RealisticGenerator generator) {
        var random=level.getChunkSource().randomState();
        for(boolean upland:new boolean[]{false,true}) {
            var bed=fixture(generator,random,upland);var chunk=level.getChunkAt(bed);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            var id=generator.woodlandCover(bed.getX(),bed.getZ(),random)>0 ? Blockbranch.id("dappled_oak_birch_woodland") : Blockbranch.id("rolling_lowland_forb_grass_meadow");
            // Keep the original altitude-hole witness; a new broad Meadow identity
            // can legitimately refine its old specific-meadow palette.
            boolean montane=MontaneMeadow.contains(random.seed(),bed.getX(),bed.getZ(),generator.drainage(random),new ClimateField(random.seed()));
            if(montane){if(!upland||generator.meadowCover(bed.getX(),bed.getZ(),random)==0)throw new AssertionError("Unexpected montane refinement of altitude-hole witness");id=net.minecraft.world.level.biome.Biomes.MEADOW.identifier();}
            for(int y:new int[]{-3000,0,bed.getY()>>2,2000})if(!chunk.getNoiseBiome(bed.getX()>>2,y,bed.getZ()>>2).is(id)||!decoded.biomes().get((bed.getX()&15)>>2,y&3,(bed.getZ()&15)>>2).is(id))throw new AssertionError("Corrected common biome palette at "+bed+" expected="+id+" actual="+chunk.getNoiseBiome(bed.getX()>>2,y,bed.getZ()>>2));
            for(int dz=0;dz<16;dz++)for(int dx=0;dx<20;dx++) {
                int x=bed.getX()+dx,z=bed.getZ()+dz,h=generator.drainage(random).sample(x,z).height();var actual=level.getChunk(x>>4,z>>4);var profile=((SparseColumn)actual).blockbranch$terrain().base();
                for(int y=h-3;y<=Math.max(h+2,profile.canopy().topY(x,z));y++)if(actual.getBlockState(new BlockPos(x,y,z))!=generator.surfaceState(x,y,z,random))throw new AssertionError("Corrected native seam/floor/canopy mismatch");
            }
            if(decoded.state(bed.getX(),bed.getY()-1,bed.getZ())!=Blocks.DIRT.defaultBlockState())throw new AssertionError("Corrected common root zone");
            Blockbranch.LOGGER.info("CLIMATE_LED_BOUNDARY_CORRECTION_PASS {} bed={} biome={}",upland ? "UPLAND" : "LOWLAND",bed,id);
        }
    }
    private BoundaryCorrectionSmoke() {}
}
