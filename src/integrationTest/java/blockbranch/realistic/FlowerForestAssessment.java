package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;

/** Read-only native coverage assessment; sites are selected by biome interior, never flower count. */
public final class FlowerForestAssessment {
    public static void verify(ServerLevel level,RealisticGenerator g)throws Exception{
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());
        if(r.seed()!=424242)throw new IllegalArgumentException("Assessment sites are documented seed-424242 witnesses");
        var results=new ArrayList<Map<String,Object>>();
        // First separated regions from the lexicographic 16 km domain survey,
        // offset at 64 m steps, requiring a complete 128 m interior at 16 m steps.
        int[][] sites={{-700098,-1788031},{-795936,-1580128},{1267936,-764128},{-28000,-508000}};
        for(int index=0;index<sites.length;index++){
            int centerX=sites[index][0],centerZ=sites[index][1];
            int biome=0,physical=0,litClear=0,shaded=0,blocked=0,flowers=0,litFlowers=0,shadedFlowers=0;
            var species=new TreeMap<String,Integer>();var tiles=new HashMap<Long,int[]>();
            for(int dz=-48;dz<=48;dz++)for(int dx=-48;dx<=48;dx++){
                int x=centerX+dx,z=centerZ+dz;
                if(TemperateForestVariants.kind(r.seed(),x,z,d,c)!=TemperateForestVariants.FLOWER)continue;
                biome++;if(TemperateForestVariants.surfaceKind(r.seed(),x,z,d,c)!=TemperateForestVariants.FLOWER)continue;physical++;
                int h=d.sample(x,z).height();var canopy=g.canopy(x,z,r);boolean shade=canopy.topY(x,z)>h+2,clear=canopy.state(x,h+1,z)==null;
                if(shade)shaded++;if(!clear)blocked++;if(!shade&&clear)litClear++;
                var pos=new BlockPos(x,h+1,z);var chunk=level.getChunkAt(pos);var state=chunk.getBlockState(pos);var base=((SparseColumn)chunk).blockbranch$terrain().base();
                require(state==base.state(x,h+1,z)&&state==g.surfaceState(x,h+1,z,r),"Actual native/profile/analytic floor agreement");
                long tile=((long)(x>>4)<<32)^((z>>4)&0xffffffffL);var counts=tiles.computeIfAbsent(tile,unused->new int[2]);counts[0]++;
                if(state.is(BlockTags.SMALL_FLOWERS)){
                    require(state.canSurvive(level,pos),"Actual flower support");flowers++;counts[1]++;if(shade)shadedFlowers++;if(!shade&&clear)litFlowers++;
                    species.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),1,Integer::sum);
                }
            }
            if(index>0)require(biome==97*97&&physical==biome,"Every column in the measured interior belongs to the new biome and physical floor domain");
            int fullTiles=0,emptyFullTiles=0;for(var n:tiles.values())if(n[0]==256){fullTiles++;if(n[1]==0)emptyFullTiles++;}
            var row=new LinkedHashMap<String,Object>();row.put("name",index==0?"preserved_edge":"independent_interior_"+index);row.put("x",centerX);row.put("z",centerZ);row.put("sample_columns",97*97);row.put("biome_columns",biome);row.put("physical_columns",physical);row.put("lit_clear_columns",litClear);row.put("shaded_columns",shaded);row.put("blocked_columns",blocked);row.put("native_flowers",flowers);row.put("lit_flowers",litFlowers);row.put("shaded_flowers",shadedFlowers);row.put("flower_fraction_of_physical_domain",flowers/(double)physical);row.put("flower_fraction_of_lit_clear",litFlowers/(double)litClear);row.put("full_16m_tiles",fullTiles);row.put("flowerless_full_16m_tiles",emptyFullTiles);row.put("species",species);results.add(row);
            Blockbranch.LOGGER.info("FLOWER_FOREST_COVERAGE {}",row);
        }
        var target=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).resolveSibling("flower-forest-assessment.json");
        Files.writeString(target,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results)+"\n");
        Blockbranch.LOGGER.info("FLOWER_FOREST_INTERIOR_NATIVE_ASSESSMENT_PASS sites={} report={}",results.size(),target);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private FlowerForestAssessment(){}
}
