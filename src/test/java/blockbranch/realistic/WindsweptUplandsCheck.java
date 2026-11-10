package blockbranch.realistic;
import java.util.*;

/** Natural relief witnesses, retained gentle biomes and physical soil/stand controls. */
public final class WindsweptUplandsCheck {
    public static void main(String[]args){
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);
            for(int kind:new int[]{1,2}){
                var p=blockbranch.realistic.WindsweptUplandsFixtures.fixture(d,seed,kind);int soil=0,rock=0,plants=0,offGridEdges=0;
                var ecology=new GroundEcology.Field(d::sample,c);
                for(int z=p.z()-48;z<p.z()+48;z++)for(int x=p.x()-48;x<p.x()+48;x++){
                    int selected=WindsweptUplands.surfaceKind(seed,x,z,d,c);if(selected==0)continue;
                    double slope=SlopeCorrection.slope(d,x,z);require(slope>.45&&slope<=1.2,"New identities remain outside completed gentle domains");
                    require(!LandVegetation.surfaceReserved(d,x,z),"Specialist habitats retained");
                    if(WindsweptUplands.soil(seed,x,z,d)){soil++;int cover=WindsweptUplands.cover(seed,x,z,ecology.sample(x,z),true,selected);require(cover==9||cover==10||cover==11||cover==12,"Actual grass/rooted forest soil and native plants only");if(cover!=11)plants++;}
                    else rock++;
                    if((x&3)!=0&&WindsweptUplands.soil(seed,x,z,d)!=WindsweptUplands.soil(seed,x-1,z,d))offGridEdges++;
                }
                var roots=WindsweptTrees.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c);require(!roots.isEmpty(),"Natural rooted stand");
                var again=WindsweptTrees.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c);require(roots.equals(again),"Request-order-independent roots");
                int leaves=0,wood=0;var heights=new TreeSet<Integer>();
                for(var root:roots){require(WindsweptUplands.surfaceKind(seed,root.x(),root.z(),d,c)>0&&WindsweptUplands.soil(seed,root.x(),root.z(),d),"Tree roots require the actual physical domain and soil ledges");heights.add(root.height());for(var v:WindsweptTrees.tree(root)){if(WoodlandTrees.logCode(v.code()))wood++;else leaves++;}}
                require(soil>0&&rock>0&&offGridEdges>0,"Soil ledges/rock scarps use physical block edges, not quart masks");require(wood>0&&leaves>0,"Connected native crowns");
                System.out.println("WINDSWEPT seed="+seed+" kind="+kind+" point="+p+" soil="+soil+" rock="+rock+" plants="+plants+" offQuartEdges="+offGridEdges+" roots="+roots.size()+" heights="+heights+" wood="+wood+" leaves="+leaves);
            }
            int wx=seed==424242?1667925:seed==1?-1435850:-652180,wz=seed==424242?-1675880:seed==1?-1684227:-1220030;
            require(WindsweptUplands.kind(seed,wx,wz,d,c)>0&&WindsweptUplands.surfaceKind(seed,wx,wz,d,c)==0&&WindsweptUplands.soil(seed,wx,wz,d),"Historical quart/physical edge witness remains distinct");
            require(WindsweptTrees.roots(seed,wx-1,wz-1,wx+1,wz+1,d,c).stream().noneMatch(r->r.x()==wx&&r.z()==wz),"Exact historical quart-only root must be excluded");
            System.out.println("WINDSWEPT_PHYSICAL_ROOT_BOUNDARY_PASS seed="+seed+" excluded="+wx+","+wz);
            for(int z=-1700000;z<=1700000;z+=32000)for(int x=-1700000;x<=1700000;x+=32000){if(SlopeCorrection.slope(d,x,z)<=.45)require(WindsweptUplands.kind(seed,x,z,d,c)==0,"Completed gentle biome selectors retained");}
        }
        System.out.println("WINDSWEPT_NATURAL_RELIEF_SOIL_LEDGE_RETAINED_DOMAINS_ORDER_PASS");
    }
    private static void require(boolean v,String s){if(!v)throw new AssertionError(s);}
    private WindsweptUplandsCheck(){}
}
