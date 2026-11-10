package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.RedDeer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Muzzle-to-grass measurement from the actual transformed head polygons. */
final class DeerGrazeAudit {
    static void verify(){
        var root=DeerModel.layer().bakeRoot();var model=new DeerModel(root);var state=new DeerRenderer.State();
        int poses=0;
        for(boolean baby:new boolean[]{false,true})for(boolean stag:new boolean[]{false,true})
        for(float yaw:new float[]{-60,0,60})for(int phase=0;phase<128;phase++){
            state.isBaby=baby;state.stag=stag;state.activity=1;state.yRot=yaw;state.xRot=30;
            state.ageInTicks=(float)(phase*Math.PI*2/(128*.25));model.setupAnim(state);
            assertFeeding(root);poses++;
        }
        // The same mesh measurement must reject a hovering feeding muzzle.
        root.getChild("neck").y-=4.8F;
        if(muzzleBottom(root)>=23)throw new AssertionError("Grazing audit missed deliberately raised muzzle");
        for(int activity:new int[]{0,2}){
            state.activity=1;model.setupAnim(state);state.activity=activity;model.setupAnim(state);
            var reference=DeerModel.layer().bakeRoot();new DeerModel(reference).setupAnim(state);
            for(String name:new String[]{"body","neck","front_left","front_right","back_left","back_right"})
                if(!root.getChild(name).storePose().equals(reference.getChild(name).storePose()))
                    throw new AssertionError("Deer grazing transform leaked into activity "+activity);
            if(!root.getChild("neck").getChild("head").storePose().equals(reference.getChild("neck").getChild("head").storePose()))
                throw new AssertionError("Deer grazing head transform leaked");
        }
        Blockbranch.LOGGER.info("DEER_GRAZE_AUDIT PASS poses={} adultHindCalf=true muzzleNearGrass=true crownAboveGrass=true shoulderAttached=true reset=true raisedMuzzleDetected=true",poses);
    }
    private static void assertFeeding(ModelPart root){
        float bottom=muzzleBottom(root);
        if(bottom<23||bottom>24)throw new AssertionError("Grazing muzzle must meet grass without burial: "+bottom);
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.startsWith("/neck"))return;
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var p=pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).mul(16);
                if(p.y>24.0001F)throw new AssertionError("Deer feeding head/crown penetrates grass: "+path);
                // The shoulder end of the neck must stay enclosed by the unchanged torso.
                if(path.equals("/neck")&&vertex.y()==2&&(p.x< -4||p.x>4||p.y<5||p.y>13||p.z< -7||p.z>8))
                    throw new AssertionError("Grazing neck detached from shoulder");
            }
        });
    }
    static float muzzleBottom(ModelPart root){
        float[] result={Float.NEGATIVE_INFINITY};int[] count={0};
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            if(!path.equals("/neck/head")||index!=1)return;
            count[0]++;
            for(var face:cube.polygons)for(var vertex:face.vertices())result[0]=Math.max(result[0],pose.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y*16);
        });
        if(count[0]!=1||!Float.isFinite(result[0]))throw new AssertionError("Finite deer muzzle mesh required");
        return result[0];
    }
    static void observe(Minecraft client,UUID id)throws Exception{
        RedDeer subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof RedDeer deer&&deer.getUUID().equals(id))subject=deer;
        if(subject==null||!subject.isAlive()||subject.isInWater())throw new AssertionError("Live land deer required");
        var renderer=(DeerRenderer)client.getEntityRenderDispatcher().getRenderer(subject);var state=renderer.createRenderState();renderer.extractRenderState(subject,state,1);
        var root=DeerModel.layer().bakeRoot();new DeerModel(root).setupAnim(state);float bottom=muzzleBottom(root);
        if(state.activity==1)assertFeeding(root);
        String line="baby="+state.isBaby+" activity="+state.activity+" age="+state.ageInTicks+" muzzleBottomModelY="+bottom+" grassModelY=24 gapModelUnits="+(24-bottom);
        Blockbranch.LOGGER.info("DEER_GRAZE_KEYFRAME {}",line);
        Files.writeString(client.gameDirectory.toPath().resolve("deer-graze-keyframes.txt"),line+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    private DeerGrazeAudit(){}
}
