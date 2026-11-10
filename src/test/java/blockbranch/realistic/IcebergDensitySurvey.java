package blockbranch.realistic;

import java.util.stream.Collectors;
import java.nio.file.*;
import java.util.*;

/** Read-only bounded survey of the final placement decision, never fixture-selected. */
public final class IcebergDensitySurvey {
 private static String json(Object v){
  if(v instanceof String s)return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";
  if(v instanceof Map<?,?> m)return m.entrySet().stream().map(e->json(e.getKey())+":"+json(e.getValue())).collect(Collectors.joining(",","{","}"));
  if(v instanceof Collection<?> c)return c.stream().map(IcebergDensitySurvey::json).collect(Collectors.joining(",","[","]"));
  if(v instanceof Number||v instanceof Boolean)return v.toString();throw new IllegalArgumentException("JSON type");
 }
 public static void main(String[]args)throws Exception{
  var method=IcebergField.class.getDeclaredMethod("berg",int.class,int.class);method.setAccessible(true);
  var root=new LinkedHashMap<String,Object>();root.put("reference_generation","23007be / 08dd358 generation; iceberg field/shape unchanged; sandy coast disabled");
  root.put("selection","First eligible fixed-grid anchor in each hemisphere per seed; no fixture, size, neighbour-count or acceptance-density criterion. Geographically stratified convenience sample, not global random sampling.");
  root.put("area_definition","Centre counts within exact 2560m square. Eligible area estimated by 64m cell-centre habitat samples, not exact block area. Bodies may extend across the survey rectangle.");
  var rows=new ArrayList<Object>();
  for(long seed:new long[]{424242,1,-17}){
   var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var field=new IcebergField(seed,d,c);
   for(int sign:new int[]{-1,1}){
    int ax=0,az=0;boolean found=false;
    search:for(int latitude:new int[]{1966080,1835008,1703936})for(int x=-1835008;x<=1835008;x+=262144){int z=sign*latitude;if(IcebergField.habitat(seed,x,z,d,c)){ax=x;az=z;found=true;break search;}}
    if(!found)throw new AssertionError("No eligible systematic anchor seed="+seed+" hemisphere="+sign);
    int gx=Math.floorDiv(ax,320)-4,gz=Math.floorDiv(az,320)-4,minX=gx*320,minZ=gz*320,maxX=minX+2560,maxZ=minZ+2560;
    int eligible=0;for(int z=minZ+32;z<maxZ;z+=64)for(int x=minX+32;x<maxX;x+=64)if(IcebergField.habitat(seed,x,z,d,c))eligible++;
    var centres=new ArrayList<IcebergField.Berg>();var points=new ArrayList<Object>();
    for(int z=gz;z<gz+8;z++)for(int x=gx;x<gx+8;x++){var b=(IcebergField.Berg)method.invoke(field,x,z);if(b!=null){centres.add(b);var p=new ArrayList<Object>();p.add(b.x());p.add(b.z());p.add(b.shape().type);p.add(b.shape().freeboard);p.add(b.shape().draft);points.add(p);}}
    var nn=new ArrayList<Double>();for(var b:centres){if(b.x()-minX<640||maxX-b.x()<640||b.z()-minZ<640||maxZ-b.z()<640)continue;double nearest=Double.POSITIVE_INFINITY;for(var q:centres)if(q!=b)nearest=Math.min(nearest,Math.hypot(q.x()-b.x(),q.z()-b.z()));if(nearest<640)nn.add(nearest);}
    Collections.sort(nn);var row=new LinkedHashMap<String,Object>();row.put("seed",seed);row.put("hemisphere",sign);row.put("min_x",minX);row.put("min_z",minZ);row.put("max_x_exclusive",maxX);row.put("max_z_exclusive",maxZ);row.put("candidate_cells",64);row.put("area_km2",6.5536);row.put("eligible_samples",eligible);row.put("habitat_sample_count",1600);double eligibleArea=eligible*.004096;row.put("estimated_eligible_area_km2",eligibleArea);row.put("final_accepted_centres",centres.size());row.put("realized_centres_per_rectangle_km2",centres.size()/6.5536);row.put("centres_per_estimated_eligible_km2",eligibleArea>0?centres.size()/eligibleArea:0);row.put("centres_x_z_type_freeboard_draft",points);row.put("nn_uncensored_interior_count",nn.size());if(!nn.isEmpty()){row.put("nn_min_m",nn.getFirst());row.put("nn_median_m",nn.get(nn.size()/2));row.put("nn_max_m",nn.getLast());}rows.add(row);
    System.out.println("ICEBERG_REALIZED seed="+seed+" sign="+sign+" rect="+minX+","+minZ+" count="+centres.size()+" eligible="+eligible+"/1600 densityRect="+(centres.size()/6.5536)+" densityEligibleEstimate="+(centres.size()/eligibleArea)+" interiorNN="+nn);
   }
  }
  root.put("regions",rows);root.put("nearest_neighbour_note","Only centres at least640m from rectangle boundary with another accepted centre closer than640m are reported, avoiding boundary censoring; not all centres contribute.");
  Path out=Path.of("/workspace/scratch/iceberg-realized-density.json");Files.writeString(out,json(root));System.out.println("ICEBERG_REALIZED_DENSITY_SURVEY_PASS "+out);
 }
}
