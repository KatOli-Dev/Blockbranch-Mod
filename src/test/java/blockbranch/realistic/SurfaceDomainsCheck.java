package blockbranch.realistic;

/** Fixed habitat anchors expose the old quart-only boundaries without changing terrain or routing. */
public final class SurfaceDomainsCheck {
    @FunctionalInterface interface Domain { boolean at(int x,int z); }
    record Case(String name,int x,int z,Domain biome,Domain surface) {}
    public static void main(String[] args){
        long seed=424242;var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);
        Case[] cases={
            new Case("meadow",1359388,-1694456,(x,z)->LowlandMeadow.potential(x,z,d,c),(x,z)->LowlandMeadow.surfacePotential(x,z,d,c)),
            new Case("woodland",-1050028,-1700004,(x,z)->OakBirchWoodland.potential(seed,x,z,d,c),(x,z)->OakBirchWoodland.surfacePotential(seed,x,z,d,c)),
            new Case("forest",-1611988,-1868041,(x,z)->Forest.potential(seed,x,z,d,c),(x,z)->Forest.surfacePotential(seed,x,z,d,c)),
            new Case("birch",-795976,-1852048,(x,z)->BirchForest.contains(seed,x,z,d,c),(x,z)->BirchForest.surfaceContains(seed,x,z,d,c)),
            new Case("warm",11991,-756038,(x,z)->WarmWoodland.contains(seed,x,z,d,c),(x,z)->WarmWoodland.surfaceContains(seed,x,z,d,c)),
            new Case("humid",-1076037,-244040,(x,z)->HumidWoodland.contains(seed,x,z,d,c),(x,z)->HumidWoodland.surfaceContains(seed,x,z,d,c)),
            new Case("jungle",-1680028,-480035,(x,z)->Jungle.contains(seed,x,z,d,c),(x,z)->Jungle.surfaceContains(seed,x,z,d,c)),
            new Case("savanna",-1727996,-452001,(x,z)->Savanna.contains(seed,x,z,d,c),(x,z)->Savanna.surfaceContains(seed,x,z,d,c)),
            new Case("steppe",-1390016,-1700000,(x,z)->DrySteppe.contains(x,z,d,c),(x,z)->DrySteppe.surfaceContains(x,z,d,c)),
            new Case("cold-steppe",-924004,-1900032,(x,z)->ColdSteppe.contains(x,z,d,c),(x,z)->ColdSteppe.surfaceContains(x,z,d,c)),
            new Case("tundra",-1372019,-1883989,(x,z)->ColdTundra.contains(x,z,d,c),(x,z)->ColdTundra.surfaceContains(x,z,d,c)),
            new Case("gravel-desert",388028,-1212032,(x,z)->GravelDesert.contains(x,z,d,c),(x,z)->GravelDesert.surfaceContains(x,z,d,c)),
            new Case("sandy-desert",404006,-1212051,(x,z)->SandyDesert.contains(seed,x,z,d,c),(x,z)->SandyDesert.surfaceContains(seed,x,z,d,c)),
            new Case("shore",-1318712,-1697680,(x,z)->LowGradientSandyShore.contains(x,z,(a,b)->LowGradientSandyShore.site(d,a,b)),(x,z)->LowGradientSandyShore.surfaceContains(x,z,(a,b)->LowGradientSandyShore.site(d,a,b))),
            new Case("reedbed",-798684,-1695080,(x,z)->InlandLakeReedbed.contains(x,z,(a,b)->InlandLakeReedbed.site(d,a,b)),(x,z)->InlandLakeReedbed.surfaceContains(x,z,(a,b)->InlandLakeReedbed.site(d,a,b))),
            new Case("gravel-bank",-1461088,-1698016,(x,z)->GravelRiverMargin.contains(x,z,(a,b)->GravelRiverMargin.site(d,a,b)),(x,z)->GravelRiverMargin.surfaceContains(x,z,(a,b)->GravelRiverMargin.site(d,a,b))),
            new Case("lake",-765612,-1700131,(x,z)->LakeSediment.contains(x,z,d),(x,z)->LakeSediment.surfaceContains(x,z,d)),
            new Case("swamp",-765464,-1700209,(x,z)->Swamp.contains(seed,x,z,d,c),(x,z)->Swamp.surfaceSoilContains(seed,x,z,d,c)),
            new Case("ocean",-1340916,-1687864,(x,z)->TemperateOcean.contains(seed,x,z,d,c),(x,z)->TemperateOcean.surfaceContains(seed,x,z,d,c)),
            new Case("shelf",-1310313,-1695745,(x,z)->MarineShelf.contains(x,z,d),(x,z)->MarineShelf.surfaceContains(x,z,d)),
            new Case("deep",1183714,-1687201,(x,z)->DeepOcean.contains(x,z,d),(x,z)->DeepOcean.surfaceContains(x,z,d)),
            new Case("river-bed",1359388,-1694456,(x,z)->RiverChannel.contains(x,z,d),(x,z)->RiverChannel.surfaceContains(x,z,d)),
            new Case("river-bank",1359388,-1694456,(x,z)->RoutedRiver.bankContains(seed,x,z,d,c),(x,z)->RoutedRiver.surfaceBankContains(seed,x,z,d,c)),
            new Case("snowy-plains",-1412024,-1844024,(x,z)->SnowyPlains.contains(x,z,d,c),(x,z)->SnowyPlains.surfaceContains(x,z,d,c))
        };
        for(var test:cases)check(test,d);
        snowOverlay(seed,d,c);
        System.out.println("SURFACE_DOMAINS_QUART_NEGATIVE_CONTROLS_HYDROLOGY_ORDER_PASS");
    }
    static void snowOverlay(long seed,DrainageNetwork d,ClimateField c){
        int cx=-1412024>>4,cz=-1844024>>4,x0=cx*16,z0=cz*16,wrong=0,placed=0;
        var ecology=new GroundEcology.Field(d::sample,c);
        var entries=OpenSnowCover.column(seed,cx,cz,new int[0],d,ecology,(x,z)->x-x0+2*(z-z0)>=13,false);
        boolean[] snow=new boolean[256];for(int entry:entries){int key=entry>>>blockbranch.cubic.CanopyLayer.STATE_BITS;snow[key&255]=true;}
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int wx=x0+x,wz=z0+z;boolean inside=x+2*z>=13;
            if(inside!=(Math.floorDiv(x,4)*4+2*Math.floorDiv(z,4)*4>=13))wrong++;
            boolean expected=inside&&!LandVegetation.surfaceReserved(d,wx,wz)&&OpenSnowCover.layers(seed,wx,wz,ecology.sample(wx,wz),false)>0;
            require(snow[z*16+x]==expected,"Snow overlay follows the exact diagonal, not its quart mask");if(snow[z*16+x])placed++;
        }
        require(wrong>10&&placed>100,"Diagonal snow negative control and nontrivial overlay");
        System.out.printf("SURFACE_SNOW_OVERLAY_DIAGONAL_PASS wrongQuartColumns=%d placed=%d%n",wrong,placed);
    }
    static void check(Case test,DrainageNetwork d){
        int[] boundary=null;boolean start=test.biome().at(test.x(),test.z());
        outer:for(int radius=16;radius<=262144;radius*=2)for(int[] v:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}){
            int bx=test.x()+v[0]*radius,bz=test.z()+v[1]*radius;
            if(test.biome().at(bx,bz)==start)continue;
            int ax=test.x(),az=test.z();
            while(Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4){int mx=Math.floorDiv(ax+bx,2),mz=Math.floorDiv(az+bz,2);if(test.biome().at(mx,mz)==start){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
            boundary=new int[]{bx,bz};break outer;
        }
        require(boundary!=null,"Fixed anchor must expose a boundary: "+test.name());
        int changed=0,oldEdges=0,oldOffGrid=0,newEdges=0,newOffGrid=0;long geometry=1;
        for(int z=boundary[1]-32;z<boundary[1]+32;z++)for(int x=boundary[0]-32;x<boundary[0]+32;x++){
            boolean old=test.biome().at(x,z),now=test.surface().at(x,z);if(old!=now)changed++;
            var before=d.sample(x,z);geometry=31*geometry+before.height();geometry=31*geometry+before.waterLevel();
            for(int axis=0;axis<2;axis++){
                int nx=x+(axis==0?1:0),nz=z+(axis==1?1:0),coordinate=axis==0?nx:nz;
                if(old!=test.biome().at(nx,nz)){oldEdges++;if(Math.floorMod(coordinate,4)!=0)oldOffGrid++;}
                if(now!=test.surface().at(nx,nz)){newEdges++;if(Math.floorMod(coordinate,4)!=0)newOffGrid++;}
            }
            require(before.equals(d.sample(x,z)),"Material selection must preserve geometry/water: "+test.name());
            test.surface().at(-x,-z);require(now==test.surface().at(x,z),"Request-order consistency: "+test.name());
        }
        require(oldEdges>0&&oldOffGrid==0,"Quart-only negative control: "+test.name());
        require(changed>0&&newOffGrid>0,"Physical boundary resolves individual blocks: "+test.name());
        System.out.printf("SURFACE_DOMAIN %s boundary=%d,%d changed=%d oldEdges=%d oldOffGrid=%d newEdges=%d newOffGrid=%d geometry=%d%n",test.name(),boundary[0],boundary[1],changed,oldEdges,oldOffGrid,newEdges,newOffGrid,geometry);
    }
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
