package blockbranch.realistic;
import java.util.*;

/** Irregular spacing, ecological cohorts, canopy-aware colonies and native-rectangle determinism. */
public final class WoodlandFoliageCheck {
    public static void main(String[]args){
        for(long seed:new long[]{424242,1,-17}){
            var points=TreeScatter.points(seed,-256,-256,255,255);int[] phase10=new int[10],phase32=new int[32];int young=0,wet=0,dry=0;
            var moist=new GroundEcology.Site(.78,.6,.05,.12,0,false);var exposed=new GroundEcology.Site(.3,-.7,.9,.12,0,false);
            for(int i=0;i<points.size();i++){
                var p=points.get(i);phase10[Math.floorMod(p.x(),10)]++;phase32[Math.floorMod(p.x(),32)]++;if(p.young())young++;
                if(TreeScatter.establishes(seed,p,0,moist))wet++;if(TreeScatter.establishes(seed,p,0,exposed))dry++;
                for(int j=i+1;j<points.size();j++){var q=points.get(j);require(Math.hypot(p.x()-q.x(),p.z()-q.z())+1e-9>=p.clearance()+q.clearance(),"Stem/cohort exclusion distances hold across cell and native chunk borders");}
            }
            require(points.size()>100&&young>30&&young<points.size()-30,"Both candidate size cohorts present");
            for(int n:phase10)require(n>0,"No forbidden legacy 10 m stem phases");for(int n:phase32)require(n>0,"No anchor rows within sampling cells");
            require(wet>dry*1.15,"Soil moisture/shelter affect establishment density");
            var rectangle=TreeScatter.points(seed,-35,-17,31,47);var expected=points.stream().filter(p->p.x()>=-35&&p.x()<=31&&p.z()>=-17&&p.z()<=47).toList();require(rectangle.equals(expected),"Rectangle/chunk order never changes accepted stems");
            var fungal=new int[3];var sunnyFungal=new int[3];var wetFern=new int[3];var dryFern=new int[3];var berry=new int[2];
            int[][] components={new int[9],new int[BorealSurface.LAST+1],new int[9]};
            for(int z=-96;z<96;z++)for(int x=-96;x<96;x++)for(int family=0;family<3;family++){
                var shade=moist.shade(true);int a=GroundEcology.forest(seed,x,z,family,shade,true),b=GroundEcology.forest(seed,x,z,family,exposed,true);
                require(a>=0&&a<components[family].length&&b>=0&&b<components[family].length,"Forest cover fits its family catalog: family="+family+" shaded="+a+" exposed="+b);
                components[family][a]++;
                int fungus=family==2?6:5,fern=family==2?5:4;if(a==fungus)fungal[family]++;if(b==fungus)sunnyFungal[family]++;if(a==fern)wetFern[family]++;if(b==fern)dryFern[family]++;
                int occupied=GroundEcology.forest(seed,x,z,family,shade,false);
                // Role IDs do not order substrate before plants: boreal soil is 15, carpet is 16.
                boolean substrateOnly=switch(family){
                    case 0->occupied==1||occupied==2||occupied==3;
                    case 1->occupied==1||occupied==BorealSurface.SOIL;
                    case 2->occupied==7;
                    default->throw new AssertionError("Unknown forest family "+family);
                };
                require(substrateOnly,"No understory plant competes with a low stem/crown voxel: family="+family+" cover="+occupied);
                if(family==1){if(a==7)berry[0]++;if(GroundEcology.forest(seed,x,z,1,moist,true)==7)berry[1]++;}
            }
            for(int family=0;family<3;family++){require(fungal[family]>5&&sunnyFungal[family]==0,"Fungi occupy shaded moist colonies");require(wetFern[family]>dryFern[family]*3,"Ferns favour sheltered humid canopy");}
            require(berry[1]>berry[0]*3,"Berry colonies favour canopy openings");
            System.out.printf("WOODLAND_FOLIAGE_PATTERN seed=%d stems=%d young=%d wetEstablished=%d dryEstablished=%d oldPhase10=%s phase32=%s shadeFungi=%s moistFerns=%s dryFerns=%s berriesShadeOpen=%s%n",seed,points.size(),young,wet,dry,Arrays.toString(phase10),Arrays.toString(phase32),Arrays.toString(fungal),Arrays.toString(wetFern),Arrays.toString(dryFern),Arrays.toString(berry));
        }
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var p=blockbranch.realistic.OakBirchWoodlandFixtures.fixture(d,seed);var roots=WoodlandTrees.roots(seed,p.x()-128,p.z()-128,p.x()+128,p.z()+128,d,c);
            require(roots.stream().anyMatch(r->r.height()<=12)&&roots.stream().anyMatch(r->r.height()>=18),"Actual woodland includes young and taller mature cohorts");
            for(var r:roots){require(!LandVegetation.reserved(d,r.x(),r.z())&&d.sample(r.x(),r.z()).height()+1==r.y(),"Actual rooted stems preserve terrain and reserved domains");}
            System.out.printf("WOODLAND_FOLIAGE_ACTUAL seed=%d fixture=%s roots=%d heightRange=%d..%d%n",seed,p,roots.size(),roots.stream().mapToInt(WoodlandTrees.Root::height).min().orElseThrow(),roots.stream().mapToInt(WoodlandTrees.Root::height).max().orElseThrow());
        }
        System.out.println("IRREGULAR_WOODLAND_COHORT_UNDERSTORY_PASS");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private WoodlandFoliageCheck(){}
}
