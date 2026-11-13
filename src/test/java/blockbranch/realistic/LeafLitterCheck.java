package blockbranch.realistic;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Gate, code-mapping and distribution controls for saved leaf-litter variation. Model-convention
 * checks only: the weights and presence field are declared conventions, not litter measurements. */
public final class LeafLitterCheck {
    public static void main(String[] args) throws Exception {
        schema();
        mapping();
        distribution();
        presence();
        preset();
        System.out.println("PASS leaf-litter variation: complete-foundation gate, 16 reserved states per soil, deterministic bounded distributions, unchanged gap rolls");
    }

    private static void schema() {
        require(FoundationSchema.schema(264,0,1)==266&&FoundationSchema.baseSchema(266,0,1)==264,"Complete saved foundation");
        for(int v:new int[]{82,257,258,259,260,261,262,263,264,265,267})require(!FoundationSchema.matches(v,0,1),"Reject partial, polar-only, combined and future foundations");
        require(FoundationSchema.matches(264,0,0)&&!FoundationSchema.matches(266,0,0)&&!FoundationSchema.matches(266,0,2),"Absent gate retains the original schema only");
        for(int d:new int[]{0,1,2})require(!FoundationSchema.matches(265,0,d)&&FoundationSchema.baseSchema(265,0,d)==Integer.MAX_VALUE,"265 is never read under a litter-only definition set");
        expect(()->FoundationSchema.schema(263,0,1));expect(()->FoundationSchema.schema(264,0,2));
    }

    private static void mapping() {
        for(int c=0;c<=127;c++){
            boolean woodland=c>=65&&c<=96,warm=c>=13&&c<=28;
            require(LeafLitter.woodlandVariant(c)==woodland&&LeafLitter.warmVariant(c)==warm,"Reserved ranges "+c);
            require(woodland||LeafLitter.woodlandBase(c)==c,"Non-variant woodland codes pass through "+c);
            require(warm||LeafLitter.warmBase(c)==c,"Non-variant warm codes pass through "+c);
            if(woodland){
                require(LeafLitter.woodlandBase(c)==(c<81?7:9),"Variant soil follows its base litter code");
                require(LeafLitter.woodlandIndex(c)==c-(c<81?65:81),"Woodland index "+c);
            }
            if(warm)require(LeafLitter.warmBase(c)==3&&LeafLitter.warmIndex(c)==c-13,"Warm index "+c);
        }
        var indexes=new HashSet<Integer>();
        for(int c=65;c<=96;c++)indexes.add(c<81?c-65:c-81);
        require(indexes.size()==16,"Every state index is reachable from each soil");
        expect(()->LeafLitter.woodlandIndex(64));expect(()->LeafLitter.woodlandIndex(97));expect(()->LeafLitter.warmIndex(12));expect(()->LeafLitter.warmIndex(29));
    }

    private static void distribution() {
        long seed=424242;var site=GroundEcology.NEUTRAL;
        int[] facing=new int[4],amount=new int[4],all=new int[16];int thin=0,thinThick=0,thick=0,thickThick=0,total=0;
        for(int z=-160;z<160;z++)for(int x=-160;x<160;x++){
            int v=LeafLitter.variant(seed,x,z,site);require(v>=0&&v<16&&v==LeafLitter.variant(seed,x,z,site),"Bounded deterministic variant");
            facing[v&3]++;amount[v>>2]++;all[v]++;total++;
            double pile=GroundEcology.colonyStrength(seed,0x4C4954504C45L,x,z,20,5,12,site);
            boolean heavy=(v>>2)>=2;
            if(pile<.1){thin++;if(heavy)thinThick++;}else if(pile>.8){thick++;if(heavy)thickThick++;}
        }
        for(int n:facing)require(n>total*.22&&n<total*.28,"Facing is close to uniform: "+Arrays.toString(facing));
        for(int n:all)require(n>0,"Every one of the 16 variants occurs: "+Arrays.toString(all));
        require(thin>500&&thick>500,"Pile-field extremes are both sampled");
        require(thickThick/(double)thick>2*(thinThick/(double)thin),"Thick piles carry markedly more 3-4 segment litter: thin "+thinThick+"/"+thin+" thick "+thickThick+"/"+thick);
        require(amount[0]>amount[3],"Single-segment litter remains the most common overall");
        require(LeafLitter.variant(1,10,10,site)!=LeafLitter.variant(2,10,10,site)||LeafLitter.variant(1,11,10,site)!=LeafLitter.variant(2,11,10,site)||LeafLitter.variant(1,12,10,site)!=LeafLitter.variant(2,12,10,site),"Seed participates");
        // Retained cover codes: variants only replace the exact unvaried litter codes.
        for(int c=0;c<=127;c++){
            int w=LeafLitter.woodland(seed,5,7,c,site),m=LeafLitter.warm(seed,5,7,c,site);
            if(c==7||c==9)require(LeafLitter.woodlandVariant(w),"Unvaried litter becomes a variant: "+c);else require(w==c,"Only 7/9 vary: "+c);
            if(c==3)require(LeafLitter.warmVariant(m),"Warm litter becomes a variant");else require(m==c,"Only warm 3 varies: "+c);
            if(c==7)require(LeafLitter.woodlandBase(w)==7&&w<81,"Podzol litter stays podzol");
            if(c==9)require(LeafLitter.woodlandBase(w)==9&&w>=81,"Rooted litter stays rooted");
        }
        System.out.println("LEAF_LITTER_DISTRIBUTION facing="+Arrays.toString(facing)+" segments="+Arrays.toString(amount)+" heavyShareThin="+(thinThick/(double)thin)+" heavyShareThick="+(thickThick/(double)thick));
    }

    private static void presence() {
        long seed=424242;
        double[] shares=new double[2];double[] dispersion=new double[2];
        for(int mode=0;mode<2;mode++){
            var site=GroundEcology.NEUTRAL.shade(mode==0);
            int present=0,total=0;double sum=0,sumSq=0;int cells=0,control=0;double controlSum=0,controlSq=0;
            for(int cz=-320;cz<320;cz+=8)for(int cx=-320;cx<320;cx+=8){
                int n=0,l=0,cn=0,cl=0;
                for(int z=cz;z<cz+8;z++)for(int x=cx;x<cx+8;x++){
                    boolean p=LeafLitter.present(seed,x,z,site);n++;if(p)l++;
                    boolean white=((OakBirchWoodland.mix(seed^0x57484954454CL^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL)>>>11)*0x1.0p-53)<.80;cn++;if(white)cl++;
                }
                present+=l;total+=n;double f=l/(double)n;sum+=f;sumSq+=f*f;cells++;double g=cl/(double)cn;controlSum+=g;controlSq+=g*g;control++;
            }
            double p=present/(double)total,mean=sum/cells,var=sumSq/cells-mean*mean,expected=p*(1-p)/64;
            double cm=controlSum/control,cv=controlSq/control-cm*cm,ce=cm*(1-cm)/64;
            shares[mode]=p;dispersion[mode]=var/expected;
            require(Math.abs(cv/ce-1)<.25,"Independent 80% control has dispersion near one, so the metric discriminates: "+cv/ce);
        }
        require(shares[0]>.45&&shares[0]<.72,"Sheltered presence is well below the former 80-86%: "+shares[0]);
        require(shares[1]>.2&&shares[1]<.5&&shares[1]<shares[0],"Open ground keeps less litter than sheltered ground: "+shares[1]);
        require(dispersion[0]>2&&dispersion[1]>2,"Presence is patchy rather than independent: "+Arrays.toString(dispersion));
        System.out.println("LEAF_LITTER_PRESENCE sheltered="+shares[0]+" open="+shares[1]+" dispersion="+Arrays.toString(dispersion));
        // The gap rolls are untouched: only the former uniform litter roll changes.
        int varied=0,total=0;
        for(long seed2:new long[]{424242,1,-17})for(int z=-96;z<96;z++)for(int x=-96;x<96;x++)for(boolean shaded:new boolean[]{false,true}){
            var site=GroundEcology.NEUTRAL.shade(shaded);
            for(boolean clear:new boolean[]{false,true}){
                int a=SummerDryWoodland.cover(seed2,x,z,site,clear),b=SummerDryWoodland.cover(seed2,x,z,site,clear,true);
                require(a==SummerDryWoodland.cover(seed2,x,z,site,clear,false),"Five-argument form is the original");
                require(a==b||a==8&&b==9||a==9&&b==8,"Summer-dry only litter/bare differs: "+a+"->"+b);
                if(a==12||!clear)require(a==b,"Grass openings and obstructed ground are untouched");
                int c=DryForest.cover(seed2,x,z,site,clear),d=DryForest.cover(seed2,x,z,site,clear,true);
                require(c==DryForest.cover(seed2,x,z,site,clear,false),"Five-argument form is the original");
                require(c==d||c==8&&d==9||c==9&&d==8,"Dry forest only litter/bare differs: "+c+"->"+d);
                if(c==10||c==11||!clear)require(c==d,"Grass/fern openings and obstructed ground are untouched");
                if(a!=b||c!=d)varied++;total++;
            }
        }
        require(varied>total/10,"The density change is observable");
    }

    private static void preset() throws Exception {
        try(var in=LeafLitterCheck.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json")){
            var text=new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);
            require(text.contains("\"shape_version\": 273")&&text.contains("\"mangrove_roots_definition\": 1")&&text.contains("\"boreal_slope_definition\": 1")&&text.contains("\"alluvial_floodplain_definition\": 1")&&text.contains("\"seasonal_forest_definition\": 1")&&text.contains("\"warm_xeric_definition\": 1")&&text.contains("\"leaf_litter_definition\": 1")&&text.contains("\"continental_ice_definition\": 1"),"New-world preset opts in explicitly to all wrapped definitions (schema 273)");
        }
    }

    private static void expect(Runnable action){try{action.run();}catch(RuntimeException expected){return;}throw new AssertionError("Expected rejection");}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private LeafLitterCheck(){}
}
