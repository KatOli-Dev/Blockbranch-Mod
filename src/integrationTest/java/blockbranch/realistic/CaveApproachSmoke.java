package blockbranch.realistic;

import blockbranch.cubic.*;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Native FULL generation, immutable snapshots, edits and driven real-player movement. */
public final class CaveApproachSmoke {
    public static void verify(ServerLevel level,RealisticGenerator generator,boolean reload)throws Exception{
        var random=level.getChunkSource().randomState();var origin=Boolean.getBoolean("blockbranch.litCaveRequireMoss")?RealisticSpawn.origin(level,generator,random):generator.getOrigin(random);
        CaveApproach approach=CaveApproach.NONE;long planStart=System.nanoTime();int sites=0;
        outer:for(int z=-3;z<=3;z++)for(int x=-3;x<=3;x++){
            sites++;var candidate=generator.plannedApproach(Math.floorDiv(origin.getMinBlockX(),384)+x,Math.floorDiv(origin.getMinBlockZ(),384)+z,random);
            if(candidate.present()&&(!Boolean.getBoolean("blockbranch.litCaveRequireMoss")||mossWitness(generator,candidate,random))){approach=candidate;break outer;}
        }
        require(approach.present(),"Bounded native-compatible approach fixture");long planNs=System.nanoTime()-planStart;
        require(!generator.entryRoutesEnabled()||approach.entry().present(),"Enabled approach reaches a natural chamber");
        var route=approach.walkingRoute(generator.drainage(random),new UndergroundStructure(random.seed()));require(route.size()>40,"Supported planned route");
        var chunks=new LinkedHashSet<net.minecraft.world.level.ChunkPos>();
        for(var p:route)for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)chunks.add(new net.minecraft.world.level.ChunkPos((p[0]+dx)>>4,(p[2]+dz)>>4));
        long start=System.nanoTime();for(var c:chunks)level.getChunk(c.x(),c.z());long generationNs=System.nanoTime()-start;
        var witnessArray=approach.entry().present()?approach.entry().route().get(approach.entry().route().size()/2):route.get(route.size()/2);var witness=new BlockPos(witnessArray[0],witnessArray[1],witnessArray[2]);
        var home=level.getChunkAt(witness);var base=((SparseColumn)home).blockbranch$terrain().base();
        if(reload)require(level.getBlockState(witness).is(Blocks.COBBLESTONE),"Approach witness persisted through complete reload");
        level.setBlock(witness,base.state(witness.getX(),witness.getY(),witness.getZ()),2);
        var factory=level.palettedContainerFactory();int changes=0,profileChecks=0,unsupportedPlants=0;
        for(var c:chunks){
            var p=((SparseColumn)level.getChunk(c.x(),c.z())).blockbranch$terrain().base();var encoded=p.encode(factory);var decoded=TerrainProfile.decode(encoded,factory);
            require(p.ores().enabled()==(generator.oreDefinition()>0),"Saved generator preserves ore opt-in without retrofit");
            var geometry=p.withLitCaves(LitCaveLayer.NONE).withDripstone(DripstoneLayer.NONE);var frozen=p.withUnderground(new UndergroundLayer(random.seed(),c.x(),c.z()));
            for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                int wx=c.getMinBlockX()+x,wz=c.getMinBlockZ()+z;
                int ground=p.groundHeight(x,z);
                if(p.state(x,ground,z).isAir()&&!frozen.state(x,ground,z).isAir())for(int y=ground+1;y<=ground+3;y++){
                    var above=p.state(x,y,z);
                    if(!above.isAir()&&!above.canSurvive(level,new BlockPos(wx,y,wz)))unsupportedPlants++;
                }
                for(int y=approach.minY()-2;y<=p.groundHeight(x,z)+4;y++){
                    var value=p.state(x,y,z);require(value==decoded.state(x,y,z),"Approach snapshot agrees");
                    var old=frozen.state(x,y,z);
                    if(geometry.state(x,y,z)!=old){require(generator.approachesEnabled()&&approach.clears(wx,y,wz,p.groundHeight(x,z))&&value.isAir()&&!old.isAir(),"Only explicit additive entrance/entry-route/vegetation clearance");changes++;}
                    require(level.getBlockState(new BlockPos(wx,y,wz))==value,"FULL native approach state agrees");profileChecks++;
                }
            }
        }
        int movementTicks=0;
        if(generator.approachesEnabled()){
            require(unsupportedPlants==0,"No unsupported vegetation above carved surface: "+unsupportedPlants);
            require(changes>0,"Approach enabled in actual generated blocks");require(base.underground().hasApproach(),"Versioned descriptor present");
            require(base.underground().encode().getIntOr("version",0)==(generator.entryRoutesEnabled()?3:2),"Approach descriptor has its own version");
            if(approach.entry().present()){
                var target=approach.entry().route().getLast();int open=0,exits=0;
                for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int dy:new int[]{0,-1,1}){
                    var pos=new BlockPos(target[0]+dx,target[1]+dy,target[2]+dz);
                    if(level.getBlockState(pos).isAir()&&level.getBlockState(pos.above()).isAir()&&!level.getBlockState(pos.below()).isAir()){open++;if(Math.abs(dx)+Math.abs(dz)==1)exits++;break;}
                }
                require(open>=7||exits>=3,"Actual terminal chamber floor or three-way junction");
            }
            if(approach.entry().present()){
                var badEntry=base.encode(factory);badEntry.getCompoundOrEmpty("underground").remove("entry_route");reject(badEntry,factory);
                badEntry=base.encode(factory);badEntry.getCompoundOrEmpty("underground").putIntArray("entry_route",new int[]{1,2,3});reject(badEntry,factory);
                badEntry=base.encode(factory);badEntry.getCompoundOrEmpty("underground").putInt("version",2);reject(badEntry,factory);
            }
            var bad=base.encode(factory);bad.getCompoundOrEmpty("underground").remove("approach");reject(bad,factory);
            bad=base.encode(factory);bad.getCompoundOrEmpty("underground").putIntArray("approach",new int[]{1,2,3});reject(bad,factory);
            bad=base.encode(factory);var water=bad.getIntArray("water").orElseThrow();int index=(witness.getZ()&15)*16+(witness.getX()&15);
            water[index]=base.groundHeight(witness.getX(),witness.getZ())+3;bad.putIntArray("water",water);
            // Isolate water exclusion from unrelated dry vegetation constraints.
            for(String key:new String[]{"scree","reedbed","river_margin","sandy_shore","meadow","woodland","steppe","boreal","warm","cold_steppe","desert"}){
                var cover=bad.getByteArray(key).orElseThrow();cover[index]=0;bad.putByteArray(key,cover);
            }
            bad.putIntArray("canopy",new int[0]);
            require(reject(bad,factory).contains("source water"),"Wet-column approach rejected at water safety boundary");
            movementTicks=walk(level,route,false)+walk(level,route,true);
            for(int k=0;k<route.size();k+=8){var p=route.get(k);for(var type:Heightmap.Types.values()){
                var chunk=level.getChunk(p[0]>>4,p[2]>>4);var profile=((SparseColumn)chunk).blockbranch$terrain().base();int expected=Integer.MIN_VALUE;
                for(int y=profile.groundHeight(p[0],p[2])+56;y>=approach.minY()-8;y--)if(type.isOpaque().test(profile.state(p[0],y,p[2]))){expected=y+1;break;}
                require(expected!=Integer.MIN_VALUE&&level.getHeight(type,p[0],p[2])==expected,"Native approach heightmap "+type);
            }}
        }else require(changes==0,"Disabled approach preserves frozen natural profile");
        level.setBlock(witness,Blocks.COBBLESTONE.defaultBlockState(),2);var terrain=((SparseColumn)home).blockbranch$terrain();terrain.flush();terrain.evictOutside(20,21);
        require(level.getBlockState(witness).is(Blocks.COBBLESTONE),"Approach edit survives eviction");home.markUnsaved();
        var result=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).resolveSibling("approach-evidence.txt");
        Files.writeString(result,"enabled="+generator.approachesEnabled()+"\nreload="+reload+"\nseed="+random.seed()+"\nplanning_sites="+sites+"\nplanning_ns="+planNs+"\ngeneration_ns="+generationNs+"\nchunks="+chunks.size()+"\nchanges="+changes+"\nprofile_checks="+profileChecks+"\nmovement_ticks="+movementTicks+"\npoints="+Arrays.toString(approach.encode())+"\nentry_points="+Arrays.toString(approach.entry().encode())+"\nwitness="+witness+"\n");
        blockbranch.Blockbranch.LOGGER.info("CAVE_APPROACH_NATIVE_PASS enabled={} reload={} changes={} movementTicks={} chunks={} generationNs={}",generator.approachesEnabled(),reload,changes,movementTicks,chunks.size(),generationNs);
    }
    private static int walk(ServerLevel level,List<int[]> original,boolean reverse){
        var route=new ArrayList<>(original);if(reverse)Collections.reverse(route);var first=route.getFirst();
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"CaveWalker"),ClientInformation.createDefault());
        player.getAbilities().flying=false;player.getAbilities().instabuild=false;player.getAbilities().invulnerable=true;player.noPhysics=false;player.setSpeed(.1f);
        player.setPos(first[0]+.5,first[1],first[2]+.5);player.setOnGround(true);int ticks=0;
        require(level.noCollision(player,player.getBoundingBox()),"Real player starts clear");
        for(int i=1;i<route.size();i++){
            var target=route.get(i);boolean arrived=false;
            for(int tick=0;tick<100;tick++){
                double dx=target[0]+.5-player.getX(),dz=target[2]+.5-player.getZ();double distance=Math.hypot(dx,dz);
                if(distance<.23&&Math.abs(target[1]-player.getY())<.12&&player.onGround()){arrived=true;break;}
                player.setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));
                if(target[1]>player.getY()+.4&&player.onGround())player.jumpFromGround();
                player.travel(new Vec3(0,0,distance>.12?1:0));ticks++;
                require(level.noCollision(player,player.getBoundingBox()),"Native collision remains clear");
                require(!player.isInWater()&&!player.getAbilities().flying&&!player.noPhysics,"Walking, no flight/water/no-clip");
                require(level.getFluidState(player.blockPosition()).isEmpty(),"Actual native player position has no fluid");
            }
            require(arrived,"Native player traversal stalled reverse="+reverse+" index="+i+" target="+Arrays.toString(target)+" position="+player.position()+" ground="+player.onGround());
        }
        return ticks;
    }
    /** Only the isolated capture copy: remove the deliberate persistence witness. */
    public static void restoreReviewWitness(ServerLevel level,RealisticGenerator generator,CaveApproach approach){
        var r=level.getChunkSource().randomState();var route=approach.route(generator.drainage(r),new UndergroundStructure(r.seed()));
        require(!route.isEmpty(),"Review route valid");var a=approach.entry().present()?approach.entry().route().get(approach.entry().route().size()/2):route.get(route.size()/2);var p=new BlockPos(a[0],a[1],a[2]);
        var base=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain().base();
        level.setBlock(p,base.state(a[0],a[1],a[2]),3);
    }
    private static String reject(net.minecraft.nbt.CompoundTag tag,net.minecraft.world.level.chunk.PalettedContainerFactory f){String message=null;try{TerrainProfile.decode(tag,f);}catch(IllegalArgumentException expected){message=expected.getMessage();}require(message!=null,"Malformed approach snapshot rejected");return message;}
    private static boolean mossWitness(RealisticGenerator generator,CaveApproach candidate,net.minecraft.world.level.levelgen.RandomState random){
        var chunks=new LinkedHashSet<net.minecraft.world.level.ChunkPos>();
        for(var point:candidate.walkingRoute(generator.drainage(random),new UndergroundStructure(random.seed())))for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)chunks.add(new net.minecraft.world.level.ChunkPos((point[0]+dx)>>4,(point[2]+dz)>>4));
        int count=0;for(var c:chunks){count+=generator.independentLitCavePlan(c.x(),c.z(),random).placements().length/2;if(count>=2)return true;}return false;
    }
    private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
