package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.AmericanBison;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Native-flag pose, horn direction and protected-mesh checks against frozen main. */
final class BisonThreatAudit {
    static void verify(){
        var root=BisonModel.layer().bakeRoot();var model=new BisonModel(root);
        var old=BisonPassiveReference.layer().bakeRoot();var legacy=new BisonPassiveReference(old);
        var state=new BisonRenderer.State();int protectedPoses=0,threatPoses=0;
        for(boolean baby:new boolean[]{false,true})for(boolean water:new boolean[]{false,true})
        for(boolean threatening:new boolean[]{false,true})for(int activity=0;activity<3;activity++)
        for(float yaw:new float[]{-30,0,30})for(float speed:new float[]{0,.5F,1})for(int phase=0;phase<8;phase++){
            state.isBaby=baby;state.isInWater=water;state.threatening=threatening;state.activity=activity;
            state.yRot=yaw;state.xRot=yaw/2;state.walkAnimationSpeed=speed;state.walkAnimationPos=phase;state.ageInTicks=phase*7;
            model.setupAnim(state);legacy.setupAnim(state);boolean allowed=threatening&&!baby&&!water&&activity==0;
            sameMesh(root,old,allowed);
            if(allowed){assertThreat(root,yaw);threatPoses++;}else protectedPoses++;
        }
        // The renderer's shared model must restore a neutral head after threat.
        state.isBaby=false;state.isInWater=false;state.activity=0;state.threatening=true;model.setupAnim(state);
        state.threatening=false;model.setupAnim(state);legacy.setupAnim(state);sameMesh(root,old,false);
        Blockbranch.LOGGER.info("BISON_THREAT_AUDIT PASS protectedPoses={} threatPoses={} onlyHeadMoves=true hornsForwardUp=true headAboveGround=true headAttached=true reset=true",protectedPoses,threatPoses);
    }
    private static void assertThreat(ModelPart root,float yaw){
        float[] shoulder={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        Vector3f[] joint={null};int[] horns={0};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(path.equals("/head")&&index==0)joint[0]=pose.pose().transformPosition(new Vector3f((cube.minX+cube.maxX)/32,(cube.minY+cube.maxY)/32,cube.maxZ/16)).mul(16);
            if(path.startsWith("/head/horn")){
                var base=pose.pose().transformPosition(new Vector3f((cube.minX+cube.maxX)/32,cube.maxY/16,(cube.minZ+cube.maxZ)/32));
                var tip=pose.pose().transformPosition(new Vector3f((cube.minX+cube.maxX)/32,cube.minY/16,(cube.minZ+cube.maxZ)/32));
                var direction=tip.sub(base).mul(16);float forward=-direction.x*Mth.sin(yaw*Mth.DEG_TO_RAD)-direction.z*Mth.cos(yaw*Mth.DEG_TO_RAD);
                if(forward<2||direction.y> -3)throw new AssertionError("Threat horns must aim forward and up");horns[0]++;
            }
            if(!path.startsWith("/head")&&!(path.equals("/shoulder")&&index==0))return;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var point=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                if(path.startsWith("/head")){if(point.y>24.0001F)throw new AssertionError("Threat head/beard buried");}
                else for(int axis=0;axis<3;axis++){shoulder[axis]=Math.min(shoulder[axis],point.get(axis));shoulder[axis+3]=Math.max(shoulder[axis+3],point.get(axis));}
            }
        });
        if(joint[0]==null||horns[0]!=2)throw new AssertionError("Head joint and both horns required");
        for(int axis=0;axis<3;axis++)if(joint[0].get(axis)<shoulder[axis]||joint[0].get(axis)>shoulder[axis+3])throw new AssertionError("Threat head detached from shoulder");
    }
    private static Map<String,List<Vector3f>> points(ModelPart root){
        var result=new TreeMap<String,List<Vector3f>>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            var vertices=new ArrayList<Vector3f>();
            for(var face:cube.polygons)for(var vertex:face.vertices())vertices.add(pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())));
            result.put(path+":"+index,vertices);
        });return result;
    }
    private static void sameMesh(ModelPart current,ModelPart reference,boolean allowHead){
        var a=points(current);var b=points(reference);
        if(!a.keySet().equals(b.keySet()))throw new AssertionError("Approved bison cube inventory changed");
        for(var key:a.keySet()){
            if(allowHead&&key.startsWith("/head"))continue;
            if(a.get(key).size()!=b.get(key).size())throw new AssertionError("Approved bison polygon inventory changed");
            for(int i=0;i<a.get(key).size();i++)if(a.get(key).get(i).distanceSquared(b.get(key).get(i))>1e-12)throw new AssertionError("Protected bison mesh changed: "+key);
        }
    }
    static void observe(Minecraft client,UUID id){
        AmericanBison subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof AmericanBison bison&&bison.getUUID().equals(id))subject=bison;
        if(subject==null||!subject.isAlive()||subject.isInWater())throw new AssertionError("Live land bison required");
        var renderer=(BisonRenderer)client.getEntityRenderDispatcher().getRenderer(subject);var state=renderer.createRenderState();renderer.extractRenderState(subject,state,1);
        if(state.threatening!=subject.isAggressive())throw new AssertionError("Native aggression flag extraction");
        var root=BisonModel.layer().bakeRoot();new BisonModel(root).setupAnim(state);
        boolean applied=state.threatening&&!state.isBaby&&!state.isInWater&&state.activity==0;
        if(applied)assertThreat(root,state.yRot);
        else {var reference=BisonPassiveReference.layer().bakeRoot();new BisonPassiveReference(reference).setupAnim(state);sameMesh(root,reference,false);}
        Blockbranch.LOGGER.info("BISON_THREAT_KEYFRAME baby={} nativeAggressive={} activity={} applied={} headPitch={}",state.isBaby,state.threatening,state.activity,applied,root.getChild("head").xRot);
    }
    private BisonThreatAudit(){}
}
