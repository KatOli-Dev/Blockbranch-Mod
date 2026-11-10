/* Historical native fixture; excluded from all current source sets.
 * Reproduce only in an isolated checkout of 09e42591858a01c25307f7a821658c482e8f280a:
 * copy this file to src/main/java/blockbranch/realistic and add the opt-in
 * FocusedHabitatSmoke case "interior_clearance" calling verify(level,generator,reload).
 * For each seed 1, 424242, -17, copy interior-routes-<seed>.json from this directory
 * into a fresh build/<case>/prior-interior.json, then run twice:
 * ./gradlew --offline --max-workers=4 -PfocusHabitat=interior_clearance
 *   -PrealisticSmokeDirectory=build/<case> -PrealisticSmokeSeed=<seed> runRealisticSmoke
 * Compare interior-clearance.json fresh/reopen; collection completion does not mean
 * all rough routes passed. These boundary-censored samples are not global cave metrics.
 * Route inputs are extracted unchanged from raw_reports[].fresh.continuousRoutes at
 * pre-cleanup commit 5e8583861dea9b33ca24ca872630ecab9441340a.
 */
package blockbranch.realistic;
import blockbranch.cubic.*;
import com.mojang.authlib.GameProfile;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
/** Read-only bounded natural-floor graph plus actual collision-driven sampled movement. */
public final class InteriorClearanceSmoke {
 static final int N=48;
 record P(int x,int y,int z){int[] a(){return new int[]{x,y,z};}double distance(P b){return Math.sqrt((x-b.x)*(x-b.x)+(y-b.y)*(y-b.y)+(z-b.z)*(z-b.z));}}
 record Edge(P a,P b){double length(){return a.distance(b);}int rise(){return b.y-a.y;}}
 public static void verify(ServerLevel level,RealisticGenerator generator,boolean reload)throws Exception {
  long seed=level.getChunkSource().randomState().seed();int[] end=seed==424242?new int[]{237076,16,-263122}:seed==1?new int[]{56384,24,-58450}:new int[]{-122692,24,180834};
  var columns=new HashMap<Long,TerrainProfile>();var floors=new LinkedHashSet<P>();
  for(int z=end[2]-N/2;z<end[2]+N/2;z++)for(int x=end[0]-N/2;x<end[0]+N/2;x++){
   int cx=x>>4,cz=z>>4;long key=(cx&0xffffffffL)|((long)cz<<32);
   var profile=columns.get(key);if(profile==null){profile=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain().base().withUnderground(new UndergroundLayer(seed,cx,cz));columns.put(key,profile);}
   for(int y=end[1]-N/2+1;y<end[1]+N/2-1;y++){
    if(profile.state(x,y,z).isAir()&&profile.state(x,y+1,z).isAir()&&!profile.state(x,y-1,z).isAir()){
     var p=new P(x,y,z);var pos=new BlockPos(x,y,z);
     // Exclude approach alterations; this sample describes the original natural interior.
     if(level.getBlockState(pos)==profile.state(x,y,z)&&level.getBlockState(pos.above())==profile.state(x,y+1,z)&&level.getBlockState(pos.below())==profile.state(x,y-1,z))floors.add(p);
    }
   }
  }
  var clearanceProbe=actor(level,new P(end[0],end[1],end[2]));int rejectedHeadroom=0;
  var edges=new ArrayList<Edge>();var graph=new HashMap<P,List<P>>();for(var p:floors)graph.put(p,new ArrayList<>());
  for(var p:floors)for(int[]d:new int[][]{{1,0},{0,1}}){P next=null;for(int delta=0;delta<=4&&next==null;delta++)for(int sign:new int[]{-1,1}){var q=new P(p.x+d[0],p.y+sign*delta,p.z+d[1]);if(floors.contains(q)){next=q;break;}}
   if(next!=null){var e=new Edge(p,next);edges.add(e);if(Math.abs(e.rise())<=1){if(clear(level,clearanceProbe,p,next)){graph.get(p).add(next);graph.get(next).add(p);}else rejectedHeadroom++;}}
  }
  var components=new ArrayList<Integer>();var visited=new HashSet<P>();
  for(var p:floors)if(visited.add(p)){var queue=new ArrayDeque<P>();queue.add(p);int count=0;while(!queue.isEmpty()){var q=queue.remove();count++;for(var n:graph.get(q))if(visited.add(n))queue.add(n);}components.add(count);}
  components.sort(Comparator.reverseOrder());
  var samples=new ArrayList<Map<String,Object>>();
  // Deterministic spread across flat, one-step, and rough 2-4m floor offsets. Both directions.
  for(int stratum=0;stratum<3;stratum++){
   final int group=stratum;var cohort=edges.stream().filter(e->group==0?e.rise()==0:group==1?Math.abs(e.rise())==1:Math.abs(e.rise())>=2).toList();
   for(int k=0;k<Math.min(12,cohort.size());k++){
    var edge=cohort.get(k*cohort.size()/Math.min(12,cohort.size()));for(boolean reverse:new boolean[]{false,true}){
     var a=reverse?edge.b:edge.a;var b=reverse?edge.a:edge.b;var result=walk(level,List.of(a,b));
     samples.add(Map.of("stratum",stratum,"from",a.a(),"to",b.a(),"length",edge.length(),"result",result));
    }
   }
  }
  // Three long shortest paths within distinct floor components; native actor is not moved after start.
  var routeResults=new ArrayList<Map<String,Object>>();var used=new HashSet<P>();
  for(int route=0;route<3;route++){
   P start=null;int largest=0;
   for(var p:floors)if(!used.contains(p)){
    var parent=bfs(graph,p);if(parent.size()>largest){largest=parent.size();start=p;}used.addAll(parent.keySet());
   }
   // After largest selection, select next components on the following pass.
   if(start==null)break;var parent=bfs(graph,start);used.clear();used.addAll(parent.keySet());
   if(route>0)break; // First largest component; choose three different distant destinations below.
   P a=parent.keySet().stream().max(Comparator.comparingDouble(start::distance)).orElseThrow();
   parent=bfs(graph,a);final P anchor=a;
   var destinations=new ArrayList<>(parent.keySet());destinations.sort(Comparator.comparingDouble((P q)->anchor.distance(q)).reversed());
   var chosen=new ArrayList<P>();
   for(var b:destinations){if(chosen.stream().anyMatch(q->q.distance(b)<8))continue;chosen.add(b);var path=new ArrayList<P>();P q=b;while(q!=null){path.add(q);q=parent.get(q);}Collections.reverse(path);
    double length=0;for(int i=1;i<path.size();i++)length+=path.get(i-1).distance(path.get(i));
    routeResults.add(Map.of("nodes",path.size(),"length",length,"path",path.stream().map(P::a).toList(),"forward",walk(level,path),"reverse",walk(level,path.reversed())));if(chosen.size()==3)break;
   }break;
  }
  double total=edges.stream().mapToDouble(Edge::length).sum(),walkable=edges.stream().filter(e->Math.abs(e.rise())<=1).mapToDouble(Edge::length).sum();
  var report=new LinkedHashMap<String,Object>();report.put("seed",seed);report.put("reload",reload);report.put("cubeSide",N);report.put("center",end);report.put("nativeChunks",columns.size());report.put("unmodifiedStandingFloorSites",floors.size());report.put("clearanceAwareFloorComponents",components);report.put("rejectedStandingBodySweeps",rejectedHeadroom);report.put("floorAdjacencyEdges",edges.size());report.put("floorAdjacencyLength",total);report.put("atMostOneStepAdjacencyLength",walkable);report.put("denominator","Sampled nearest standing-floor adjacency length, NOT passage centreline length or cave systems; 48m boundary-censored cube; rough offsets may cross barriers.");report.put("edgeTrials",samples);report.put("continuousRoutes",routeResults);
  var random=level.getChunkSource().randomState();var drainage=generator.drainage(random);
  java.util.function.BiPredicate<Integer,Integer> roots=(x,z)->{int ground=drainage.sample(x,z).height();var root=generator.canopy(x,z,random).state(x,ground+1,z);return root!=null&&!UndergroundLayer.approachMaterial(root);};
  var surveys=new ArrayList<Map<String,Object>>();int centerX=Math.floorDiv(end[0],384),centerZ=Math.floorDiv(end[2],384);
  for(int offset:new int[]{0,16}){int tested=0,eligible=0,selected=0;var sites=new ArrayList<Map<String,Object>>();int radius=offset==0?3:2;
   for(int z=-radius;z<=radius;z++)for(int x=-radius;x<=radius;x++){int cx=centerX+offset+x,cz=centerZ+offset+z;tested++;var opportunity=CaveApproach.eligibleSite(seed,cx,cz,drainage,roots);var planned=CaveApproach.plan(seed,cx,cz,drainage,roots);if(opportunity.present())eligible++;if(planned.present())selected++;if(opportunity.present()||planned.present())sites.add(Map.of("cellX",cx,"cellZ",cz,"eligible",opportunity.present(),"selected",planned.present()));}
   surveys.add(Map.of("offsetCells",offset,"testedSites",tested,"rootCheckedSupportedOpportunities",eligible,"selectedApproaches",selected,"sites",sites));
  }
  report.put("nativeRootCheckedSiteSurveys",surveys);
  var strata=new ArrayList<Map<String,Object>>();for(int group=0;group<3;group++){final int g=group;var subset=edges.stream().filter(e->g==0?e.rise()==0:g==1?Math.abs(e.rise())==1:Math.abs(e.rise())>=2).toList();strata.add(Map.of("stratum",g,"edges",subset.size(),"length",subset.stream().mapToDouble(Edge::length).sum()));}report.put("edgeStrata",strata);
  report.put("floorSitesOnSampleBoundary",floors.stream().filter(p->p.x==end[0]-N/2||p.x==end[0]+N/2-1||p.z==end[2]-N/2||p.z==end[2]+N/2-1||p.y==end[1]-N/2+1||p.y==end[1]+N/2-2).count());

  var priorPath=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).resolveSibling("prior-interior.json");
  var prior=com.google.gson.JsonParser.parseString(Files.readString(priorPath)).getAsJsonObject();var oldRoutes=new ArrayList<Map<String,Object>>();
  for(var entry:prior.getAsJsonArray("continuousRoutes")){var route=new ArrayList<P>();for(var item:entry.getAsJsonObject().getAsJsonArray("path")){var xyz=item.getAsJsonArray();route.add(new P(xyz.get(0).getAsInt(),xyz.get(1).getAsInt(),xyz.get(2).getAsInt()));}oldRoutes.add(Map.of("forward",walk(level,route),"reverse",walk(level,route.reversed())));}
  report.put("originalRouteRetests",oldRoutes);
  if(seed==1){
   int[] expected={56296,76,-58538,56328,59,-58512,56351,42,-58477,56384,24,-58450};var approach=generator.plannedApproach(Math.floorDiv(expected[0],384),Math.floorDiv(expected[2],384),random);if(!Arrays.equals(expected,approach.encode()))throw new AssertionError("Known native approach unchanged");
   int[][] pairs={{56389,15,-58436,56389,16,-58437},{56385,22,-58472,56385,23,-58471},{56387,24,-58465,56387,25,-58464}};var failures=new ArrayList<Map<String,Object>>();
   for(var pair:pairs){var a=new P(pair[0],pair[1],pair[2]);var b=new P(pair[3],pair[4],pair[5]);var blocks=new ArrayList<String>();for(int y=0;y<=4;y++)blocks.add(level.getBlockState(new BlockPos(a.x,a.y+y,a.z)).toString());var midpoint=clearanceProbe.getBoundingBox().move((a.x+b.x)/2.0+.5-clearanceProbe.getX(),Math.max(a.y,b.y)-clearanceProbe.getY(),(a.z+b.z)/2.0+.5-clearanceProbe.getZ());
    failures.add(Map.of("lower",a.a(),"higher",b.a(),"lowerColumnFootThroughFootPlus4",blocks,"raisedMidpointCollision",!level.noCollision(clearanceProbe,midpoint),"standingSweepClear",clear(level,clearanceProbe,a,b),"distanceToApproachXZ",distance(approach.encode(),a),"approachContainsLowerFeet",approach.contains(a.x,a.y,a.z),"approachContainsLowerCeiling",approach.contains(a.x,a.y+2,a.z),"up",walk(level,List.of(a,b)),"down",walk(level,List.of(b,a))));
   }report.put("bottleneckDiagnostics",failures);
  }
  var output=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).resolveSibling("interior-clearance.json");Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(report));
 }
 static Map<P,P> bfs(Map<P,List<P>> graph,P start){var parent=new LinkedHashMap<P,P>();var queue=new ArrayDeque<P>();parent.put(start,null);queue.add(start);while(!queue.isEmpty()){var q=queue.remove();for(var p:graph.get(q))if(!parent.containsKey(p)){parent.put(p,q);queue.add(p);}}return parent;}
 static boolean clear(ServerLevel level,ServerPlayer probe,P a,P b){
  // Conservative upright clearance over the higher floor. Native jumping is verified separately.
  int high=Math.max(a.y,b.y);for(int n=0;n<=8;n++){double t=n/8.0;var box=probe.getBoundingBox().move(a.x+.5+t*(b.x-a.x)-probe.getX(),high-probe.getY(),a.z+.5+t*(b.z-a.z)-probe.getZ());if(!level.noCollision(probe,box))return false;}return true;
 }
 static double distance(int[] points,P q){double nearest=Double.POSITIVE_INFINITY;for(int i=3;i<12;i+=3){double dx=points[i]-points[i-3],dz=points[i+2]-points[i-1];double t=Math.clamp(((q.x-points[i-3])*dx+(q.z-points[i-1])*dz)/(dx*dx+dz*dz),0,1);nearest=Math.min(nearest,Math.hypot(q.x-points[i-3]-t*dx,q.z-points[i-1]-t*dz));}return nearest;}
 static ServerPlayer actor(ServerLevel level,P first){var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"ClearanceWalk"),ClientInformation.createDefault());player.getAbilities().flying=false;player.getAbilities().invulnerable=true;player.noPhysics=false;player.setSpeed(.1f);player.setPos(first.x+.5,first.y,first.z+.5);player.setOnGround(true);return player;}
 static Map<String,Object> walk(ServerLevel level,List<P> route){
  var first=route.getFirst();var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"InteriorWalk"),ClientInformation.createDefault());player.getAbilities().flying=false;player.getAbilities().invulnerable=true;player.noPhysics=false;player.setSpeed(.1f);player.setPos(first.x+.5,first.y,first.z+.5);player.setOnGround(true);int ticks=0;
  if(!level.noCollision(player,player.getBoundingBox()))return Map.of("pass",false,"reason","starting collision","ticks",ticks,"reached",0);
  for(int i=1;i<route.size();i++){var target=route.get(i);boolean arrived=false;
   for(int tick=0;tick<100;tick++){
    double dx=target.x+.5-player.getX(),dz=target.z+.5-player.getZ(),distance=Math.hypot(dx,dz);
    if(distance<.23&&Math.abs(target.y-player.getY())<.12&&player.onGround()){arrived=true;break;}
    player.setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));if(target.y>player.getY()+.4&&player.onGround())player.jumpFromGround();player.travel(new Vec3(0,0,distance>.12?1:0));ticks++;
    if(!level.noCollision(player,player.getBoundingBox())||!level.getFluidState(player.blockPosition()).isEmpty())return Map.of("pass",false,"reason","collision or fluid","ticks",ticks,"reached",i-1,"position",player.position().toString());
   }
   if(!arrived)return Map.of("pass",false,"reason","bounded driver stalled","ticks",ticks,"reached",i-1,"target",target.a(),"position",player.position().toString());
  }
  return Map.of("pass",true,"ticks",ticks,"reached",route.size()-1);
 }
}
