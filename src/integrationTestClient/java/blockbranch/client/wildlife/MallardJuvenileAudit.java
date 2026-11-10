package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Juvenile silhouette and adult-preservation checks against the frozen accepted model. */
final class MallardJuvenileAudit {
    static void verify(){
        var root=MallardModel.layer().bakeRoot();var model=new MallardModel(root);
        var old=MallardAdultReference.layer().bakeRoot();var legacy=new MallardAdultReference(old);
        var state=new MallardRenderer.State();int adults=0;
        for(boolean water:new boolean[]{false,true})for(int activity=0;activity<4;activity++)
        for(float yaw:new float[]{-30,0,30})for(float speed:new float[]{0,.25F,1})for(int phase=0;phase<8;phase++){
            state.isBaby=false;state.isInWater=water;state.activity=activity;state.yRot=yaw;state.xRot=15;
            state.walkAnimationSpeed=speed;state.walkAnimationPos=phase;state.ageInTicks=phase*7;
            model.setupAnim(state);legacy.setupAnim(state);flightReference(old,state);sameMesh(root,old);adults++;
        }
        state=new MallardRenderer.State();state.isBaby=true;model.setupAnim(state);
        var head=bounds(root,"/head",0);var body=bounds(root,"/body",0);
        float headBodyWidth=(head[3]-head[0])/(body[3]-body[0]);
        float bodyLengthHeight=(body[5]-body[2])/(body[4]-body[1]);
        if(headBodyWidth<.85||headBodyWidth>1||bodyLengthHeight<1.3||bodyLengthHeight>1.6)throw new AssertionError("Readable juvenile proportions required");
        if(Math.abs(body[4]-22)>.00001)throw new AssertionError("Juvenile belly detached from feet");
        float neutralHeight=(1.501F-head[1]/16)*.325F;
        if(neutralHeight>.275F)throw new AssertionError("Neutral juvenile exceeds existing collision height");
        if(points(root).size()!=7)throw new AssertionError("Keep seven simple juvenile cuboids");
        int juveniles=0;
        for(boolean water:new boolean[]{false,true})for(int activity=0;activity<4;activity++)
        for(float yaw:new float[]{-30,0,30})for(int phase=0;phase<16;phase++){
            state.isInWater=water;state.activity=activity;state.yRot=yaw;state.xRot=15;state.ageInTicks=phase*5;
            model.setupAnim(state);var first=points(root);model.setupAnim(state);samePoints(first,points(root));juveniles++;
        }
        // A shared renderer must reset juvenile proportions before drawing an adult.
        state.isBaby=false;model.setupAnim(state);legacy.setupAnim(state);flightReference(old,state);sameMesh(root,old);
        MallardPeckAudit.verify();WildlifeModelGroundAudit.verify();WildlifeFaceAudit.verify();
        Blockbranch.LOGGER.info("MALLARD_JUVENILE_AUDIT PASS adultPoses={} juvenilePoses={} headBodyWidthRatio={} bodyLengthHeightRatio={} neutralHeight={} cuboids=7 feetAttached=true adultAfterBaby=true",adults,juveniles,headBodyWidth,bodyLengthHeight,neutralHeight);
    }
    // The juvenile-only frozen reference predates the adult flight posture.
    // The posture audit independently protects non-flight and all juvenile states.
    private static void flightReference(ModelPart root,MallardRenderer.State state){
        if(!state.isBaby&&state.activity==3){
            var head=root.getChild("head");head.xRot+=.25F;head.z-=2.5F;
            for(String name:new String[]{"left_wing","right_wing"}){var wing=root.getChild(name);wing.yScale=1.5F;wing.xRot=.2F;wing.yRot=name.equals("left_wing")?.25F:-.25F;}
            for(String name:new String[]{"left_leg","right_leg"}){var leg=root.getChild(name);leg.y=21;leg.z=6;leg.yScale=.6F;leg.xRot=0;}
        }
    }
    private static float[] bounds(ModelPart root,String part,int cubeIndex){
        float[] b={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.equals(part)||index!=cubeIndex)return;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var p=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                for(int axis=0;axis<3;axis++){b[axis]=Math.min(b[axis],p.get(axis));b[axis+3]=Math.max(b[axis+3],p.get(axis));}
            }
        });
        for(float value:b)if(!Float.isFinite(value))throw new AssertionError("Finite juvenile bounds required");
        return b;
    }
    private static Map<String,List<Vector3f>> points(ModelPart root){
        var result=new TreeMap<String,List<Vector3f>>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            var vertices=new ArrayList<Vector3f>();
            for(var face:cube.polygons)for(var vertex:face.vertices())vertices.add(pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())));
            result.put(path+":"+index,vertices);
        });return result;
    }
    private static void samePoints(Map<String,List<Vector3f>> current,Map<String,List<Vector3f>> reference){
        if(!current.keySet().equals(reference.keySet()))throw new AssertionError("Mallard cube inventory changed");
        for(var key:current.keySet()){
            var a=current.get(key);var b=reference.get(key);
            if(a.size()!=b.size())throw new AssertionError("Mallard polygon inventory changed");
            for(int i=0;i<a.size();i++)if(a.get(i).distanceSquared(b.get(i))>1e-12)throw new AssertionError("Mallard protected mesh changed: "+key);
        }
    }
    private static void sameMesh(ModelPart current,ModelPart reference){
        samePoints(points(current),points(reference));
        for(String name:new String[]{"body","head","left_wing","right_wing","left_leg","right_leg"})
            if(current.getChild(name).visible!=reference.getChild(name).visible)throw new AssertionError("Adult part visibility changed");
    }
    private MallardJuvenileAudit(){}
}
