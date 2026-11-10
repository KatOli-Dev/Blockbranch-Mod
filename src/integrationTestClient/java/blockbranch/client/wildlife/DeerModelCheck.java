package blockbranch.client.wildlife;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
final class DeerModelCheck {
    static void verifyRevision(){
        var root=DeerModel.layer().bakeRoot();var model=new DeerModel(root);var state=new DeerRenderer.State();
        model.setupAnim(state);float standing=root.getChild("front_left").y+13;
        state.activity=2;model.setupAnim(state);
        if(root.getChild("body").y+4!=standing-.25F||Math.abs(root.getChild("front_left").y+1-(standing-.025F))>.0001F)throw new AssertionError("Deer rest must meet native sole plane");
        for(int i=0;i<40;i++)model.setupAnim(state);
        if(root.getChild("body").y+4!=standing-.25F)throw new AssertionError("Deer rest accumulates");
        state.activity=0;model.setupAnim(state);
        if(root.getChild("body").y!=9||root.getChild("neck").y!=7||root.getChild("front_left").y!=11)throw new AssertionError("Deer standing pose changed");
        int[] antlerCubes={0},otherCubes={0};
        root.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->{if(path.contains("/antler"))antlerCubes[0]++;else otherCubes[0]++;});
        if(antlerCubes[0]>8||otherCubes[0]!=11)throw new AssertionError("Deer antler simplicity/body preservation changed");
        for(boolean baby:new boolean[]{false,true})for(boolean stag:new boolean[]{false,true}){
            state.isBaby=baby;state.stag=stag;model.setupAnim(state);
            for(int sign:new int[]{-1,1})if(root.getChild("neck").getChild("head").getChild("antler"+sign).visible!=(stag&&!baby))throw new AssertionError("Deer antler visibility changed");
        }
        state.isBaby=false;state.stag=true;model.setupAnim(state);
        blockbranch.Blockbranch.LOGGER.info("DEER_ANTLER_MODEL_PASS antlerCuboids={} otherCuboids={} adultHindCalfVisibility=true",antlerCubes[0],otherCubes[0]);
        root.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->{
            if(!path.endsWith("/neck")||index!=0)return;
            float minX=100,maxX=-100,minZ=100,maxZ=-100;
            for(var face:cube.polygons)for(var vertex:face.vertices()){minX=Math.min(minX,vertex.x());maxX=Math.max(maxX,vertex.x());minZ=Math.min(minZ,vertex.z());maxZ=Math.max(maxZ,vertex.z());}
            if(Math.abs(minX+1.75F)>.001||Math.abs(maxX-1.75F)>.001||Math.abs(minZ+1.5F)>.001||Math.abs(maxZ-2.5F)>.001)throw new AssertionError("Deer neck inset changed");
        });
        boolean[] muzzle={false},head={false};root.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->{
            if(!path.endsWith("/head"))return;
            if(index==0){
                if(cube.minX!=-2||cube.maxX!=2||cube.minY!=-2||cube.maxY!=2||cube.minZ!=-5||cube.maxZ!=2)throw new AssertionError("Deer head geometry changed");
                for(var face:cube.polygons)if(face.normal().z()<-.99F){
                    float minU=128,maxU=0,minV=96,maxV=0;
                    for(var vertex:face.vertices()){minU=Math.min(minU,vertex.u()*128);maxU=Math.max(maxU,vertex.u()*128);minV=Math.min(minV,vertex.v()*96);maxV=Math.max(maxV,vertex.v()*96);}
                    if(Math.abs(minU-94)>.001||Math.abs(maxU-102)>.001||Math.abs(minV-46)>.001||Math.abs(maxV-54)>.001)throw new AssertionError("Deer head UV region changed");
                    head[0]=true;
                }
            }
            if(index!=1)return;
            if(cube.minX!=-1.5F||cube.maxX!=1.5F||cube.minY!=0||cube.maxY!=2||cube.minZ!=-8||cube.maxZ!=-3)throw new AssertionError("Deer muzzle geometry changed");
            for(var face:cube.polygons)if(face.normal().z()<-.99F){
                float minU=128,maxU=0,minV=96,maxV=0;
                for(var vertex:face.vertices()){minU=Math.min(minU,vertex.u()*128);maxU=Math.max(maxU,vertex.u()*128);minV=Math.min(minV,vertex.v()*96);maxV=Math.max(maxV,vertex.v()*96);}
                if(Math.abs(minU-90)>.001||Math.abs(maxU-96)>.001||Math.abs(minV-74)>.001||Math.abs(maxV-78)>.001)throw new AssertionError("Deer muzzle UV region changed");
                muzzle[0]=true;
            }
        });
        if(!muzzle[0]||!head[0])throw new AssertionError("Deer painted face missing");
        blockbranch.Blockbranch.LOGGER.info("DEER_POSE_REGRESSION PASS grounded=true standingRestStanding=true");
    }
}
