package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Frozen pre-change poses protect land and active swimming while auditing water rest. */
final class BeaverWaterPoseAudit {
    static void verify(){
        int poses=0;
        for(boolean baby:new boolean[]{false,true})for(boolean water:new boolean[]{false,true})
        for(int activity=0;activity<=2;activity++)for(int phase=0;phase<16;phase++)for(float yaw:new float[]{-30,0,30}){
            var state=new BeaverRenderer.State();state.isBaby=baby;state.swimming=water;state.activity=activity;
            state.ageInTicks=phase*5;state.walkAnimationPos=phase;state.walkAnimationSpeed=.8F;state.yRot=yaw;state.xRot=15;
            var current=BeaverModel.layer().bakeRoot();new BeaverModel(current).setupAnim(state);
            var reference=BeaverModel.layer().bakeRoot();legacy(reference,state);BeaverStrideAudit.normalizeReference(reference,state);
            boolean waterRest=water&&activity==2;
            var actual=points(current);var expected=points(reference);
            if(!actual.keySet().equals(expected.keySet()))throw new AssertionError("Beaver part inventory changed");
            for(var path:actual.keySet()){
                if(waterRest&&path.startsWith("/head"))continue;
                var a=actual.get(path);var b=expected.get(path);
                if(a.size()!=b.size())throw new AssertionError("Beaver polygon inventory changed");
                for(int i=0;i<a.size();i++)if(a.get(i).distanceSquared(b.get(i))>1e-12)throw new AssertionError("Protected beaver pose changed: "+path);
            }
            for(String leg:new String[]{"front_left","front_right","back_left","back_right"})
                if(current.getChild(leg).visible!=reference.getChild(leg).visible)throw new AssertionError("Beaver leg visibility changed");
            if(waterRest){
                if(noseY(current)>=noseY(reference))throw new AssertionError("Floating rest must raise nose");
                var headStack=new PoseStack();current.getChild("head").translateAndRotate(headStack);
                var joint=headStack.last().pose().transformPosition(new Vector3f(0,.5F/16,3F/16)).mul(16);
                // Rear head-face center must remain inside the accepted forebody rest box.
                if(joint.x< -4||joint.x>4||joint.y<18.025F||joint.y>24||joint.z< -6||joint.z>1)throw new AssertionError("Raised head detached from forebody");
            }
            poses++;
        }
        // At the observed immersion, the frozen kit rest must expose the submerged-nose fault.
        // Renderer origin: 1.501m; kit scale: .75 * .5; native immersion: .09494m.
        var old=BeaverModel.layer().bakeRoot();var state=new BeaverRenderer.State();state.swimming=true;state.activity=2;legacy(old,state);
        if((24.016F-noseY(old))*.375F/16-.09494F>=0)throw new AssertionError("Submerged-nose control missed");
        Blockbranch.LOGGER.info("BEAVER_WATER_POSE_AUDIT PASS poses={} exactLandAndActiveMesh=true waterRestOnlyHeadChanges=true noseRaised=true headAttached=true submergedNoseControl=true",poses);
    }
    private static Map<String,List<Vector3f>> points(ModelPart root){
        var result=new TreeMap<String,List<Vector3f>>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            var vertices=new ArrayList<Vector3f>();
            for(var face:cube.polygons)for(var vertex:face.vertices())vertices.add(pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())));
            result.put(path+":"+index,vertices);
        });return result;
    }
    private static float noseY(ModelPart root){
        var stack=new PoseStack();root.getChild("head").translateAndRotate(stack);
        return stack.last().pose().transformPosition(new Vector3f(0,.5F/16,-5F/16)).y*16;
    }
    /** Pose rules frozen from main 516e0a36; mesh is the unchanged accepted layer. */
    private static void legacy(ModelPart root,BeaverRenderer.State state){
        root.getAllParts().forEach(ModelPart::resetPose);
        var head=root.getChild("head");head.yRot=state.yRot*Mth.DEG_TO_RAD;head.xRot=state.xRot*Mth.DEG_TO_RAD;
        float stride=state.swimming?Mth.sin(state.ageInTicks*.4F)*.4F:Mth.cos(state.walkAnimationPos*.7F)*state.walkAnimationSpeed;
        root.getChild("front_left").xRot=root.getChild("back_right").xRot=stride;
        root.getChild("front_right").xRot=root.getChild("back_left").xRot=-stride;
        root.getChild("tail").yRot=state.swimming?Mth.sin(state.ageInTicks*.25F)*.18F:0;
        if(state.activity==1)head.xRot=.3F+.08F*Mth.sin(state.ageInTicks*.35F);
        for(String leg:new String[]{"front_left","front_right","back_left","back_right"})root.getChild(leg).visible=state.activity!=2;
        if(state.activity==2){root.getChild("body").y+=2;head.y+=2;head.xRot=.2F;root.getChild("tail").y+=1;}
    }
    private BeaverWaterPoseAudit(){}
}
