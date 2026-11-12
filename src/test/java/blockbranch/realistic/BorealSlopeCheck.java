package blockbranch.realistic;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Frozen published-270 boreal placement stays byte-for-byte reproducible. */
public final class BorealSlopeCheck {
    public static void main(String[] args)throws Exception{
        try(var input=new BufferedReader(new InputStreamReader(Objects.requireNonNull(BorealSlopeCheck.class.getResourceAsStream("/fixtures/boreal-slope-270/oracle.txt")),StandardCharsets.UTF_8))){
            int i=0;String line;
            while((line=input.readLine())!=null){
                var s=BorealSlopeOracle.SITES[i];var head=s[0]+" "+s[1]+" "+s[2]+" ";
                require(line.startsWith(head),"Window identity "+i);
                String live=BorealSlopeOracle.describe(s[0],(int)s[1],(int)s[2]);
                require(line.equals(head+live),"Published boreal placement window "+i+": "+live);
                i++;
            }
            require(i==BorealSlopeOracle.SITES.length,"Every frozen window is checked");
        }
        System.out.println("BOREAL_SLOPE_PUBLISHED_ORACLE_PASS");
        schema();flags();tolerance();
        System.out.println("BOREAL_SLOPE_GATED_PASS");
    }
    /** Saved schema 271 exists only as the complete 270 plus the explicit definition. */
    private static void schema(){
        for(int saved=82;saved<=275;saved++){
            require(BorealSlopeSchema.schema(saved,0)==saved,"Absent definition retains "+saved);
            require(BorealSlopeSchema.baseSchema(saved,0)==(saved==271?Integer.MAX_VALUE:saved),"Absent definition never reads 271 ("+saved+")");
            require(BorealSlopeSchema.matches(saved,1)==(saved==271),"Only 271 reads under the definition ("+saved+")");
            require(!BorealSlopeSchema.matches(saved,2)&&!BorealSlopeSchema.matches(saved,-1),"Unknown definitions never match ("+saved+")");
            if(saved!=270){boolean threw=false;try{BorealSlopeSchema.schema(saved,1);}catch(IllegalArgumentException e){threw=true;}require(threw,"Only complete 270 is wrapped ("+saved+")");}
        }
        require(BorealSlopeSchema.schema(270,1)==271&&BorealSlopeSchema.baseSchema(271,1)==270,"Complete independent wrapper");
    }
    /** The saved tree-clear flag, not preset text or a global, selects the relief. */
    private static void flags(){
        for(int flags=0;flags<128;flags++)require(BorealSlope.relief(flags)==((flags&64)!=0?BorealSlope.RELIEF:BorealSlope.PUBLISHED_RELIEF),"Relief for flags "+flags);
        require(BorealSlope.PUBLISHED_RELIEF==3&&BorealSlope.RELIEF==5,"Bounded relief values");
    }
    /** Tolerant roots only add stems on steep ground; flat stands and every published stem stay put. */
    private static void tolerance()throws Exception{
        boolean[] flat=new boolean[BorealSlopeOracle.SITES.length];int gained=0;
        for(int i=0;i<BorealSlopeOracle.SITES.length;i++){
            var s=BorealSlopeOracle.SITES[i];long seed=s[0];int x=(int)s[1],z=(int)s[2],h=BorealSlopeOracle.HALF;
            var d=BorealSlopeOracle.terrain(seed);var c=new ClimateField(seed,true);
            var published=BorealTrees.roots(seed,x-h,z-h,x+h,z+h,d,c);
            var tolerant=BorealTrees.roots(seed,x-h,z-h,x+h,z+h,d,c,BorealSlope.RELIEF);
            require(BorealSlopeOracle.fingerprint(BorealTrees.roots(seed,x-h,z-h,x+h,z+h,d,c,BorealSlope.PUBLISHED_RELIEF)).equals(BorealSlopeOracle.fingerprint(published)),"Published relief is the default");
            require(tolerant.containsAll(published),"Tolerance never removes a published stem, window "+i);
            if(published.size()==tolerant.size()){flat[i]=true;require(published.equals(tolerant),"Unchanged window is identical "+i);}
            else gained+=tolerant.size()-published.size();
            for(var root:tolerant)for(var voxel:BorealTrees.tree(root))
                require(voxel.y()-d.sample(voxel.x(),voxel.z()).height()>=1,"No crown voxel inside terrain, window "+i+" at "+voxel.x()+","+voxel.y()+","+voxel.z());
        }
        require(BorealSlopeOracle.SITES[0][1]==-1392000&&BorealTrees.roots(1,-1392000-64,-1616000-64,-1392000+64,-1616000+64,BorealSlopeOracle.terrain(1),new ClimateField(1,true)).isEmpty(),"The sampled window is treeless under published relief");
        require(BorealTrees.roots(1,-1392000-64,-1616000-64,-1392000+64,-1616000+64,BorealSlopeOracle.terrain(1),new ClimateField(1,true),BorealSlope.RELIEF).size()>100,"The sampled treeless slope gains a stand");
        require(!flat[0]&&flat[1]&&flat[3]&&gained>0,"Flat windows are untouched; steep windows gain stems");
        // The saved vegetation flag reaches the generated canopy columns: 16 chunks around the site.
        var d=BorealSlopeOracle.terrain(1);var c=new ClimateField(1,true);int base=-1392000>>4,baseZ=-1616000>>4,off=0,on=0;
        for(int cz=baseZ-2;cz<baseZ+2;cz++)for(int cx=base-2;cx<base+2;cx++){
            for(int v:WoodlandTrees.column(1,cx,cz,d,c,true,16))if(v!=0)off++;
            for(int v:WoodlandTrees.column(1,cx,cz,d,c,true,16|BorealSlope.FLAG))if(v!=0)on++;
        }
        require(off==0&&on>0,"Canopy columns: published flags treeless ("+off+"), slope flag stands ("+on+")");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private BorealSlopeCheck(){}
}
