package blockbranch.realistic;

/** Positive candidate plus exact legacy, protected-domain and query-order controls. */
public final class SandyCoastCheck {
 public static void main(String[]args){
  for(long seed:new long[]{424242,1,-17}){
   var shape=new TerrainShape(seed);var old=new DrainageNetwork(shape,seed);var d=new DrainageNetwork(shape,seed,true,true);var c=new ClimateField(seed);
   for(boolean snow:new boolean[]{false,true}){
    var scene=CoastalBeachScenes.forWorld(seed,snow,d,c);int changed=0,flat=0,wasFlat=0,maxStep=0;
    for(int z=scene.z()-128;z<=scene.z()+128;z+=4)for(int x=scene.x()-128;x<=scene.x()+128;x+=4){
     var a=old.sample(x,z);var b=d.sample(x,z);int delta=b.height()-a.height();
     require(delta>=0&&delta<=4,"Low bounded depositional relief");
     require(a.basin()==b.basin()&&a.closed()==b.closed()&&a.regionalHeight()==b.regionalHeight()&&a.regionalWaterLevel()==b.regionalWaterLevel(),"Drainage graph and macroclimate references retained");
     if(a.bedrockCoast()||a.closed()||old.channelAt(x,z).edge()>=0)require(a.equals(b),"Approved rock, closed basins and river channel unchanged");
     if(a.waterLevel()!=b.waterLevel())require(a.waterLevel()==64&&b.waterLevel()==DrainageNetwork.DRY&&b.height()>=63,"Only local marine filling; no new water/datum change");
     if(delta!=0)changed++;if(a.height()==63)wasFlat++;if(b.height()==63)flat++;
     maxStep=Math.max(maxStep,Math.abs(b.height()-d.sample(x+1,z).height()));maxStep=Math.max(maxStep,Math.abs(b.height()-d.sample(x,z+1).height()));
     if((x&31)==0&&(z&31)==0)require(c.sample(x,z,old::sample).equals(c.sample(x,z,d::sample)),"Exact climate preservation");
    }
    require(changed>50&&flat<wasFlat&&maxStep<=1,"Break the datum plateau with gentle continuous voxel slopes");
    var t=CoastalBeachScenes.transect(scene,d,c,0);require(t.width()>=48&&t.width()<200,"Bounded real beach width");
    System.out.println("SANDY_POSITIVE seed="+seed+" snow="+snow+" scene="+scene+" changed="+changed+" oldFlat="+wasFlat+" newFlat="+flat+" maxNeighbourStep="+maxStep+" width="+t.width());
    if(seed==424242&&snow){
     var ecology=new GroundEcology.Field(d::sample,c);var layers=new java.util.TreeSet<Integer>();
     for(int z=scene.z()-512;z<scene.z()+512;z+=8)for(int x=scene.x()-512;x<scene.x()+512;x+=8)
      if(CoastalBeach.surfaceContains(x,z,d,c,true))layers.add(OpenSnowCover.beachLayers(seed,x,z,ecology.sample(x,z)));
     require(layers.equals(java.util.Set.of(1,2,3)),"Regional thin snow variation retained; individual small windows may be uniform");
     System.out.println("SANDY_REGIONAL_SNOW_VARIATION_PASS layers="+layers);
    }
    for(boolean elevated:new boolean[]{false,true})System.out.println("SANDY_CAMERA "+CoastalBeachScenes.camera(scene,d,c,elevated));
    // Repeat in reverse order in a fresh cache; no chunk or request-order state.
    var reverse=new DrainageNetwork(shape,seed,true,true);for(int z=scene.z()+32;z>=scene.z()-32;z--)for(int x=scene.x()+32;x>=scene.x()-32;x--)require(d.sample(x,z).equals(reverse.sample(x,z)),"Exact cache/seam/order agreement");
   }
   int untouched=0;for(int z=-1900000;z<=1900000;z+=32000)for(int x=-1900000;x<=1900000;x+=32000){var a=old.sample(x,z);var b=d.sample(x,z);if(a.closed()||a.height()<61||a.height()>67||a.bedrockCoast()){require(a.equals(b),"Unrelated inland/deep/wetland/rock exact preservation");untouched++;}}
   var rock=blockbranch.realistic.StonyShoreFixtures.fixture(old,seed);int protectedRock=0;
   for(int z=rock.z()-128;z<=rock.z()+128;z+=8)for(int x=rock.x()-128;x<=rock.x()+128;x+=8){
    require(old.sample(x,z).equals(d.sample(x,z)),"Approved headland and surrounding cove retain exact samples");protectedRock++;
   }
   int mouths=0,protectedRiver=0;
   for(int i=0;i<old.size()&&mouths<3;i++)if(old.river(i)&&old.waterLevel(i)==TerrainShape.SEA_LEVEL){
    int x=(int)Math.round(old.edgeX(i,.5)),z=(int)Math.round(old.edgeZ(i,.5));
    for(int dz=-64;dz<=64;dz+=16)for(int dx=-64;dx<=64;dx+=16){
     require(old.sample(x+dx,z+dz).equals(d.sample(x+dx,z+dz)),"River mouth and established dry banks retain exact samples");protectedRiver++;
    }mouths++;
   }
   require(mouths>0,"Actual marine river outlets exercised");
   System.out.println("SANDY_PROTECTED_DOMAINS seed="+seed+" headlandCells="+protectedRock+" riverMouths="+mouths+" riverCells="+protectedRiver);
   System.out.println("SANDY_GLOBAL_PRESERVED seed="+seed+" points="+untouched);
  }
  System.out.println("SANDY_BOUNDED_PROFILES_WIDTH_SEAMS_PRESERVATION_PASS");
 }
 static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
