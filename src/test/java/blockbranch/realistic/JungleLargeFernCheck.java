package blockbranch.realistic;

/** Mature native ferns occupy suitable existing fern colonies; other ground roles stay unchanged. */
public final class JungleLargeFernCheck {
    public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){
        int large=0,small=0;var humid=new GroundEcology.Site(.8,.3,.1,0,0,true);
        for(int z=-96;z<96;z++)for(int x=-96;x<96;x++){
            int old=Jungle.cover(seed,x,z,humid,true),now=Jungle.withLargeFern(seed,x,z,humid,old,true);
            require(now==old||old==10&&now==53,"Only existing small-fern sites may mature");
            require(Jungle.withLargeFern(seed,x,z,humid,old,false)==old,"Low headroom retains supported small ferns");
            require(Jungle.withLargeFern(seed,x,z,new GroundEcology.Site(.6,0,0,0,0,true),old,true)==old,"Less-moist sites unchanged");
            require(Jungle.withLargeFern(seed,x,z,new GroundEcology.Site(.8,0,0,0,0,false),old,true)==old,"Open sites unchanged");
            if(now==53)large++;if(now==10)small++;
        }
        require(large>50&&small>large,"Mature colonies coexist with many small ferns and empty gaps");
        System.out.println("JUNGLE_LARGE_FERN seed="+seed+" mature="+large+" small="+small);
    }System.out.println("JUNGLE_NATIVE_MATURE_FERN_HABITAT_HEADROOM_ROLE_PASS");}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private JungleLargeFernCheck(){}
}
