package blockbranch.realistic;
import java.util.*;
/** Climate/terrain separation, ecological colonies, no grid seam bias and seeded native-boundary consistency. */
public final class ColdTundraCheck {
    public static void main(String[]args){
        frozenSelectors();
        var good=new ClimateField.Sample(-9,.55);var dry=new DrainageNetwork.Sample(2300,DrainageNetwork.DRY,0,false);
        for(double t:new double[]{-14,-6.00001})for(double m:new double[]{.20,.85})require(ColdTundra.climateSuitable(new ClimateField.Sample(t,m)),"Inclusive lower and exclusive upper cold endpoints");
        for(var c:List.of(new ClimateField.Sample(-14.001,.5),new ClimateField.Sample(-6,.5),new ClimateField.Sample(-9,.19999),new ClimateField.Sample(-9,.85001)))require(!ColdTundra.climateSuitable(c),"Reject polar-bare, boreal, arid and saturated climate proxies");
        require(ColdTundra.eligible(dry,.35,false,good)&&!ColdTundra.eligible(dry,.35001,false,good)&&!ColdTundra.eligible(dry,0,true,good)&&!ColdTundra.eligible(new DrainageNetwork.Sample(2300,2303,0,true),0,false,good),"Slope, specialist and water guards");
        for(int t=-20;t<30;t++)for(int m=0;m<=100;m++){var c=new ClimateField.Sample(t,m/100.0);if(ColdTundra.climateSuitable(c))require(!BorealWoodland.climateSuitable(c)&&!ColdSteppe.climateSuitable(c)&&!OakBirchWoodland.climateSuitable(c)&&!WarmWoodland.climateSuitable(c)&&!HumidWoodland.climateSuitable(c),"Existing woodland and steppe climate domains disjoint");}
        for(long seed:new long[]{424242,1,-17}){
            var wet=new GroundEcology.Site(.78,.5,.05,.1,.04,false);var exposed=new GroundEcology.Site(.25,-.6,.9,.1,.04,false);
            int[] components=new int[BorealSurface.LAST+1];int wetShrub=0,dryShrub=0,wetMoss=0,dryMoss=0,edge=0,interior=0,edgeN=0,interiorN=0;
            for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
                int a=ColdTundra.cover(seed,x,z,wet),b=ColdTundra.cover(seed,x,z,exposed);components[a]++;if(a==9)wetShrub++;if(b==9)dryShrub++;if(BorealSurface.mossSite(a))wetMoss++;if(BorealSurface.mossSite(b))dryMoss++;
                require(a==ColdTundra.cover(seed,x,z,wet),"Stable local colonies");boolean changed=a!=ColdTundra.cover(seed,x+1,z,wet);
                if(Math.floorMod(x+1,16)==0){edgeN++;if(changed)edge++;}else{interiorN++;if(changed)interior++;}
            }
            for(int code:new int[]{2,BorealSurface.SOIL,BorealSurface.CARPET,8,9,10,11})require(components[code]>50,"Every tundra ground/vegetation component");
            System.out.printf("TUNDRA_SITE_RESPONSE seed=%d shrubs=%d/%d moss=%d/%d%n",seed,wetShrub,dryShrub,wetMoss,dryMoss);
            require(wetShrub>dryShrub*2&&wetMoss>dryMoss*1.8,"Shrubs favour moist shelter; moss favours moisture");
            require(Math.abs(edge/(double)edgeN-interior/(double)interiorN)<.035,"No native/cell-grid seam bias in foliage");
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var p=blockbranch.realistic.ColdTundraFixtures.fixture(d,seed);var ecology=new GroundEcology.Field(d::sample,c);var actual=new int[BorealSurface.LAST+1];
            require(ColdTundra.contains(p.x(),p.z(),d,c),"Selected natural tundra");
            for(int z=p.z()-64;z<=p.z()+64;z+=2)for(int x=p.x()-64;x<=p.x()+64;x+=2){
                require(ColdTundra.contains(x,z,d,c)==ColdTundra.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c),"Negative and positive global quart sampling");
                if(!ColdTundra.contains(x,z,d,c))continue;require(PlacementBaseline.definition18(seed,x,z,d,c).equals("placeholder"),"Existing habitats retained at every sampled natural cell");actual[ColdTundra.cover(seed,x,z,ecology.sample(x,z))]++;
            }
            for(int code:new int[]{2,BorealSurface.SOIL,BorealSurface.CARPET,8,9,10,11})require(actual[code]>0,"All components in actual terrain");
            for(boolean climateOnly:new boolean[]{false,true}){
                var e=blockbranch.realistic.ColdTundraFixtures.transition(d,seed,climateOnly);boolean in=climateOnly?ColdTundra.climateSuitable(c.sample(e.insideX(),e.insideZ(),d::sample)):ColdTundra.contains(e.insideX(),e.insideZ(),d,c),out=climateOnly?ColdTundra.climateSuitable(c.sample(e.outsideX(),e.outsideZ(),d::sample)):ColdTundra.contains(e.outsideX(),e.outsideZ(),d,c);
                require(in&&!out&&Math.max(Math.abs(e.insideX()-e.outsideX()),Math.abs(e.insideZ()-e.outsideZ()))<=4,"Adjacent climate/terrain transitions");require(e.equals(blockbranch.realistic.ColdTundraFixtures.transition(d,seed,climateOnly)),"Seeded transition continuity");
            }
            int cx=p.x()>>4,cz=p.z()>>4;var forward=WoodlandTrees.column(seed,cx,cz,d,c);require(forward.length==0,"Tundra interior remains treeless");WoodlandTrees.column(seed,cx+1,cz,d,c);require(Arrays.equals(forward,WoodlandTrees.column(seed,cx,cz,d,c)),"Chunk order unchanged");
            System.out.printf("TUNDRA_PATTERN seed=%d fixture=%s climate=%s cover=%s wetDryShrub=%d/%d wetDryMoss=%d/%d gridEdgeInterior=%.4f/%.4f%n",seed,p,c.sample(p.x(),p.z(),d::sample),Arrays.toString(actual),wetShrub,dryShrub,wetMoss,dryMoss,edge/(double)edgeN,interior/(double)interiorN);
        }
        System.out.println("COLD_DWARF_SHRUB_TUNDRA_CLIMATE_FOLIAGE_PASS");
    }
    /** Baseline 9893bb9 non-bare roles; z then x over [-128,128), big-endian x/z/legacy-code tuples. */
    private static void frozenSelectors(){
        String[][] expected={
            {"1dc6e9678107281c27d01459f1316403a9cf65ef3807c98a44284caf20b7b7b7","93217745cfc59aca895f7ed401580e96fe0d20151f69e2b8c8e13fafca4bfa3a","1dd66920040b86e816ae0f6b35dd9abdb0bfb645e723689084b5c0464fd9d60d"},
            {"138e024c82d640606d112f7436d3e21aa6463c73ac9b99d54035e6c19f1ddea7","862df40aaa88a6a759d3d2d334545fba34f2cb1f7d44ddff044dd76356360ab4","a0b090aa30596444fa6b9be22bcfea958e06c5235743e5792ddbce6d35d83e40"},
            {"11ea16017149fc5de684ecd0c572021cbe9ba9e80f95b7d56ef63cdf3b121954","9a5305b35c6556f2851ef841941b4b35964ed700abb68ac43f5aaf14f0f052d8","29cb110d04245a7e66a43d8e6eb1f6741d2d88caefa6fc9c60a9255748c223ee"}
        };
        int[][] protectedCounts={{35175,43420,9244},{34160,41929,8940},{34086,41993,8756}};
        int[][] legacyGravel={{7081,5284,42471},{7383,5725,42730},{7452,5747,43070}};
        long[] seeds={424242,1,-17};
        var sites=new GroundEcology.Site[]{GroundEcology.NEUTRAL,new GroundEcology.Site(.78,.5,.05,.1,.04,false),new GroundEcology.Site(.25,-.6,.9,.1,.04,false)};
        for(int seed=0;seed<seeds.length;seed++)for(int site=0;site<sites.length;site++){
            final java.security.MessageDigest hash;
            try{hash=java.security.MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException failure){throw new AssertionError(failure);}
            int soil=0,carpet=0,protectedCount=0,gravel=0;byte[] cover=new byte[256*256];
            for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
                int fresh=ColdTundra.cover(seeds[seed],x,z,sites[site]);cover[(z+128)*256+x+128]=(byte)fresh;
                require(fresh==2||fresh==8||fresh==9||fresh==10||fresh==11||fresh==BorealSurface.SOIL||fresh==BorealSurface.CARPET,"Fresh tundra emitted a legacy moss cube or an unsupported role");
                if(fresh==10)gravel++;
                if(fresh==2||fresh==10)continue;
                protectedCount++;
                if(fresh==BorealSurface.SOIL)soil++;if(fresh==BorealSurface.CARPET)carpet++;
                // Only bare soil/gravel may move. Normalize appended moss roles for the frozen baseline.
                int legacy=fresh==BorealSurface.SOIL?3:fresh==BorealSurface.CARPET?6:fresh;
                integer(hash,x);integer(hash,z);integer(hash,legacy);
            }
            require(soil>0&&carpet>0,"Every reference ecology retains both original moss-site identities");
            require(protectedCount==protectedCounts[seed][site]&&HexFormat.of().formatHex(hash.digest()).equals(expected[seed][site]),"Bare clustering moved an original moss/plant identity seed="+seeds[seed]+" site="+site);
            require(gravel>=.9*legacyGravel[seed][site]&&gravel<=1.1*legacyGravel[seed][site],"Gravel balance changed by more than ten percent");
            clusteredBare(cover,seeds[seed],site,gravel,legacyGravel[seed][site]);
        }
    }
    /** Compare cardinal bare-neighbor observations with independent mixing at the same pair density. */
    private static void clusteredBare(byte[] cover,long seed,int site,int gravel,int legacyGravel){
        long edges=0,gravelEnds=0,bothGravel=0,same=0;
        for(int z=0;z<256;z++)for(int x=0;x<256;x++){
            int i=z*256+x;if(cover[i]!=2&&cover[i]!=10)continue;
            for(int direction=0;direction<2;direction++){
                if(direction==0&&x==255||direction==1&&z==255)continue;
                int next=direction==0?i+1:i+256;if(cover[next]!=2&&cover[next]!=10)continue;
                edges++;if(cover[i]==10)gravelEnds++;if(cover[next]==10)gravelEnds++;
                if(cover[i]==10&&cover[next]==10)bothGravel++;if(cover[i]==cover[next])same++;
            }
        }
        require(edges>0&&gravelEnds>0&&gravelEnds<2*edges,"Bare sediment adjacency fixture is degenerate");
        double fraction=gravelEnds/(2.*edges),randomSame=fraction*fraction+(1-fraction)*(1-fraction);
        double sameRatio=same/(double)edges/randomSame,gravelLift=bothGravel/(double)edges/(fraction*fraction);
        require(sameRatio>1.10,"Bare soil/gravel remains independently speckled");
        // High-gravel exposed ground cannot reach 1.4x: even perfect clustering is bounded by 1/fraction.
        if(site<2)require(gravelLift>1.4,"Sheltered gravel lacks coherent small clusters");
        System.out.printf(Locale.ROOT,"TUNDRA_BARE_CLUSTER seed=%d site=%d gravel=%d/%d sameRatio=%.3f gravelLift=%.3f%n",seed,site,gravel,legacyGravel,sameRatio,gravelLift);
    }
    private static void integer(java.security.MessageDigest hash,int value){hash.update((byte)(value>>>24));hash.update((byte)(value>>>16));hash.update((byte)(value>>>8));hash.update((byte)value);}
    private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
    private ColdTundraCheck(){}
}
