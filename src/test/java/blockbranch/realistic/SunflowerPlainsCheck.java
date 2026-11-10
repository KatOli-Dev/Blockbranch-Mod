package blockbranch.realistic;
import java.util.*;

/** Warm mesic composition, climate/terrain partition and ecological colony/seam regressions. */
public final class SunflowerPlainsCheck {
    public static void main(String[]args){
        for(double t:new double[]{9,22})for(double m:new double[]{.36,.55})require(SunflowerPlains.climateSuitable(new ClimateField.Sample(t,m)),"Inclusive sunflower climate");
        for(var c:List.of(new ClimateField.Sample(8.999,.4),new ClimateField.Sample(22.001,.4),new ClimateField.Sample(15,.359999),new ClimateField.Sample(15,.550001)))require(!SunflowerPlains.climateSuitable(c),"Cool,dry,hot or wetter grasslands retained");
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var field=new GroundEcology.Field(d::sample,c);var p=blockbranch.realistic.SunflowerPlainsFixtures.fixture(d,seed);var covers=new int[11];var pattern=new boolean[256*256];int neighbours=0,pairs=0,seamChanges=0,originalFlowers=0,extensions=0,edgeExtensions=0,retained=0;
            for(int z=p.z()-128;z<p.z()+128;z++)for(int x=p.x()-128;x<p.x()+128;x++){
                boolean selected=SunflowerPlains.contains(seed,x,z,d,c);int qx=Math.floorDiv(x,4)*4,qz=Math.floorDiv(z,4)*4;
                require(selected==SunflowerPlains.contains(seed,qx,qz,d,c),"World-quart seams");
                if(selected)require(Plains.potential(seed,x,z,d,c)&&!Plains.contains(seed,x,z,d,c)&&!LowlandMeadow.contains(x,z,d,c)&&!LandVegetation.reserved(d,x,z),"Exclusive grassland subset,reserved domains retained");
                var site=field.sample(x,z);int code=SunflowerPlains.cover(seed,x,z,site,true);covers[code]++;
                double original=GroundEcology.colonyStrength(seed,0x53554E464C4F5743L,x,z,40,10,23,site);
                double soil=Math.clamp(.5+site.wetness()+.25*site.retention()-.35*site.exposure(),.15,1);
                long prior=OakBirchWoodland.mix(seed^0x53554E504C414E54L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
                boolean existing=(prior>>>11)*0x1.0p-53<.65*original*soil;
                if(existing){originalFlowers++;if(code==10)retained++;}
                else if(code==10){extensions++;if(original<.35)edgeExtensions++;}
                pattern[(z-p.z()+128)*256+x-p.x()+128]=code==10;
                require(code==SunflowerPlains.cover(seed,x,z,site,true),"Seeded order-independent flowers");require(SunflowerPlains.cover(seed,x,z,site,false)==7,"Both plant heights must be clear");require(SunflowerPlains.cover(seed,x,z,site.shade(true),true)!=10,"Sunflowers require open light");
                if(Math.floorMod(x,16)==15&&code!=SunflowerPlains.cover(seed,x+1,z,field.sample(x+1,z),true))seamChanges++;
            }
            for(int z=0;z<255;z++)for(int x=0;x<255;x++)if(pattern[z*256+x]){pairs+=2;if(pattern[z*256+x+1])neighbours++;if(pattern[(z+1)*256+x])neighbours++;}
            require(covers[10]>1000&&covers[10]<16000&&covers[7]>20000&&covers[8]>5000&&covers[9]>100,"Mixed colonies and substantial grassy openings,not monoculture");require(neighbours/(double)pairs>covers[10]/65536.0*1.5,"Flower colonies measurably clustered rather than uniform scatter");require(seamChanges>0,"Flower cover does not reset at chunk boundaries");
            require(extensions>100&&retained<originalFlowers*.95,"Fresh stands reshape compact cores and add irregular edges");
            require(covers[10]>originalFlowers*.7&&covers[10]<originalFlowers*1.4,"Flower mass remains comparable while grass gaps change");
            require(edgeExtensions>extensions*.5,"Expanded stands retain irregular outer edges");
            System.out.printf("SUNFLOWER_STANDS seed=%d old=%d retained=%d added=%d edge=%d%n",seed,originalFlowers,retained,extensions,edgeExtensions);
            require(Math.floorMod(d.sample(p.x(),p.z()).height(),16)==14,"Natural paired flower crosses section seam");
            System.out.printf("SUNFLOWER_PATTERN seed=%d point=%s climate=%s province=%.4f covers=%s flowerNeighbour=%.3f globalFlower=%.3f%n",seed,p,c.sample(p.x(),p.z(),d::sample),SunflowerPlains.province(seed,p.x(),p.z()),Arrays.toString(covers),neighbours/(double)pairs,covers[10]/65536.0);
        }
        int seeded=0;for(int z=-48000;z<=48000;z+=3000)for(int x=-48000;x<=48000;x+=3000){require(Math.abs(SunflowerPlains.province(424242,x,z)-SunflowerPlains.province(424242,x+1,z))<.001,"24km province continuous across grid seams");if(SunflowerPlains.province(424242,x,z)!=SunflowerPlains.province(1,x,z))seeded++;}require(seeded>1000,"Composition provinces seeded");
        System.out.println("SUNFLOWER_CLIMATE_PARTITION_COLONIES_SEAMS_PASS");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SunflowerPlainsCheck(){}
}
