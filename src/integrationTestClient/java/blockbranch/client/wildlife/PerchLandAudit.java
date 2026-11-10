package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Vector3f;

/** Actual rendered land contact, frozen swimming/special meshes and defect controls. */
final class PerchLandAudit {
    static void verify(PerchRenderer renderer){
        var root=PerchModel.layer().bakeRoot();var model=new PerchModel(root);
        var old=PerchLandReference.layer().bakeRoot();var reference=new PerchLandReference(old);
        var state=new LivingEntityRenderState();state.scale=state.ageScale=1;int protectedPoses=0,landPoses=0;
        for(boolean water:new boolean[]{false,true})for(float death:new float[]{0,5})for(boolean upside:new boolean[]{false,true})for(boolean spin:new boolean[]{false,true})for(int age=0;age<32;age++){
            state.isInWater=water;state.deathTime=death;state.isUpsideDown=upside;state.isAutoSpinAttack=spin;state.ageInTicks=age*.7F;
            model.setupAnim(state);reference.setupAnim(state);boolean land=PerchModel.landPose(state);
            sameMesh(points(renderer,root,state,0),points(renderer,old,state,0),land);
            if(land)landPoses++;else protectedPoses++;
        }
        state.isInWater=false;state.deathTime=0;state.isUpsideDown=state.isAutoSpinAttack=false;int contacts=0;
        for(float scale:new float[]{.5F,1,2})for(float yaw:new float[]{0,90,180,270})for(int age=0;age<256;age++){
            state.scale=scale;state.ageInTicks=age/8F;model.setupAnim(state);
            var mesh=points(renderer,root,state,yaw);float bodyMin=minimum(mesh,"/body:");float allMin=minimum(mesh,"");
            if(Math.abs(bodyMin-.00065F*scale)>.00001F||allMin<-.000001F)throw new AssertionError("Land fish must contact floor without sinking: "+bodyMin+" / "+allMin);
            float angle=root.getChild("tail").yRot;if(angle>0||angle<-.35001F)throw new AssertionError("Land tail must sweep upward within its lower maximum bend");if(maximum(mesh,"/tail:")>maximum(mesh,"/body:")+.002F*scale)throw new AssertionError("Land tail tip must remain near resting body height");contacts++;
        }
        state.scale=1;state.ageInTicks=7;reference.setupAnim(state);
        if(minimum(points(renderer,old,state,0),"")>=-.01F)throw new AssertionError("Original appendage penetration control missed");
        var legacy=new PoseStack();legacy.rotateDegrees(com.mojang.math.Axis.YP,180);legacy.translate(.1,.1,-.1);legacy.rotateDegrees(com.mojang.math.Axis.ZP,90);legacy.scale(-1,-1,1);renderer.scale(state,legacy);legacy.translate(0,-1.501F,0);
        if(minimum(points(old,legacy),"/body:")<.05F)throw new AssertionError("Original hovering body control missed");
        for(boolean water:new boolean[]{false,true}){
            state.isInWater=false;model.setupAnim(state);state.isInWater=water;model.setupAnim(state);reference.setupAnim(state);sameMesh(points(renderer,root,state,0),points(renderer,old,state,0),!water);
        }
        Blockbranch.LOGGER.info("PERCH_LAND_AUDIT PASS protectedPoses={} landPoses={} contactPoses={} bodyOnFloor=true appendagesClear=true originalBodyGapDetected=true originalAppendageIntrusionDetected=true waterReset=true sevenCuboidsUnchanged=true",protectedPoses,landPoses,contacts);
    }
    private static Map<String,List<Vector3f>> points(PerchRenderer renderer,ModelPart root,LivingEntityRenderState state,float yaw){
        var pose=new PoseStack();pose.scale(state.scale,state.scale,state.scale);renderer.setupRotations(state,pose,yaw,state.scale);pose.scale(-1,-1,1);renderer.scale(state,pose);pose.translate(0,-1.501F,0);return points(root,pose);
    }
    private static Map<String,List<Vector3f>> points(ModelPart root,PoseStack pose){
        var result=new TreeMap<String,List<Vector3f>>();root.visit(pose,(matrix,path,index,cube)->{
            var vertices=new ArrayList<Vector3f>();for(var face:cube.polygons)for(var vertex:face.vertices())vertices.add(matrix.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())));
            result.put(path+":"+index,vertices);
        });return result;
    }
    private static float minimum(Map<String,List<Vector3f>> points,String prefix){
        float min=Float.POSITIVE_INFINITY;for(var entry:points.entrySet())if(entry.getKey().startsWith(prefix))for(var p:entry.getValue())min=Math.min(min,p.y);return min;
    }
    private static float maximum(Map<String,List<Vector3f>> points,String prefix){
        float max=Float.NEGATIVE_INFINITY;for(var entry:points.entrySet())if(entry.getKey().startsWith(prefix))for(var p:entry.getValue())max=Math.max(max,p.y);return max;
    }
    private static void sameMesh(Map<String,List<Vector3f>> a,Map<String,List<Vector3f>> b,boolean land){
        if(!a.keySet().equals(b.keySet())||a.size()!=7)throw new AssertionError("Keep seven accepted fish cuboids");
        for(var key:a.keySet()){
            if(land&&(key.startsWith("/tail:")||key.startsWith("/left_fin:")||key.startsWith("/right_fin:")))continue;
            if(a.get(key).size()!=b.get(key).size())throw new AssertionError("Fish polygon inventory changed");
            for(int i=0;i<a.get(key).size();i++)if(a.get(key).get(i).distanceSquared(b.get(key).get(i))>1e-12)throw new AssertionError("Protected fish mesh changed: "+key);
        }
    }
    private PerchLandAudit(){}
}
