package blockbranch.realistic;

/** Dryland climate limits, prior-biome separation, actual terrain and stable transitions. */
public final class GravelDesertCheck {
    public static void main(String[] args) {
        protectedPlantsAndMatrix();
        for(double t:new double[]{5,30}) for(double m:new double[]{0,0.119999}) require(GravelDesert.climateSuitable(new ClimateField.Sample(t,m)),"Inclusive temperature/lower moisture limits");
        for(var s:new ClimateField.Sample[]{new ClimateField.Sample(4.99,0.1),new ClimateField.Sample(30.01,0.1),new ClimateField.Sample(10,0.12)}) require(!GravelDesert.climateSuitable(s),"Exclude colder, hotter and non-arid climate");
        for(int t=-10;t<=35;t++) for(int m=0;m<=100;m++) {
            var s=new ClimateField.Sample(t,m/100.0);
            if(GravelDesert.climateSuitable(s)) require(!OakBirchWoodland.climateSuitable(s) && !BorealWoodland.climateSuitable(s) && !DrySteppe.climateSuitable(s) && !WarmWoodland.climateSuitable(s) && !ColdSteppe.climateSuitable(s) && !OakBirchWoodland.meadowClimateSuitable(s),"Other woodland and warmer steppe climates retained");
        }
        var good=new ClimateField.Sample(10,0.1);var soil=new DrainageNetwork.Sample(263,DrainageNetwork.DRY,0,false);
        require(GravelDesert.eligible(soil,0.3,false,good),"Inclusive dry slope");
        require(!GravelDesert.eligible(soil,0.30001,false,good),"Reject steep terrain");
        require(!GravelDesert.eligible(soil,0,true,good),"Specialists and existing meadow retained");
        require(!GravelDesert.eligible(new DrainageNetwork.Sample(263,266,0,true),0,false,good),"Exclude water");
        int[] covers=new int[8];int changed=0;
        for(int z=-128;z<128;z++) for(int x=-128;x<128;x++) {
            int c=GravelDesert.cover(424242,x,z,true,true);covers[c]++;
            require(c==GravelDesert.cover(424242,x,z,true,true),"Stable clumps");
            require(GravelDesert.cover(424242,x,z,true,false)==0 && GravelDesert.cover(424242,x,z,false,true)==0,"No dry cover in wet or excluded cells");
            if(c!=GravelDesert.cover(1,x,z,true,true)) changed++;
        }
        for(int c=1;c<=7;c++) require(covers[c]>20,"Every pavement/sediment/live/dead component present");
        require(covers[1]>30000 && covers[2]>8000 && covers[3]>4000 && covers[5]+covers[6]+covers[7]<1000 && changed>20000,"Dominant barren pavement, gravel and sand; very sparse live/dead plants and seeded structure");
        for(long seed:new long[]{424242,1,-17}) {
            var drainage=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);var p=blockbranch.realistic.GravelDesertFixtures.fixture(drainage,seed);
            require(GravelDesert.contains(p.x(),p.z(),drainage,climate),"Natural selected desert");
            int[] actual=new int[8];long started=System.nanoTime();
            for(int z=p.z()-256;z<p.z()+256;z+=2) for(int x=p.x()-256;x<p.x()+256;x+=2) {
                boolean habitat=GravelDesert.contains(x,z,drainage,climate);
                require(habitat==GravelDesert.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,drainage,climate),"World-coordinate quart boundaries");
                var s=drainage.sample(x,z);int cover=GravelDesert.cover(seed,x,z,habitat,s.waterLevel()==DrainageNetwork.DRY && drainage.channelAt(x,z).edge()<0);actual[cover]++;
                if(cover>0)require(!(OakBirchWoodland.meadowClimateSuitable(climate.sample(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,drainage::sample)) && LowlandMeadow.contains(x,z,(wx,wz)->LowlandMeadow.site(drainage,wx,wz))),"Actual preexisting meadow preserved");
                if(cover>0) require(s.waterLevel()==DrainageNetwork.DRY && GravelDesert.climateSuitable(climate.sample(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,drainage::sample)),"Actual dry climate and hydrology");
            }
            for(int c=1;c<=7;c++) require(actual[c]>0,"Natural component for seed="+seed+" code="+c);
            for(boolean climateOnly:new boolean[]{false,true}) {
                var transition=blockbranch.realistic.GravelDesertFixtures.transition(drainage,seed,climateOnly);
                boolean inside=climateOnly ? GravelDesert.climateSuitable(climate.sample(transition.insideX(),transition.insideZ(),drainage::sample)) : GravelDesert.contains(transition.insideX(),transition.insideZ(),drainage,climate);
                boolean outside=climateOnly ? GravelDesert.climateSuitable(climate.sample(transition.outsideX(),transition.outsideZ(),drainage::sample)) : GravelDesert.contains(transition.outsideX(),transition.outsideZ(),drainage,climate);
                require(inside && !outside && Math.max(Math.abs(transition.insideX()-transition.outsideX()),Math.abs(transition.insideZ()-transition.outsideZ()))<=4,"Adjacent actual climate/terrain transition");
                require(transition.equals(blockbranch.realistic.GravelDesertFixtures.transition(drainage,seed,climateOnly)),"Deterministic transition search");
            }
            System.out.printf("Gravel desert seed=%d fixture=%s climate=%s cover=%s sampleMillis=%d%n",seed,p,climate.sample(p.x(),p.z(),drainage::sample),java.util.Arrays.toString(actual),(System.nanoTime()-started)/1000000);
        }
        System.out.println("CONTINENTAL_GRAVEL_DESERT_SHAPE_CLIMATE_PASS");
    }
    /** Frozen on main 82a9165, before sediment redistribution; no current selector as oracle. */
    private static void protectedPlantsAndMatrix() {
        long[] seeds={424242,1,-17};
        String[] expected={"7921891d5d061399d57790a573d8ef079985b193f3831e865211f9344ea14130",
            "fa07871d26a5b1daa36142afb8e3be5d5bda1452c366273fdf29147817ef050e",
            "a7208f74d4bc02c46611d5efc510ae6683a02be39830846ca8df60f24bbc472b"};
        for(int i=0;i<seeds.length;i++) {
            java.security.MessageDigest digest;
            try { digest=java.security.MessageDigest.getInstance("SHA-256"); }
            catch(java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
            var tuple=java.nio.ByteBuffer.allocate(12);int[] roles=new int[8];byte[] grid=new byte[1024*1024];
            for(int z=-512;z<512;z++)for(int x=-512;x<512;x++) {
                int code=GravelDesert.cover(seeds[i],x,z,true,true);roles[code]++;grid[(z+512)*1024+x+512]=(byte)code;
                if(code>=5){tuple.clear();tuple.putInt(x).putInt(z).putInt(code);digest.update(tuple.array());}
            }
            require(java.util.HexFormat.of().formatHex(digest.digest()).equals(expected[i]),"Original vegetation coordinates/codes retained seed="+seeds[i]);
            int bare=roles[1]+roles[2]+roles[3];
            require(roles[1]>.62*bare && roles[1]<.72*bare && roles[2]>.17*bare && roles[2]<.26*bare && roles[3]>.08*bare && roles[3]<.15*bare,"Pavement retains substantial gravel and sand seed="+seeds[i]);
            require(roles[4]>.025*grid.length&&roles[4]<.05*grid.length,"Sparse exposed fine-soil pockets seed="+seeds[i]);
            for(int z=2;z<1022;z++)for(int x=2;x<1022;x++)if(grid[z*1024+x]==4){
                boolean plant=false;
                for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)if(dx*dx+dz*dz<=4&&grid[(z+dz)*1024+x+dx]>=5)plant=true;
                require(plant,"Fine soil remains near original plant microsites");
            }
            // The old equal-scale mosaic fails this bound for each seed. Test connected
            // material geometry, not the noise function or threshold implementation.
            require(componentAreas(grid,1,1)[1]>.97*roles[1],"Connected pavement matrix seed="+seeds[i]);
            // Edge grains are intentional; compare the substantial sediment lenses.
            int gravelP90=componentAreas(grid,2,64)[0],sandP90=componentAreas(grid,3,64)[0];
            require(gravelP90>3*sandP90&&sandP90>200&&sandP90<450,"Smaller sand pockets among broader gravel lenses seed="+seeds[i]);
        }
    }
    /** Returns the 90th-percentile and largest four-connected areas above a minimum. */
    private static int[] componentAreas(byte[] grid,int role,int minimum) {
        int[] queue=new int[grid.length],areas=new int[grid.length];int count=0;
        for(int start=0;start<grid.length;start++)if(grid[start]==role) {
            int head=0,tail=1;queue[0]=start;grid[start]=0;
            while(head<tail) {
                int cell=queue[head++],x=cell%1024,z=cell/1024;
                if(x>0&&grid[cell-1]==role){grid[cell-1]=0;queue[tail++]=cell-1;}
                if(x<1023&&grid[cell+1]==role){grid[cell+1]=0;queue[tail++]=cell+1;}
                if(z>0&&grid[cell-1024]==role){grid[cell-1024]=0;queue[tail++]=cell-1024;}
                if(z<1023&&grid[cell+1024]==role){grid[cell+1024]=0;queue[tail++]=cell+1024;}
            }
            if(tail>=minimum)areas[count++]=tail;
        }
        require(count>0,"Material components present");java.util.Arrays.sort(areas,0,count);
        return new int[]{areas[(int)((count-1)*.9)],areas[count-1]};
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
