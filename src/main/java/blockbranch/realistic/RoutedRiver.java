package blockbranch.realistic;

/** Common gently graded inner river channel; existing vegetated fringes/rapids retain their specific identity. */
public final class RoutedRiver {
    private static final java.util.Map<DrainageNetwork,java.util.Map<Long,Boolean>> INNER_CELLS=new java.util.WeakHashMap<>();
    /** Mixed4m cells retain the specific fringe habitat. Bounded per-world
     * memoization avoids repeating sixteen hydrology probes for every block. */
    private static boolean innerCell(int x,int z,DrainageNetwork d){
        java.util.Map<Long,Boolean> cache;
        synchronized(INNER_CELLS){cache=INNER_CELLS.computeIfAbsent(d,key->new java.util.LinkedHashMap<Long,Boolean>(512,.75f,true){@Override protected boolean removeEldestEntry(java.util.Map.Entry<Long,Boolean> eldest){return size()>4096;}});}
        long key=((long)x<<32)|(z&0xffffffffL);
        synchronized(cache){var found=cache.get(key);if(found!=null)return found;}
        boolean inside=true;
        outer:for(int oz=0;oz<4;oz++)for(int ox=0;ox<4;ox++){
            var site=RiverChannel.site(d,x+ox,z+oz);
            if(!RiverChannel.eligible(site)||site.lateral()>=.5||site.channel().gradient()>.003){inside=false;break outer;}
        }
        synchronized(cache){cache.put(key,inside);}return inside;
    }
    public static boolean climate(ClimateField.Sample c){return c.meanTemperatureC()>=-4;}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        if(bankContains(seed,x,z,d,c))return true;
        if(!RiverChannel.contains(x,z,d))return false;
        var s=RiverChannel.site(d,x,z);
        return s.channel().gradient()<=.003&&s.lateral()<.5&&innerCell(x,z,d)&&climate(c.sample(x,z,d::sample))&&!MangroveSwamp.contains(seed,x,z,d,c);
    }
    /** Reuse nearby routed geometry for the existing 8m soil-support transition; never reroute water. */
    static DrainageNetwork.Channel bankChannel(DrainageNetwork d,int x,int z){var ch=d.channelAt(x,z);if(ch.edge()>=0)return ch;double best=Double.POSITIVE_INFINITY;var result=DrainageNetwork.Channel.NONE;
        for(int[]o:new int[][]{{-8,0},{8,0},{0,-8},{0,8}}){var q=d.channelAt(x+o[0],z+o[1]);if(q.edge()<0)continue;double t=d.closestT(q.edge(),x,z),distance=d.channelDistance(q.edge(),t,x,z);if(distance<=q.halfWidth()*1.4+8&&distance<best){best=distance;result=new DrainageNetwork.Channel(q.edge(),q.waterLevel(),q.halfWidth(),distance,q.gradient());}}
        return result;
    }
    public static boolean bankContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceBankContains(seed,x,z,d,c);
    }
    static int bankCover(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        if(!surfaceBankContains(seed,x,z,d,c)||!surfaceBankCandidate(x,z,d))return 0;var s=d.sample(x,z);var ch=bankChannel(d,x,z);int above=s.height()+1-ch.waterLevel();
        double wetness=SurfacePatches.sample(seed^0x46494E4542414E4BL,x,z,28)+(2.5-above)*.08;
        return wetness>.5?13:14; // Explicit DRY mud/sand, never aquatic plant codes.
    }
    /** Sand/gravel transport-lane patches are static geometry proxies, not sediment transport. */
    public static int cover(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        int bank=bankCover(seed,x,z,d,c);if(bank!=0)return bank;
        return RiverChannel.cover(seed,x,z,true,RiverChannel.site(d,x,z),c.sample(x,z,d::sample));
    }
    public record Point(int x,int z){}

    static double[] axis(DrainageNetwork d,int x,int z){
        int edge=d.channelAt(x,z).edge();double best=Double.POSITIVE_INFINITY,t=.5;
        for(int n=1;n<100;n++){double a=n/100.,distance=Math.hypot(x-d.edgeX(edge,a),z-d.edgeZ(edge,a));if(distance<best){best=distance;t=a;}}
        double dx=d.edgeX(edge,t+.001)-d.edgeX(edge,t-.001),dz=d.edgeZ(edge,t+.001)-d.edgeZ(edge,t-.001),length=Math.hypot(dx,dz);return new double[]{dx/length,dz/length};
    }
    /** Block-resolution material eligibility; the original selector retains biome routing. */
    static boolean surfaceBankCandidate(int x,int z,DrainageNetwork d){
        var s=d.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY)return false;var ch=bankChannel(d,x,z);if(ch.edge()<0||ch.gradient()>.003)return false;
        int above=s.height()+1-ch.waterLevel();double lateral=ch.distance()/ch.halfWidth();if(above<0||above>8||lateral<.8||ch.distance()>ch.halfWidth()*1.4+8)return false;
        if(AlpineScree.surfaceContains(x,z,d::sample)||GravelRiverMargin.surfaceContains(x,z,(a,b)->GravelRiverMargin.site(d,a,b))||InlandLakeReedbed.surfaceContains(x,z,(a,b)->InlandLakeReedbed.site(d,a,b))||LowGradientSandyShore.surfaceContains(x,z,(a,b)->LowGradientSandyShore.site(d,a,b)))return false;
        double dx=(d.sample(x+8,z).height()-d.sample(x-8,z).height())/16.,dz=(d.sample(x,z+8).height()-d.sample(x,z-8).height())/16.;if(Math.hypot(dx,dz)>.45)return false;
        double t=d.closestT(ch.edge(),x,z);int wx=(int)Math.round(d.edgeX(ch.edge(),t)),wz=(int)Math.round(d.edgeZ(ch.edge(),t));var wet=RiverChannel.site(d,wx,wz);
        return RiverChannel.eligible(wet)&&wet.water()==ch.waterLevel();
    }
    /** Block-resolution material eligibility; the original selector retains biome routing. */
    static boolean surfaceBankContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){if(!surfaceBankCandidate(x,z,d)||!climate(c.sample(x,z,d::sample))||MangroveSwamp.exact(seed,x,z,d,c))return false;
        if(!LandVegetation.surfaceReserved(d,x,z)&&(LowlandMeadow.surfacePotential(x,z,d,c)||OakBirchWoodland.surfacePotential(seed,x,z,d,c)||WarmWoodland.surfaceContains(seed,x,z,d,c)||HumidWoodland.surfaceContains(seed,x,z,d,c)||DrySteppe.surfaceContains(x,z,d,c)||BorealWoodland.surfaceContains(seed,x,z,d,c)||ColdSteppe.surfaceContains(x,z,d,c)||GravelDesert.surfaceContains(x,z,d,c)||ColdTundra.surfaceContains(x,z,d,c)||SnowySlopes.surfaceContains(x,z,d,c)||SandyDesert.surfaceContains(seed,x,z,d,c)||Savanna.surfaceContains(seed,x,z,d,c)||Jungle.surfaceContains(seed,x,z,d,c)))return false;
        return true;}
    private RoutedRiver(){}
}
