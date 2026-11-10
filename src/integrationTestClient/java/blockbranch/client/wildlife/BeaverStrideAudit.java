package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Actual transformed foot corners relative to the model's Y=24 sole plane. */
final class BeaverStrideAudit {
    private static final List<String> LEGS=List.of("front_left","front_right","back_left","back_right");
    static void verify(){
        var root=BeaverModel.layer().bakeRoot();var model=new BeaverModel(root);
        var old=BeaverWalkReference.layer().bakeRoot();var reference=new BeaverWalkReference(old);
        var state=new BeaverRenderer.State();int protectedPoses=0,movingPoses=0,motionSamples=0;
        for(boolean baby:new boolean[]{false,true})for(boolean water:new boolean[]{false,true})
        for(int activity=0;activity<3;activity++)for(float yaw:new float[]{-30,0,30})
        for(float speed:new float[]{0,.25F,.5F,1})for(int phase=0;phase<8;phase++){
            state.isBaby=baby;state.swimming=water;state.activity=activity;state.yRot=yaw;state.xRot=yaw/2;
            state.walkAnimationSpeed=speed;state.walkAnimationPos=phase;state.ageInTicks=phase*5;
            model.setupAnim(state);reference.setupAnim(state);boolean allowed=!water&&activity==0&&speed>0;
            sameMesh(root,old,allowed);
            if(allowed){assertWalking(root);movingPoses++;}else protectedPoses++;
        }
        boolean lifted=false;
        for(boolean baby:new boolean[]{false,true})for(float speed:new float[]{.25F,.5F,1})for(int phase=0;phase<256;phase++){
            state.isBaby=baby;state.swimming=false;state.activity=0;state.walkAnimationSpeed=speed;
            state.walkAnimationPos=(float)(phase*Math.PI*2/(256*.7));model.setupAnim(state);assertWalking(root);
            for(float value:footBottoms(root).values())lifted|=value<23.2F;motionSamples++;
        }
        if(!lifted)throw new AssertionError("Stride must lift a swing foot, not slide all four on the floor");
        state.walkAnimationSpeed=1;state.walkAnimationPos=(float)(Math.acos(Math.PI/4)/.7);reference.setupAnim(state);
        if(bottom(old)<24.8F)throw new AssertionError("Original foot-burial control not detected");
        for(int activity:new int[]{1,2}){
            state.activity=0;model.setupAnim(state);state.activity=activity;model.setupAnim(state);
            reference.setupAnim(state);sameMesh(root,old,false);
        }
        state.activity=0;state.walkAnimationSpeed=0;model.setupAnim(state);reference.setupAnim(state);sameMesh(root,old,false);
        BeaverGroomAudit.verify();
        Blockbranch.LOGGER.info("BEAVER_STRIDE_AUDIT PASS protectedPoses={} movingPoses={} motionSamples={} onlyWalkingFeetChange=true flatContactPads=true stanceFeetGrounded=true swingFeetLift=true rootsAttached=true reset=true originalBurialDetected=true",protectedPoses,movingPoses,motionSamples);
    }
    private static void assertWalking(ModelPart root){
        int support=0;
        for(var entry:footBottoms(root).entrySet()){
            if(entry.getValue()>24.0001F)throw new AssertionError("Walking foot buried: "+entry);
            if(Math.abs(entry.getValue()-24)<.0001F)support++;
        }
        if(support<2)throw new AssertionError("At least one diagonal must retain ground contact");
        for(String name:LEGS){var leg=root.getChild(name);if(!leg.visible||leg.y<16.025F||leg.y>22||Math.abs(leg.x)>3.0001F||leg.z< -4.0001F||leg.z>5.0001F)throw new AssertionError("Walking foot root detached from body");}
    }
    static float bottom(ModelPart root){return Collections.max(footBottoms(root).values());}
    private static Map<String,Float> footBottoms(ModelPart root){
        var result=new HashMap<String,Float>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!LEGS.contains(path.substring(1)))return;
            float bottom=Float.NEGATIVE_INFINITY;
            for(var face:cube.polygons)for(var vertex:face.vertices())bottom=Math.max(bottom,pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y*16);
            result.put(path,bottom);
        });
        if(result.size()!=4)throw new AssertionError("All four beaver feet required");return result;
    }
    private static Map<String,List<Vector3f>> points(ModelPart root){
        var result=new TreeMap<String,List<Vector3f>>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            var vertices=new ArrayList<Vector3f>();for(var face:cube.polygons)for(var vertex:face.vertices())vertices.add(pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())));
            result.put(path+":"+index,vertices);
        });return result;
    }
    private static void sameMesh(ModelPart current,ModelPart reference,boolean walking){
        var a=points(current);var b=points(reference);if(!a.keySet().equals(b.keySet()))throw new AssertionError("Accepted beaver cuboid inventory changed");
        for(var key:a.keySet()){
            if(walking&&LEGS.stream().anyMatch(name->key.startsWith("/"+name+":")))continue;
            if(a.get(key).size()!=b.get(key).size())throw new AssertionError("Accepted polygon inventory changed");
            for(int i=0;i<a.get(key).size();i++)if(a.get(key).get(i).distanceSquared(b.get(key).get(i))>1e-12)throw new AssertionError("Protected beaver mesh changed: "+key);
        }
        for(String name:LEGS){
            var actual=current.getChild(name);var expected=reference.getChild(name);
            if(actual.xScale!=expected.xScale||actual.yScale!=expected.yScale||actual.zScale!=expected.zScale||actual.visible!=expected.visible)
                throw new AssertionError("Accepted foot size/visibility changed");
            if(walking&&actual.xRot!=0)throw new AssertionError("Walking contact pad must remain level");
        }
    }
    // Earlier quiet/water references predate the intentional walking-foot correction.
    // Their protected parts remain checked; this audit independently guards other modes.
    static void normalizeReference(ModelPart root,BeaverRenderer.State state){
        if(!state.swimming&&state.activity==0&&state.walkAnimationSpeed>0){
            float stride=Mth.cos(state.walkAnimationPos*.7F)*state.walkAnimationSpeed;
            for(String name:LEGS)root.getChild(name).xRot=0;
            float spread=.525F*Mth.clamp(state.walkAnimationSpeed*4,0,1);
            root.getChild("front_left").x=root.getChild("back_left").x=2.475F+spread;
            root.getChild("front_right").x=root.getChild("back_right").x=-2.475F-spread;
            root.getChild("front_left").z=-3+stride;root.getChild("back_right").z=4+stride;
            root.getChild("front_right").z=-3-stride;root.getChild("back_left").z=4-stride;
            float swing=Mth.sin(state.walkAnimationPos*.7F)*Mth.clamp(state.walkAnimationSpeed,0,1);
            root.getChild("front_left").y=root.getChild("back_right").y=22-.9F*Math.max(0,swing);
            root.getChild("front_right").y=root.getChild("back_left").y=22-.9F*Math.max(0,-swing);
        }
    }
    private BeaverStrideAudit(){}
}
