package blockbranch.realistic;

import java.util.*;

/** Negative climate controls, finite sensitivity/ownership and sparse-cover contracts. */
public final class CoolDesertCheck {
    public static void main(String[] args)throws Exception {
        require(!CoolDesert.climateSuitable(0,new ClimateField.Sample(2,.05)),"Year-round cool equatorial upland is not continental desert");
        require(!CoolDesert.climateSuitable(1600000,new ClimateField.Sample(-7,.05)),"Extreme cold retains polar/alpine ownership");
        require(!CoolDesert.climateSuitable(1600000,new ClimateField.Sample(5,.05)),"Existing warm-desert seam retained");
        require(!CoolDesert.climateSuitable(1600000,new ClimateField.Sample(2,.12)),"Existing steppe seam retained");
        require(CoolDesert.climateSuitable(1600000,new ClimateField.Sample(2,.119999)),"Cool dry continental control");
        require(!new CoolDesert.Season(0,15,5).growingSeason(),"Winter freezing is strict");
        require(!new CoolDesert.Season(-20,10,0).growingSeason(),"No warm growing season");
        require(!new CoolDesert.Season(-15,11,1).growingSeason(),"One marginal month is insufficient");
        for(int z:new int[]{0,1000000,2097152})for(double t:new double[]{-20,0,4,12}){
            var p=CoolDesert.season(z,t);require(p.equals(CoolDesert.season(-z,t)),"Local seasonal aggregates are hemisphere symmetric");
            require(Math.abs((p.coldestC()+p.warmestC())*.5-t)<1e-10,"Annual mean retained");
        }
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true);var c=new ClimateField(seed);
            int[] count=new int[3];int physical=0;
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var q=c.sample(x,z,d::sample);
                if(q.meanTemperatureC()<-6||q.meanTemperatureC()>=5||q.moisture()>=.12||!CoolDesert.physicalSupport(x,z,d))continue;
                if(CoolDesert.eligible(x,z,d,c))physical++;
                for(int i=0;i<3;i++)if(CoolDesert.season(z,q.meanTemperatureC(),new double[]{14.4,16,17.6}[i]).growingSeason())count[i]++;
            }
            require(physical>0,"Finite seed has supported candidates");
            System.out.println("COOL_DESERT_FIXED_GRID seed="+seed+" samples=14400 admitted="+physical+" thermalGradientMinus10NominalPlus10="+Arrays.toString(count));
        }
        var d=new DrainageNetwork(new TerrainShape(424242),424242,true,true,true,true);var c=new ClimateField(424242);
        require(CoolDesert.eligible(400000,-1200000,d,c),"Supported plateau witness, not an altitude ban");
        require(!CoolDesert.eligible(-1424000,16000,d,c),"Existing steep alpine candidate rejected");
        require(!CoolDesert.physicalSupport(400000,-944000,d),"Confirmed marine outlet cannot become dry desert");
        var never=CoolDesert.domain(d,c,(qx,qz)->false);require(!never.test(400000,-1200000),"Unselected quart cannot gain physical cover");
        var selected=CoolDesert.domain(d,c,(qx,qz)->true);require(!selected.test(-1424000,16000),"Selected quart cannot bypass physical eligibility");
        int[] histogram=new int[8];for(int z=-128;z<=128;z++)for(int x=-128;x<=128;x++){
            int a=CoolDesert.cover(424242,x,z,GroundEcology.NEUTRAL);require(a==CoolDesert.cover(424242,x,z,GroundEcology.NEUTRAL),"Deterministic composition");histogram[a]++;
        }
        int n=257*257;require(histogram[0]==0&&histogram[3]==0,"Stony analog without cactus/deep sand roles");
        require(histogram[7]>n*.005&&histogram[7]<n*.06&&histogram[6]<histogram[7],"Sparse shrub-dominant vegetation, not grass sward");
        require(histogram[1]+histogram[2]+histogram[4]>n*.90,"Open mineral ground");
        require(CoolDesertSchema.schema(259,1)==260&&CoolDesertSchema.baseSchema(260,1)==259,"Complete new saved gate");
        for(int old:new int[]{82,257,258,259})require(CoolDesertSchema.schema(old,0)==old&&CoolDesertSchema.baseSchema(old,0)==old,"Disabled gate retains old definitions");
        require(!CoolDesertSchema.matches(260,0)&&!CoolDesertSchema.matches(259,1)&&!CoolDesertSchema.matches(261,1),"Incomplete/future gate rejects");
        System.out.println("COOL_DESERT_NEGATIVES_COVER_SCHEMA_PASS histogram="+Arrays.toString(histogram));
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private CoolDesertCheck(){}
}
