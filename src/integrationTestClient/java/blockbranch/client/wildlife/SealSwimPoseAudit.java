package blockbranch.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Real rendered mesh: travel attitude, anchored torso, preserved gaze and land/rest poses. */
final class SealSwimPoseAudit {
    private record Mesh(List<Float> vertices,Vector3f front,Vector3f back,Vector3f gaze,Vector3f center){}
    static void verify(SealRenderer renderer){
        int poses=0;
        for(boolean baby:new boolean[]{false,true})for(boolean water:new boolean[]{false,true})
        for(int activity:new int[]{0,2})for(float yaw:new float[]{-30,0,30})for(float look:new float[]{-30,0,30}){
            var state=new SealRenderer.State();state.scale=1;state.ageScale=baby?.5F:1;state.isBaby=baby;
            state.swimming=state.isInWater=water;state.activity=activity;state.yRot=yaw;state.xRot=look;
            var reference=mesh(renderer,state);
            for(float tilt:new float[]{-35,-26,0,26,35}){
                state.swimPitch=tilt;var result=mesh(renderer,state);poses++;
                if(!water||activity==2){
                    if(!reference.vertices.equals(result.vertices))throw new AssertionError("Swim pitch changed a land/rest mesh");
                    continue;
                }
                var axis=new Vector3f(result.front).sub(result.back);
                float measured=(float)Math.toDegrees(Math.atan2(-axis.y,axis.z));
                if(Math.abs(measured-tilt)>.001F)throw new AssertionError("Rendered torso pitch mismatch: "+measured+" expected "+tilt);
                if(result.center.distance(reference.center)>1e-5F)throw new AssertionError("Swimming rotation displaced torso anchor");
                float yr=yaw*Mth.DEG_TO_RAD,xr=look*Mth.DEG_TO_RAD;
                var expected=new Vector3f(-Mth.sin(yr)*Mth.cos(xr),-Mth.sin(xr),Mth.cos(yr)*Mth.cos(xr));
                if(result.gaze.distance(expected)>1e-4F)throw new AssertionError("Swim tilt changed gaze direction: "+result.gaze+" expected "+expected);
            }
        }
        blockbranch.Blockbranch.LOGGER.info("SEAL_SWIM_AUDIT PASS transforms={} adultPup=true torsoAnchor=true gazeDirection=true landRestUnchanged=true",poses);
    }
    private static Mesh mesh(SealRenderer renderer,SealRenderer.State state){
        var root=SealModel.layer().bakeRoot();new SealModel(root).setupAnim(state);
        var pose=new PoseStack();pose.scale(state.scale,state.scale,state.scale);renderer.setupRotations(state,pose,0,1);
        pose.scale(-1,-1,1);renderer.scale(state,pose);pose.translate(0,-1.501F,0);
        var center=pose.last().pose().transformPosition(new Vector3f(0,19.5F/16,0));
        var vertices=new ArrayList<Float>();var front=new Vector3f();var back=new Vector3f();var gaze=new Vector3f();int[] ends={0,0},heads={0};
        root.visit(pose,(matrix,path,index,cube)->{
            for(var face:cube.polygons){
                for(var vertex:face.vertices()){
                    var p=matrix.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ()));
                    vertices.add(p.x);vertices.add(p.y);vertices.add(p.z);
                    if(path.equals("/body")&&index==0&&Math.abs(face.normal().z())>.99F){
                        int end=face.normal().z()<0?0:1;(end==0?front:back).add(p);ends[end]++;
                    }
                }
                if(path.equals("/head")&&index==0&&face.normal().z()<-.99F){
                    gaze.set(matrix.pose().transformDirection(new Vector3f(face.normal())).normalize());heads[0]++;
                }
            }
        });
        if(ends[0]!=4||ends[1]!=4||heads[0]!=1)throw new AssertionError("Finite torso/head surfaces required");
        return new Mesh(vertices,front.div(4),back.div(4),gaze,center);
    }
    private SealSwimPoseAudit(){}
}
