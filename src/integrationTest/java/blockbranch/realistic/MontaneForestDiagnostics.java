package blockbranch.realistic;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Finite attribution and structural evidence, never an Earth-area estimator. */
final class MontaneForestDiagnostics {
    static Object selectors(HabitatPalette p){
        var rows=new ArrayList<Object>();int total=0;
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true,true,true);var c=new ClimateField(seed,true);
            var before=source(p,seed,d,c,false);var after=source(p,seed,d,c,true);int changed=0,retained=0;var owners=new TreeMap<String,Integer>();var witnesses=new ArrayList<Object>();int[] admitted=new int[7],delta=new int[7];
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var a=before.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);var b=after.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);
                boolean ordinary=a==p.additional("forest")||a==p.additional("birch_forest")||a==p.land().woodland();
                if(ordinary){
                    var q=c.sample(x,z,d::sample);int elevation=d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;double rain=DrySeasonPolicy.annualModelRain(c.atmosphericSupply(x,z,d::sample));
                    boolean nominal=MontaneForest.climateSuitable(z,elevation,q.meanTemperatureC(),rain);
                    for(int i=0;i<7;i++){
                        var season=CoolDesert.season(z,q.meanTemperatureC(),i==3?14.4:i==4?17.6:16);
                        double r=rain*(i==1?.9:i==2?1.1:1);int e=elevation+(i==5?-100:i==6?100:0);
                        boolean yes=e>=800&&r>=1000&&r<=2500&&season.coldestC()<0&&season.warmMonths()>=4&&season.warmMonths()<=8;
                        if(yes)admitted[i]++;if(yes!=nominal)delta[i]++;
                    }
                }
                if(a==b){if(ordinary)retained++;continue;}
                require(ordinary&&b==p.additional(MontaneForest.KEY)&&MontaneForest.eligible(seed,x,z,d,c),"Only physically supported ordinary forest may refine");
                changed++;owners.merge(a.unwrapKey().orElseThrow().identifier().toString(),1,Integer::sum);
                var q=c.sample(x,z,d::sample);witnesses.add(Map.of("x",x,"z",z,"elevation",d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL,"relief1024m",MontaneTerrain.relief(d,x,z),"meanC",q.meanTemperatureC(),"season",CoolDesert.season(z,q.meanTemperatureC()).toString(),"modelRain",DrySeasonPolicy.annualModelRain(c.atmosphericSupply(x,z,d::sample))));
            }
            total+=changed;rows.add(Map.of("seed",seed,"samples",14400,"changed",changed,"priorOwners",owners,"retainedOrdinaryForest",retained,"sensitivityOrder","nominal,rain-.10,rain+.10,thermalGradient-.10,thermalGradient+.10,elevation-100,elevation+100","climateAdmitted",admitted,"changedClimateAdmission",delta,"exactWitnesses",witnesses));
        }
        require(total>0,"Nonvacuous three-seed refinement");return rows;
    }
    private static ScreeBiomeSource source(HabitatPalette p,long seed,DrainageNetwork d,ClimateField c,boolean montane){
        var s=new ScreeBiomeSource(p,true,true).withRedwoodForests(true).withSurfaceOwnership(true).withDryForests(true).withCoolDeserts(true).withLandIce(true).withSummerDryWoodlands(true).withWarmXeric(true).withSeasonalForests(true).withMontaneForests(montane);s.bind(seed,d,c);return s;
    }
    static int[] edge(ServerLevel l,RealisticGenerator g,int[][] points){
        var random=l.getChunkSource().randomState();var owner=g.montaneForestOwner(random);
        for(int n=0;n<3;n++){var p=points[n];for(int z=p[1]-512;z<=p[1]+512;z+=16)for(int x=p[0]-512;x<=p[0]+512;x+=16){
            if(!owner.test(x,z))continue;
            for(int[] step:new int[][]{{16,0},{0,16},{-16,0},{0,-16}}){int bx=x+step[0],bz=z+step[1];if(owner.test(bx,bz))continue;int ax=x,az=z;while(Math.abs(ax-bx)+Math.abs(az-bz)>1){int mx=Math.floorDiv(ax+bx,2),mz=Math.floorDiv(az+bz,2);if(owner.test(mx,mz)){ax=mx;az=mz;}else{bx=mx;bz=mz;}}return new int[]{ax,az};}
        }}throw new AssertionError("No owner edge within three fixed512m neighborhoods");
    }
    static Object structure(ServerLevel l,RealisticGenerator g,int[][] points,Set<Long> edited){
        var random=l.getChunkSource().randomState();var d=g.drainage(random);var owner=g.montaneForestOwner(random);var rows=new ArrayList<Object>();
        for(int n=0;n<points.length;n++){
            var p=points[n];int owned=0,canopied=0,spruce=0,other=0,api=0,minHeight=Integer.MAX_VALUE,maxHeight=0;var heights=new ArrayList<Integer>();
            var roots=MontaneForestTrees.roots(l.getSeed(),p[0]-16,p[1]-16,p[0]+31,p[1]+31,d,g.climate(random),owner);
            double nearest=Double.POSITIVE_INFINITY;for(int i=0;i<roots.size();i++)for(int j=i+1;j<roots.size();j++)nearest=Math.min(nearest,Math.hypot(roots.get(i).x()-roots.get(j).x(),roots.get(i).z()-roots.get(j).z()));
            for(var r:roots){heights.add(r.height());minHeight=Math.min(minHeight,r.height());maxHeight=Math.max(maxHeight,r.height());require(owner.test(r.x(),r.z())&&r.y()==d.sample(r.x(),r.z()).height()+1,"Physically owned grounded stems");}
            for(int z=p[1]-16;z<=p[1]+16;z++)for(int x=p[0]-16;x<=p[0]+16;x++){
                boolean selected=owner.test(x,z);if(selected)owned++;var canopy=g.canopy(x,z,random);int ground=d.sample(x,z).height();if(selected&&canopy.topY(x,z)>ground+2)canopied++;
                for(int y=ground+1;y<=ground+40;y++){var state=canopy.state(x,y,z);if(state==null||!selected)continue;if(state.is(Blocks.SPRUCE_LEAVES))spruce++;else if(state.is(net.minecraft.tags.BlockTags.LEAVES))other++;}
                if(((x-p[0])&7)!=0||((z-p[1])&7)!=0)continue;
                int wx=x,wz=z;if(edited.stream().map(BlockPos::of).anyMatch(a->a.getX()==wx&&a.getZ()==wz))continue;
                for(var bounds:List.of((net.minecraft.world.level.LevelHeightAccessor)l,net.minecraft.world.level.LevelHeightAccessor.create(ground+5,8))){
                    var column=g.getBaseColumn(x,z,bounds,random);for(int y=Math.max(ground-3,bounds.getMinY());y<=Math.min(ground+40,bounds.getMaxY());y++){require(column.getBlock(y).equals(l.getBlockState(new BlockPos(x,y,z))),"Direct base-column/native parity");api++;}
                    for(var type:Heightmap.Types.values()){
                        int expected=bounds.getMinY();if(bounds==l)expected=l.getChunk(x>>4,z>>4).getHeight(type,x&15,z&15)+1;
                        else for(int y=bounds.getMaxY();y>=bounds.getMinY();y--)if(type.isOpaque().test(l.getBlockState(new BlockPos(x,y,z)))){expected=y+1;break;}
                        require(g.getBaseHeight(x,z,type,bounds,random)==expected,"Direct base-height/native/clipped parity");api++;
                    }
                }
            }
            if(n<3)require(owned>0&&spruce>0&&roots.size()>0,"Core has actual owned needleleaf canopy and stems");
            if(n==points.length-1)require(owned>0&&owned<1089,"Native boundary has both ownership branches");
            var row=new LinkedHashMap<String,Object>();row.put("x",p[0]);row.put("z",p[1]);row.put("ownedColumns",owned);row.put("canopiedOwnedColumns",canopied);row.put("spruceLeaves",spruce);row.put("otherLeaves",other);row.put("rootsInExpanded48mWindow",roots.size());row.put("rootHeights",heights);row.put("minimumPairSpacing",Double.isFinite(nearest)?nearest:null);row.put("apiComparisons",api);row.put("relief1024m",MontaneTerrain.relief(d,p[0],p[1]));rows.add(row);
        }
        return rows;
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private MontaneForestDiagnostics(){}
}
