package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.JsonObject;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Cross-process save/reopen of varied leaf litter on a gate-on world. {@code prepare} generates a
 * forest window, applies saved edits (a break, a state exchange between two differently varied
 * litter blocks, and a placed cobblestone), saves and records every ground+1 state; {@code verify}
 * reopens the closed world in a second process and requires the identical states, the edits, and
 * at least {@link #MIN_STATES} distinct litter states. Edits cannot be regenerated, so they
 * show the chunks were read from disk, not rebuilt. */
public final class LeafLitterReopen {
    private static final int SITE_X=304000,SITE_Z=784000,HALF_CHUNKS=3,MIN_STATES=8;
    public static void register(String mode){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.leafLitterDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var d=g.drainage(level.getChunkSource().randomState());
            var json=RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().getAsJsonObject();
            int ice=json.has("continental_ice_definition")?json.get("continental_ice_definition").getAsInt():0;
            if(!json.has("leaf_litter_definition")||json.get("leaf_litter_definition").getAsInt()!=1||json.get("shape_version").getAsInt()!=(g.borealSlopeDefinition()==1?271:g.alluvialDefinition()==1?270:g.seasonalForestDefinition()==1?269:g.warmXericDefinition()==1?268:ice==1?267:266))throw new AssertionError("Litter-gated world expected (267 with ice, 266 litter only; 268 adds xeric shrubs, 269 seasonal forests): "+json.get("shape_version")+" "+json.get("leaf_litter_definition"));
            if(mode.equals("prepare")&&ice!=1)throw new AssertionError("Prepare writes the bundled combined world");
            int cx=SITE_X>>4,cz=SITE_Z>>4,x0=(cx-HALF_CHUNKS)*16,z0=(cz-HALF_CHUNKS)*16,size=HALF_CHUNKS*32;
            for(int dz=-HALF_CHUNKS;dz<HALF_CHUNKS;dz++)for(int dx=-HALF_CHUNKS;dx<HALF_CHUNKS;dx++)level.getChunk(cx+dx,cz+dz);
            Path expected=dir.resolve("reopen-expected.txt");
            if(mode.equals("prepare")){
                var litterColumns=new ArrayList<BlockPos>();BlockPos airColumn=null;
                for(int z=0;z<size;z++)for(int x=0;x<size;x++){int wx=x0+x,wz=z0+z;var pos=new BlockPos(wx,d.sample(wx,wz).height()+1,wz);var st=level.getBlockState(pos);
                    if(st.is(Blocks.LEAF_LITTER))litterColumns.add(pos);else if(st.isAir()&&airColumn==null&&d.sample(wx,wz).waterLevel()==DrainageNetwork.DRY&&level.getBlockState(pos.below()).isSolid())airColumn=pos;}
                var distinct=new TreeSet<String>();for(var p:litterColumns)distinct.add(level.getBlockState(p).toString());
                if(distinct.size()<MIN_STATES||airColumn==null)throw new AssertionError("Fixture needs varied litter and an open column: states "+distinct.size());
                var a=litterColumns.get(0);BlockPos b=null;for(var p:litterColumns)if(!level.getBlockState(p).equals(level.getBlockState(a))){b=p;break;}
                if(b==null)throw new AssertionError("No second distinct state");
                BlockState sa=level.getBlockState(a),sb=level.getBlockState(b);var c=litterColumns.get(litterColumns.size()/2);
                if(c.equals(a)||c.equals(b))c=litterColumns.get(litterColumns.size()/3);
                level.setBlock(a,sb,3);level.setBlock(b,sa,3);level.setBlock(c,Blocks.AIR.defaultBlockState(),3);level.setBlock(airColumn,Blocks.COBBLESTONE.defaultBlockState(),3);
                var out=new StringBuilder("states "+distinct.size()+"\nexchange "+a.toShortString()+" "+sb+" "+b.toShortString()+" "+sa+"\nbreak "+c.toShortString()+"\ncobble "+airColumn.toShortString()+"\n");
                out.append(snapshot(level,d,x0,z0,size));Files.writeString(expected,out.toString());
                server.saveEverything(false,true,true);
                Files.writeString(dir.resolve("reopen-result.txt"),"PASS prepare: "+distinct.size()+" distinct litter states, 4 edits saved\n");
            }else{
                var lines=Files.readAllLines(expected);var now=snapshot(level,d,x0,z0,size).split("\n");var want=lines.subList(4,lines.size());
                if(now.length!=want.size())throw new AssertionError("Window size changed");
                int diff=0;String first=null;for(int i=0;i<now.length;i++)if(!now[i].equals(want.get(i))){diff++;if(first==null)first=now[i]+" != "+want.get(i);}
                if(diff!=0)throw new AssertionError("Reopened window differs in "+diff+" columns, first "+first);
                var distinct=new TreeSet<String>();for(var l:now)if(l.contains("leaf_litter"))distinct.add(l.substring(l.indexOf(' ',l.indexOf(' ')+1)+1));
                if(distinct.size()<MIN_STATES-1)throw new AssertionError("Variants lost: "+distinct.size());
                if(!Arrays.stream(now).anyMatch(l->l.contains("cobblestone")))throw new AssertionError("Saved cobblestone edit lost");
                Files.writeString(dir.resolve("reopen-result.txt"),"PASS verify: "+now.length+" columns identical after reopen, "+distinct.size()+" distinct litter states, break/exchange/cobblestone edits retained\n");
            }
        }catch(Throwable t){Blockbranch.LOGGER.error("LEAF_LITTER_REOPEN_FAIL",t);try{Files.writeString(dir.resolve("reopen-result.txt"),"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static String snapshot(net.minecraft.server.level.ServerLevel level,DrainageNetwork d,int x0,int z0,int size){
        var b=new StringBuilder();
        for(int z=0;z<size;z++)for(int x=0;x<size;x++){int wx=x0+x,wz=z0+z;b.append(wx).append(' ').append(wz).append(' ').append(level.getBlockState(new BlockPos(wx,d.sample(wx,wz).height()+1,wz))).append('\n');}
        return b.toString();
    }
    private LeafLitterReopen(){}
}
