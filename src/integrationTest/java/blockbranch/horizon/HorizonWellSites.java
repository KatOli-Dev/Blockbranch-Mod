package blockbranch.horizon;

import blockbranch.Blockbranch;
import blockbranch.realistic.*;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Known atlas wells for horizon checks and review captures: the nearest planned well to the
 * world spawn and, when that one does not, the nearest whose footprint crosses a chunk border.
 * Stateless; shared by {@link HorizonWellCheck} and the client's well views.
 */
public final class HorizonWellSites {
    /** Planning square half-width ({@link DesertWell#forChunk}). */
    public static final int REACH=6;
    public record Site(String name,DesertWell.Plan plan,List<ChunkPos> chunks) {}

    /** Footprint half-width: the vanilla desert well spans ±2 blocks around its centre, the mod's own cistern ±5. */
    public static int span(RealisticGenerator generator){return generator.nativeOrdinaryDefinition()>0?2:5;}

    /** The nearest well, then the nearest border-crossing well when it is a different one. */
    public static List<Site> find(ServerLevel level) {
        var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
        if(generator.structureDefinition()==0)throw new AssertionError("This world places no atlas wells");
        int span=span(generator);
        var wells=generator.wells(level.getChunkSource().randomState());
        var spawn=level.getRespawnData().pos();
        int sx=Math.floorDiv(spawn.getX(),BiomeRegionPlacement.GRID),sz=Math.floorDiv(spawn.getZ(),BiomeRegionPlacement.GRID);
        // Wells are sparse (one per qualifying desert region), so search outward ring by ring
        // until the rings hold a well and one whose footprint crosses a chunk border (the
        // atlas spans cells -1024 to 1023).
        var plans=new ArrayList<DesertWell.Plan>();int rings=0;
        for(int r=0;r<2048&&(plans.isEmpty()||plans.stream().noneMatch(p->border(p,span)));r++,rings++)
            for(int z=sz-r;z<=sz+r;z++)for(int x=sx-r;x<=sx+r;x++)if(Math.max(Math.abs(x-sx),Math.abs(z-sz))==r)
                wells.plan(new BiomeRegionPlacement.Cell(x,z)).ifPresent(plans::add);
        if(plans.isEmpty())throw new AssertionError("No planned wells in the atlas");
        plans.sort(Comparator.comparingDouble(p->Math.hypot(p.x()-spawn.getX(),p.z()-spawn.getZ())));
        var sites=new ArrayList<Site>();
        sites.add(site("nearest",plans.getFirst()));
        plans.stream().filter(p->border(p,span)).findFirst().filter(p->p!=plans.getFirst()).ifPresent(p->sites.add(site("border",p)));
        Blockbranch.LOGGER.info("HORIZON_WELL_SITES wells {}, nativeOrdinary={}, {} planned within {} cell rings, spawn {}, sites {}",
            span==2?"vanilla feature":"mod cistern",generator.nativeOrdinaryDefinition(),plans.size(),rings,spawn.toShortString(),sites);
        return sites;
    }
    private static boolean border(DesertWell.Plan p,int span){return (p.x()-span)>>4!=(p.x()+span)>>4||(p.z()-span)>>4!=(p.z()+span)>>4;}
    /** Chunks a well's planning square reaches, as {@link DesertWell#forChunk} admits them. */
    private static Site site(String name,DesertWell.Plan p) {
        var chunks=new ArrayList<ChunkPos>();
        for(int cz=(p.z()-REACH)>>4;cz<=(p.z()+REACH)>>4;cz++)for(int cx=(p.x()-REACH)>>4;cx<=(p.x()+REACH)>>4;cx++)chunks.add(new ChunkPos(cx,cz));
        return new Site(name,p,chunks);
    }
    private HorizonWellSites(){}
}
