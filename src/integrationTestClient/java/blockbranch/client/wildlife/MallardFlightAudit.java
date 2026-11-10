package blockbranch.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Actual wing outer-edge clearance and non-flight pose preservation. */
final class MallardFlightAudit {
    record Bounds(float back,float min,float max){}
    static void verify(){
        var root=MallardModel.layer().bakeRoot();var model=new MallardModel(root);var state=new MallardRenderer.State();state.activity=3;
        boolean above=false,below=false;int samples=0;
        for(int phase=0;phase<256;phase++){
            state.ageInTicks=(float)(phase*Math.PI*2/(256*.8));model.setupAnim(state);var b=measure(root);
            above|=b.max<b.back-.25F;below|=b.min>b.back+3;
            if(Math.abs(root.getChild("left_wing").zRot+root.getChild("right_wing").zRot)>1e-6F)throw new AssertionError("Flight wings must mirror");
            samples++;
        }
        if(!above||!below)throw new AssertionError("Flight stroke must pass both above the back and down along the flanks");
        // Capture clock windows include changing render partial ticks, not one exact age.
        for(int clock:new int[]{1,5})for(int i=0;i<=64;i++){
            state.ageInTicks=clock+i/64F;model.setupAnim(state);var b=measure(root);
            if(clock==1&&b.max>=b.back)throw new AssertionError("Upper capture window has wings below the back");
            if(clock==5&&b.min<=b.back+3)throw new AssertionError("Lower capture window has raised wings");
            samples++;
        }
        int[] cubes={0};root.visit(new PoseStack(),(pose,path,index,cube)->cubes[0]++);
        if(cubes[0]!=7)throw new AssertionError("Mallard geometry must remain seven cuboids");
        for(int activity:new int[]{0,1,2}){
            state.activity=3;model.setupAnim(state);state.activity=activity;model.setupAnim(state);
            var reference=MallardModel.layer().bakeRoot();new MallardModel(reference).setupAnim(state);
            if(!vertices(root).equals(vertices(reference)))throw new AssertionError("Flight rotation leaked into activity "+activity);
        }
        blockbranch.Blockbranch.LOGGER.info("MALLARD_FLIGHT_AUDIT PASS ages={} aboveBack=true lowerStroke=true mirrored=true cuboids=7 nonFlightReset=true",samples);
    }
    static Bounds measure(ModelPart root){
        float[] back={Float.POSITIVE_INFINITY},min={Float.POSITIVE_INFINITY},max={Float.NEGATIVE_INFINITY};int[] tips={0};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            boolean wing=path.equals("/left_wing")||path.equals("/right_wing");
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                float y=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y*16;
                if(!Float.isFinite(y))throw new AssertionError("Finite mallard mesh required");
                if(path.equals("/body"))back[0]=Math.min(back[0],y);
                if(wing&&vertex.y()==4){min[0]=Math.min(min[0],y);max[0]=Math.max(max[0],y);tips[0]++;}
            }
        });
        if(tips[0]<8||!Float.isFinite(back[0]))throw new AssertionError("Body and both outer wing edges required");
        return new Bounds(back[0],min[0],max[0]);
    }
    private static List<Float> vertices(ModelPart root){
        var result=new ArrayList<Float>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var p=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ()));
                result.add(p.x);result.add(p.y);result.add(p.z);
            }
        });return result;
    }
    private MallardFlightAudit(){}
}
