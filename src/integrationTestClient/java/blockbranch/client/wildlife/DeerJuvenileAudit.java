package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Protect adult meshes, calf neck/limbs and the existing grazing/resting poses. */
final class DeerJuvenileAudit {
    static void verify(){
        var root=DeerModel.layer().bakeRoot();var model=new DeerModel(root);
        var old=DeerAgeReference.layer().bakeRoot();var reference=new DeerAgeReference(old);
        var state=new DeerRenderer.State();int adultPoses=0,calfPoses=0;
        for(boolean baby:new boolean[]{false,true})for(boolean stag:new boolean[]{false,true})
        for(boolean water:new boolean[]{false,true})for(int activity=0;activity<3;activity++)
        for(float yaw:new float[]{-30,0,30})for(float speed:new float[]{0,.5F,1})for(int phase=0;phase<8;phase++){
            state.isBaby=baby;state.stag=stag;state.isInWater=water;state.activity=activity;
            state.yRot=yaw;state.xRot=yaw/2;state.walkAnimationSpeed=speed;state.walkAnimationPos=phase;state.ageInTicks=phase*7;
            model.setupAnim(state);reference.setupAnim(state);sameMesh(root,old,baby);
            if(baby){assertAttached(root);calfPoses++;}else adultPoses++;
        }
        state.isBaby=true;state.activity=0;state.xRot=state.yRot=0;
        model.setupAnim(state);reference.setupAnim(state);assertYoung(root,old);
        boolean detected=false;try{assertYoung(old,old);}catch(AssertionError expected){detected=true;}
        if(!detected)throw new AssertionError("Age audit missed adult-proportioned calf control");
        state.isBaby=false;model.setupAnim(state);reference.setupAnim(state);sameMesh(root,old,false);
        DeerGrazeAudit.verify();DeerRestAudit.verify();
        Blockbranch.LOGGER.info("DEER_JUVENILE_AUDIT PASS adultPoses={} calfPoses={} onlyCalfHeadAndTorsoChange=true compactBody=true broadYoungFace=true headAttached=true adultReset=true adultProportionControl=true grazingAndRestRetained=true",adultPoses,calfPoses);
    }
    private static void assertAttached(ModelPart root){
        var head=root.getChild("neck").getChild("head");var stack=new PoseStack();head.translateAndRotate(stack);
        // Lower rear head-face center stays inside the actual inset neck box.
        var p=stack.last().pose().transformPosition(new Vector3f(0,2F/16,2F/16)).mul(16);
        if(Math.abs(p.x)>1.75F||p.y< -8||p.y>2||p.z< -1.5F||p.z>2.5F)throw new AssertionError("Calf head detached from neck");
        var body=root.getChild("body");
        if(4*body.xScale<3||8*body.zScale<6||-7*body.zScale> -5)throw new AssertionError("Calf torso must retain neck/leg attachment overlap");
    }
    private static void assertYoung(ModelPart root,ModelPart old){
        var face=bounds(root,"/neck/head",0);var adultFace=bounds(old,"/neck/head",0);
        var torso=bounds(root,"/body",0);var adultTorso=bounds(old,"/body",0);
        float ratio=(face[3]-face[0])/(torso[3]-torso[0]);
        float oldRatio=(adultFace[3]-adultFace[0])/(adultTorso[3]-adultTorso[0]);
        if(ratio<oldRatio*1.2F||ratio>.8F)throw new AssertionError("Calf face must be broader relative to torso");
        float length=torso[5]-torso[2],oldLength=adultTorso[5]-adultTorso[2];
        if(length>oldLength*.95F||length<oldLength*.85F)throw new AssertionError("Calf torso must remain compact without an extreme squeeze");
        if(face[5]-face[2]>(adultFace[5]-adultFace[2])*.97F)throw new AssertionError("Calf face must be shorter");
    }
    private static float[] bounds(ModelPart root,String wanted,int wantedIndex){
        float[] b={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.equals(wanted)||index!=wantedIndex)return;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var p=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                for(int axis=0;axis<3;axis++){b[axis]=Math.min(b[axis],p.get(axis));b[axis+3]=Math.max(b[axis+3],p.get(axis));}
            }
        });return b;
    }
    private static Map<String,List<Vector3f>> points(ModelPart root){
        var result=new TreeMap<String,List<Vector3f>>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            var vertices=new ArrayList<Vector3f>();
            for(var face:cube.polygons)for(var vertex:face.vertices())vertices.add(pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())));
            result.put(path+":"+index,vertices);
        });return result;
    }
    private static void sameMesh(ModelPart current,ModelPart reference,boolean calf){
        var a=points(current);var b=points(reference);
        if(!a.keySet().equals(b.keySet()))throw new AssertionError("Deer cube inventory changed");
        for(var key:a.keySet()){
            if(calf&&(key.startsWith("/body")||key.startsWith("/neck/head")))continue;
            if(a.get(key).size()!=b.get(key).size())throw new AssertionError("Deer polygon inventory changed");
            for(int i=0;i<a.get(key).size();i++)if(a.get(key).get(i).distanceSquared(b.get(key).get(i))>1e-12)throw new AssertionError("Protected deer mesh changed: "+key);
        }
    }
    private DeerJuvenileAudit(){}
}
