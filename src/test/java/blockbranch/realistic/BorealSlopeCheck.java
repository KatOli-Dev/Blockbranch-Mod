package blockbranch.realistic;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
        schema();flags();tolerance();montaneDisjoint();
        System.out.println("BOREAL_SLOPE_GATED_PASS");
    }
    /** Saved schema 272 exists only as the complete montane 271 plus the explicit definition. */
    private static void schema(){
        for(int saved=82;saved<=279;saved++){
            require(BorealSlopeSchema.schema(saved,0)==saved,"Absent definition retains "+saved);
            require(BorealSlopeSchema.baseSchema(saved,0)==(saved==272?Integer.MAX_VALUE:saved),"Absent definition never reads 272 ("+saved+")");
            require(BorealSlopeSchema.matches(saved,1)==(saved==272),"Only 272 reads under the definition ("+saved+")");
            require(!BorealSlopeSchema.matches(saved,2)&&!BorealSlopeSchema.matches(saved,-1),"Unknown definitions never match ("+saved+")");
            if(saved!=271){boolean threw=false;try{BorealSlopeSchema.schema(saved,1);}catch(IllegalArgumentException e){threw=true;}require(threw,"Only complete 271 is wrapped ("+saved+")");}
        }
        require(BorealSlopeSchema.schema(271,1)==272&&BorealSlopeSchema.baseSchema(272,1)==271,"Complete independent wrapper");
    }
    /** The saved tree-clear flag, not preset text or a global, selects the relief. */
    private static void flags(){
        for(int flags=0;flags<256;flags++)require(BorealSlope.relief(flags)==((flags&128)!=0?BorealSlope.RELIEF:BorealSlope.PUBLISHED_RELIEF),"Relief for flags "+flags);
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
        // Flat stands: the saved flag changes nothing, for every flag combination that needs no saved owner.
        for(int site=0;site<flat.length;site++)if(flat[site]){
            var s=BorealSlopeOracle.SITES[site];long seed=s[0];var fd=BorealSlopeOracle.terrain(seed);var fc=new ClimateField(seed,true);int fx=(int)s[1]>>4,fz=(int)s[2]>>4;
            for(int flags:new int[]{0,1,2,16,17,18})for(int cz=fz-1;cz<=fz;cz++)for(int cx=fx-1;cx<=fx;cx++)
                require(Arrays.equals(WoodlandTrees.column(seed,cx,cz,fd,fc,true,flags),WoodlandTrees.column(seed,cx,cz,fd,fc,true,flags|BorealSlope.FLAG)),"Flat window "+site+" chunk "+cx+","+cz+" flags "+flags+" identical under the slope flag");
        }
    }
    /** Boreal stems are never filtered by the montane owner (published behaviour), so the new stems must be
     * impossible on, or beside, montane ground. Montane eligibility requires ordinary temperate forest
     * potential (mean temperature at least 6 C); boreal woodland requires a mean below 3 C. The cells are
     * checked, not only the thresholds: no sampled cell satisfies both, no added stem has an eligible cell
     * within the crown/halo reach, and the sampled temperature step bounds how close the domains can come. */
    private static void montaneDisjoint()throws Exception{
        long[][] montane={{1,-1744000,-1040000},{1,1584000,-944000},{1,1552000,-912000},{424242,592000,-496000},{424242,48000,-432000}};
        int eligibleCells=0,borealCells=0;double maxStep=0;
        var windows=new java.util.ArrayList<long[]>();for(var m:montane)windows.add(m);windows.add(BorealSlopeOracle.SITES[0]);windows.add(BorealSlopeOracle.SITES[3]);
        for(var w:windows){
            long seed=w[0];int cx=(int)w[1],cz=(int)w[2];var d=BorealSlopeOracle.terrain(seed);var c=new ClimateField(seed,true);
            var eligible=new java.util.ArrayList<int[]>();
            for(int z=cz-256;z<=cz+256;z+=4){double previous=Double.NaN;for(int x=cx-256;x<=cx+256;x+=4){
                double t=c.sample(x,z,d::sample).meanTemperatureC();if(!Double.isNaN(previous))maxStep=Math.max(maxStep,Math.abs(t-previous));previous=t;
                if(z>cz-256)maxStep=Math.max(maxStep,Math.abs(t-c.sample(x,z-4,d::sample).meanTemperatureC()));
                boolean owned=MontaneForest.eligible(seed,x,z,d,c),boreal=BorealWoodland.contains(seed,x,z,d,c);
                require(!(owned&&boreal),"Cell "+x+","+z+" is both montane-eligible and boreal woodland");
                if(owned){eligibleCells++;eligible.add(new int[]{x,z});require(t>=6,"Montane-eligible cells are temperate (>=6 C): "+t);}
                if(boreal){borealCells++;require(t<3.0000001,"Boreal woodland is cold (<3 C): "+t);}
            }}
            if(w==BorealSlopeOracle.SITES[0]||w==BorealSlopeOracle.SITES[3]){
                int h=256;var added=new java.util.HashSet<>(BorealTrees.roots(seed,cx-h,cz-h,cx+h,cz+h,d,c,BorealSlope.RELIEF));added.removeAll(BorealTrees.roots(seed,cx-h,cz-h,cx+h,cz+h,d,c,BorealSlope.PUBLISHED_RELIEF));
                for(var root:added)for(var cell:eligible)require(Math.hypot(root.x()-cell[0],root.z()-cell[1])>24,"Added boreal stem within 24 blocks of montane-eligible ground at "+root.x()+","+root.z());
                if(w==BorealSlopeOracle.SITES[0])require(added.size()>100,"Non-vacuous: the audited window has added stems ("+added.size()+")");
            }
        }
        require(eligibleCells>1000&&borealCells>1000,"Both domains are sampled (montane "+eligibleCells+", boreal "+borealCells+")");
        // 3 C of mean temperature separate the domains; the steepest sampled 4-block step bounds the gap at far more than crown reach.
        require(maxStep>0&&3.0/maxStep*4>24,"Sampled temperature step "+maxStep+" C per 4 blocks leaves a domain gap above 24 blocks");
        System.out.println("BOREAL_MONTANE_DISJOINT_PASS montaneCells="+eligibleCells+" borealCells="+borealCells+" maxStepC/4m="+maxStep);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private BorealSlopeCheck(){}
}
