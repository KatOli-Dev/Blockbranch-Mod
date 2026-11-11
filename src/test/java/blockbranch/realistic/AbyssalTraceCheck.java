package blockbranch.realistic;

import java.util.*;

/** Finite placement/domain controls; density conventions are not empirical estimates. */
public final class AbyssalTraceCheck {
    public static void main(String[] args){
        for(int depth:new int[]{2,200,2999,6001,10000})require(!AbyssalTraces.eligible(depth,0,5,true,true),"Shelf/transition/hadal exclusion");
        for(int bed:new int[]{0,7,8,9,10,11,12})require(!AbyssalTraces.eligible(4000,0,bed,true,true),"Only fines, no rock/gravel/sand/photic vegetation");
        require(!AbyssalTraces.eligible(4000,.081,5,true,true)&&!AbyssalTraces.eligible(4000,0,5,false,true)&&!AbyssalTraces.eligible(4000,0,5,true,false),"Slope, ownership and support independently matter");
        require(AbyssalTraces.eligible(3000,.08,5,true,true)&&AbyssalTraces.eligible(6000,0,6,true,true),"Inclusive chosen depth/slope bounds");
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true);var c=new ClimateField(seed);var arrays=new LinkedHashMap<Long,int[]>();int total=0;var forms=new HashSet<Integer>();
            // These fixed positive windows are the first three fine-bed/slope
            // witnesses in the pre-existing 32 km grid, not depth-only cells.
            int[][] centers=seed==424242?new int[][]{{-1744000,-1904000},{-528000,-1904000},{304000,-1904000}}:
                seed==1?new int[][]{{-1008000,-1904000},{1168000,-1904000},{592000,-1872000}}:new int[][]{{-496000,-1904000},{-400000,-1904000},{-144000,-1904000}};
            for(var center:centers)for(int cz=(center[1]>>4)-4;cz<=(center[1]>>4)+4;cz++)for(int cx=(center[0]>>4)-4;cx<=(center[0]>>4)+4;cx++){
                var a=AbyssalTraces.column(seed,cx,cz,d,c,true);arrays.put((cx&0xffffffffL)|((long)cz<<32),a);
                for(int v:a){int key=v>>>8,x=cx*16+(key&15),z=cz*16+(key>>4&15),y=(key>>>8)+TerrainShape.FLOOR,code=v&255;
                    require(y==d.sample(x,z).height()+1&&AbyssalTraces.eligible(seed,x,z,d,c,true),"Physical bed/depth/ownership support");require(code>=173&&code<=184,"Append-only packed states");forms.add((code-173)/4);total++;
                }
            }
            var order=new ArrayList<>(arrays.keySet());Collections.reverse(order);for(long k:order)require(Arrays.equals(arrays.get(k),AbyssalTraces.column(seed,(int)k,(int)(k>>32),d,c,true)),"Order-independent chunk seams");
            System.out.println("ABYSSAL_TRACE_FINITE seed="+seed+" chunks="+arrays.size()+" traces="+total+" forms="+forms);
            require(total>0&&forms.size()==3,"Supported finite world witnesses exercise every form; no universal trace density");
            // A count minimum tests the declared candidate convention only on
            // an all-eligible control. Heterogeneous world windows have no
            // source-backed lower density bound (presence-only observations).
            int control=0;var controlForms=new HashSet<Integer>();var positions=new HashSet<Long>();
            for(int z=-10;z<10;z++)for(int x=-10;x<10;x++){
                var p=AbyssalTraces.candidate(seed,x,z);if(p==null)continue;
                require(p.x()>=x*12+3&&p.x()<=x*12+8&&p.z()>=z*12+3&&p.z()<=z*12+8,"In-cell jitter bounds preserve spacing across zero");
                require(positions.add((p.x()&0xffffffffL)|((long)p.z()<<32)),"No duplicate control location");
                require(AbyssalTraces.eligible(4000,0,5,true,true),"Flat fine-bed control admits the proposed structure");control++;controlForms.add(p.form());
            }
            require(control>20&&controlForms.size()==3,"Nonvacuous all-eligible candidate control");
            System.out.println("ABYSSAL_TRACE_CONTROL seed="+seed+" eligibleCells=400 traces="+control+" forms="+controlForms);
        }
        require(AbyssalTraceSchema.schema(260,1)==261&&AbyssalTraceSchema.baseSchema(261,1)==260,"New complete foundation");
        for(int old:new int[]{82,257,258,259,260})require(AbyssalTraceSchema.schema(old,0)==old&&AbyssalTraceSchema.baseSchema(old,0)==old,"Old definitions retained");
        require(!AbyssalTraceSchema.matches(261,0)&&!AbyssalTraceSchema.matches(260,1)&&!AbyssalTraceSchema.matches(262,1),"Partial/future gates reject");
        System.out.println("ABYSSAL_TRACES_DOMAINS_DETERMINISM_SCHEMA_PASS");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private AbyssalTraceCheck(){}
}
