package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Adult flight silhouette, stroke-rhythm preservation, and protected land/juvenile mesh. */
final class MallardPostureAudit {
    static void verify(){
        var root=MallardModel.layer().bakeRoot();var model=new MallardModel(root);
        var old=MallardPostureReference.layer().bakeRoot();var reference=new MallardPostureReference(old);
        var state=new MallardRenderer.State();int protectedPoses=0,flightPoses=0;
        for(boolean baby:new boolean[]{false,true})for(boolean water:new boolean[]{false,true})
        for(int activity=0;activity<4;activity++)for(float yaw:new float[]{-60,0,60})
        for(float pitch:new float[]{-35,0,35})for(float speed:new float[]{0,.5F,1})for(int phase=0;phase<8;phase++){
            state.isBaby=baby;state.isInWater=water;state.activity=activity;state.yRot=yaw;state.xRot=pitch;
            state.walkAnimationSpeed=speed;state.walkAnimationPos=phase;state.ageInTicks=phase*7;
            model.setupAnim(state);reference.setupAnim(state);boolean allowed=!baby&&activity==3;
            sameMesh(root,old,allowed);
            if(allowed){
                assertFlight(root);
                for(String name:new String[]{"left_wing","right_wing"})if(root.getChild(name).zRot!=old.getChild(name).zRot)throw new AssertionError("Existing wing stroke angle changed");
                flightPoses++;
            }else protectedPoses++;
        }
        state.isBaby=false;state.activity=3;state.xRot=state.yRot=0;reference.setupAnim(state);
        boolean detected=false;try{assertFlight(old);}catch(AssertionError expected){detected=true;}
        if(!detected)throw new AssertionError("Flight audit missed the original dangling feet");
        for(int activity:new int[]{0,1,2}){
            state.activity=3;model.setupAnim(state);state.activity=activity;model.setupAnim(state);
            reference.setupAnim(state);sameMesh(root,old,false);
        }
        MallardJuvenileAudit.verify();
        Blockbranch.LOGGER.info("MALLARD_POSTURE_AUDIT PASS protectedPoses={} flightPoses={} onlyAdultFlightPartsChange=true wingRhythmExact=true headAttached=true tuckedTrailingFeet=true noLandLeak=true originalFeetDetected=true",protectedPoses,flightPoses);
    }
    private static void assertFlight(ModelPart root){
        var stack=new PoseStack();root.getChild("head").translateAndRotate(stack);
        // A point just inside the torso's forward face must also be inside the
        // rotated head box: the flight neck remains connected during gaze turns.
        var p=new Matrix4f(stack.last().pose()).invert().transformPosition(new Vector3f(0,16.25F/16,-5.9F/16)).mul(16);
        if(Math.abs(p.x)>2.5F||p.y< -5||p.y>1||p.z< -3||p.z>2)throw new AssertionError("Flight head detached from torso");
        for(String name:new String[]{"left_leg","right_leg"}){
            var leg=root.getChild(name);
            if(!leg.visible||Math.abs(leg.x)>4||leg.y<16||leg.y>22||leg.z< -6||leg.z>6)throw new AssertionError("Flight foot root detached");
        }
        var bounds=new HashMap<String,float[]>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(path.endsWith("_wing")){
                for(var face:cube.polygons)for(var vertex:face.vertices())if(pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y*16>24.0001F)throw new AssertionError("Unfolded wing must clear the sole plane");
            }
            if(!path.endsWith("_leg"))return;
            var b=new float[]{Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var q=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                b[0]=Math.min(b[0],q.z);b[1]=Math.max(b[1],q.z);b[2]=Math.max(b[2],q.y);
            }
            bounds.put(path,b);
        });
        if(bounds.size()!=2)throw new AssertionError("Both flight feet required");
        for(var b:bounds.values())if(b[0]<2||b[1]<6||b[1]>7.5F||b[2]>22.75F)throw new AssertionError("Feet must trail and tuck close to belly: "+Arrays.toString(b));
    }
    private static Map<String,List<Vector3f>> points(ModelPart root){
        var result=new TreeMap<String,List<Vector3f>>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            var vertices=new ArrayList<Vector3f>();
            for(var face:cube.polygons)for(var vertex:face.vertices())vertices.add(pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())));
            result.put(path+":"+index,vertices);
        });return result;
    }
    private static void sameMesh(ModelPart current,ModelPart reference,boolean flight){
        var a=points(current);var b=points(reference);
        if(!a.keySet().equals(b.keySet()))throw new AssertionError("Mallard cuboid inventory changed");
        for(var key:a.keySet()){
            if(flight&&(key.startsWith("/head:")||key.startsWith("/left_leg:")||key.startsWith("/right_leg:")||key.startsWith("/left_wing:")||key.startsWith("/right_wing:")))continue;
            if(a.get(key).size()!=b.get(key).size())throw new AssertionError("Mallard polygon inventory changed");
            for(int i=0;i<a.get(key).size();i++)if(a.get(key).get(i).distanceSquared(b.get(key).get(i))>1e-12)throw new AssertionError("Protected mallard mesh changed: "+key);
        }
    }
    private MallardPostureAudit(){}
}
