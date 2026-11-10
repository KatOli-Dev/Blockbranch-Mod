package blockbranch.realistic;

import java.util.*;

/** Real interiors, supported oak cohorts, block-resolution caps and an absolute sedimentary datum. */
public final class BadlandsCheck {
    public static void main(String[]args){
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var e=new GroundEcology.Field(d::sample,c);
            for(int kind:new int[]{1,2}){
                var p=blockbranch.realistic.BadlandsFixtures.fixture(d,seed,kind);require(Badlands.interiorCount(seed,p.x(),p.z(),kind,d,c)>=60,"Natural interior");
                int selected=0,offQuart=0,soil=0,sand=0,exposed=0,plants=0;var beds=new HashSet<Integer>();
                for(int dz=-64;dz<64;dz++)for(int dx=-64;dx<64;dx++){
                    int x=p.x()+dx,z=p.z()+dz;if(Badlands.surfaceKind(seed,x,z,d,c)!=kind)continue;selected++;
                    int code=Badlands.cover(seed,x,z,kind,d,e),cap=Badlands.cap(code);require(Badlands.encoded(code),"Packed arid material fits signed byte");
                    require(d.sample(x,z).waterLevel()==DrainageNetwork.DRY&&d.channelAt(x,z).edge()<0,"Dry land, not a water/bank replacement");
                    if(cap>=3)soil++;else if(cap>0)sand++;else exposed++;
                    if(cap==2||cap>=4)plants++;
                    int h=d.sample(x,z).height();for(int y=h-63;y<=h;y++)beds.add(Badlands.stratum(code,y));
                    if((x&3)!=0&&Badlands.surfaceKind(seed,x-1,z,d,c)==kind){int prior=Badlands.cover(seed,x-1,z,kind,d,e);if((Badlands.cap(prior)>=3)!=(cap>=3)||(Badlands.cap(prior)>0)!=(cap>0))offQuart++;}
                    if((x&15)==0&&(z&15)==0){
                        require(!SandyDesert.contains(seed,x,z,d,c)&&!SandyDesert.surfaceContains(seed,x,z,d,c)&&!Savanna.contains(seed,x,z,d,c)&&SavannaUplands.kind(seed,x,z,d,c)==0,"Completed dry biomes preserved");
                        require(!Forest.potential(seed,x,z,d,c)&&TemperateForestVariants.kind(seed,x,z,d,c)==0&&DarkCherryWoodland.kind(seed,x,z,d,c)==0&&WindsweptUplands.kind(seed,x,z,d,c)==0,"Completed forest/upland groups preserved");
                        require(code==Badlands.cover(seed,x,z,kind,d,e),"Material request order deterministic");
                    }
                }
                require(selected>8000&&beds.size()==6,"Representative exposed sedimentary sequence");
                var roots=Badlands.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c);var heights=new TreeSet<Integer>();
                for(var root:roots){
                    require(Badlands.kind(seed,root.x(),root.z(),d,c)==2&&Badlands.surfaceKind(seed,root.x(),root.z(),d,c)==2,"Oak owns physical woodland habitat");
                    require(Badlands.cap(Badlands.cover(seed,root.x(),root.z(),2,d,e))>=3,"Oak roots stand on actual mineral soil");
                    require(WindsweptTrees.supportedFootprint(root.x(),root.z(),d,e.sample(root.x(),root.z()),false),"Existing physical support retained");heights.add(root.height());
                }
                if(kind==2)require(soil>1000&&exposed>100&&offQuart>0&&roots.size()>=8&&heights.size()>=4,"Wooded soil patches, bare gaps and varied oak cohorts");
                System.out.println("BADLANDS_CHECK seed="+seed+" kind="+kind+" point="+p+" selected="+selected+" soil="+soil+" sand="+sand+" exposed="+exposed+" plants="+plants+" offQuart="+offQuart+" roots="+roots.size()+" heights="+heights);
            }
        }
        System.out.println("BADLANDS_NATIVE_PALETTE_SOIL_SUPPORT_STRATA_PHYSICAL_EDGES_PRESERVED_BIOMES_PASS");
    }
    private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
    private BadlandsCheck(){}
}
