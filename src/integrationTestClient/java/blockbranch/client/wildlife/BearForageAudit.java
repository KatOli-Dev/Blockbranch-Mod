package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.BrownBear;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Grass clearance measured from the transformed snout polygons. */
final class BearForageAudit {
    static void verify(){
        var root=BearModel.layer().bakeRoot();var model=new BearModel(root);var state=new BearRenderer.State();int poses=0;
        for(boolean baby:new boolean[]{false,true})for(float yaw:new float[]{-30,0,30})for(int phase=0;phase<128;phase++){
            state.isBaby=baby;state.activity=1;state.yRot=yaw;state.ageInTicks=(float)(phase*Math.PI*2/(128*.25));model.setupAnim(state);
            assertForaging(root);poses++;
        }
        root.getChild("head").y-=2;
        if(muzzleBottom(root)>=23)throw new AssertionError("Foraging audit missed raised snout");
        for(int activity:new int[]{0,2}){
            state.activity=1;model.setupAnim(state);state.activity=activity;model.setupAnim(state);
            var reference=BearModel.layer().bakeRoot();new BearModel(reference).setupAnim(state);
            for(String name:new String[]{"head","body","hump","front_left","front_right","back_left","back_right"})
                if(!root.getChild(name).storePose().equals(reference.getChild(name).storePose()))throw new AssertionError("Foraging transform leaked into activity "+activity);
        }
        int moving=0;
        for(boolean baby:new boolean[]{false,true})for(float speed:new float[]{.25F,.6F,1})for(float yaw:new float[]{-30,0,30})for(int phase=0;phase<16;phase++){
            state.isBaby=baby;state.activity=1;state.walkAnimationSpeed=speed;state.walkAnimationPos=phase;state.ageInTicks=phase*5;state.yRot=yaw;model.setupAnim(state);
            var reference=BearModel.layer().bakeRoot();state.activity=0;new BearModel(reference).setupAnim(state);
            reference.getChild("head").xRot=.45F+.04F*net.minecraft.util.Mth.sin(state.ageInTicks*.25F);
            for(String name:new String[]{"head","body","hump","front_left","front_right","back_left","back_right"})
                if(!root.getChild(name).storePose().equals(reference.getChild(name).storePose()))throw new AssertionError("Walking forage pose changed: "+name);
            moving++;
        }
        Blockbranch.LOGGER.info("BEAR_FORAGE_AUDIT PASS stationaryPoses={} movingPoses={} muzzleNearGrass=true headEarsAboveGrass=true rearHeadInsideTorso=true reset=true raisedSnoutDetected=true approachPoseUnchanged=true",poses,moving);
    }
    private static void assertForaging(ModelPart root){
        float bottom=muzzleBottom(root);
        if(bottom<23||bottom>24)throw new AssertionError("Foraging snout must meet grass without burial: "+bottom);
        float[] torso={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        Vector3f[] joint={null};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(path.equals("/head")&&index==0)joint[0]=pose.pose().transformPosition(new Vector3f((cube.minX+cube.maxX)/32,(cube.minY+cube.maxY)/32,cube.maxZ/16)).mul(16);
            if(!path.startsWith("/head")&&!(path.equals("/body")&&index==0))return;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var p=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                if(path.startsWith("/head")){if(p.y>24.0001F)throw new AssertionError("Foraging head/ears buried: "+path);}
                else for(int axis=0;axis<3;axis++){torso[axis]=Math.min(torso[axis],p.get(axis));torso[axis+3]=Math.max(torso[axis+3],p.get(axis));}
            }
        });
        if(joint[0]==null)throw new AssertionError("Head joint required");
        for(int axis=0;axis<3;axis++)if(joint[0].get(axis)<torso[axis]||joint[0].get(axis)>torso[axis+3])throw new AssertionError("Foraging head detached from torso");
    }
    static float muzzleBottom(ModelPart root){
        float[] bottom={Float.NEGATIVE_INFINITY};int[] count={0};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.equals("/head")||index!=1)return;
            count[0]++;
            for(var face:cube.polygons)for(var vertex:face.vertices())bottom[0]=Math.max(bottom[0],pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y*16);
        });
        if(count[0]!=1||!Float.isFinite(bottom[0]))throw new AssertionError("Finite bear snout required");
        return bottom[0];
    }
    static void observe(Minecraft client,UUID id){
        BrownBear subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof BrownBear bear&&bear.getUUID().equals(id))subject=bear;
        if(subject==null||!subject.isAlive()||subject.isInWater())throw new AssertionError("Live land bear required");
        var renderer=(BearRenderer)client.getEntityRenderDispatcher().getRenderer(subject);var state=renderer.createRenderState();renderer.extractRenderState(subject,state,1);
        var root=BearModel.layer().bakeRoot();new BearModel(root).setupAnim(state);
        if(state.activity==1&&state.walkAnimationSpeed==0)assertForaging(root);
        Blockbranch.LOGGER.info("BEAR_FORAGE_KEYFRAME baby={} activity={} muzzleBottomModelY={} gapModelUnits={}",state.isBaby,state.activity,muzzleBottom(root),24-muzzleBottom(root));
    }
    private BearForageAudit(){}
}
