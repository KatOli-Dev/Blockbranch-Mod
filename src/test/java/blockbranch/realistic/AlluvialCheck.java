package blockbranch.realistic;

/** Construction bounds and saved-definition identity; not ecological calibration. */
public final class AlluvialCheck {
    public static void main(String[] args){
        for(int schema=TerrainShape.SHAPE_VERSION;schema<=269;schema++){
            require(AlluvialSchema.schema(schema,0)==schema&&AlluvialSchema.baseSchema(schema,0)==schema,"Absent retains historical schema");
            require(!AlluvialSchema.matches(schema,1),"Incomplete/old schemas never promoted");
        }
        require(AlluvialSchema.schema(269,1)==270&&AlluvialSchema.baseSchema(270,1)==269,"Complete independent wrapper");
        require(!AlluvialSchema.matches(270,0)&&!AlluvialSchema.matches(270,2)&&!AlluvialSchema.matches(271,1),"Malformed/future combinations reject");
        require(!AlluvialFloodplain.belowStage(104,100,4)&&AlluvialFloodplain.belowStage(103,100,4),"A crest touching first-air stage has zero water depth and cannot connect inundation");
        require(AlluvialFloodplain.loweredFirstAir(110,100,Double.NaN,20,1)==110&&AlluvialFloodplain.loweredFirstAir(110,100,40,20,Double.NaN)==110,"Non-finite geometry retains original ground");
        int w=37,h=35;int[] raw=new int[w*h];
        for(int z=0;z<h;z++)for(int x=0;x<w;x++)raw[z*w+x]=(x<3||z<2)?0:16;
        raw[17*w+19]=0;var limited=raw.clone();AlluvialFloodplain.limitCutGradient(limited,w,h);
        for(int z=0;z<h;z++)for(int x=0;x<w;x++){
            int expected=16;for(int qz=0;qz<h;qz++)for(int qx=0;qx<w;qx++)expected=Math.min(expected,raw[qz*w+qx]+Math.abs(x-qx)+Math.abs(z-qz));
            int i=z*w+x;require(limited[i]==expected&&limited[i]<=raw[i],"Exact conservative Manhattan envelope, including an isolated excluded cell");
            if(x>0)require(Math.abs(limited[i]/2-limited[i-1]/2)<=1,"No added multi-block edge across x");
            if(z>0)require(Math.abs(limited[i]/2-limited[i-w]/2)<=1,"No added multi-block edge across z");
        }
        require(limited[18*w+35]==16&&limited[17*w+19]==0,"Useful interior bench survives while the exclusion remains uncut");
        int cases=0;
        for(double width:new double[]{8,20,60,200,400})for(double grade:new double[]{0,.001,.002,.003,.03})for(int distance=0;distance<=1600;distance+=2)for(int air=95;air<=130;air++){
            int changed=AlluvialFloodplain.loweredFirstAir(air,100,distance,width,AlluvialFloodplain.gradeResponse(grade));
            require(changed<=air&&air-changed<=AlluvialFloodplain.MAX_CUT,"Cut-only bounded geometry");
            require(changed>=Math.min(air,102),"Prescribed dry bench never enters ordinary water");
            if(distance<=width||distance>=Math.min(3*width+48,AlluvialFloodplain.MAX_REACH)||grade>=.003)require(changed==air,"Wet channel, distant ground and steep reaches untouched");cases++;
        }
        System.out.println("ALLUVIAL_SCHEMA_CONSTRUCTION_BOUNDS_PASS cases="+cases);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private AlluvialCheck(){}
}
