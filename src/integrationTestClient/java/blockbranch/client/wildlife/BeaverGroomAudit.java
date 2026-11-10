package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Protect approved poses while checking the quiet land forepaw gesture. */
final class BeaverGroomAudit {
    static void verify(){
        var root=BeaverModel.layer().bakeRoot();var model=new BeaverModel(root);
        var old=BeaverQuietReference.layer().bakeRoot();var reference=new BeaverQuietReference(old);
        var state=new BeaverRenderer.State();int protectedPoses=0,gesturePoses=0;
        for(boolean baby:new boolean[]{false,true})for(boolean water:new boolean[]{false,true})
        for(int activity=0;activity<3;activity++)for(float yaw:new float[]{-30,0,30})
        for(float speed:new float[]{0,.125F,.25F,.5F,1})for(int phase=0;phase<16;phase++){
            state.isBaby=baby;state.swimming=water;state.activity=activity;state.yRot=yaw;state.xRot=yaw/2;
            state.walkAnimationSpeed=speed;state.walkAnimationPos=phase;state.ageInTicks=phase*5;
            model.setupAnim(state);reference.setupAnim(state);BeaverStrideAudit.normalizeReference(old,state);
            boolean allowed=!water&&activity==1&&speed<.25F;
            sameMesh(root,old,allowed);
            if(allowed){assertGesture(root,speed==0);gesturePoses++;}else protectedPoses++;
        }
        state.swimming=false;state.activity=1;state.walkAnimationSpeed=0;reference.setupAnim(state);BeaverStrideAudit.normalizeReference(old,state);
        boolean detected=false;try{assertGesture(old,true);}catch(AssertionError expected){detected=true;}
        if(!detected)throw new AssertionError("Gesture audit failed to reject the unchanged ground paw");
        for(int activity:new int[]{0,2}){
            state.activity=1;model.setupAnim(state);state.activity=activity;model.setupAnim(state);
            reference.setupAnim(state);BeaverStrideAudit.normalizeReference(old,state);sameMesh(root,old,false);
        }
        BeaverWaterPoseAudit.verify();
        Blockbranch.LOGGER.info("BEAVER_GROOM_AUDIT PASS protectedPoses={} gesturePoses={} onlyRightForepawMoves=true rootAttached=true threeSupportFeetPreserved=true aboveGround=true reset=true groundPawDetected=true",protectedPoses,gesturePoses);
    }
    private static void assertGesture(ModelPart root,boolean stationary){
        var paw=root.getChild("front_right");
        if(!paw.visible)throw new AssertionError("Gesture forepaw must remain visible");
        float[] bottom={Float.NEGATIVE_INFINITY},front={Float.POSITIVE_INFINITY};
        float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.equals("/front_right"))return;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var p=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                for(int axis=0;axis<3;axis++){bounds[axis]=Math.min(bounds[axis],p.get(axis));bounds[axis+3]=Math.max(bounds[axis+3],p.get(axis));}
                bottom[0]=Math.max(bottom[0],p.y);front[0]=Math.min(front[0],p.z);
            }
        });
        if(bounds[0]>=4||bounds[3]<=-4||bounds[1]>=22||bounds[4]<=16.025F||bounds[2]>=1||bounds[5]<=-6)
            throw new AssertionError("Raised paw must overlap the forebody at its attachment");
        if(bottom[0]>=24||stationary&&(bottom[0]>23||front[0]> -7))throw new AssertionError("Gesture paw must rise toward cheek above soil");
    }
    private static Map<String,List<Vector3f>> points(ModelPart root){
        var result=new TreeMap<String,List<Vector3f>>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            var vertices=new ArrayList<Vector3f>();
            for(var face:cube.polygons)for(var vertex:face.vertices())vertices.add(pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())));
            result.put(path+":"+index,vertices);
        });return result;
    }
    private static void sameMesh(ModelPart current,ModelPart reference,boolean allowPaw){
        var a=points(current);var b=points(reference);
        if(!a.keySet().equals(b.keySet()))throw new AssertionError("Approved beaver cube inventory changed");
        for(var key:a.keySet()){
            if(allowPaw&&key.startsWith("/front_right:"))continue;
            if(a.get(key).size()!=b.get(key).size())throw new AssertionError("Approved beaver polygon inventory changed");
            for(int i=0;i<a.get(key).size();i++)if(a.get(key).get(i).distanceSquared(b.get(key).get(i))>1e-12)throw new AssertionError("Protected beaver mesh changed: "+key);
        }
        for(String name:new String[]{"front_left","front_right","back_left","back_right"})
            if(current.getChild(name).visible!=reference.getChild(name).visible)throw new AssertionError("Protected beaver visibility changed");
    }
    private BeaverGroomAudit(){}
}
