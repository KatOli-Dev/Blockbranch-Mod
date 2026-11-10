package blockbranch.realistic;

import static blockbranch.realistic.SnowySlopes.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class SnowySlopesFixtures {
    static Point fixture(DrainageNetwork d,long seed){
  var c=new ClimateField(seed);var e=new GroundEcology.Field(d::sample,c);
  for(int z=-1900000;z<=1900000;z+=8000)for(int x=-1900000;x<=1900000;x+=8000){
   if(!contains(x,z,d,c))continue;
   // Resolve the same128m interior requirement around a regional candidate;
   // coherent new gullies can move it between the old8km search points.
   for(int radius:new int[]{0,64,128,256})for(int dz=-radius;dz<=radius;dz+=64)for(int dx=-radius;dx<=radius;dx+=64){
    if(radius>0&&Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
    int px=x+dx,pz=z+dz;if(!contains(px,pz,d,c)||!AlpineScree.contains(px,pz,d::sample))continue;
    boolean interior=true;outer:for(int oz=-64;oz<=64;oz+=32)for(int ox=-64;ox<=64;ox+=32)if(!contains(px+ox,pz+oz,d,c)){interior=false;break outer;}
    if(!interior)continue;
    for(int oz=-24;oz<=24;oz+=4)for(int ox=-24;ox<=24;ox+=4){int wx=px+ox,wz=pz+oz;if(AlpineScree.depth(seed,wx,wz,AlpineScree.contains(wx,wz,d::sample))>0&&OpenSnowCover.layers(seed,wx,wz,e.sample(wx,wz),true)>=3)return new Point(wx,wz);}
   }
  }throw new AssertionError("No natural sheltered snowy debris-slope128m interior");
 }
    private SnowySlopesFixtures() {}
}
