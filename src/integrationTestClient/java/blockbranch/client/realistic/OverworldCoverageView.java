package blockbranch.client.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;

/** One unattended batch of actual native renders; no modified structure templates. */
public final class OverworldCoverageView {
    static final String[] SITES=System.getProperty("blockbranch.overworldViewSites","village_plains,village_taiga,abandoned_camp_swamp,monument,ancient_city,trial_chambers").split(",");
    static boolean opened,saving;static volatile boolean posed;static volatile int index;static int settled;
    static volatile BlockPos anchor;static long began;static List<Object> reports=new ArrayList<>();
    static Path dir(){return Path.of(System.getProperty("blockbranch.overworldViewDir"));}
    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.isDedicatedServer()||posed||server.getPlayerList().getPlayers().isEmpty())return;
            try{
                var site=SITES[index];var all=com.google.gson.JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.overworldViewWitness")))).getAsJsonObject();
                var entry=all.getAsJsonObject(site);var at=entry.getAsJsonArray("chunk");var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Identifier.withDefaultNamespace(site)));
                var chunk=level.getChunk(at.get(0).getAsInt(),at.get(1).getAsInt());var start=level.structureManager().getStartForStructure(holder.value(),chunk);
                if(start==null||!start.isValid())throw new AssertionError("Actual native visual start "+site);
                var b=start.getBoundingBox();for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)level.getChunk(x,z);
                b=StructurePiece.createBoundingBox(start.getPieces().stream());
                var aim=b.getCenter();double x=aim.getX()-Math.min(35,b.getXSpan()*.65+10),z=aim.getZ()+Math.min(35,b.getZSpan()*.65+10),y=b.maxY()+Math.min(30,b.getXSpan()*.25+6);
                if(site.equals("monument")){x=b.minX()-10;z=b.minZ()+20;y=57;aim=new BlockPos(b.minX()+12,48,b.minZ()+22);}
                if(site.startsWith("abandoned_camp")){x=aim.getX()+12;z=aim.getZ()+12;y=b.maxY()+28;}
                if(site.equals("ancient_city")||site.equals("trial_chambers")){
                    var piece=start.getPieces().getFirst().getBoundingBox();BlockPos found=null;double best=-1;
                    for(int wz=piece.minZ();wz<=piece.maxZ();wz++)for(int wx=piece.minX();wx<=piece.maxX();wx++)for(int wy=piece.minY()+2;wy<=piece.maxY()-2;wy++){
                        if(!level.getBlockState(new BlockPos(wx,wy,wz)).isAir()||!level.getBlockState(new BlockPos(wx,wy+1,wz)).isAir())continue;
                        int open=0;for(var d:Direction.Plane.HORIZONTAL){for(int range=1;range<=7;range++){if(!level.getBlockState(new BlockPos(wx,wy,wz).relative(d,range)).isAir())break;open++;}}
                        double score=open-.02*Math.abs(wy-piece.getCenter().getY());if(score>best){best=score;found=new BlockPos(wx,wy,wz);}
                    }
                    if(found==null)throw new AssertionError("Walkable native inspection camera "+site);
                    x=found.getX()+.5;y=found.getY()-player.getEyeHeight()+.5;z=found.getZ()+.5;
                    aim=new BlockPos((int)x+8,found.getY(),(int)z);if(site.equals("ancient_city"))aim=piece.getCenter().above(2);
                }
                anchor=aim;var source=server.createCommandSourceStack();var commands=server.getCommands();String name=player.getName().getString();
                commands.performPrefixedCommand(source,"time set 6000");commands.performPrefixedCommand(source,"weather clear");commands.performPrefixedCommand(source,"gamemode spectator "+name);
                commands.performPrefixedCommand(source,"effect give "+name+" minecraft:night_vision 100000 0 true");
                double dx=aim.getX()+.5-x,dz=aim.getZ()+.5-z,dy=aim.getY()+.5-y-player.getEyeHeight();
                commands.performPrefixedCommand(source,String.format(Locale.ROOT,"tp %s %.3f %.3f %.3f %.3f %.3f",name,x,y,z,Math.toDegrees(Math.atan2(-dx,dz)),Math.toDegrees(Math.atan2(-dy,Math.hypot(dx,dz)))));
                reports.add(Map.of("site",site,"camera",List.of(x,y,z),"aim",aim.toShortString(),"bounds",b.toString()));posed=true;
                blockbranch.Blockbranch.LOGGER.info("OVERWORLD_VIEW_POSED {}",site);
            }catch(Throwable t){fail(t);}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{try{
            if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;began=System.nanoTime();client.createWorldOpenFlows().openWorld("OverworldCoverage",()->client.gui.setScreen(new TitleScreen()));return;}
            if(opened&&System.nanoTime()-began>900_000_000_000L)throw new IllegalStateException("Batch render timeout");
            if(Files.exists(dir().resolve("result.txt"))){client.stop();return;}
            if(!posed||saving||client.level==null||client.player==null)return;
            client.options.fov().set(75);if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();
            var area=client.levelRenderer.viewArea();if(area==null)return;var section=area.getRenderSectionAt(anchor);
            if(section==null||!section.getSectionMesh().hasRenderableLayers()){settled=0;return;}
            if(++settled<160)return;saving=true;
            net.minecraft.client.Screenshot.grab(client.gameDirectory,"coverage-"+SITES[index]+".png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{
                if(++index<SITES.length){posed=false;saving=false;settled=0;}else{Files.writeString(dir().resolve("views.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(reports));Files.writeString(dir().resolve("result.txt"),"PASS "+SITES.length+" native structure renders");client.stop();}
            }catch(Throwable t){fail(t);}}));
        }catch(Throwable t){fail(t);client.stop();}});
    }
    static void fail(Throwable t){blockbranch.Blockbranch.LOGGER.error("OVERWORLD_VIEW_FAIL",t);try{Files.writeString(dir().resolve("result.txt"),"FAIL "+t);}catch(Exception ignored){}}
}
