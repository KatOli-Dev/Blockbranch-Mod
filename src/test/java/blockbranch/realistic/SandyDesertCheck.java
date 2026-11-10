package blockbranch.realistic;
import java.util.*;

/** Sandy substrate suitability, real cactus support/spacing, quart boundaries and seeded colonies. */
public final class SandyDesertCheck {
    public static void main(String[] args)throws Throwable{
        var frozen=legacy();
        for(double t:new double[]{10,30})for(double m:new double[]{0,.119999})require(SandyDesert.climateSuitable(new ClimateField.Sample(t,m)),"Inclusive sandy Desert climate");
        for(var s:List.of(new ClimateField.Sample(9.999,.1),new ClimateField.Sample(30.001,.1),new ClimateField.Sample(20,.12)))require(!SandyDesert.climateSuitable(s),"Cool/non-arid climates retained");
        require(SandyDesert.bareCover(SandyDesert.mantleFromSite(.02,0,1,0))==8,"Depositional gentle ground retains deep sand");
        require(SandyDesert.bareCover(SandyDesert.mantleFromSite(.28,-12,0,1))==16,"Real convex exposed ground retains rock");
        require(SandyDesert.mantleFromSite(.08,-.75,.3,.2)==SandyDesert.mantleFromSite(.08,0,.3,.2),"Sub-metre contour quantisation cannot switch substrate");
        int[] totals=new int[21];

        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var p=frozen.windows.get(seed);var actual=new int[21];int edge=0;
            java.security.MessageDigest footprint;try{footprint=java.security.MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new AssertionError(e);}
            for(int z=p.z()-128;z<p.z()+128;z++)for(int x=p.x()-128;x<p.x()+128;x++){
                boolean habitat=SandyDesert.contains(seed,x,z,d,c);int qx=Math.floorDiv(x,4)*4,qz=Math.floorDiv(z,4)*4;
                require(habitat==SandyDesert.contains(seed,qx,qz,d,c),"Stable world quart membership");
                int cover=SandyDesert.cover(seed,x,z,habitat,d);actual[cover]++;
                if(cover==9||cover==17||SandyDesert.cactus(cover)){for(int value:new int[]{x,z})for(int shift:new int[]{24,16,8,0})footprint.update((byte)(value>>>shift));}

                require(cover==SandyDesert.cover(seed,x,z,habitat,d),"Order-independent seeded cover");
                require(SandyDesert.cover(seed,x,z,false,d)==0,"No sandy cover outside habitat");
                if(habitat){
                    require(!GravelDesert.contains(x,z,d,c),"Gravel domain retains distinct identity");
                    require(SandyDesert.climateSuitable(c.sample(qx,qz,d::sample))&&SlopeCorrection.slope(d,qx,qz)<=.30&&SandyDesert.province(seed,qx,qz)>=.48,"Actual arid gentle sandy substrate domain");
                    require(d.sample(qx,qz).waterLevel()==DrainageNetwork.DRY&&!LandVegetation.reserved(d,qx,qz),"Water/specialist domains preserved");
                    for(int[] step:DIR)if(!SandyDesert.contains(seed,x+step[0]*4,z+step[1]*4,d,c))edge++;
                }
                if(SandyDesert.cactus(cover)){
                    int h=d.sample(x,z).height();for(int[] step:DIR){int nx=x+step[0],nz=z+step[1];require(d.sample(nx,nz).height()<=h,"Cactus sides clear natural terrain");require(!SandyDesert.cactus(SandyDesert.cover(seed,nx,nz,SandyDesert.contains(seed,nx,nz,d,c),d)),"No contacting cactus columns");}
                }
            }
            require(actual[8]>1000&&actual[9]>0&&actual[10]>0&&actual[11]>0&&actual[12]>0,"Natural sand, dead stems and all cactus heights");
            require(actual[9]+actual[10]+actual[11]+actual[12]+actual[17]+actual[18]+actual[19]+actual[20]<actual[8]/8,"Predominantly bare arid surface");
            require(java.util.HexFormat.of().formatHex(footprint.digest()).equals(frozen.footprints.get(seed)),"Original vegetated ground footprint remains exact");
            int retained=0;
            for(var root:frozen.cactuses.get(seed)){
                require(SandyDesert.cover(seed,root[0],root[1],true,d)==root[2],"Legacy natural cactus position and height remain exact");retained++;
                for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)if(dx!=0||dz!=0)require(!SandyDesert.cactus(SandyDesert.cover(seed,root[0]+dx,root[1]+dz,SandyDesert.contains(seed,root[0]+dx,root[1]+dz,d,c),d)),"Legacy cactus halo remains clear across chunk seams");
            }
            int cactuses=actual[10]+actual[11]+actual[12]+actual[18]+actual[19]+actual[20];
            require(cactuses>retained*1.15,"Strong cactus colonies gain a meaningful composition change");
            System.out.printf("DESERT_COMPOSITION seed=%d oldCacti=%d newCacti=%d bushes=%d footprintExact=true%n",seed,retained,cactuses,actual[9]+actual[17]);
            for(int i=0;i<totals.length;i++)totals[i]+=actual[i];
            System.out.printf("SANDY_DESERT seed=%d point=%s climate=%s cover=%s terrainEdgeWitnesses=%d%n",seed,p,c.sample(p.x(),p.z(),d::sample),Arrays.toString(actual),edge);
        }
        benchmark(frozen);
        int seedChanges=0;for(int z=-128;z<=128;z+=8)for(int x=-128;x<=128;x+=8){double a=SandyDesert.province(424242,x,z),b=SandyDesert.province(424242,x+1,z);require(Math.abs(a-b)<.001,"Continuous regional substrate at cell seams");if(a!=SandyDesert.province(1,x,z))seedChanges++;}
        require(seedChanges>1000,"Seeded regional substrate variation");System.out.println("SANDY_DESERT_CLIMATE_SUBSTRATE_CACTUS_SUPPORT_COLONIES_PASS");
    }
    private record Legacy(Map<Long,SandyDesert.Point> windows,Map<Long,String> footprints,Map<Long,List<int[]>> cactuses){}
    private static Legacy legacy()throws Exception{
        var windows=new HashMap<Long,SandyDesert.Point>();var footprints=new HashMap<Long,String>();var roots=new HashMap<Long,List<int[]>>();
        try(var input=SandyDesertCheck.class.getResourceAsStream("/fixtures/desert/legacy-cactuses-c76b141.csv")){
            require(input!=null,"Frozen prior-selector fixture present");
            for(String row:new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().toList()){
                var fields=row.split(",");
                if(row.startsWith("# window,")){long seed=Long.parseLong(fields[1]);windows.put(seed,new SandyDesert.Point(Integer.parseInt(fields[2]),Integer.parseInt(fields[3])));}
                else if(row.startsWith("# footprint,"))footprints.put(Long.parseLong(fields[1]),fields[2]);
                else if(!row.isBlank()&&!row.startsWith("#"))roots.computeIfAbsent(Long.parseLong(fields[0]),key->new ArrayList<>()).add(new int[]{Integer.parseInt(fields[1]),Integer.parseInt(fields[2]),Integer.parseInt(fields[3])});
            }
        }
        return new Legacy(windows,footprints,roots);
    }
    /** Opt-in archived-selector timing, isolated from native render/FPS qualification. */
    private static void benchmark(Legacy frozen)throws Throwable{
        String archive=System.getProperty("blockbranch.desertPriorSelectorJar","");if(archive.isEmpty())return;
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{java.nio.file.Path.of(archive).toUri().toURL()},SandyDesertCheck.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                if(!name.equals("blockbranch.realistic.SandyDesert")&&!name.startsWith("blockbranch.realistic.SandyDesert$"))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
            }
        }){
            var prior=loader.loadClass("blockbranch.realistic.SandyDesert");require(prior!=SandyDesert.class,"Authentic old selector independently loaded");
            var cover=java.lang.invoke.MethodHandles.publicLookup().findStatic(prior,"cover",java.lang.invoke.MethodType.methodType(int.class,long.class,int.class,int.class,boolean.class,DrainageNetwork.class));
            long seed=424242,oldTime=0,newTime=0,oldSum=0,newSum=0;var d=new DrainageNetwork(new TerrainShape(seed),seed);var p=frozen.windows.get(seed);
            for(int z=p.z()-32;z<p.z()+32;z++)for(int x=p.x()-32;x<p.x()+32;x++){
                int old=(int)cover.invokeExact(seed,x,z,true,d),now=SandyDesert.cover(seed,x,z,true,d);
                require(old==9?now>=9&&now<=12:old==17?now>=17&&now<=20:now==old,"Actual prior selector changes only dead-bush composition");
            }
            for(int pass=0;pass<9;pass++)for(int turn=0;turn<2;turn++){
                boolean old=((pass+turn)&1)==0;long started=System.nanoTime(),sum=0;
                for(int z=p.z()-32;z<p.z()+32;z++)for(int x=p.x()-32;x<p.x()+32;x++)sum+=old?(int)cover.invokeExact(seed,x,z,true,d):SandyDesert.cover(seed,x,z,true,d);
                long elapsed=System.nanoTime()-started;if(old){oldSum+=sum;if(pass>=3)oldTime+=elapsed;}else{newSum+=sum;if(pass>=3)newTime+=elapsed;}
            }
            System.out.printf("DESERT_SELECTOR_KERNEL oldNanos=%d newNanos=%d ratio=%.3f checksums=%d/%d noRenderClaim=true%n",oldTime,newTime,newTime/(double)oldTime,oldSum,newSum);
        }
    }
    private static final int[][] DIR={{1,0},{-1,0},{0,1},{0,-1}};
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SandyDesertCheck(){}
}
